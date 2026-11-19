package ohheck.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

import ohheck.util.Log;

/**
 * A full-screen pass: bloom, colour grading, vignette and grain, drawn from a reduced-size
 * buffer.
 */
public final class PostProcessor implements Disposable {

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

	private static final String BRIGHT = HEADER
			+ "void main() {\n"
			+ "  vec3 c = texture2D(u_texture, v_uv).rgb;\n"
			+ "  float m = max(c.r, max(c.g, c.b));\n"
			+ "  gl_FragColor = vec4(c * smoothstep(0.96, 1.0, m), 1.0);\n"
			+ "}\n";

	private static final String BLUR = HEADER
			+ "uniform vec2 u_step;\n"
			+ "void main() {\n"
			+ "  vec3 c = texture2D(u_texture, v_uv).rgb * 0.227;\n"
			+ "  c += (texture2D(u_texture, v_uv + u_step * 1.385).rgb + texture2D(u_texture, v_uv - u_step * 1.385).rgb) * 0.316;\n"
			+ "  c += (texture2D(u_texture, v_uv + u_step * 3.231).rgb + texture2D(u_texture, v_uv - u_step * 3.231).rgb) * 0.070;\n"
			+ "  gl_FragColor = vec4(c, 1.0);\n"
			+ "}\n";

	private static final String COMBINE = HEADER
			+ "uniform sampler2D u_bloom;\n"
			+ "uniform sampler2D u_noise;\n"
			+ "uniform vec2 u_noiseScale;\n"
			+ "uniform vec2 u_noiseOffset;\n"
			+ "uniform vec2 u_screen;\n"
			+ "uniform vec4 u_light;\n"
			+ "void main() {\n"
			+ "  vec3 c = texture2D(u_texture, v_uv).rgb;\n"
			+ "  c += texture2D(u_bloom, v_uv).rgb * 0.55;\n"
			+ "  c *= vec3(1.04, 1.0, 0.92);\n"
			+ "  c = mix(c, c * c * (3.0 - 2.0 * c), 0.25);\n"
			+ "  vec2 p = (v_uv - 0.5) * vec2(1.0, 0.8);\n"
			+ "  float vig = mix(0.5, 1.0, smoothstep(0.75, 0.2, length(p)));\n"
			+ "  vec2 q = gl_FragCoord.xy / u_screen.y;\n"
			+ "  vec2 l = vec2(u_light.x * u_screen.x / u_screen.y, u_light.y);\n"
			+ "  float lit = u_light.z > 0.0 ? smoothstep(u_light.z, 0.0, distance(q, l)) * u_light.w : 0.0;\n"
			+ "  c *= mix(vig, 1.08, lit);\n"
			+ "  float n = texture2D(u_noise, v_uv * u_noiseScale + u_noiseOffset).r;\n"
			+ "  c += (n - 0.5) * 0.03;\n"
			+ "  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);\n"
			+ "}\n";

	private static final int MAX_SCENE_H = 900;
	private static final int NOISE_SIZE = 128;

	private final SpriteBatch batch = new SpriteBatch(4);
	private final TextureRegion region = new TextureRegion();
	private final Texture noise;
	private ShaderProgram bright, blur, combine;
	private FrameBuffer scene, bloomA, bloomB;
	private int width, height, sceneW, sceneH;
	private boolean ok;

	public PostProcessor() {
		ShaderProgram.pedantic = false;
		bright = new ShaderProgram(VERTEX, BRIGHT);
		blur = new ShaderProgram(VERTEX, BLUR);
		combine = new ShaderProgram(VERTEX, COMBINE);
		ok = bright.isCompiled() && blur.isCompiled() && combine.isCompiled();
		if (!ok) {
			Log.warn("PostProcessor", "Effects disabled: " + bright.getLog() + blur.getLog() + combine.getLog(), null);
		}
		batch.getProjectionMatrix().setToOrtho2D(0, 0, 1, 1);
		Pixmap p = new Pixmap(NOISE_SIZE, NOISE_SIZE, Pixmap.Format.RGBA8888);
		java.util.Random r = new java.util.Random(7);
		for (int y = 0; y < NOISE_SIZE; y++) {
			for (int x = 0; x < NOISE_SIZE; x++) {
				float v = r.nextFloat();
				p.drawPixel(x, y, com.badlogic.gdx.graphics.Color.rgba8888(v, v, v, 1f));
			}
		}
		noise = new Texture(p);
		noise.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		noise.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
		p.dispose();
	}

	private float lightX, lightY, lightR, lightA;

	public void light(float x, float y, float radius, float strength) {
		lightX = x;
		lightY = y;
		lightR = radius;
		lightA = strength;
	}

	public int sceneWidth() {
		return sceneW;
	}

	public int sceneHeight() {
		return sceneH;
	}

	public boolean begin() {
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
				Log.warn("PostProcessor", "Effects disabled: no frame buffers", e);
				ok = false;
				return false;
			}
		}
		scene.begin();
		return true;
	}

	public void end(float delta) {
		scene.end();

		pass(bloomA, bright, scene.getColorBufferTexture());
		for (int i = 0; i < 2; i++) {
			blurPass(bloomB, bloomA.getColorBufferTexture(), 1f / bloomA.getWidth(), 0);
			blurPass(bloomA, bloomB.getColorBufferTexture(), 0, 1f / bloomA.getHeight());
		}

		Gdx.gl.glViewport(0, 0, width, height);
		batch.setShader(combine);
		batch.begin();
		bloomA.getColorBufferTexture().bind(1);
		noise.bind(2);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		combine.setUniformi("u_bloom", 1);
		combine.setUniformi("u_noise", 2);
		combine.setUniformf("u_screen", width, height);
		combine.setUniformf("u_light", lightX, lightY, lightR, lightA);
		lightR = 0;
		combine.setUniformf("u_noiseScale", width / (float) NOISE_SIZE, height / (float) NOISE_SIZE);
		combine.setUniformf("u_noiseOffset", MathUtils.random(), MathUtils.random());
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

	private void draw(Texture t) {
		region.setRegion(t);
		region.flip(false, true);
		batch.draw(region, 0, 0, 1, 1);
	}

	private void allocate(int w, int h) {
		disposeBuffers();
		width = w;
		height = h;
		float scale = Math.min(1f, MAX_SCENE_H / (float) h);
		sceneW = Math.max(1, Math.round(w * scale));
		sceneH = Math.max(1, Math.round(h * scale));
		scene = new FrameBuffer(Pixmap.Format.RGBA8888, sceneW, sceneH, false);
		int bw = Math.max(1, sceneW / 4), bh = Math.max(1, sceneH / 4);
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
		noise.dispose();
	}
}
