package ohheck.rules;

import static ohheck.rules.TestCards.cards;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class HandSortTest {

	private static List<Card> sorted(HandSort sort, Suit trump, String hand) {
		List<Card> cards = new ArrayList<Card>(cards(hand));
		cards.sort(sort.order(trump));
		return cards;
	}

	@Test
	public void bySuitTrumpsComeFirstThenTheColoursAlternate() {
		assertEquals(cards("3H JH 2S 9S 4D KC AC"), sorted(HandSort.SUIT, Suit.HEARTS, "KC 2S JH 4D AC 9S 3H"));
	}

	@Test
	public void byRankTrumpsComeFirstThenLowToHigh() {
		assertEquals(cards("JH 3H AC KC 9S 4D 2S"), sorted(HandSort.RANK, Suit.HEARTS, "KC 2S JH 4D AC 9S 3H"));
	}

	@Test
	public void theSettingCyclesThroughEveryOrder() {
		assertEquals(HandSort.RANK, HandSort.SUIT.next());
		assertEquals(HandSort.SUIT, HandSort.RANK.next());
	}
}
