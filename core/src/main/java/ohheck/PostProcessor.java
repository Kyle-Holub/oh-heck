package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * A finishing pass over the whole screen: a soft bloom on bright highlights,
 * warm colour grading, a dark vignette and fine film grain.
 * <p>
 * The frame is drawn into an off-screen buffer; its bright parts are copied
 * to a quarter-size buffer and blurred, then everything is combined onto the
 * screen. If the device can't compile the shaders, frames are drawn directly.
 */
final class PostProcessor implements Disposable {

	private static final String VERTEX = ""
			+ "attribute vec4 a_position;\n"
			+ "attribute vec4 a_color;\n"
			+ "attribute vec2 a_texCoord0;\n"
			+ "uniform mat4 u_projTrans;\n"
			+ "varying vec2 v_uv;\n"
			+ "void main() {\n"
			+ "  v_uv = a_texCoord0;\n"
			+ "  gl_Position = u_projTrans * a_position;\n"
			+ "}\n";

	private static final String HEADER = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
			+ "varying vec2 v_uv;\nuniform sampler2D u_texture;\n";

	/**
	 * Keeps only the brightest parts: sparkles, glints and gold highlights, not
	 * the cream card faces (whose brightest channel stays below the threshold).
	 */
	private static final String BRIGHT = HEADER
			+ "void main() {\n"
			+ "  vec3 c = texture2D(u_texture, v_uv).rgb;\n"
			+ "  float m = max(c.r, max(c.g, c.b));\n"
			+ "  gl_FragColor = vec4(c * smoothstep(0.96, 1.0, m), 1.0);\n"
			+ "}\n";

	/** One direction of a 9-tap Gaussian blur. */
	private static final String BLUR = HEADER
			+ "uniform vec2 u_step;\n"
			+ "void main() {\n"
			+ "  vec3 c = texture2D(u_texture, v_uv).rgb * 0.227;\n"
			+ "  c += (texture2D(u_texture, v_uv + u_step * 1.385).rgb + texture2D(u_texture, v_uv - u_step * 1.385).rgb) * 0.316;\n"
			+ "  c += (texture2D(u_texture, v_uv + u_step * 3.231).rgb + texture2D(u_texture, v_uv - u_step * 3.231).rgb) * 0.070;\n"
			+ "  gl_FragColor = vec4(c, 1.0);\n"
			+ "}\n";

	/** Scene + bloom, warm grade, vignette and grain. */
	private static final String COMBINE = HEADER
			+ "uniform sampler2D u_bloom;\n"
			+ "uniform float u_time;\n"
			+ "uniform vec2 u_size;\n"
			+ "void main() {\n"
			+ "  vec3 c = texture2D(u_texture, v_uv).rgb;\n"
			+ "  c += texture2D(u_bloom, v_uv).rgb * 0.55;\n"
			// warm grade: lift reds, ease blues, a touch more contrast in the mids
			+ "  c *= vec3(1.04, 1.0, 0.92);\n"
			+ "  c = mix(c, c * c * (3.0 - 2.0 * c), 0.25);\n"
			// vignette, slightly oval to suit a wide screen
			+ "  vec2 p = (v_uv - 0.5) * vec2(1.0, 0.8);\n"
			+ "  c *= mix(0.5, 1.0, smoothstep(0.75, 0.2, length(p)));\n"
			// fine moving grain
			+ "  float n = fract(sin(dot(floor(v_uv * u_size) + u_time * 7.0, vec2(12.9898, 78.233))) * 43758.5453);\n"
			+ "  c += (n - 0.5) * 0.03;\n"
			+ "  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);\n"
			+ "}\n";

	private final SpriteBatch batch = new SpriteBatch(4);
	private ShaderProgram bright, blur, combine;
	private FrameBuffer scene, bloomA, bloomB;
	private int width, height;
	private float time;
	private boolean ok;

	PostProcessor() {
		ShaderProgram.pedantic = false;
		bright = new ShaderProgram(VERTEX, BRIGHT);
		blur = new ShaderProgram(VERTEX, BLUR);
		combine = new ShaderProgram(VERTEX, COMBINE);
		ok = bright.isCompiled() && blur.isCompiled() && combine.isCompiled();
		if (!ok) {
			Gdx.app.error("PostProcessor", "Effects disabled: " + bright.getLog() + blur.getLog() + combine.getLog());
		}
		batch.getProjectionMatrix().setToOrtho2D(0, 0, 1, 1);
	}

	boolean isAvailable() {
		return ok;
	}

	/** Starts capturing a frame. Returns false if effects can't run, in which case draw normally. */
	boolean begin() {
		if (!ok) {
			return false;
		}
		int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
		if (w <= 0 || h <= 0) {
			return false;
		}
		if (scene == null || w != width || h != height) {
			try {
				allocate(w, h);
			} catch (RuntimeException e) {
				Gdx.app.error("PostProcessor", "Effects disabled: no frame buffers", e);
				ok = false;
				return false;
			}
		}
		scene.begin();
		return true;
	}

	/** Finishes the frame: applies the effects and draws it to the screen. */
	void end(float delta) {
		scene.end();
		time = (time + delta) % 1000f;

		pass(bloomA, bright, scene.getColorBufferTexture());
		for (int i = 0; i < 2; i++) {
			blurPass(bloomB, bloomA.getColorBufferTexture(), 1f / bloomA.getWidth(), 0);
			blurPass(bloomA, bloomB.getColorBufferTexture(), 0, 1f / bloomA.getHeight());
		}

		Gdx.gl.glViewport(0, 0, width, height);
		batch.setShader(combine);
		batch.begin();
		bloomA.getColorBufferTexture().bind(1);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		combine.setUniformi("u_bloom", 1);
		combine.setUniformf("u_time", time);
		combine.setUniformf("u_size", width, height);
		draw(scene.getColorBufferTexture());
		batch.end();
	}

	private void pass(FrameBuffer target, ShaderProgram shader, Texture source) {
		target.begin();
		batch.setShader(shader);
		batch.begin();
		draw(source);
		batch.end();
		target.end();
	}

	private void blurPass(FrameBuffer target, Texture source, float dx, float dy) {
		target.begin();
		batch.setShader(blur);
		batch.begin();
		blur.setUniformf("u_step", dx, dy);
		draw(source);
		batch.end();
		target.end();
	}

	/** Draws a frame-buffer texture over the whole target (they are stored upside down). */
	private void draw(Texture t) {
		TextureRegion r = new TextureRegion(t);
		r.flip(false, true);
		batch.draw(r, 0, 0, 1, 1);
	}

	private void allocate(int w, int h) {
		disposeBuffers();
		width = w;
		height = h;
		scene = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
		int bw = Math.max(1, w / 4), bh = Math.max(1, h / 4);
		bloomA = new FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false);
		bloomB = new FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false);
		for (FrameBuffer f : new FrameBuffer[] { scene, bloomA, bloomB }) {
			f.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}
	}

	private void disposeBuffers() {
		for (FrameBuffer f : new FrameBuffer[] { scene, bloomA, bloomB }) {
			if (f != null) {
				f.dispose();
			}
		}
		scene = bloomA = bloomB = null;
	}

	@Override
	public void dispose() {
		disposeBuffers();
		bright.dispose();
		blur.dispose();
		combine.dispose();
		batch.dispose();
	}
}
