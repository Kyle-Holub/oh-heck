package ohheck.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** Shared plumbing for the menu-style screens: viewport, taps, text and boxes. */
public abstract class BaseScreen extends ScreenAdapter implements GameHost.Tagged {

	public static final float WORLD_W = 800, WORLD_H = 480;
	protected static final Color GOLD = Palette.GOLD;

	// the bar across the top of the settings-style screens (Options, Roster)
	protected static final float HEADER_H = 56;
	protected static final Color HEADER = new Color(0.06f, 0.04f, 0.03f, 0.97f);
	/** The Back button: centre x, size. */
	static final float BACK_X = 70, BACK_W = 110, BACK_H = 40;

	protected final GameHost game;
	protected final Assets assets;
	protected final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	protected final SpriteBatch batch;
	/** The drawing tools (shared with the helpers that draw for this screen). */
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

	@Override
	public void show() {
		shownFor = 0;
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

	// drawing: the Painter's tools, for this screen's own code

	protected BitmapFont ui(float size) {
		return paint.ui(size);
	}

	protected BitmapFont serif(float size) {
		return paint.serif(size);
	}

	protected void centred(BitmapFont f, String s, float x, float y) {
		paint.centred(f, s, x, y);
	}

	protected void textIn(BitmapFont f, String s, float x, float y, float bw, float bh) {
		paint.textIn(f, s, x, y, bw, bh);
	}

	protected void text(BitmapFont f, String s, float x, float y, boolean centre) {
		paint.text(f, s, x, y, centre);
	}

	protected void numeral(int n, float cx, float cy, float h, Color c) {
		paint.numeral(n, cx, cy, h, c);
	}

	protected void rect(float x, float y, float rw, float rh, Color c) {
		paint.rect(x, y, rw, rh, c);
	}

	protected void frame(NinePatch p, float x, float y, float fw, float fh) {
		paint.frame(p, x, y, fw, fh);
	}

	protected Rectangle button(Rectangle area, String label, float cx, float y, float bw, float bh, NinePatch style) {
		return paint.button(area, label, cx, y, bw, bh, style);
	}

	/** The bar across the top: a gold rule under it, the title in the middle, Back on the left. */
	protected void header(String title, Rectangle back) {
		rect(0, h - HEADER_H, w, HEADER_H, HEADER);
		rect(0, h - HEADER_H - 2, w, 2, GOLD);
		BitmapFont t = serif(1.6f);
		t.setColor(GOLD);
		centred(t, title, w / 2, h - 16);
		backButton(back, h - HEADER_H + (HEADER_H - BACK_H) / 2);
	}

	/** The Back button, its bottom at y. */
	protected Rectangle backButton(Rectangle area, float y) {
		return button(area, "Back", BACK_X, y, BACK_W, BACK_H, assets.buttonAlt);
	}

	/** A section's name, in gold, above its rows (x is the rows' left edge). */
	protected void heading(String name, float x, float y) {
		BitmapFont f = serif(1.35f);
		f.setColor(GOLD);
		f.draw(batch, name, x + 6, y);
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
