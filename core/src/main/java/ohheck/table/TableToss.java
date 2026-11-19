package ohheck.table;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import ohheck.fx.FallingCards;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Assets;

/** Throws the last hand's cards off the table at the game's end. */
final class TableToss {

	private static final float TOSS_TRUMP = 0.15f, TOSS_FIRST = 0.35f, TOSS_STEP = 0.025f;

	private final Assets assets;
	private final TableLayout table;
	private FallingCards tumble;

	TableToss(Assets assets, TableLayout table) {
		this.assets = assets;
		this.table = table;
	}

	void toss(Match m) {
		tumble = FallingCards.forTable(assets, new Random(finalScoresSeed(m)));
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

	void clear() {
		tumble = null;
	}

	void update(float dt) {
		if (tumble != null) {
			tumble.update(dt, table.w, table.h);
		}
	}

	boolean active() {
		return tumble != null && tumble.active();
	}

	boolean allGone() {
		return tumble != null && !tumble.active();
	}

	boolean trumpGone() {
		return tumble != null && (tumble.anyThrown() || !tumble.active());
	}

	void draw(SpriteBatch batch) {
		if (tumble != null) {
			tumble.draw(batch, table.w / 2, table.h / 2);
		}
	}

	private static long finalScoresSeed(Match m) {
		long seed = 0;
		for (int s = 0; s < m.players(); s++) {
			seed = seed * 31 + m.score(s);
		}
		return seed;
	}
}
