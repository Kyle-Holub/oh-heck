package ohheck.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import ohheck.players.Bot;
import ohheck.players.Difficulty;

public class SaveTest {

	private final Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));

	@Test
	public void roundTripsAtEveryStep() {
		for (long seed = 0; seed < 400; seed++) {
			Match m = Match.random(4, new Random(seed));
			while (m.phase() != Match.Phase.GAME_OVER) {
				String saved = m.save();
				Match r = Match.load(saved, new Random(99));
				assertEquals(saved, r.save());
				if (m.phase() == Match.Phase.HAND_OVER) {
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
			m.placeBid(s, bot.chooseBid(m.view(s)));
		} else {
			m.playCard(s, bot.choosePlay(m.view(s)));
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
		String[] bad = { "", "hello", good.replace("oh-heck-match-3", "something-else"),
				good.substring(0, good.length() / 2), good.replaceFirst("hand=0", "hand=12"),
				good.replaceFirst("players=4", "players=x") };
		for (String b : bad) {
			try {
				Match.load(b, new Random());
				fail("accepted: " + b);
			} catch (IllegalArgumentException expected) {
			}
		}
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

	private static final String FORMAT_1 = "oh-heck-match-1\n" + "players=4\n" + "phase=PLAYING\n" + "hand=2\n" + "dealer=0\n" + "current=3\n" + "trump=40\n" + "hand0=45,29,49\n" + "hand1=33,4\n" + "hand2=39,47\n" + "hand3=46,31,0\n" + "bids=2,0,2,2\n" + "tricks=0,0,0,0\n" + "scores=5,7,20,20\n" + "points=-5,12,10,10\n" + "trick=17,5\n" + "trickSeats=1,2\n" + "last=23,12,21,16\n" + "lastSeats=0,1,2,3\n" + "lastWinner=1\n";

	@Test
	public void cardsAreWrittenAsText() {
		String s = Match.random(4, new Random(8)).save();
		for (String line : s.split("\n")) {
			if (line.startsWith("hand0=")) {
				assertTrue(line, line.substring(6).matches("((10|[2-9JQKA])[CDHS],?)+"));
			}
		}
	}

	@Test
	public void savesItsRules() {
		Match m = new Match(4, new Random(5), 0);
		String saved = m.save();
		assertTrue(saved.contains("rules=UP/8"));
		assertEquals(RuleSet.STANDARD, Match.load(saved, new Random(1)).rules());
		try {
			Match.load(saved.replace("rules=UP/8", "rules=madeup"), new Random(1));
			fail("a game by rules this build doesn't know");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void aHandWithNoTrumpReadsBack() {
		RuleSet rules = new RuleSet(HandOrder.DOWN, RuleSet.MOST_CARDS, true);
		Match m = new Match(4, new Random(5), 0, rules);
		assertNull(m.trumpCard());
		Match back = Match.load(m.save(), new Random(1));
		assertNull(back.trumpCard());
		assertEquals(rules, back.rules());
		assertEquals(m.save(), back.save());
	}
}
