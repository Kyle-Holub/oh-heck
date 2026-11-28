package ohheck.table;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import ohheck.rules.Suit;

/* Where the overlays' buttons were drawn, for taps to find. An undrawn button has no area. */
final class OverlayTargets {

	enum Button {
		MENU, MAIN_MENU, CONTINUE, FORCE_WIN, KEEP_HAND, TRICKS, REVIEW_BACK, REVIEW_CLOSE
	}

	private final Rectangle[] buttons = new Rectangle[Button.values().length];
	private final Rectangle[] menuItems = new Rectangle[MenuItem.ALL.length];
	private final List<Rectangle> bids = new ArrayList<Rectangle>();
	private final Rectangle[] suits = new Rectangle[Suit.values().length];
	private final Rectangle[] widow = { new Rectangle(), new Rectangle() };
	private int widowChoice = -1;
	static final int REVIEW_CLOSED = -2, REVIEW_ALL = -1;
	private final List<Rectangle> tricks = new ArrayList<Rectangle>();
	private int review = REVIEW_CLOSED;

	OverlayTargets() {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = new Rectangle();
		}
		for (int i = 0; i < menuItems.length; i++) {
			menuItems[i] = new Rectangle();
		}
		for (int i = 0; i < suits.length; i++) {
			suits[i] = new Rectangle();
		}
	}

	Rectangle button(Button b) {
		return buttons[b.ordinal()];
	}

	Rectangle menuItem(MenuItem item) {
		return menuItems[item.ordinal()];
	}

	List<Rectangle> bids() {
		return bids;
	}

	Rectangle bid(int b) {
		return b >= 0 && b < bids.size() ? bids.get(b) : null;
	}

	Rectangle suit(Suit s) {
		return suits[s.ordinal()];
	}

	Suit suitAt(float x, float y) {
		for (Suit s : Suit.values()) {
			if (hits(suit(s), x, y)) {
				return s;
			}
		}
		return null;
	}

	Rectangle widowCard(int i) {
		return widow[i];
	}

	int widowAt(float x, float y) {
		for (int i = 0; i < widow.length; i++) {
			if (hits(widow[i], x, y)) {
				return i;
			}
		}
		return -1;
	}

	int widowChoice() {
		return widowChoice;
	}

	void chooseWidow(int i) {
		widowChoice = i;
	}

	List<Rectangle> tricks() {
		return tricks;
	}

	int trickAt(float x, float y) {
		for (int i = 0; i < tricks.size(); i++) {
			if (hits(tricks.get(i), x, y)) {
				return i;
			}
		}
		return -1;
	}

	int review() {
		return review;
	}

	void review(int which) {
		review = which;
		button(Button.REVIEW_BACK).set(0, 0, 0, 0);
	}

	void clearGameOverButtons() {
		button(Button.MAIN_MENU).set(0, 0, 0, 0);
		button(Button.CONTINUE).set(0, 0, 0, 0);
	}

	boolean hit(Button b, float x, float y) {
		return hits(button(b), x, y);
	}

	MenuItem menuItemAt(float x, float y) {
		for (MenuItem item : MenuItem.ALL) {
			if (hits(menuItem(item), x, y)) {
				return item;
			}
		}
		return null;
	}

	static boolean hits(Rectangle r, float x, float y) {
		return r.width > 0 && r.contains(x, y);
	}

	int bidAt(float x, float y) {
		for (int b = 0; b < bids.size(); b++) {
			if (hits(bids.get(b), x, y)) {
				return b;
			}
		}
		return -1;
	}
}
