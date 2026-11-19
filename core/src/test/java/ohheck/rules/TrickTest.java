package ohheck.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrickTest {

	@Test
	public void theLastCardCompletesItAndNamesTheWinner() {
		Trick t = new Trick(3, RuleSet.STANDARD);
		assertNull(t.leadSuit());
		assertEquals(-1, t.add(2, Card.of(Rank.TEN, Suit.HEARTS), Suit.SPADES));
		assertFalse(t.revealsVoid(Card.of(Rank.TWO, Suit.HEARTS)));
		assertTrue(t.revealsVoid(Card.of(Rank.TWO, Suit.CLUBS)));
		assertEquals(-1, t.add(0, Card.of(Rank.ACE, Suit.HEARTS), Suit.SPADES));
		assertEquals(1, t.add(1, Card.of(Rank.TWO, Suit.SPADES), Suit.SPADES)); // trumped
		assertEquals(3, t.size()); // kept until cleared
		t.clear();
		assertTrue(t.isEmpty());
	}
}
