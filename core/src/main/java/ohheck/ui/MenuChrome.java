package ohheck.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;

/* The menu screens' header bar, Back button and section headings. */
public final class MenuChrome {

	public static final float HEADER_H = 56;
	public static final Color HEADER = new Color(0.06f, 0.04f, 0.03f, 0.97f);
	static final float BACK_X = 70, BACK_W = 110, BACK_H = 40;

	private final Painter paint;

	public MenuChrome(Painter paint) {
		this.paint = paint;
	}

	public void header(String title, Rectangle back, float w, float h) {
		paint.rect(0, h - HEADER_H, w, HEADER_H, HEADER);
		paint.rect(0, h - HEADER_H - 2, w, 2, Palette.GOLD);
		BitmapFont t = paint.serif(1.6f, Palette.GOLD);
		paint.centred(t, title, w / 2, h - 16);
		backButton(back, h - HEADER_H + (HEADER_H - BACK_H) / 2);
	}

	public Rectangle headerButton(Rectangle area, String label, float width, boolean lit, float w, float h) {
		float margin = BACK_X - BACK_W / 2, y = h - HEADER_H + (HEADER_H - BACK_H) / 2;
		return paint.button(area, label, w - margin - width / 2, y, width, BACK_H,
				lit ? paint.assets.button : paint.assets.buttonAlt);
	}

	public Rectangle backButton(Rectangle area, float y) {
		paint.back(area);
		return paint.button(area, "Back", BACK_X, y, BACK_W, BACK_H, paint.assets.buttonAlt);
	}

	public void heading(String name, float x, float y) {
		BitmapFont f = paint.serif(1.35f, Palette.GOLD);
		f.draw(paint.batch, name, x + 6, y);
	}
}
