package ohheck;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Shuffling and dealing at the start of each hand. The deck comes from the
 * dealer to the middle, is split and riffled (twice for a new game, once
 * between hands) and goes back to the dealer; then the cards fly one at a time,
 * clockwise from the dealer's left, straight to where they'll stay.
 * <p>
 * One clock runs through both parts: shuffling until {@link #shuffleLength},
 * dealing after that.
 */
final class DealAnimation {

	static final float FLIP_TIME = 0.32f;
	private static final float DEAL_INTERVAL = 0.09f, DEAL_FLIGHT = 0.4f;
	private static final float GATHER = 0.4f, SPLIT = 0.3f, RIFFLE = 0.85f, SETTLE = 0.3f, HOME = 0.4f;
	private static final float PASS = SPLIT + RIFFLE + SETTLE;
	private static final int RIFFLE_CARDS = 24;
	/** Each riffled card's time in the air. */
	private static final float DROP = 0.15f;
	private static final float DECK_SCALE = 0.85f, HALF_GAP = 52f;

	private int handSize, dealer, passes, soundsPlayed;
	private final int[] reached = new int[4];
	private float t, shuffleLength, length;

	/** Starts the animation for a hand of handSize cards each, dealt by dealer. */
	void start(int handSize, int dealer, boolean newGame) {
		this.handSize = handSize;
		this.dealer = dealer;
		passes = newGame ? 2 : 1;
		shuffleLength = GATHER + passes * PASS + HOME;
		length = shuffleLength + 4 * handSize * DEAL_INTERVAL + DEAL_FLIGHT + FLIP_TIME + 0.1f;
		t = 0;
		soundsPlayed = 0;
	}

	/** Advances the clock; returns true when a riffle begins (time for the shuffle sound). */
	boolean update(float dt) {
		t += dt;
		if (soundsPlayed < passes && t >= GATHER + soundsPlayed * PASS + SPLIT) {
			soundsPlayed++;
			return true;
		}
		return false;
	}

	boolean isShuffling() {
		return t < shuffleLength;
	}

	boolean isDone() {
		return t >= length;
	}

	private float dealT() {
		return Math.max(0, t - shuffleLength);
	}

	/** Cards that have already landed at a seat. */
	int dealtTo(int seat) {
		int arrived = 0;
		for (int k = 0; k < 4 * handSize; k++) {
			if ((dealer + 1 + k) % 4 == seat && dealT() >= k * DEAL_INTERVAL + DEAL_FLIGHT) {
				arrived++;
			}
		}
		return arrived;
	}

	/** How far your n-th card has turned face up: 0 face down, 1 face up. */
	float flipOfYourCard(int n) {
		int count = 0;
		for (int k = 0; k < 4 * handSize; k++) {
			if ((dealer + 1 + k) % 4 == TableLayout.HUMAN && count++ == n) {
				return MathUtils.clamp((dealT() - (k * DEAL_INTERVAL + DEAL_FLIGHT)) / FLIP_TIME, 0, 1);
			}
		}
		return 1;
	}

	/** Draws the deck being shuffled, or the cards in flight. */
	void draw(SpriteBatch batch, Assets assets, CardArt art, TableLayout table) {
		if (isShuffling()) {
			drawShuffle(batch, assets, art, table);
			return;
		}
		Vector2 from = table.seatAnchor(dealer);
		float fromX = from.x, fromY = from.y;
		float fromW = TableLayout.backW(), fromH = TableLayout.backH();
		table.layoutHand(handSize);
		float dealT = dealT();
		java.util.Arrays.fill(reached, 0);
		for (int k = 0; k < 4 * handSize; k++) {
			int seat = (dealer + 1 + k) % 4;
			int j = reached[seat]++; // this is the j-th card to reach that seat
			float start = k * DEAL_INTERVAL;
			if (dealT < start || dealT >= start + DEAL_FLIGHT) {
				continue;
			}
			// fly straight to the place the card will stay, face down; yours flip once they land
			float tx, ty, tw, th;
			if (seat == TableLayout.HUMAN) {
				tw = TableLayout.handCardW();
				th = TableLayout.handCardH();
				tx = table.handStart + j * table.handGap;
				ty = TableLayout.HAND_Y;
			} else {
				tw = TableLayout.backW();
				th = TableLayout.backH();
				tx = table.fanCardX(seat, j, handSize);
				ty = table.fanOrigin(seat).y;
			}
			float p = Interpolation.pow2Out.apply((dealT - start) / DEAL_FLIGHT);
			float cw = MathUtils.lerp(fromW, tw, p), ch = MathUtils.lerp(fromH, th, p);
			float x = MathUtils.lerp(fromX - fromW / 2, tx, p), y = MathUtils.lerp(fromY - fromH / 2, ty, p);
			art.shadow(x, y, cw, ch, 26 * (1 - p) + 4);
			batch.draw(assets.back(), x, y, cw, ch);
		}
	}

	private void drawShuffle(SpriteBatch batch, Assets assets, CardArt art, TableLayout table) {
		float cw = Assets.CARD_W * DECK_SCALE, ch = Assets.CARD_H * DECK_SCALE;
		float cx = table.centreX(), cy = table.centreY();
		Vector2 d = table.seatAnchor(dealer);
		float dx = d.x, dy = d.y;
		float t = this.t;

		// gather: the deck comes from the dealer to the middle of the table
		if (t < GATHER) {
			float p = Interpolation.pow2Out.apply(t / GATHER);
			art.stack(MathUtils.lerp(dx, cx, p), MathUtils.lerp(dy, cy, p), 52, cw, ch, 0);
			return;
		}
		t -= GATHER;

		// riffle passes: split into two halves, then cards fall alternately from each onto one pile
		if (t < passes * PASS) {
			t %= PASS;
			if (t < SPLIT) {
				float p = Interpolation.pow2Out.apply(t / SPLIT);
				art.stack(cx - HALF_GAP * p, cy, 26, cw, ch, 8 * p);
				art.stack(cx + HALF_GAP * p, cy, 26, cw, ch, -8 * p);
				return;
			}
			t -= SPLIT;
			if (t < RIFFLE) {
				drawRiffle(batch, assets, art, t, cx, cy, cw, ch);
				return;
			}
			// settle: the deck is squared up
			art.stack(cx, cy, 52, cw, ch, 0);
			return;
		}
		t -= passes * PASS;

		// home: the deck goes back to the dealer, ready to deal
		float p = Interpolation.pow2In.apply(MathUtils.clamp(t / HOME, 0, 1));
		float s = MathUtils.lerp(1f, TableLayout.BACK_SCALE / DECK_SCALE, p);
		art.stack(MathUtils.lerp(cx, dx, p), MathUtils.lerp(cy, dy, p), 52, cw * s, ch * s, 0);
	}
	/** t seconds into a riffle: cards fall alternately from the two halves onto one pile in the middle. */
	private void drawRiffle(SpriteBatch batch, Assets assets, CardArt art, float t, float cx, float cy, float cw, float ch) {
		float step = (RIFFLE - DROP) / RIFFLE_CARDS;
		int landed = 0, leftGone = 0, rightGone = 0;
		for (int i = 0; i < RIFFLE_CARDS; i++) {
			if (t >= i * step) {
				if (i % 2 == 0) {
					leftGone++;
				} else {
					rightGone++;
				}
			}
			if (t >= i * step + DROP) {
				landed++;
			}
		}
		int perHalf = RIFFLE_CARDS / 2;
		art.stack(cx - HALF_GAP, cy, 26 * (perHalf - leftGone) / perHalf + 1, cw, ch, 8);
		art.stack(cx + HALF_GAP, cy, 26 * (perHalf - rightGone) / perHalf + 1, cw, ch, -8);
		art.stack(cx, cy, landed * 52 / RIFFLE_CARDS, cw, ch, 0);
		// cards in the air, arcing from their half to the pile
		for (int i = 0; i < RIFFLE_CARDS; i++) {
			float p = (t - i * step) / DROP;
			if (p < 0 || p >= 1) {
				continue;
			}
			float from = i % 2 == 0 ? cx - HALF_GAP : cx + HALF_GAP;
			float x = MathUtils.lerp(from, cx, p), y = cy + 8 + MathUtils.sin(MathUtils.PI * p) * 16;
			float rot = MathUtils.lerp(i % 2 == 0 ? 8 : -8, 0, p);
			batch.draw(assets.back(), x - cw / 2, y - ch / 2, cw / 2, ch / 2, cw, ch, 1f, 1f, rot);
		}
	}

}
