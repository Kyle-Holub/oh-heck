package ohheck.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ohheck.players.Difficulty;
import ohheck.rules.GameMode;
import ohheck.rules.HandOrder;
import ohheck.rules.RuleSet;

public class HighScoresTest {

	@Test
	public void keepsTheBestTenInOrder() {
		HighScores h = new HighScores();
		for (int i = 0; i < 15; i++) {
			h.add(new HighScores.Entry(i * 10, 1, "2026-11-16", Difficulty.NORMAL, RuleSet.STANDARD));
		}
		assertEquals(10, h.entries().size());
		assertEquals(140, h.entries().get(0).score());
		assertEquals(50, h.entries().get(9).score());
		assertEquals(-1, h.add(new HighScores.Entry(20, 3, "2026-11-17", Difficulty.NORMAL, RuleSet.STANDARD)));
		assertEquals(0, h.add(new HighScores.Entry(200, 1, "2026-11-17", Difficulty.NORMAL, RuleSet.STANDARD)));
	}

	@Test
	public void earlierGameStaysAheadOnATie() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(50, 1, "first", Difficulty.NORMAL, RuleSet.STANDARD));
		assertEquals(1, h.add(new HighScores.Entry(50, 2, "second", Difficulty.NORMAL, RuleSet.STANDARD)));
		assertEquals("first", h.entries().get(0).date());
	}

	@Test
	public void roundTripsAndSkipsJunk() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(72, 1, "2026-11-16", Difficulty.NORMAL, RuleSet.STANDARD));
		h.add(new HighScores.Entry(-15, 4, "2026-11-15", Difficulty.NORMAL, RuleSet.STANDARD));
		HighScores back = HighScores.decode(h.encode() + ";garbage;1,2");
		assertEquals(h.encode(), back.encode());
		assertTrue(HighScores.decode("").entries().isEmpty());
		assertTrue(HighScores.decode(null).entries().isEmpty());
		assertTrue(h.encode().startsWith("score:72,place:1,date:2026-11-16,level:NORMAL"));
	}

	@Test
	public void eachModeKeepsItsOwnTen() {
		HighScores h = new HighScores();
		RuleSet marathon = GameMode.MARATHON.rules(null), custom = new RuleSet(HandOrder.DOWN, 8, true);
		for (int i = 0; i < 12; i++) {
			h.add(new HighScores.Entry(100 + i, 1, "d", Difficulty.EASY, RuleSet.STANDARD));
			h.add(new HighScores.Entry(300 + i, 1, "d", Difficulty.EASY, marathon));
		}
		assertTrue(h.add(new HighScores.Entry(5, 4, "d", Difficulty.HARD, custom)) >= 0);
		assertEquals(10, h.of(GameMode.CLASSIC).size());
		assertEquals(10, h.of(GameMode.MARATHON).size());
		assertEquals(1, h.of(GameMode.CUSTOM).size());
		HighScores back = HighScores.decode(h.encode());
		assertEquals(h.entries(), back.entries());
		assertEquals(custom, back.of(GameMode.CUSTOM).get(0).rules());
	}
}
