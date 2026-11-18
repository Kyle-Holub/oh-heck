package ohheck;

import com.badlogic.gdx.math.Rectangle;

import rules.Card;
import rules.Match;
import rules.Suit;

/**
 * What your taps and drags do at the table: playing a card (tap it, or drag it up onto
 * the table), bidding (tap a number), moving the table on, and the pause menu.
 * PlayScreen passes its touches here; this reads the table ({@link TableScene}) and acts
 * through {@link TableActions}.
 */
final class TableInput {

	private static final int HUMAN = TableLayout.HUMAN;
	/** How far a finger moves on a card before it counts as dragging it. */
	private static final float DRAG_START = 12f;

	private final TableScene s;
	private final TableActions act;

	private boolean menuOpen;
	// the card under your finger, and whether it's being dragged
	private Card pressed;
	private boolean dragging;
	private float dragX, dragY;

	TableInput(TableScene scene, TableActions actions) {
		s = scene;
		act = actions;
	}

	boolean menuOpen() {
		return menuOpen;
	}

	boolean dragging() {
		return dragging;
	}

	Card pressed() {
		return pressed;
	}

	float dragX() {
		return dragX;
	}

	float dragY() {
		return dragY;
	}

	// ---------------------------------------------------------- touches

	void touchDown(float x, float y) {
		clearTouch();
		if (!s.humanCanPlay()) {
			return;
		}
		int i = cardAt(x, y);
		if (i >= 0) {
			pressed = s.handOrder().get(i);
			dragX = x;
			dragY = y;
		}
	}

	/** moved: how far the finger has gone since it went down. */
	void touchDragged(float x, float y, float moved) {
		if (pressed == null) {
			return;
		}
		if (!dragging && moved > DRAG_START && s.match().legalPlays(HUMAN).contains(pressed)) {
			dragging = true;
		}
		dragX = x;
		dragY = y;
	}

	/** Ends a drag; true if this touch was one (so it isn't a tap as well). */
	boolean touchUp(float x, float y) {
		if (!dragging) {
			pressed = null;
			return false;
		}
		Card c = pressed;
		clearTouch();
		float handTop = s.handRects().isEmpty() ? 150 : s.handRects().get(0).y + s.handRects().get(0).height;
		if (y > handTop + 10 && s.humanCanPlay()) {
			// dropped on the table: play it from where it was let go
			act.humanPlay(c, x - TableLayout.handCardW() / 2, y - TableLayout.handCardH() / 2);
		}
		return true; // otherwise it drops back into your hand
	}

	/** A tap at world position (x, y). Autoplay taps come through here too. */
	void tap(float x, float y) {
		TableOverlays o = s.overlays();
		if (menuOpen) {
			for (int i = 0; i < o.menuItems.size(); i++) {
				if (o.menuItems.get(i).contains(x, y)) {
					menuChoice(i);
					return;
				}
			}
			return;
		}
		if (o.menuButton.contains(x, y)) {
			openMenu();
			return;
		}
		Match m = s.match();
		switch (s.state()) {
		case SHUFFLE:
		case DEAL:
		case HAND_END:
			s.flow().skip(); // the shuffle and deal, or the hand-end message
			break;
		case TURN:
			if (m.currentSeat() == HUMAN) {
				if (m.phase() == Match.Phase.BIDDING) {
					tapBid(x, y);
				} else {
					tapCard(x, y);
				}
			}
			break;
		case TRICK:
			break;
		case RESULTS:
			s.flow().nextHand();
			break;
		case GAME_OVER:
			// only the buttons: a stray tap mustn't throw away the final scores
			if (o.highScoresButton.contains(x, y)) {
				act.showHighScores();
			} else if (o.playAgainButton.contains(x, y)) {
				act.newGame(); // a fresh table, at the difficulty now chosen in Options
			}
			break;
		}
	}

	private void tapBid(float x, float y) {
		for (int b = 0; b < s.overlays().bidButtons.size(); b++) {
			if (s.overlays().bidButtons.get(b).contains(x, y)) {
				if (!s.flow().bid(b)) {
					s.overlays().hint("As dealer you can't bid " + b + ": the bids would add up to " + s.match().handSize());
				}
				return;
			}
		}
	}

	private void tapCard(float x, float y) {
		int i = cardAt(x, y);
		if (i < 0) {
			return;
		}
		Card c = s.handOrder().get(i);
		if (!s.match().legalPlays(HUMAN).contains(c)) {
			s.overlays().hint("You must follow suit: play a " + singular(s.match().leadSuit()));
			return;
		}
		Rectangle r = s.handRects().get(i);
		act.humanPlay(c, r.x, r.y);
	}

	private int cardAt(float x, float y) {
		// topmost first: later cards are drawn over earlier ones
		for (int i = s.handRects().size() - 1; i >= 0; i--) {
			if (s.handRects().get(i).contains(x, y)) {
				return i;
			}
		}
		return -1;
	}

	void clearTouch() {
		pressed = null;
		dragging = false;
	}

	// ------------------------------------------------------------- menu

	/** The back button: opens the menu, or closes it. */
	void back() {
		if (menuOpen) {
			menuOpen = false;
		} else {
			openMenu();
		}
	}

	/** Leaving the app (home button, a call): come back to the menu. */
	void openMenu() {
		menuOpen = true;
		clearTouch();
	}

	private void menuChoice(int i) {
		if (i == 1) {
			act.showOptions(); // the menu is still open when you come back
			return;
		}
		menuOpen = false;
		if (i == 2) {
			act.newGame(); // a fresh table, at the difficulty now chosen in Options
		} else if (i == 3) {
			act.showTitle();
		}
	}

	/** "heart", "spade"... */
	private static String singular(Suit suit) {
		String n = suit.name().toLowerCase();
		return n.substring(0, n.length() - 1);
	}
}
