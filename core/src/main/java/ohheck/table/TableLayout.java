package ohheck.table;

import com.badlogic.gdx.math.Vector2;

import ohheck.players.Seating;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;

/*
 * Where everything sits on the table for the current screen size. Seat 0 is you at the bottom,
 * then left, top and right.
 */
final class TableLayout {

	static final float HAND_SCALE = 1.2f, BACK_SCALE = 0.55f, TRUMP_SCALE = 0.95f;
	static final float PANEL_W = PlayerPanel.W, PANEL_H = PlayerPanel.H;
	static final float EDGE = 12;
	static final float HAND_Y = 8, LIFT = 22;
	static final float FAN_STEP = 12;
	static final float SIDE_PANEL_RAISE = 78, TOP_PANEL_INSET = 6;
	static final float FAN_GAP = 10, FAN_DROP = 6;
	static final float HAND_SIDE_GAP = 26, MIN_HAND_W = 330;
	static final float HAND_ANCHOR_Y = 70;
	static final float TRICK_DX = 108, TRICK_DY = 60, TRICK_RAISE = 15;
	static final float HINT_ABOVE_HAND = 50;
	static final float CENTRE_RAISE = 5;

	float w = BaseScreen.WORLD_W, h = BaseScreen.WORLD_H;

	float handStart, handGap;

	private final Vector2 fan = new Vector2(), anchor = new Vector2(), slot = new Vector2();

	void resize(float w, float h) {
		this.w = w;
		this.h = h;
	}

	static float handCardW() {
		return Assets.CARD_W * HAND_SCALE;
	}

	static float handCardH() {
		return Assets.CARD_H * HAND_SCALE;
	}

	static float backW() {
		return Assets.CARD_W * BACK_SCALE;
	}

	static float backH() {
		return Assets.CARD_H * BACK_SCALE;
	}

	float panelX(int seat) {
		return seat == Seating.YOU || seat == 1 ? EDGE : seat == 2 ? w / 2 - PANEL_W / 2 : w - EDGE - PANEL_W;
	}

	float panelTop(int seat) {
		return seat == Seating.YOU ? EDGE + PANEL_H : seat == 2 ? h - TOP_PANEL_INSET : h / 2 + SIDE_PANEL_RAISE;
	}

	Vector2 fanOrigin(int seat) {
		float x = panelX(seat), top = panelTop(seat);
		if (seat == 2) {
			return fan.set(x + PANEL_W + FAN_GAP, top - PANEL_H / 2 - backH() / 2);
		}
		return fan.set(seat == 3 ? x + PANEL_W : x, top - PANEL_H - FAN_DROP - backH());
	}

	float fanCardX(int seat, int i, int n) {
		float x = fanOrigin(seat).x;
		return seat == 3 ? x - backW() - (n - 1 - i) * FAN_STEP : x + i * FAN_STEP;
	}

	void layoutHand(int n) {
		float cw = handCardW();
		float maxWidth = Math.max(MIN_HAND_W, w - 2 * (PANEL_W + HAND_SIDE_GAP));
		handGap = n <= 1 ? 0 : Math.min(cw + 6, (maxWidth - cw) / (n - 1));
		handStart = (w - (handGap * (n - 1) + cw)) / 2;
	}

	Vector2 seatAnchor(int seat) {
		if (seat == Seating.YOU) {
			return anchor.set(w / 2, HAND_ANCHOR_Y);
		}
		Vector2 f = fanOrigin(seat);
		return anchor.set(seat == 3 ? f.x - backW() : f.x + backW(), f.y + backH() / 2);
	}

	Vector2 deckSpot(int seat) {
		if (seat == Seating.YOU) {
			return anchor.set(panelX(Seating.YOU) + PANEL_W - backW(), panelTop(Seating.YOU) + FAN_DROP + backH() / 2);
		}
		return seatAnchor(seat);
	}

	Vector2 dealSpot(int seat) {
		Vector2 trick = trickSlot(seat);
		return slot.set(trick.x + Assets.CARD_W / 2f, trick.y + Assets.CARD_H / 2f);
	}

	float centreX() {
		return w / 2;
	}

	float centreY() {
		return h / 2 + CENTRE_RAISE;
	}

	Vector2 trickSlot(int seat) {
		float cx = w / 2, cy = h / 2 + TRICK_RAISE;
		float x = cx - Assets.CARD_W / 2f + (seat == 1 ? -TRICK_DX : seat == 3 ? TRICK_DX : 0);
		float y = cy - Assets.CARD_H / 2f + (seat == Seating.YOU ? -TRICK_DY : seat == 2 ? TRICK_DY : 0);
		return slot.set(x, y);
	}

	float trumpW() {
		return Assets.CARD_W * TRUMP_SCALE;
	}

	float trumpH() {
		return Assets.CARD_H * TRUMP_SCALE;
	}

	float trumpX() {
		return w - EDGE - trumpW();
	}

	float trumpY() {
		return h - EDGE - trumpH();
	}

	float hintY() {
		return HAND_Y + handCardH() + HINT_ABOVE_HAND;
	}
}
