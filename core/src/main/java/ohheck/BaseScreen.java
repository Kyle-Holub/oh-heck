package ohheck;

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
abstract class BaseScreen extends ScreenAdapter implements OhHeckGame.Tagged {

	static final float WORLD_W = 800, WORLD_H = 480;
	static final Color GOLD = Palette.GOLD;

	protected final OhHeckGame game;
	protected final Assets assets;
	protected final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	protected final SpriteBatch batch;
	protected final GlyphLayout glyphs = new GlyphLayout();
	protected float w = WORLD_W, h = WORLD_H;
	/** Time since the screen was shown, at game speed. */
	protected float shownFor;

	private final Vector2 tmp = new Vector2();
	private float downX, downY, lastY;

	BaseScreen(OhHeckGame game) {
		this.game = game;
		this.assets = game.assets;
		this.batch = game.batch; // shared: not disposed by the screen
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
		Gdx.input.setInputProcessor(new InputAdapter() {
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
		});
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

	/*
	 * The two fonts are shared by every screen, so their size and colour are
	 * whatever the last caller set. Always get a font through ui() or serif(),
	 * which set the size and reset the colour, then set the colour you want:
	 * never rely on a size or colour left over from earlier drawing.
	 */

	/** The sans-serif font at a size factor (1 = about 16 world units), coloured white. */
	protected BitmapFont ui(float size) {
		assets.font.getData().setScale(size * Assets.UI_BASE);
		assets.font.setColor(Color.WHITE);
		return assets.font;
	}

	/** The serif font at a size factor, coloured white. */
	protected BitmapFont serif(float size) {
		assets.serif.getData().setScale(size * Assets.SERIF_BASE);
		assets.serif.setColor(Color.WHITE);
		return assets.serif;
	}

	protected void centred(BitmapFont f, String s, float x, float y) {
		glyphs.setText(f, s);
		f.draw(batch, s, x - glyphs.width / 2, y);
	}

	/** Draws text centred in the box (x, y, bw, bh), horizontally and vertically. */
	protected void textIn(BitmapFont f, String s, float x, float y, float bw, float bh) {
		glyphs.setText(f, s);
		f.draw(batch, s, x + (bw - glyphs.width) / 2, y + (bh + glyphs.height) / 2);
	}

	/** Draws text centred on x, or right-aligned to x when centre is false. */
	protected void text(BitmapFont f, String s, float x, float y, boolean centre) {
		glyphs.setText(f, s);
		f.draw(batch, s, centre ? x - glyphs.width / 2 : x - glyphs.width, y);
	}

	protected void rect(float x, float y, float rw, float rh, Color c) {
		batch.setColor(c);
		batch.draw(assets.white, x, y, rw, rh);
		batch.setColor(Color.WHITE);
	}

	/** Draws a rounded frame (panel or button), at the current {@link #fade}. */
	protected void frame(NinePatch p, float x, float y, float fw, float fh) {
		batch.setColor(1f, 1f, 1f, fade);
		p.draw(batch, x, y, fw, fh);
		batch.setColor(Color.WHITE);
	}

	/** Opacity for frames and button labels, for fading UI in; normally 1. */
	protected float fade = 1f;

	/** A labelled button; sets {@code area} to its tap area and returns it. */
	protected Rectangle button(Rectangle area, String label, float cx, float y, float bw, float bh, NinePatch style) {
		frame(style, cx - bw / 2, y, bw, bh);
		BitmapFont f = serif(1.5f);
		Color c = style == assets.buttonOff ? Palette.MUTED : Palette.CREAM;
		f.setColor(c.r, c.g, c.b, fade);
		glyphs.setText(f, label);
		f.draw(batch, label, cx - glyphs.width / 2, y + bh / 2 + glyphs.height / 2);
		return area.set(cx - bw / 2, y, bw, bh);
	}

	/** Grows or shrinks a list of reusable tap areas to n, keeping the existing objects. */
	static void sizeTo(java.util.List<Rectangle> list, int n) {
		while (list.size() > n) {
			list.remove(list.size() - 1);
		}
		while (list.size() < n) {
			list.add(new Rectangle());
		}
	}

	/**
	 * Is something moving that needs full frame rate? When false the game
	 * draws at a lower rate to save battery (see OhHeckGame.render).
	 */
	boolean animating() {
		return false;
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}


}
