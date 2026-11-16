package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** Shared plumbing for the menu-style screens: viewport, taps, text and boxes. */
abstract class BaseScreen extends ScreenAdapter implements OhHeckGame.Tagged {

	static final float WORLD_W = 800, WORLD_H = 480;
	static final Color PANEL = new Color(0f, 0f, 0f, 0.72f);
	static final Color BUTTON = new Color(0.16f, 0.42f, 0.24f, 1f);
	static final Color GOLD = new Color(1f, 0.82f, 0.25f, 1f);

	protected final OhHeckGame game;
	protected final Assets assets;
	protected final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	protected final SpriteBatch batch = new SpriteBatch();
	protected final GlyphLayout glyphs = new GlyphLayout();
	protected float w = WORLD_W, h = WORLD_H;
	/** Time since the screen was shown, at game speed. */
	protected float shownFor;

	private final Vector2 tmp = new Vector2();
	private float downX, downY, lastY;

	BaseScreen(OhHeckGame game) {
		this.game = game;
		this.assets = game.assets;
	}

	/** A tap at world position (x, y). */
	protected abstract void tap(float x, float y);

	/** Back button or Escape. */
	protected abstract void back();

	/** A vertical drag by dy world units (positive = finger moved up). */
	protected void drag(float dy) {
	}

	@Override
	public void show() {
		shownFor = 0;
		Gdx.input.setCatchKey(Input.Keys.BACK, true);
		Gdx.input.setInputProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int x, int y, int pointer, int button) {
				toWorld(x, y);
				downX = tmp.x;
				downY = lastY = tmp.y;
				return true;
			}

			@Override
			public boolean touchDragged(int x, int y, int pointer) {
				toWorld(x, y);
				drag(tmp.y - lastY);
				lastY = tmp.y;
				return true;
			}

			@Override
			public boolean touchUp(int x, int y, int pointer, int button) {
				toWorld(x, y);
				if (Vector2.dst(tmp.x, tmp.y, downX, downY) < 20) {
					tap(tmp.x, tmp.y);
				}
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
		w = viewport.getWorldWidth();
		h = viewport.getWorldHeight();
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();
		batch.setColor(Color.WHITE);
	}

	protected BitmapFont ui(float size) {
		assets.font.getData().setScale(size * Assets.UI_BASE);
		return assets.font;
	}

	protected BitmapFont serif(float size) {
		assets.serif.getData().setScale(size * Assets.SERIF_BASE);
		return assets.serif;
	}

	protected void centred(BitmapFont f, String s, float x, float y) {
		glyphs.setText(f, s);
		f.draw(batch, s, x - glyphs.width / 2, y);
	}

	protected void rect(float x, float y, float rw, float rh, Color c) {
		batch.setColor(c);
		batch.draw(assets.white, x, y, rw, rh);
		batch.setColor(Color.WHITE);
	}

	/** A labelled button; returns its tap area. */
	protected Rectangle button(String label, float cx, float y, float bw, float bh, Color fill) {
		rect(cx - bw / 2, y, bw, bh, fill);
		BitmapFont f = serif(1.5f);
		f.setColor(Color.WHITE);
		glyphs.setText(f, label);
		f.draw(batch, label, cx - glyphs.width / 2, y + bh / 2 + glyphs.height / 2);
		return new Rectangle(cx - bw / 2, y, bw, bh);
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void dispose() {
		batch.dispose();
	}
}
