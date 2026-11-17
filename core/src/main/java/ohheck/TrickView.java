package ohheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import rules.Card;
import rules.Match;

/**
 * The cards in the middle of the table: each one flies in from where it was
 * played and settles at a slight tilt; a completed trick stays a moment with
 * the winning card framed, then the winner gathers it in.
 */
final class TrickView {

	private static final float PLAY_FLIGHT = 0.25f;
	/** A completed trick: on show, then gathered in over the last COLLECT_TIME. */
	static final float TRICK_PAUSE = 1.7f, COLLECT_TIME = 0.45f;
	/** Extra time for "Trick Won!" when you took it. */
	private static final float TRICK_WON_SHOW = 0.9f;
	private static final float MAX_TILT = 9f;

	/** Per card in flight: start x, start y, start time. */
	private final Map<Card, float[]> flights = new HashMap<Card, float[]>();

	/** A card just played from (x, y) at game time now. */
	void fly(Card c, float x, float y, float now) {
		flights.put(c, new float[] { x, y, now });
	}

	void clear() {
		flights.clear();
	}

	boolean anyInFlight(float now) {
		for (float[] f : flights.values()) {
			if (now - f[2] < PLAY_FLIGHT) {
				return true;
			}
		}
		return false;
	}

	/** How long a completed trick stays on screen. */
	static float showLength(boolean youWon) {
		return TRICK_PAUSE + (youWon ? TRICK_WON_SHOW : 0);
	}

	/**
	 * Draws the trick in progress, or (completed true) the trick just won,
	 * elapsed seconds after it was completed.
	 */
	void draw(SpriteBatch batch, Assets assets, CardArt art, TableLayout table, Match match, boolean completed,
			float elapsed, float now, float fade) {
		List<Card> cards;
		List<Integer> seats;
		int winner = -1;
		float collect = 0; // 0 = on the table, 1 = gathered in by the winner
		if (completed) {
			cards = match.lastTrick();
			seats = match.lastTrickSeats();
			winner = match.lastTrickWinner();
			collect = MathUtils.clamp((elapsed - (TRICK_PAUSE - COLLECT_TIME)) / COLLECT_TIME, 0, 1);
			if (winner == TableLayout.HUMAN && elapsed > TRICK_PAUSE - COLLECT_TIME / 2) {
				float a = MathUtils.clamp((elapsed - (TRICK_PAUSE - COLLECT_TIME / 2)) / 0.3f, 0, 1);
				art.art(assets.trickWonText, table.centreX(), table.centreY(), 0.8f, a);
			}
			if (elapsed >= TRICK_PAUSE) {
				return; // the cards have gone to the winner
			}
		} else {
			cards = match.currentTrick();
			seats = match.currentTrickSeats();
		}
		float cw = Assets.CARD_W, ch = Assets.CARD_H;
		float sinkX = 0, sinkY = 0;
		if (winner >= 0) {
			Vector2 s = table.seatAnchor(winner);
			sinkX = s.x;
			sinkY = s.y;
		}
		for (int i = 0; i < cards.size(); i++) {
			Card c = cards.get(i);
			int seat = seats.get(i);
			Vector2 slot = table.trickSlot(seat);
			float x = slot.x, y = slot.y, scale = 1f, lift = 5f, turn = 1f;
			float rot = tilt(c, seat, match.handIndex());
			float[] f = flights.get(c);
			if (f != null) {
				float raw = MathUtils.clamp((now - f[2]) / PLAY_FLIGHT, 0, 1);
				float p = Interpolation.pow2Out.apply(raw);
				x = MathUtils.lerp(f[0], slot.x, p);
				y = MathUtils.lerp(f[1], slot.y, p);
				lift += (1 - raw) * 30;
				rot *= p; // settles into its tilt as it lands
				if (seat != TableLayout.HUMAN) {
					turn = MathUtils.clamp(raw * 1.4f, 0, 1); // opponents' cards turn over on the way
				}
			}
			if (collect > 0) {
				float p = Interpolation.pow2In.apply(collect);
				x = MathUtils.lerp(x, sinkX - cw / 4, p);
				y = MathUtils.lerp(y, sinkY - ch / 4, p);
				scale = MathUtils.lerp(1f, 0.5f, p);
				lift += p * 18;
				rot *= 1 - p;
			}
			art.shadow(x, y, cw * scale, ch * scale, lift);
			if (collect == 0 && seat == winner) {
				batch.setColor(1f, 1f, 1f, fade);
				assets.panelTurn.draw(batch, x - 7, y - 7, cw / 2 + 7, ch / 2 + 7, cw + 14, ch + 14, 1f, 1f, rot);
				batch.setColor(Color.WHITE);
			}
			art.flip(c, x, y, cw * scale, ch * scale, turn, rot);
		}
	}

	/** A card's resting tilt: random-looking, but steady for that card in that hand. */
	private static float tilt(Card c, int seat, int hand) {
		long k = ((c.suit().ordinal() * 13L + c.rank().ordinal()) * 31 + hand) * 8 + seat;
		k = (k + 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
		k ^= k >>> 31;
		float r = (k & 0xFFFF) / 65535f;
		return (r * 2 - 1) * MAX_TILT;
	}
}
