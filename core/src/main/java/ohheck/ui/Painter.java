package ohheck.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/* Fonts, text, frames and buttons. The two fonts are shared, so each call to ui() or serif() sets their size and colour afresh. */
public final class Painter {

	public final SpriteBatch batch;
	public final Assets assets;
	public final GlyphLayout glyphs = new GlyphLayout();
	private float fade = 1f;

	public Painter(SpriteBatch batch, Assets assets) {
		this.batch = batch;
		this.assets = assets;
	}

	public float fade() {
		return fade;
	}

	public void fade(float alpha) {
		fade = alpha;
	}

	public BitmapFont ui(float size) {
		return ui(size, Color.WHITE);
	}

	public BitmapFont ui(float size, Color color) {
		return sized(assets.font, size * Assets.UI_BASE, color);
	}

	public BitmapFont ui(float size, Color color, float alpha) {
		return faded(ui(size, color), color, alpha);
	}

	public BitmapFont serif(float size) {
		return serif(size, Color.WHITE);
	}

	public BitmapFont serif(float size, Color color) {
		return sized(assets.serif, size * Assets.SERIF_BASE, color);
	}

	public BitmapFont serif(float size, Color color, float alpha) {
		return faded(serif(size, color), color, alpha);
	}

	private static BitmapFont faded(BitmapFont font, Color color, float alpha) {
		font.setColor(color.r, color.g, color.b, alpha);
		return font;
	}

	private static BitmapFont sized(BitmapFont font, float scale, Color color) {
		font.getData().setScale(scale);
		font.setColor(color);
		return font;
	}

	public void centred(BitmapFont f, String s, float x, float y) {
		glyphs.setText(f, s);
		f.draw(batch, s, x - glyphs.width / 2, y);
	}

	public void textIn(BitmapFont f, String s, float x, float y, float bw, float bh) {
		glyphs.setText(f, s);
		f.draw(batch, s, x + (bw - glyphs.width) / 2, y + (bh + glyphs.height) / 2);
	}

	public void text(BitmapFont f, String s, float x, float y, boolean centre) {
		glyphs.setText(f, s);
		f.draw(batch, s, centre ? x - glyphs.width / 2 : x - glyphs.width, y);
	}

	public void numeral(int n, float cx, float cy, float h, Color c) {
		String s = Numbers.text(n);
		float w = h * Assets.DIGIT_W / Assets.DIGIT_H, step = w * 0.74f;
		float x = cx - (w + step * (s.length() - 1)) / 2;
		batch.setColor(c.r, c.g, c.b, c.a * fade);
		for (int i = 0; i < s.length(); i++) {
			int d = s.charAt(i) - '0';
			batch.draw(assets.digit(d == 0 && s.length() > 1 ? Assets.PLAIN_ZERO : d), x + i * step, cy - h / 2, w, h);
		}
		batch.setColor(Color.WHITE);
	}

	public void rect(float x, float y, float rw, float rh, Color c) {
		batch.setColor(c);
		batch.draw(assets.white, x, y, rw, rh);
		batch.setColor(Color.WHITE);
	}

	public void frame(NinePatch p, float x, float y, float fw, float fh) {
		batch.setColor(1f, 1f, 1f, fade);
		p.draw(batch, x, y, fw, fh);
		batch.setColor(Color.WHITE);
	}

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
