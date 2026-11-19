package ohheck.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScrollerTest {

	@Test
	public void staysWithinTheList() {
		Scroller s = new Scroller();
		s.drag(-50);
		s.update(0, true, 300);
		assertEquals("not above the top", 0, s.offset(), 0);
		s.drag(500);
		s.update(0, true, 300);
		assertEquals("not past the end", 300, s.offset(), 0);
		s.scrollTo(9999);
		s.update(0, true, 120);
		assertEquals(120, s.offset(), 0);
	}

	@Test
	public void aFlingCoastsAndSlows() {
		Scroller s = new Scroller();
		s.drag(10);
		s.update(0, true, 10000);
		float before = s.offset();
		s.update(0.1f, false, 10000);
		float first = s.offset() - before;
		s.update(0.1f, false, 10000);
		float second = s.offset() - before - first;
		assertTrue("coasts on", first > 0);
		assertTrue("and slows", second < first);
	}
}
