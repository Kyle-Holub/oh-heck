package ohheck.table;

import java.util.Random;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.rules.Card;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;

/* At a lost game's end: a deck near the top of the screen sprays its cards out, each in its own arc to one side. */
final class DeckShower {

	private static final float ARRIVE = 0.5f, FIRST = 0.7f, EVERY = 0.045f, GRAVITY = 900;
	private static final float SIDE_MIN = 120, SIDE_RANGE = 340, UP_MIN = 80, UP_RANGE = 240, SPIN = 720;
	private static final float DECK_SCALE = 0.8f, DECK_DROP = 150, DECK_RISE = 220;
	private static final Tween ARRIVING = Tween.over(ARRIVE, Ease.OUT);

	private final Card[] cards = new Card[Card.COUNT];
	private final float[] x = new float[Card.COUNT], y = new float[Card.COUNT], vx = new float[Card.COUNT],
			vy = new float[Card.COUNT], turn = new float[Card.COUNT], spin = new float[Card.COUNT];
	private final boolean[] launched = new boolean[Card.COUNT], gone = new boolean[Card.COUNT];
	private boolean started;
	private float t;

	void start(long seed) {
		Random rng = new Random(seed);
		for (int i = 0; i < Card.COUNT; i++) {
			cards[i] = Card.ofIndex(i);
		}
		for (int i = Card.COUNT - 1; i > 0; i--) {
			int j = rng.nextInt(i + 1);
			Card c = cards[i];
			cards[i] = cards[j];
			cards[j] = c;
		}
		for (int i = 0; i < Card.COUNT; i++) {
			vx[i] = (rng.nextBoolean() ? 1 : -1) * (SIDE_MIN + rng.nextFloat() * SIDE_RANGE);
			vy[i] = UP_MIN + rng.nextFloat() * UP_RANGE;
			spin[i] = (rng.nextFloat() - 0.5f) * SPIN;
			turn[i] = 0;
			launched[i] = false;
			gone[i] = false;
		}
		t = 0;
		started = true;
	}

	void clear() {
		started = false;
	}

	void update(float dt, float w, float h) {
		if (!started) {
			return;
		}
		t += dt;
		float cw = Assets.CARD_W * DECK_SCALE, ch = Assets.CARD_H * DECK_SCALE;
		for (int i = 0; i < Card.COUNT; i++) {
			if (gone[i] || t < launchAt(i)) {
				continue;
			}
			if (!launched[i]) {
				launched[i] = true;
				x[i] = w / 2;
				y[i] = deckY(h);
			}
			vy[i] -= GRAVITY * dt;
			x[i] += vx[i] * dt;
			y[i] += vy[i] * dt;
			turn[i] += spin[i] * dt;
			gone[i] = y[i] < -ch || x[i] < -cw || x[i] > w + cw;
		}
	}

	boolean active() {
		return started && !done();
	}

	boolean done() {
		if (!started) {
			return false;
		}
		for (boolean g : gone) {
			if (!g) {
				return false;
			}
		}
		return true;
	}

	void draw(SpriteBatch batch, Assets assets, CardArt art, float w, float h) {
		if (!started || done()) {
			return;
		}
		float cw = Assets.CARD_W * DECK_SCALE, ch = Assets.CARD_H * DECK_SCALE;
		int left = 0;
		for (int i = 0; i < Card.COUNT; i++) {
			left += launched[i] ? 0 : 1;
		}
		if (left > 0) {
			float arrive = ARRIVING.progress(t);
			art.stack(w / 2, MathUtils.lerp(deckY(h) + DECK_RISE, deckY(h), arrive), left, cw, ch, 0);
		}
		for (int i = 0; i < Card.COUNT; i++) {
			if (launched[i] && !gone[i]) {
				batch.draw(assets.face(cards[i]), x[i] - cw / 2, y[i] - ch / 2, cw / 2, ch / 2, cw, ch, 1, 1, turn[i]);
			}
		}
	}

	private static float launchAt(int i) {
		return FIRST + i * EVERY;
	}

	private static float deckY(float h) {
		return h - DECK_DROP;
	}
}
