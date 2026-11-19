package ohheck.table;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Wording;

/**
 * What your taps and drags do at the table: playing a card (tap it, or drag it up onto
 * the table), bidding (tap a number), moving the table on, and the pause menu.
 * PlayScreen passes its touches here; this reads the table ({@link TableScene}), finds
 * what was tapped in the {@link OverlayTargets}, and acts through {@link TableActions}.
 */
final class TableInput {

	private static final int HUMAN = Seating.YOU;
	/** How far a finger moves on a card before it counts as dragging it. */
	private static final float DRAG_START = 12f;

	private final TableScene s;
	private final TableActions act;
	private final OverlayTargets targets;

	private boolean menuOpen;
	// the card under your finger, and whether it's being dragged
	private Card pressed;
	private boolean dragging;
	private float dragX, dragY;

	TableInput(TableScene scene, TableActions actions, OverlayTargets targets) {
		s = scene;
		act = actions;
		this.targets = targets;
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
			act.play(c); // dropped on the table: it flies to the trick from where it was let go
		}
		return true; // otherwise it drops back into your hand
	}

	/** A tap at world position (x, y). Autoplay taps come through here too. */
	void tap(float x, float y) {
		if (menuOpen) {
			MenuItem item = targets.menuItemAt(x, y);
			if (item != null) {
				choose(item);
			}
		} else if (targets.hit(OverlayTargets.Button.MENU, x, y)) {
			openMenu();
		} else {
			tapTable(x, y);
		}
	}

	/** What a tap does depends on the moment (see StateView). */
	private void tapTable(float x, float y) {
		StateView.of(s.state()).tap(this, x, y);
	}

	/** Hurries the shuffle and deal, or the hand-end message, along. */
	void skip() {
		act.skip();
	}

	/** On from the hand's scores. */
	void nextHand() {
		act.nextHand();
	}

	/** Your bid or your card, if it's your turn. */
	void tapTurn(float x, float y) {
		Match m = s.match();
		if (m.currentSeat() != HUMAN) {
			return;
		}
		if (m.phase() == Match.Phase.BIDDING) {
			tapBid(x, y);
		} else {
			tapCard(x, y);
		}
	}

	/** Only the buttons: a stray tap mustn't throw away the final scores. */
	void tapGameOver(float x, float y) {
		if (targets.hit(OverlayTargets.Button.HIGH_SCORES, x, y)) {
			act.showHighScores();
		} else if (targets.hit(OverlayTargets.Button.PLAY_AGAIN, x, y)) {
			act.newGame(); // a fresh table, at the difficulty now chosen in Options
		}
	}


	private void tapBid(float x, float y) {
		int b = targets.bidAt(x, y);
		if (b >= 0 && !act.bid(b)) {
			act.hint(Wording.dealerCantBid(b, s.match().handSize()));
		}
	}

	private void tapCard(float x, float y) {
		int i = cardAt(x, y);
		if (i < 0) {
			return;
		}
		Card c = s.handOrder().get(i);
		if (!s.match().legalPlays(HUMAN).contains(c)) {
			act.hint(Wording.mustFollow(s.match().leadSuit()));
			return;
		}
		act.play(c);
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

	private void choose(MenuItem item) {
		if (item.closesMenu()) {
			menuOpen = false;
		}
		item.choose(act);
	}

}
