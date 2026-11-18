package ohheck.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * The drawing tools every screen shares: the two fonts, text placed and aligned, the bid
 * numerals, plain rectangles, rounded frames and buttons. A screen has one (BaseScreen's
 * shortcuts call it); helpers that draw for a screen (the table's overlays, placards and
 * counters) take a Painter rather than the screen.
 */
public final class Painter {

	public final SpriteBatch batch;
	public final Assets assets;
	/** For measuring text; shared, so measure just before you use it. */
	public final GlyphLayout glyphs = new GlyphLayout();
	private float fade = 1f;

	public Painter(SpriteBatch batch, Assets assets) {
		this.batch = batch;
		this.assets = assets;
	}

	/** Opacity for frames and button labels, for fading UI in; normally 1. */
	public float fade() {
		return fade;
	}

	public void fade(float alpha) {
		fade = alpha;
	}

	/*
	 * The two fonts are shared by every screen, so their size and colour are
	 * whatever the last caller set. Always get a font through ui() or serif(),
	 * which set the size and reset the colour, then set the colour you want:
	 * never rely on a size or colour left over from earlier drawing.
	 */

	/** The sans-serif font at a size factor (1 = about 16 world units), coloured white. */
	public BitmapFont ui(float size) {
		assets.font.getData().setScale(size * Assets.UI_BASE);
		assets.font.setColor(Color.WHITE);
		return assets.font;
	}

	/** The serif font at a size factor, coloured white. */
	public BitmapFont serif(float size) {
		assets.serif.getData().setScale(size * Assets.SERIF_BASE);
		assets.serif.setColor(Color.WHITE);
		return assets.serif;
	}

	/** Draws text centred on x. */
	public void centred(BitmapFont f, String s, float x, float y) {
		glyphs.setText(f, s);
		f.draw(batch, s, x - glyphs.width / 2, y);
	}

	/** Draws text centred in the box (x, y, bw, bh), horizontally and vertically. */
	public void textIn(BitmapFont f, String s, float x, float y, float bw, float bh) {
		glyphs.setText(f, s);
		f.draw(batch, s, x + (bw - glyphs.width) / 2, y + (bh + glyphs.height) / 2);
	}

	/** Draws text centred on x, or right-aligned to x when centre is false. */
	public void text(BitmapFont f, String s, float x, float y, boolean centre) {
		glyphs.setText(f, s);
		f.draw(batch, s, centre ? x - glyphs.width / 2 : x - glyphs.width, y);
	}

	/**
	 * Draws a bid in the gold bid numerals, h tall, centred on (cx, cy), tinted c (white for
	 * plain gold). A lone 0 is struck through, like the zero-bid icon.
	 */
	public void numeral(int n, float cx, float cy, float h, Color c) {
		String s = String.valueOf(n);
		float w = h * Assets.DIGIT_W / Assets.DIGIT_H, step = w * 0.74f;
		float x = cx - (w + step * (s.length() - 1)) / 2;
		batch.setColor(c.r, c.g, c.b, c.a * fade);
		for (int i = 0; i < s.length(); i++) {
			batch.draw(assets.digit(s.charAt(i) - '0'), x + i * step, cy - h / 2, w, h);
		}
		batch.setColor(Color.WHITE);
	}

	public void rect(float x, float y, float rw, float rh, Color c) {
		batch.setColor(c);
		batch.draw(assets.white, x, y, rw, rh);
		batch.setColor(Color.WHITE);
	}

	/** Draws a rounded frame (panel or button), at the current {@link #fade()}. */
	public void frame(NinePatch p, float x, float y, float fw, float fh) {
		batch.setColor(1f, 1f, 1f, fade);
		p.draw(batch, x, y, fw, fh);
		batch.setColor(Color.WHITE);
	}


	/** A labelled button; sets {@code area} to its tap area and returns it. */
	public Rectangle button(Rectangle area, String label, float cx, float y, float bw, float bh, NinePatch style) {
		frame(style, cx - bw / 2, y, bw, bh);
		BitmapFont f = serif(1.5f);
		Color c = style == assets.buttonOff ? Palette.MUTED : Palette.CREAM;
		f.setColor(c.r, c.g, c.b, fade);
		glyphs.setText(f, label);
		f.draw(batch, label, cx - glyphs.width / 2, y + bh / 2 + glyphs.height / 2);
		return area.set(cx - bw / 2, y, bw, bh);
	}
}
