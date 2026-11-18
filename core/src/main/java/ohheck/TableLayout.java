package ohheck;

import com.badlogic.gdx.math.Vector2;

/**
 * Where everything sits on the card table, in world units, for the current
 * screen size. Seat 0 is you (bottom), then 1 left, 2 top, 3 right.
 * <p>
 * Methods returning a Vector2 reuse one object per method, so read the result
 * before calling the same method again.
 */
final class TableLayout {

	static final int HUMAN = 0;

	static final float HAND_SCALE = 1.2f, BACK_SCALE = 0.55f, TRUMP_SCALE = 0.95f;
	static final float PANEL_W = PlayerPanel.W, PANEL_H = PlayerPanel.H;
	/** Margin from the screen edges. */
	static final float EDGE = 12;
	/** Bottom of your cards; playable ones rise by LIFT. */
	static final float HAND_Y = 8, LIFT = 22;
	/** Gap between the cards of an opponent's face-down fan. */
	static final float FAN_STEP = 12;
	/** The side players' panels sit this far above the middle; the top one this far below the top edge. */
	static final float SIDE_PANEL_RAISE = 78, TOP_PANEL_INSET = 6;
	/** Space between a panel and its player's fan of cards. */
	static final float FAN_GAP = 10, FAN_DROP = 6;
	/** Your hand keeps clear of your panel and the menu button, but is never squeezed narrower than MIN_HAND_W. */
	static final float HAND_SIDE_GAP = 26, MIN_HAND_W = 330;
	/** Where your played cards come from: the middle of your hand. */
	static final float HAND_ANCHOR_Y = 70;
	/** The trick: the side cards sit TRICK_DX either side of the centre, top and bottom ones TRICK_DY above and below. */
	static final float TRICK_DX = 108, TRICK_DY = 60, TRICK_RAISE = 15;
	/** The hint box sits this far above your cards (clear of a raised card). */
	static final float HINT_ABOVE_HAND = 50;
	/** The centre line for the trick, the deck and big messages sits a little above the middle. */
	static final float CENTRE_RAISE = 5;

	float w = BaseScreen.WORLD_W, h = BaseScreen.WORLD_H;

	/** Set by {@link #layoutHand}: left edge of your first card and the step between cards. */
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

	// ---- player panels

	/** Left edge of a seat's panel: you and the left player on the left, the top player centred. */
	float panelX(int seat) {
		return seat == HUMAN || seat == 1 ? EDGE : seat == 2 ? w / 2 - PANEL_W / 2 : w - EDGE - PANEL_W;
	}

	/** Top edge of a seat's panel: yours at the bottom, the top one at the top, the side ones above the middle. */
	float panelTop(int seat) {
		return seat == HUMAN ? EDGE + PANEL_H : seat == 2 ? h - TOP_PANEL_INSET : h / 2 + SIDE_PANEL_RAISE;
	}

	// ---- cards

	/** Where an opponent's face-down cards start: beside the top panel, under the side panels. */
	Vector2 fanOrigin(int seat) {
		float x = panelX(seat), top = panelTop(seat);
		if (seat == 2) {
			return fan.set(x + PANEL_W + FAN_GAP, top - PANEL_H / 2 - backH() / 2);
		}
		return fan.set(seat == 3 ? x + PANEL_W : x, top - PANEL_H - FAN_DROP - backH());
	}

	/** Left edge of the i-th of n face-down cards in an opponent's fan (the right player's fans leftwards). */
	float fanCardX(int seat, int i, int n) {
		float x = fanOrigin(seat).x;
		return seat == 3 ? x - backW() - (n - 1 - i) * FAN_STEP : x + i * FAN_STEP;
	}

	/** Lays out your hand of n cards between your panel and the menu button. */
	void layoutHand(int n) {
		float cw = handCardW();
		float maxWidth = Math.max(MIN_HAND_W, w - 2 * (PANEL_W + HAND_SIDE_GAP));
		handGap = n <= 1 ? 0 : Math.min(cw + 6, (maxWidth - cw) / (n - 1));
		handStart = (w - (handGap * (n - 1) + cw)) / 2;
	}

	/** Centre of where a seat's cards are, for cards flying to and from it. */
	Vector2 seatAnchor(int seat) {
		if (seat == HUMAN) {
			return anchor.set(w / 2, HAND_ANCHOR_Y);
		}
		Vector2 f = fanOrigin(seat);
		return anchor.set(seat == 3 ? f.x - backW() : f.x + backW(), f.y + backH() / 2);
	}

	/** Centre of the table, where the trick, the deck and the big messages go. */
	float centreX() {
		return w / 2;
	}

	float centreY() {
		return h / 2 + CENTRE_RAISE;
	}

	/** Bottom-left of a seat's card in the middle of the table. */
	Vector2 trickSlot(int seat) {
		float cx = w / 2, cy = h / 2 + TRICK_RAISE;
		float x = cx - Assets.CARD_W / 2f + (seat == 1 ? -TRICK_DX : seat == 3 ? TRICK_DX : 0);
		float y = cy - Assets.CARD_H / 2f + (seat == HUMAN ? -TRICK_DY : seat == 2 ? TRICK_DY : 0);
		return slot.set(x, y);
	}

	/** The trump card, top right. */
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

	/** Bottom of the hint box: just above a raised card in your hand. */
	float hintY() {
		return HAND_Y + handCardH() + HINT_ABOVE_HAND;
	}
}
