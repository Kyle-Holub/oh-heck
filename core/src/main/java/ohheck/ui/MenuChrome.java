package ohheck.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;

/**
 * What the menu screens are framed with: the bar across the top of the settings-style
 * screens (Options, Roster) with its title and Back, Back on its own, and section headings.
 */
public final class MenuChrome {

	/** The bar across the top: its height and colour. */
	public static final float HEADER_H = 56;
	public static final Color HEADER = new Color(0.06f, 0.04f, 0.03f, 0.97f);
	/** The Back button: centre x, size. */
	static final float BACK_X = 70, BACK_W = 110, BACK_H = 40;

	private final Painter paint;

	public MenuChrome(Painter paint) {
		this.paint = paint;
	}

	/** The bar across the top of a w by h screen: a gold rule under it, the title in the middle, Back on the left. */
	public void header(String title, Rectangle back, float w, float h) {
		paint.rect(0, h - HEADER_H, w, HEADER_H, HEADER);
		paint.rect(0, h - HEADER_H - 2, w, 2, Palette.GOLD);
		BitmapFont t = paint.serif(1.6f);
		t.setColor(Palette.GOLD);
		paint.centred(t, title, w / 2, h - 16);
		backButton(back, h - HEADER_H + (HEADER_H - BACK_H) / 2);
	}

	/** The Back button, its bottom at y. */
	public Rectangle backButton(Rectangle area, float y) {
		return paint.button(area, "Back", BACK_X, y, BACK_W, BACK_H, paint.assets.buttonAlt);
	}

	/** A section's name, in gold, above its rows (x is the rows' left edge). */
	public void heading(String name, float x, float y) {
		BitmapFont f = paint.serif(1.35f);
		f.setColor(Palette.GOLD);
		f.draw(paint.batch, name, x + 6, y);
	}
}
