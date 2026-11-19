package ohheck.anim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FixedStepsTest {

	@Test
	public void framesBecomeWholeSteps() {
		FixedSteps s = new FixedSteps(1 / 60f, 0.1f);
		assertEquals(1, s.frame(1 / 60f));
		assertEquals("a tenth of a second is six steps", 6, s.frame(0.1f));
		assertEquals("a long stall counts as a tenth", 6, s.frame(2f));
	}

	@Test
	public void shortFramesAddUp() {
		FixedSteps s = new FixedSteps(1 / 60f, 0.1f);
		int total = 0;
		for (int i = 0; i < 120; i++) {
			total += s.frame(1 / 120f); // a 120 Hz screen: a step every other frame
		}
		assertEquals(60, total);
	}
}
