package ohheck.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class NumbersTest {

	@Test
	public void numbersAreTextAndKept() {
		assertEquals("42", Numbers.text(42));
		assertEquals("-5", Numbers.text(-5));
		assertSame("the same String each time", Numbers.text(75), Numbers.text(75));
		assertEquals("+14", Numbers.signed(14));
		assertEquals("0", Numbers.signed(0));
		assertEquals("-10", Numbers.signed(-10));
		assertEquals("123456", Numbers.text(123456));
	}
}
