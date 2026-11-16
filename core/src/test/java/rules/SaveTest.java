package rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

public class SaveTest {

	private final Bot bot = new Bot();

	/**
	 * At every step of many games: saving and loading gives back the same state,
	 * and the restored game accepts the same next move with the same result.
	 */
	@Test
	public void roundTripsAtEveryStep() {
		for (long seed = 0; seed < 400; seed++) {
			Match m = Match.random(4, new Random(seed));
			while (m.phase() != Match.Phase.GAME_OVER) {
				String saved = m.save();
				Match r = Match.load(saved, new Random(99));
				assertEquals(saved, r.save());
				if (m.phase() == Match.Phase.HAND_OVER) {
					// the next deal comes from each game's own Random, so stop comparing here
					m.nextHand();
					continue;
				}
				move(m);
				move(r);
				assertEquals(m.save(), r.save());
			}
			String end = m.save();
			assertEquals(end, Match.load(end, new Random()).save());
		}
	}

	private void move(Match m) {
		int s = m.currentSeat();
		if (m.phase() == Match.Phase.BIDDING) {
			m.placeBid(s, bot.chooseBid(m.hand(s), m.trump(), m.bidsSoFar()));
		} else {
			m.playCard(s, bot.choosePlay(m.hand(s), m.currentTrick(), m.trump(), m.bid(s), m.tricksWon(s)));
		}
	}

	@Test
	public void restoredGamePlaysToTheEnd() {
		Match m = Match.random(4, new Random(8));
		for (int i = 0; i < 30; i++) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				m.nextHand();
			} else {
				move(m);
			}
		}
		Match r = Match.load(m.save(), new Random(1));
		while (r.phase() != Match.Phase.GAME_OVER) {
			if (r.phase() == Match.Phase.HAND_OVER) {
				r.nextHand();
			} else {
				move(r);
			}
		}
		assertEquals(8, r.handIndex() + 1);
	}

	@Test
	public void rejectsDamagedSaves() {
		String good = Match.random(4, new Random(2)).save();
		String[] bad = { "", "hello", good.replace("oh-heck-match-1", "something-else"),
				good.substring(0, good.length() / 2), good.replaceFirst("hand=0", "hand=12"),
				good.replaceFirst("players=4", "players=x") };
		for (String b : bad) {
			try {
				Match.load(b, new Random());
				fail("accepted: " + b);
			} catch (IllegalArgumentException expected) {
			}
		}
		// the same card in two hands
		String[] lines = good.split("\n");
		String hand0 = null, hand1 = null;
		for (String l : lines) {
			if (l.startsWith("hand0=")) {
				hand0 = l;
			} else if (l.startsWith("hand1=")) {
				hand1 = l;
			}
		}
		try {
			Match.load(good.replace(hand1, "hand1=" + hand0.substring(6)), new Random());
			fail("accepted a duplicated card");
		} catch (IllegalArgumentException expected) {
		}
	}
}
