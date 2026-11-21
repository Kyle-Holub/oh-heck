package ohheck.table;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import ohheck.anim.Ease;
import ohheck.anim.Phases;
import ohheck.anim.Tween;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;

/* The shuffle and the deal's timing. */
final class DealAnimation implements FlowView.Deal {

	static final float FLIP_TIME = 0.32f;
	static final float DEAL_INTERVAL = 0.09f, DEAL_FLIGHT = 0.4f;
	private static final float GATHER = 0.4f, SPLIT = 0.3f, RIFFLE = 0.85f, SETTLE = 0.3f, EXIT = 0.5f;
	private static final float PASS = SPLIT + RIFFLE + SETTLE;
	private static final int RIFFLE_CARDS = 24, DECK = 52;
	private static final float DROP = 0.15f;
	static final float DECK_SCALE = 0.85f;
	private static final float HALF_GAP = 52f;

	private static final Tween GATHERING = Tween.over(GATHER, Ease.OUT), SPLITTING = Tween.over(SPLIT, Ease.OUT),
			LEAVING = Tween.over(EXIT, Ease.IN);
	private static final Phases PASS_STAGES = new Phases(SPLIT, RIFFLE, SETTLE);

	private int handSize, dealer, passes, soundsPlayed;
	private float t, shuffleLength, exitStart, length;

	void start(int handSize, int dealer, boolean newGame) {
		this.handSize = handSize;
		this.dealer = dealer;
		passes = newGame ? 2 : 1;
		shuffleLength = GATHER + passes * PASS;
		float cardsLanded = cardsDealt() * DEAL_INTERVAL + DEAL_FLIGHT + FLIP_TIME + 0.1f;
		exitStart = cardsDealt() * DEAL_INTERVAL;
		length = shuffleLength + Math.max(cardsLanded, exitStart + EXIT);
		t = 0;
		soundsPlayed = 0;
	}

	boolean update(float dt) {
		t += dt;
		if (soundsPlayed < passes && t >= GATHER + soundsPlayed * PASS + SPLIT) {
			soundsPlayed++;
			return true;
		}
		return false;
	}

	@Override
	public boolean isShuffling() {
		return t < shuffleLength;
	}

	boolean isDone() {
		return t >= length;
	}

	@Override
	public float dealTime() {
		return Math.max(0, t - shuffleLength);
	}

	@Override
	public float startOf(int seat, int j) {
		int k = ((seat - dealer - 1) % 4 + 4) % 4 + 4 * j;
		return k * DEAL_INTERVAL;
	}

	@Override
	public int dealer() {
		return dealer;
	}

	private int cardsDealt() {
		return 4 * handSize;
	}

	private int cardsLeftIn(float dealTime) {
		return DECK - Math.min(cardsDealt(), (int) (dealTime / DEAL_INTERVAL) + 1);
	}

	@Override
	public void drawDeck(SpriteBatch batch, Assets assets, CardArt art, TableLayout layout) {
		float cw = Assets.CARD_W * DECK_SCALE, ch = Assets.CARD_H * DECK_SCALE;
		Vector2 home = layout.deckSpot(dealer);
		float hx = home.x, hy = home.y;
		Vector2 spot = layout.dealSpot(dealer);
		float cx = spot.x, cy = spot.y;
		if (t < GATHER) {
			float p = GATHERING.progress(t);
			art.stack(MathUtils.lerp(hx, cx, p), MathUtils.lerp(hy, cy, p), DECK, cw, ch, 0);
		} else if (isShuffling()) {
			drawPass((t - GATHER) % PASS, batch, assets, art, cx, cy, cw, ch);
		} else if (dealTime() < exitStart) {
			art.stack(cx, cy, cardsLeftIn(dealTime()), cw, ch, 0);
		} else {
			float p = LEAVING.progress(dealTime() - exitStart);
			float ox = layout.w + cw, oy = layout.h + ch;
			art.stack(MathUtils.lerp(cx, ox, p), MathUtils.lerp(cy, oy, p), cardsLeftIn(dealTime()), cw, ch, 0);
		}
	}

	private void drawPass(float u, SpriteBatch batch, Assets assets, CardArt art, float cx, float cy, float cw, float ch) {
		int step = PASS_STAGES.at(u);
		float v = PASS_STAGES.into(u);
		if (step == 0) {
			float p = SPLITTING.progress(v);
			art.stack(cx - HALF_GAP * p, cy, DECK / 2, cw, ch, 8 * p);
			art.stack(cx + HALF_GAP * p, cy, DECK / 2, cw, ch, -8 * p);
		} else if (step == 1) {
			drawRiffle(batch, assets, art, v, cx, cy, cw, ch);
		} else {
			art.stack(cx, cy, DECK, cw, ch, 0);
		}
	}

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
