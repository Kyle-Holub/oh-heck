package ohheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HighScoresTest {

	@Test
	public void keepsTheBestTenInOrder() {
		HighScores h = new HighScores();
		for (int i = 0; i < 15; i++) {
			h.add(new HighScores.Entry(i * 10, 1, "2026-11-16"));
		}
		assertEquals(10, h.entries().size());
		assertEquals(140, h.entries().get(0).score);
		assertEquals(50, h.entries().get(9).score);
		assertEquals(-1, h.add(new HighScores.Entry(20, 3, "2026-11-17")));
		assertEquals(0, h.add(new HighScores.Entry(200, 1, "2026-11-17")));
	}

	@Test
	public void earlierGameStaysAheadOnATie() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(50, 1, "first"));
		assertEquals(1, h.add(new HighScores.Entry(50, 2, "second")));
		assertEquals("first", h.entries().get(0).date);
	}

	@Test
	public void roundTripsAndSkipsJunk() {
		HighScores h = new HighScores();
		h.add(new HighScores.Entry(72, 1, "2026-11-16"));
		h.add(new HighScores.Entry(-15, 4, "2026-11-15"));
		HighScores back = HighScores.decode(h.encode() + ";garbage;1,2");
		assertEquals(h.encode(), back.encode());
		assertTrue(HighScores.decode("").entries().isEmpty());
		assertTrue(HighScores.decode(null).entries().isEmpty());
	}

	@Test
	public void ordinals() {
		assertEquals("1st", HighScores.ordinal(1));
		assertEquals("2nd", HighScores.ordinal(2));
		assertEquals("3rd", HighScores.ordinal(3));
		assertEquals("4th", HighScores.ordinal(4));
	}
}
