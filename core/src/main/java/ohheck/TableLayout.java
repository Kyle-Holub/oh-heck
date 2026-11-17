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
		return seat == HUMAN ? EDGE + PANEL_H : seat == 2 ? h - 6 : h / 2 + 78;
	}

	// ---- cards

	/** Where an opponent's face-down cards start: beside the top panel, under the side panels. */
	Vector2 fanOrigin(int seat) {
		float x = panelX(seat), top = panelTop(seat);
		if (seat == 2) {
			return fan.set(x + PANEL_W + 10, top - PANEL_H / 2 - backH() / 2);
		}
		return fan.set(seat == 3 ? x + PANEL_W : x, top - PANEL_H - 6 - backH());
	}

	/** Left edge of the i-th of n face-down cards in an opponent's fan (the right player's fans leftwards). */
	float fanCardX(int seat, int i, int n) {
		float x = fanOrigin(seat).x;
		return seat == 3 ? x - backW() - (n - 1 - i) * FAN_STEP : x + i * FAN_STEP;
	}

	/** Lays out your hand of n cards between your panel and the menu button. */
	void layoutHand(int n) {
		float cw = handCardW();
		float maxWidth = Math.max(330, w - 2 * (PANEL_W + 26));
		handGap = n <= 1 ? 0 : Math.min(cw + 6, (maxWidth - cw) / (n - 1));
		handStart = (w - (handGap * (n - 1) + cw)) / 2;
	}

	/** Centre of where a seat's cards are, for cards flying to and from it. */
	Vector2 seatAnchor(int seat) {
		if (seat == HUMAN) {
			return anchor.set(w / 2, 70);
		}
		Vector2 f = fanOrigin(seat);
		return anchor.set(seat == 3 ? f.x - backW() : f.x + backW(), f.y + backH() / 2);
	}

	/** Centre of the table, where the trick, the deck and the big messages go. */
	float centreX() {
		return w / 2;
	}

	float centreY() {
		return h / 2 + 5;
	}

	/** Bottom-left of a seat's card in the middle of the table. */
	Vector2 trickSlot(int seat) {
		float cx = w / 2, cy = h / 2 + 15;
		float x = cx - Assets.CARD_W / 2f + (seat == 1 ? -108 : seat == 3 ? 108 : 0);
		float y = cy - Assets.CARD_H / 2f + (seat == HUMAN ? -60 : seat == 2 ? 60 : 0);
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
		return HAND_Y + handCardH() + 50;
	}
}
