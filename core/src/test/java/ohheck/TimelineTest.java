package ohheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimelineTest {

	@Test
	public void aLoopingSceneHappensAgainAfterItsPause() {
		Timeline tl = new Timeline();
		tl.restart();
		int fired = 0;
		for (int i = 0; i < 60; i++) { // 6 s of a 2 s scene with a 1 s pause: twice round, and into a third
			tl.advance(0.1f, 2f, true, 1f);
			if (tl.passed(0.5f)) {
				fired++;
			}
		}
		assertEquals(2, fired);
		assertEquals(6f, tl.shown(), 1e-4);
	}

	@Test
	public void aOnceOnlySceneHoldsItsEnd() {
		Timeline tl = new Timeline();
		tl.advance(5f, 2f, false, 1f);
		assertEquals(2f, tl.t(), 0);
		assertTrue(tl.reached(2f));
		assertEquals(1f, tl.at(0, 1), 0);
	}

	@Test
	public void easesAndCounts() {
		Timeline tl = new Timeline();
		tl.advance(1f, 10f, false, 0);
		assertEquals(0f, tl.at(2, 3), 0);
		assertEquals(0.5f, tl.at(0.5f, 1.5f), 1e-6);
		assertEquals(2, tl.count(0.5f, 1f, 1.5f));
		assertFalse(tl.reached(1.01f));
	}
}
