package ohheck.rules;

import static ohheck.rules.TestCards.c;
import static ohheck.rules.TestCards.cards;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import ohheck.players.Bot;
import ohheck.players.Difficulty;

public class RulesTest {

	@Test
	public void deckHas52DistinctCards() {
		Deck d = Deck.shuffled(new Random(1));
		Set<Card> seen = new HashSet<Card>();
		while (d.size() > 0) {
			seen.add(d.deal());
		}
		assertEquals(52, seen.size());
	}

	@Test
	public void sameSeedSameDeal() {
		assertEquals(Deck.shuffled(new Random(42)).deal(10), Deck.shuffled(new Random(42)).deal(10));
	}

	@Test(expected = IllegalStateException.class)
	public void dealingFromEmptyDeckFails() {
		Deck d = new Deck();
		d.deal(53);
	}

	@Test
	public void cardsAreCanonical() {
		assertTrue(Card.of(Rank.ACE, Suit.SPADES) == Card.of(Rank.ACE, Suit.SPADES));
		assertEquals("ACE of SPADES", c("AS").toString());
	}

	@Test
	public void eightHandsOfOneToEightCardsEitherWay() {
		assertEquals(8, RuleSet.STANDARD.numberOfHands());
		for (int i = 0; i < 8; i++) {
			assertEquals(i + 1, HandOrder.UP.handSize(i, 8));
			assertEquals(8 - i, HandOrder.DOWN.handSize(i, 8));
		}
	}

	@Test
	public void highestOfLeadSuitWins() {
		assertEquals(2, RuleSet.STANDARD.trickWinner(cards("5H 9H KH 2H"), Suit.CLUBS));
	}

	@Test
	public void aceIsHigh() {
		assertEquals(1, RuleSet.STANDARD.trickWinner(cards("KH AH"), Suit.CLUBS));
	}

	@Test
	public void offSuitCardNeverWins() {
		assertEquals(0, RuleSet.STANDARD.trickWinner(cards("2H AS AD AC"), Suit.HEARTS));
		assertEquals(0, RuleSet.STANDARD.trickWinner(cards("2H AS AD"), Suit.CLUBS));
	}

	@Test
	public void trumpBeatsLeadSuit() {
		assertEquals(3, RuleSet.STANDARD.trickWinner(cards("AH KH QH 2C"), Suit.CLUBS));
	}

	@Test
	public void higherTrumpBeatsLowerTrump() {
		assertEquals(2, RuleSet.STANDARD.trickWinner(cards("AH 3C 9C 4C"), Suit.CLUBS));
	}

	@Test
	public void trumpLeadIsJustTheLeadSuit() {
		assertEquals(1, RuleSet.STANDARD.trickWinner(cards("5C 7C AH"), Suit.CLUBS));
	}

	@Test
	public void mustFollowSuitWhenAble() {
		List<Card> hand = cards("2H KS 9H 4D");
		assertEquals(cards("2H 9H"), RuleSet.STANDARD.legalPlays(hand, Suit.HEARTS));
		assertFalse(RuleSet.STANDARD.isLegalPlay(hand, c("KS"), Suit.HEARTS));
	}

	@Test
	public void anyCardWhenVoidInLeadSuit() {
		List<Card> hand = cards("2H KS 4D");
		assertEquals(hand, RuleSet.STANDARD.legalPlays(hand, Suit.CLUBS));
	}

	@Test
	public void anyCardWhenLeading() {
		List<Card> hand = cards("2H KS 4D");
		assertEquals(hand, RuleSet.STANDARD.legalPlays(hand, null));
	}

	@Test
	public void cardNotInHandIsNeverLegal() {
		assertFalse(RuleSet.STANDARD.isLegalPlay(cards("2H"), c("3H"), null));
	}

	@Test
	public void dealerCannotMakeTotalEqualTricks() {
		assertFalse(RuleSet.STANDARD.isLegalBid(2, 5, 3, true));
		assertTrue(RuleSet.STANDARD.isLegalBid(1, 5, 3, true));
		assertTrue(RuleSet.STANDARD.isLegalBid(3, 5, 3, true));
	}

	@Test
	public void nonDealerMayBidAnythingInRange() {
		for (int b = 0; b <= 5; b++) {
			assertTrue(RuleSet.STANDARD.isLegalBid(b, 5, 3, false));
		}
		assertFalse(RuleSet.STANDARD.isLegalBid(6, 5, 0, false));
		assertFalse(RuleSet.STANDARD.isLegalBid(-1, 5, 0, false));
	}

	@Test
	public void dealerUnrestrictedWhenOthersOverbid() {
		assertEquals(-2, RuleSet.STANDARD.forbiddenDealerBid(3, 5));
		for (int b = 0; b <= 3; b++) {
			assertTrue(RuleSet.STANDARD.isLegalBid(b, 3, 5, true));
		}
	}

	@Test
	public void makingBidScoresTenPlusTwoPerTrick() {
		assertEquals(10, RuleSet.STANDARD.score(0, 0));
		assertEquals(16, RuleSet.STANDARD.score(3, 3));
		assertEquals(26, RuleSet.STANDARD.score(8, 8));
	}

	@Test
	public void missingBidLosesFivePerTrickOff() {
		assertEquals(-5, RuleSet.STANDARD.score(2, 3));
		assertEquals(-5, RuleSet.STANDARD.score(2, 1));
		assertEquals(-15, RuleSet.STANDARD.score(0, 3));
	}

	@Test
	public void cardListHelperWorks() {
		assertEquals(Arrays.asList(c("10H"), c("JC")), cards("10H JC"));
	}

	private static final RuleSet DOUBLE_SHORT = new RuleSet("double-short") {
		@Override
		public int numberOfHands() {
			return 5;
		}

		@Override
		public int made() {
			return 20;
		}
	};

	@Test
	public void aVariantPlaysByItsOwnRules() {
		Match m = Match.random(4, new java.util.Random(4), HandOrder.DOWN, DOUBLE_SHORT);
		assertEquals(5, m.handSize());
		assertEquals(24, m.rules().score(2, 2));
		Bot bot = Bot.of(Difficulty.NORMAL, new java.util.Random(1));
		int hands = 0;
		while (m.phase() != Match.Phase.GAME_OVER) {
			if (m.phase() == Match.Phase.BIDDING) {
				m.placeBid(m.currentSeat(), bot.chooseBid(m.view(m.currentSeat())));
			} else if (m.phase() == Match.Phase.PLAYING) {
				m.playCard(m.currentSeat(), bot.choosePlay(m.view(m.currentSeat())));
			} else {
				hands++;
				m.nextHand();
			}
		}
		assertEquals(5, hands);
		assertEquals("1 to 8", HandOrder.UP.label());
		assertEquals("8 to 1", HandOrder.DOWN.label());
	}
}
