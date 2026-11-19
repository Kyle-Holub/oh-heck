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

/** What every screen shares: the game loop in fixed steps, the viewport, touches, and the drawing tools. */
public abstract class BaseScreen extends ScreenAdapter implements GameHost.Tagged {

	public static final float WORLD_W = 800, WORLD_H = 480;

	protected final GameHost game;
	protected final Assets assets;
	protected final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	protected final SpriteBatch batch;
	/** The drawing tools: text, frames, buttons (shared with the helpers that draw for this screen). */
	protected final Painter paint;
	protected final GlyphLayout glyphs;
	protected float w = WORLD_W, h = WORLD_H;
	/** Time since the screen was shown, at game speed. */
	protected float shownFor;

	private final Vector2 tmp = new Vector2();
	private float downX, downY, lastY;

	public BaseScreen(GameHost game) {
		this.game = game;
		this.assets = game.assets();
		this.batch = game.batch(); // shared: not disposed by the screen
		this.paint = new Painter(batch, assets);
		this.glyphs = paint.glyphs;
	}

	/** A tap at world position (x, y). */
	protected abstract void tap(float x, float y);

	/** Back button or Escape. */
	protected abstract void back();

	/** A vertical drag by dy world units (positive = finger moved up). */
	protected void drag(float dy) {
	}

	/** How far a finger may move between down and up for it to still count as a tap. */
	static final float TAP_SLOP = 24f;

	/** Finger down at world (x, y). The default starts tracking a tap or drag. */
	protected void touchDown(float x, float y) {
		downX = x;
		downY = lastY = y;
	}

	/** Finger moved to world (x, y). The default reports vertical drags (scrolling). */
	protected void touchDragged(float x, float y) {
		drag(y - lastY);
		lastY = y;
	}

	/** Finger up at world (x, y). The default reports a tap if it didn't move far. */
	protected void touchUp(float x, float y) {
		if (isTap(x, y)) {
			tap(x, y);
		}
	}

	/** Did the finger stay close to where it went down? */
	protected boolean isTap(float x, float y) {
		return Vector2.dst(x, y, downX, downY) <= TAP_SLOP;
	}

	/** Distance from where the finger went down. */
	protected float movedFrom(float x, float y) {
		return Vector2.dst(x, y, downX, downY);
	}

	// ---- the game loop: fixed steps, then one picture

	/** The game moves on in steps this long (seconds), whatever the frame rate, on every device and in CI. */
	public static final float STEP = 1 / 60f;
	/** The most one frame may move the game on: after a stall it catches up only this far. */
	private static final float MAX_FRAME = 0.1f;
	private final FixedSteps steps = new FixedSteps(STEP, MAX_FRAME);
	/** This frame's real length. */
	private float frameDelta;
	/** Set once another screen has taken over (the steps stop at once). */
	private boolean hidden;

	/** Moves the screen on by whole steps for the time that has passed, then draws it once. */
	@Override
	public final void render(float delta) {
		frameDelta = Math.min(delta, MAX_FRAME);
		for (int n = steps.frame(delta); n > 0; n--) {
			update(STEP);
			if (hidden) {
				return; // gone to another screen
			}
		}
		draw();
	}

	/** One step of the game: step seconds of real time (screens scale it by the game speed). */
	protected abstract void update(float step);

	/** The screen as it is now. */
	protected abstract void draw();

	/** This frame's real length (for things tied to frames rather than steps, like a finger's fling). */
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

	/** Turns the device's touches and keys into the screen's, in world coordinates. */
	private final class Touches extends InputAdapter {
		// first finger only: a second finger on the screen is ignored
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

	/** Prepares the batch for a frame; call batch.end() when done. */
	protected void begin() {
		viewport.apply();
		game.fitViewportToTarget(); // the effects pass may draw the scene at a lower resolution
		w = viewport.getWorldWidth();
		h = viewport.getWorldHeight();
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();
		batch.setColor(Color.WHITE);
	}

	/**
	 * Is something moving that needs full frame rate? When false the game
	 * draws at a lower rate to save battery (see OhHeckGame.render).
	 */
	public boolean animating() {
		return false;
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}


}
