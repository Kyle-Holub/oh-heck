package ohheck.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class CardTest {

	@Test
	public void everyCardHasItsOwnIndexAndComesBackFromIt() {
		Set<Integer> seen = new HashSet<Integer>();
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				Card c = Card.of(r, s);
				seen.add(c.index());
				assertSame(c, Card.ofIndex(c.index()));
			}
		}
		assertEquals(Card.COUNT, seen.size());
	}
}
