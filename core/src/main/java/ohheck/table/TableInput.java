package ohheck.table;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Suit;
import ohheck.ui.Wording;

/* Turns touches at the table into actions. */
final class TableInput {

	private static final float DRAG_START = 12f;

	private final TableScene scene;
	private final TableActions actions;
	private final OverlayTargets targets;

	private boolean menuOpen;
	private Card pressed;
	private boolean dragging;
	private float dragX, dragY;

	TableInput(TableScene scene, TableActions actions, OverlayTargets targets) {
		this.scene = scene;
		this.actions = actions;
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
		if (!scene.humanCanPlay()) {
			return;
		}
		int i = cardAt(x, y);
		if (i >= 0) {
			pressed = scene.handOrder().get(i);
			dragX = x;
			dragY = y;
		}
	}

	void touchDragged(float x, float y, float moved) {
		if (pressed == null) {
			return;
		}
		if (!dragging && moved > DRAG_START && scene.playable(pressed)) {
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
		float handTop = scene.handRects().isEmpty() ? 150 : scene.handRects().get(0).y + scene.handRects().get(0).height;
		if (y > handTop + 10 && scene.humanCanPlay()) {
			actions.play(c);
		}
		return true;
	}

	void tap(float x, float y) {
		if (scene.finishing()) {
			return;
		}
		if (scene.tip() != null) {
			actions.dismissTip();
			return;
		}
		if (menuOpen) {
			MenuItem item = targets.menuItemAt(x, y);
			if (item != null) {
				choose(item);
			}
		} else if (targets.hit(OverlayTargets.Button.MENU, x, y)) {
			openMenu();
		} else if (targets.hit(OverlayTargets.Button.FORCE_WIN, x, y)) {
			actions.forceWin();
		} else {
			tapTable(x, y);
		}
	}

	private void tapTable(float x, float y) {
		scene.state().tap(this, x, y);
	}

	void skip() {
		actions.skip();
	}

	void nextHand() {
		actions.nextHand();
	}

	void tapTurn(float x, float y) {
		Match m = scene.match();
		if (m.currentSeat() != Seating.YOU) {
			return;
		}
		if (m.swapping()) {
			tapWidow(x, y);
		} else if (m.pickingTrump()) {
			Suit s = targets.suitAt(x, y);
			if (s != null) {
				actions.pickTrump(s);
			}
		} else if (m.phase() == Match.Phase.BIDDING) {
			tapBid(x, y);
		} else {
			tapCard(x, y);
		}
	}

	void tapGameOver(float x, float y) {
		if (!scene.resultsShown()) {
			actions.skipGameEnd();
		} else if (targets.hit(OverlayTargets.Button.CONTINUE, x, y)) {
			actions.showHighScores();
		} else if (targets.hit(OverlayTargets.Button.MAIN_MENU, x, y)) {
			actions.showTitle();
		}
	}

	private void tapWidow(float x, float y) {
		int w = targets.widowAt(x, y);
		if (w >= 0) {
			targets.chooseWidow(w == targets.widowChoice() ? -1 : w);
			return;
		}
		if (targets.hit(OverlayTargets.Button.KEEP_HAND, x, y)) {
			targets.chooseWidow(-1);
			actions.swapWidow(null, null);
			return;
		}
		int i = cardAt(x, y);
		if (i >= 0 && targets.widowChoice() >= 0) {
			Card take = scene.match().widow().get(targets.widowChoice());
			targets.chooseWidow(-1);
			actions.swapWidow(scene.handOrder().get(i), take);
		}
	}

	private void tapBid(float x, float y) {
		int b = targets.bidAt(x, y);
		if (b >= 0 && !actions.bid(b)) {
			actions.refuseBid(b);
			actions.hint(Wording.dealerCantBid(b, scene.match().handSize()));
		}
	}

	private void tapCard(float x, float y) {
		int i = cardAt(x, y);
		if (i < 0) {
			return;
		}
		Card c = scene.handOrder().get(i);
		if (!scene.match().legalPlays(Seating.YOU).contains(c)) {
			actions.hint(Wording.mustFollow(scene.match().leadSuit()));
			return;
		}
		if (!scene.playable(c)) {
			return;
		}
		actions.play(c);
	}

	private int cardAt(float x, float y) {
		for (int i = scene.handRects().size() - 1; i >= 0; i--) {
			if (scene.handRects().get(i).contains(x, y)) {
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
		item.choose(actions);
	}
}
