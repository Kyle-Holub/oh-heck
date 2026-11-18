package ohheck;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import rules.Card;
import rules.Match;

/**
 * At the game's end: the trump and every card of the final hand thrown up from where they
 * lay and tumbling off the screen, as on the title screen. Idle (nothing drawn) until then.
 */
final class TableToss {

	/** When the trump goes, then each card of the final hand in the order played. */
	private static final float TOSS_TRUMP = 0.15f, TOSS_FIRST = 0.35f, TOSS_STEP = 0.025f;

	private final Assets assets;
	private final TableLayout table;
	private FallingCards tumble;

	TableToss(Assets assets, TableLayout table) {
		this.assets = assets;
		this.table = table;
	}

	/** Throws the trump and every card of the final hand (from where each was played) off the table. */
	void toss(Match m) {
		// (its own seed, so the throw never changes the game's random numbers)
		tumble = FallingCards.forTable(assets, new Random(m.score(TableLayout.HUMAN) * 31L + m.score(0)));
		float cx = table.w / 2, cy = table.h / 2;
		tumble.toss(m.trumpCard(), table.trumpX() + table.trumpW() / 2 - cx, table.trumpY() + table.trumpH() / 2 - cy,
				table.trumpW(), 0, TOSS_TRUMP);
		List<Card> played = m.playedThisHand();
		List<Integer> seats = m.playedThisHandSeats();
		for (int i = 0; i < played.size(); i++) {
			int seat = seats.get(i);
			Vector2 slot = table.trickSlot(seat);
			tumble.toss(played.get(i), slot.x + Assets.CARD_W / 2f - cx, slot.y + Assets.CARD_H / 2f - cy, Assets.CARD_W,
					TrickView.tilt(played.get(i), seat, m.handIndex()), TOSS_FIRST + i * TOSS_STEP);
		}
	}

	/** A new game: nothing thrown. */
	void clear() {
		tumble = null;
	}

	void update(float dt) {
		if (tumble != null) {
			tumble.update(dt, table.w, table.h);
		}
	}

	/** Whether cards have been thrown and are still on their way off the screen. */
	boolean active() {
		return tumble != null && tumble.active();
	}

	/** Whether cards were thrown and have all fallen away. */
	boolean allGone() {
		return tumble != null && !tumble.active();
	}

	/** Whether the trump card has left the table (so it's no longer drawn in its place). */
	boolean trumpGone() {
		return tumble != null && (tumble.anyThrown() || !tumble.active()); // (once all have fallen, none is left)
	}

	void draw(SpriteBatch batch) {
		if (tumble != null) {
			tumble.draw(batch, table.w / 2, table.h / 2);
		}
	}
}
