package ohheck.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SeatingTest {

	@Test
	public void readsBackWhatItWrites() {
		Seating s = Seating.of(4, 10, 1);
		assertEquals("4,10,1", s.encode());
		assertEquals(s, Seating.parse(s.encode()));
		assertEquals(10, s.idAt(2));
		assertEquals(Player.byId(1), s.at(3));
	}

	@Test
	public void onlyOneIdPerComputerSeat() {
		assertNull(Seating.parse("4,10"));
		assertNull(Seating.parse("4,x,1"));
		try {
			Seating.of(1, 2);
			assertTrue("two ids for three seats", false);
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void yourSeatHasNoComputerPlayer() {
		Seating.of(4, 10, 1).idAt(Seating.YOU);
	}

	@Test
	public void knowsWhenSomeoneHasLeftTheRoster() {
		assertTrue(Seating.of(4, 10, 1).onRoster());
		assertFalse(Seating.of(4, 99, 1).onRoster());
	}
}
