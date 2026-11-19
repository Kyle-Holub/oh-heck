package ohheck.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import ohheck.anim.FixedSteps;

/** The base of every screen: fixed-step updates, the viewport and touches. */
public abstract class BaseScreen extends ScreenAdapter implements Tagged {

	public static final float WORLD_W = 800, WORLD_H = 480;

	protected final GameHost game;
	protected final Assets assets;
	protected final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	protected final SpriteBatch batch;
	protected final Painter paint;
	protected final GlyphLayout glyphs;
	protected float w = WORLD_W, h = WORLD_H;
	protected float shownFor;

	private final Vector2 tmp = new Vector2();
	private float downX, downY, lastY;

	public BaseScreen(GameHost game) {
		this.game = game;
		this.assets = game.assets();
		this.batch = game.batch();
		this.paint = new Painter(batch, assets);
		this.glyphs = paint.glyphs;
	}

	protected abstract void tap(float x, float y);

	protected abstract void back();

	protected void drag(float dy) {
	}

	static final float TAP_SLOP = 24f;

	protected void touchDown(float x, float y) {
		downX = x;
		downY = lastY = y;
	}

	protected void touchDragged(float x, float y) {
		drag(y - lastY);
		lastY = y;
	}

	protected void touchUp(float x, float y) {
		if (isTap(x, y)) {
			tap(x, y);
		}
	}

	protected boolean isTap(float x, float y) {
		return Vector2.dst(x, y, downX, downY) <= TAP_SLOP;
	}

	protected float movedFrom(float x, float y) {
		return Vector2.dst(x, y, downX, downY);
	}

	public static final float STEP = 1 / 60f;
	private static final float MAX_FRAME = 0.1f;
	private final FixedSteps steps = new FixedSteps(STEP, MAX_FRAME);
	private float frameDelta;
	private boolean hidden;

	@Override
	public final void render(float delta) {
		frameDelta = Math.min(delta, MAX_FRAME);
		for (int n = steps.frame(delta); n > 0; n--) {
			update(STEP);
			if (!hidden && game.tour() != null) {
				game.tour().step(this, tourClock());
			}
			if (hidden) {
				return;
			}
		}
		draw();
	}

	protected abstract void update(float step);

	protected abstract void draw();

	protected float tourClock() {
		return shownFor;
	}

	protected float frameDelta() {
		return frameDelta;
	}

	@Override
	public void show() {
		shownFor = 0;
		hidden = false;
		steps.reset();
		Gdx.input.setCatchKey(Input.Keys.BACK, true);
		Gdx.input.setInputProcessor(touches);
	}

	private final Touches touches = new Touches();

	private final class Touches extends InputAdapter {
		@Override
		public boolean touchDown(int x, int y, int pointer, int button) {
			if (pointer > 0) {
				return false;
			}
			toWorld(x, y);
			BaseScreen.this.touchDown(tmp.x, tmp.y);
			return true;
		}

		@Override
		public boolean touchDragged(int x, int y, int pointer) {
			if (pointer > 0) {
				return false;
			}
			toWorld(x, y);
			BaseScreen.this.touchDragged(tmp.x, tmp.y);
			return true;
		}

		@Override
		public boolean touchUp(int x, int y, int pointer, int button) {
			if (pointer > 0) {
				return false;
			}
			toWorld(x, y);
			BaseScreen.this.touchUp(tmp.x, tmp.y);
			return true;
		}

		@Override
		public boolean keyDown(int keycode) {
			if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
				back();
				return true;
			}
			return false;
		}
	}

	@Override
	public void hide() {
		hidden = true;
		Gdx.input.setInputProcessor(null);
	}

	private void toWorld(int x, int y) {
		tmp.set(x, y);
		viewport.unproject(tmp);
	}

	protected void begin() {
		viewport.apply();
		game.fitViewportToTarget();
		w = viewport.getWorldWidth();
		h = viewport.getWorldHeight();
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();
		batch.setColor(Color.WHITE);
	}

	public boolean animating() {
		return false;
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}
}
