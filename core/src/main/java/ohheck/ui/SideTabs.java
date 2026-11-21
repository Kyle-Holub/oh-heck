package ohheck.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;

/*
 * The tab column down the left of a menu screen, with the panel beside it for the chosen tab:
 * where both go, how the tabs look, and which one a tap picks.
 */
public final class SideTabs {

	private static final float MAX_W = 940, MARGIN = 24, GAP = 18, W = 210, H = 50, TAB_GAP = 8, HEADING_H = 26;
	private static final float BADGE = 24, BADGE_INSET = 14;

	private final Painter paint;
	private final String heading;
	private final String[] names;
	private final Texture[] badges;
	private final List<Rectangle> areas = new ArrayList<Rectangle>();
	private int chosen;

	public SideTabs(Painter paint, String heading, String[] names, Texture[] badges) {
		this.paint = paint;
		this.heading = heading;
		this.names = names;
		this.badges = badges;
		for (int i = 0; i < names.length; i++) {
			areas.add(new Rectangle());
		}
	}

	public int chosen() {
		return chosen;
	}

	public void choose(int tab) {
		chosen = tab;
	}

	public int tabAt(float x, float y) {
		for (int i = 0; i < areas.size(); i++) {
			if (areas.get(i).contains(x, y)) {
				return i;
			}
		}
		return -1;
	}

	public void draw(float w, float h, Rectangle panel) {
		float width = Math.min(MAX_W, w - 2 * MARGIN), left = (w - width) / 2;
		float top = h - MenuChrome.HEADER_H - GAP;
		panel.set(left + W + GAP, MARGIN, width - W - GAP, top - MARGIN);
		float y = top;
		if (heading != null) {
			paint.ui(0.95f, Palette.MUTED).draw(paint.batch, heading, left + 6, y - 4);
			y -= HEADING_H;
		}
		for (int i = 0; i < names.length; i++) {
			Rectangle tab = areas.get(i).set(left, y - H, W, H);
			boolean on = i == chosen;
			paint.frame(on ? paint.assets.button : paint.assets.buttonAlt, tab.x, tab.y, tab.width, tab.height);
			if (badges != null && badges[i] != null) {
				paint.batch.draw(badges[i], tab.x + BADGE_INSET, tab.y + (H - BADGE) / 2, BADGE, BADGE);
			}
			BitmapFont f = paint.serif(1.3f, on ? Palette.CREAM : Palette.MUTED);
			paint.textIn(f, names[i], tab.x, tab.y, tab.width, tab.height);
			y -= H + TAB_GAP;
		}
	}
}
