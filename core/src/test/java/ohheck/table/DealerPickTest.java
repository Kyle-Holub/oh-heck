package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DealerPickTest {

	@Test
	public void theLightStopsOnTheDealer() {
		for (int players : new int[] { 2, 4 }) {
			for (int dealer = 0; dealer < players; dealer++) {
				assertEquals(dealer, DealerPick.litSeat(DealerPick.LANDED, dealer, players));
				assertEquals(dealer, DealerPick.litSeat(DealerPick.LENGTH, dealer, players));
			}
		}
	}

	@Test
	public void theLightRunsRoundTheTableInOrder() {
		int before = DealerPick.litSeat(DealerPick.APPEAR, 1, 4), next = DealerPick.litSeat(DealerPick.APPEAR + 0.15f, 1, 4);
		assertEquals((before + 1) % 4, next);
		assertEquals("no seat is lit while the coin appears", -1, DealerPick.litSeat(DealerPick.APPEAR / 2, 1, 4));
		assertFalse(DealerPick.landed(1f));
		assertTrue(DealerPick.landed(DealerPick.LANDED));
	}
}
