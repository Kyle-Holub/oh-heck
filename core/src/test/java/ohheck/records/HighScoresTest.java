package ohheck.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ohheck.players.Difficulty;

public class HighScoresTest {

	@Test
	public void keepsTheBestTenInOrder() {
		HighScores h = new HighScores();
		for (int i = 0; i < 15; i++) {
			h.add(new HighScores.Entry(i * 10, 1, "2026-11-16", Difficulty.NORMAL));
		}
		assertEquals(10, h.entries().size());
		assertEquals(140, h.entries().get(0).score());
		assertEquals(50, h.entries().get(9).score());
		assertEquals(-1, h.add(new HighScores.Entry(20, 3, "2026-11-17", Difficulty.NORMAL)));
		assertEquals(0, h.add(new HighScores.Entry(200, 1, "2026-11-17", Difficulty.NORMAL)));
	}

	@Test
	public void earlierGameStaysAheadOnATie() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(50, 1, "first", Difficulty.NORMAL));
		assertEquals(1, h.add(new HighScores.Entry(50, 2, "second", Difficulty.NORMAL)));
		assertEquals("first", h.entries().get(0).date());
	}

	@Test
	public void roundTripsAndSkipsJunk() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(72, 1, "2026-11-16", Difficulty.NORMAL));
		h.add(new HighScores.Entry(-15, 4, "2026-11-15", Difficulty.NORMAL));
		HighScores back = HighScores.decode(h.encode() + ";garbage;1,2");
		assertEquals(h.encode(), back.encode());
		assertTrue(HighScores.decode("").entries().isEmpty());
		assertTrue(HighScores.decode(null).entries().isEmpty());
		assertTrue(h.encode().startsWith("score:72,place:1,date:2026-11-16,level:NORMAL"));
		HighScores old = HighScores.decode("40,1,2026-11-01");
		assertEquals(40, old.entries().get(0).score());
		assertNull(old.entries().get(0).level());
		HighScores numbered = HighScores.decode("55,2,2026-11-10,2");
		assertEquals(Difficulty.HARD, numbered.entries().get(0).level());
		assertEquals("score:55,place:2,date:2026-11-10,level:HARD", numbered.encode());
		HighScores unknown = HighScores.decode("55,2,2026-11-10,7");
		assertNull(unknown.entries().get(0).level());
		assertEquals("score:55,place:2,date:2026-11-10,level:-", unknown.encode());
	}
}
