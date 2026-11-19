package ohheck.table;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Wording;

/** Turns touches at the table into actions. */
final class TableInput {

	private static final int HUMAN = Seating.YOU;
	private static final float DRAG_START = 12f;

	private final TableScene s;
	private final TableActions act;
	private final OverlayTargets targets;

	private boolean menuOpen;
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

	boolean touchUp(float x, float y) {
		if (!dragging) {
			pressed = null;
			return false;
		}
		Card c = pressed;
		clearTouch();
		float handTop = s.handRects().isEmpty() ? 150 : s.handRects().get(0).y + s.handRects().get(0).height;
		if (y > handTop + 10 && s.humanCanPlay()) {
			act.play(c);
		}
		return true;
	}

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

	private void tapTable(float x, float y) {
		StateView.of(s.state()).tap(this, x, y);
	}

	void skip() {
		act.skip();
	}

	void nextHand() {
		act.nextHand();
	}

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

	void tapGameOver(float x, float y) {
		if (targets.hit(OverlayTargets.Button.HIGH_SCORES, x, y)) {
			act.showHighScores();
		} else if (targets.hit(OverlayTargets.Button.PLAY_AGAIN, x, y)) {
			act.newGame();
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

	void back() {
		if (menuOpen) {
			menuOpen = false;
		} else {
			openMenu();
		}
	}

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
