package ohheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HighScoresTest {

	@Test
	public void keepsTheBestTenInOrder() {
		HighScores h = new HighScores();
		for (int i = 0; i < 15; i++) {
			h.add(new HighScores.Entry(i * 10, 1, "2026-11-16", 1));
		}
		assertEquals(10, h.entries().size());
		assertEquals(140, h.entries().get(0).score);
		assertEquals(50, h.entries().get(9).score);
		assertEquals(-1, h.add(new HighScores.Entry(20, 3, "2026-11-17", 1)));
		assertEquals(0, h.add(new HighScores.Entry(200, 1, "2026-11-17", 1)));
	}

	@Test
	public void earlierGameStaysAheadOnATie() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(50, 1, "first", 1));
		assertEquals(1, h.add(new HighScores.Entry(50, 2, "second", 1)));
		assertEquals("first", h.entries().get(0).date);
	}

	@Test
	public void roundTripsAndSkipsJunk() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(72, 1, "2026-11-16", 1));
		h.add(new HighScores.Entry(-15, 4, "2026-11-15", 1));
		HighScores back = HighScores.decode(h.encode() + ";garbage;1,2");
		assertEquals(h.encode(), back.encode());
		assertTrue(HighScores.decode("").entries().isEmpty());
		assertTrue(HighScores.decode(null).entries().isEmpty());
		assertTrue(h.encode().startsWith("score:72,place:1,date:2026-11-16,level:NORMAL"));
		// entries saved before difficulty levels existed still load, with no level
		HighScores old = HighScores.decode("40,1,2026-11-01");
		assertEquals(40, old.entries().get(0).score);
		assertEquals(-1, old.entries().get(0).level);
		// and the numbered form with a level, from the first builds that had one
		HighScores numbered = HighScores.decode("55,2,2026-11-10,2");
		assertEquals(2, numbered.entries().get(0).level);
		assertEquals("score:55,place:2,date:2026-11-10,level:HARD", numbered.encode());
	}

	@Test
	public void ordinals() {
		assertEquals("1st", HighScores.ordinal(1));
		assertEquals("2nd", HighScores.ordinal(2));
		assertEquals("3rd", HighScores.ordinal(3));
		assertEquals("4th", HighScores.ordinal(4));
	}
}
