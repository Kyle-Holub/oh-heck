package ohheck.table;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import ohheck.ui.TapAreas;

/**
 * Where the buttons drawn over the table are, for taps to find: the menu button, the pause
 * menu's choices, the bid numbers and the game-over buttons. {@link TableOverlays} places
 * each one as it draws it (tests place them by hand); {@link TableInput} asks what a tap hit,
 * and {@link Autoplay} where to tap. A button not drawn this game has no area, so it can't
 * be hit.
 */
final class OverlayTargets {

	/** The single buttons. */
	enum Button {
		MENU, PLAY_AGAIN, HIGH_SCORES
	}

	private final Rectangle[] buttons = new Rectangle[Button.values().length];
	private final Rectangle[] menuItems = new Rectangle[MenuItem.ALL.length];
	private final List<Rectangle> bids = new ArrayList<Rectangle>();

	OverlayTargets() {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = new Rectangle();
		}
		for (int i = 0; i < menuItems.length; i++) {
			menuItems[i] = new Rectangle();
		}
	}

	// ---- placing

	/** The button's area, to be placed (by setting it) as it's drawn. */
	Rectangle button(Button b) {
		return buttons[b.ordinal()];
	}

	/** The menu choice's area, to be placed as it's drawn. */
	Rectangle menuItem(MenuItem item) {
		return menuItems[item.ordinal()];
	}

	/** Makes room for the bids 0 to most (so a bigger bid has no area). */
	void bidsUpTo(int most) {
		TapAreas.sizeTo(bids, most + 1);
	}

	/** Bid b's button, to be placed as it's drawn, or null if there's none so high. */
	Rectangle bid(int b) {
		return b >= 0 && b < bids.size() ? bids.get(b) : null;
	}

	/** The game-over buttons can't be tapped until they're drawn again. */
	void clearGameOverButtons() {
		button(Button.PLAY_AGAIN).set(0, 0, 0, 0);
		button(Button.HIGH_SCORES).set(0, 0, 0, 0);
	}

	// ---- finding

	boolean hit(Button b, float x, float y) {
		return hits(button(b), x, y);
	}

	/** The menu choice at (x, y), or null. */
	MenuItem menuItemAt(float x, float y) {
		for (MenuItem item : MenuItem.ALL) {
			if (hits(menuItem(item), x, y)) {
				return item;
			}
		}
		return null;
	}

	/** Whether r is placed (not still empty) and (x, y) is on it. */
	static boolean hits(Rectangle r, float x, float y) {
		return r.width > 0 && r.contains(x, y);
	}

	/** The bid whose button is at (x, y), or -1. */
	int bidAt(float x, float y) {
		for (int b = 0; b < bids.size(); b++) {
			if (hits(bids.get(b), x, y)) {
				return b;
			}
		}
		return -1;
	}
}
