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

	@Test
	public void everyModePlaysToTheEnd() {
		Bot bot = Bot.of(Difficulty.NORMAL, new java.util.Random(1));
		for (GameMode mode : GameMode.values()) {
			RuleSet rules = mode.rules(RuleSet.STANDARD.withOrder(HandOrder.DOWN));
			Match m = Match.random(4, new java.util.Random(4), rules);
			int hands = 0;
			while (m.phase() != Match.Phase.GAME_OVER) {
				if (m.phase() == Match.Phase.BIDDING) {
					assertEquals(rules.handSize(hands), m.handSize());
					assertEquals(m.handSize() == 13, m.trumpCard() == null);
					m.placeBid(m.currentSeat(), bot.chooseBid(m.view(m.currentSeat())));
				} else if (m.phase() == Match.Phase.PLAYING) {
					m.playCard(m.currentSeat(), bot.choosePlay(m.view(m.currentSeat())));
				} else {
					hands++;
					m.nextHand();
				}
			}
			assertEquals(mode.label, rules.numberOfHands(), hands);
		}
	}

	@Test
	public void theModesDealTheirHands() {
		assertEquals(8, GameMode.CLASSIC.rules(null).numberOfHands());
		RuleSet marathon = GameMode.MARATHON.rules(null);
		assertEquals(25, marathon.numberOfHands());
		assertEquals(1, marathon.handSize(0));
		assertEquals(13, marathon.handSize(12));
		assertEquals(12, marathon.handSize(13));
		assertEquals(1, marathon.handSize(24));
		assertEquals("1 to 13 to 1", marathon.order.label(13));
		assertEquals("8 to 1", HandOrder.DOWN.label(8));
	}

	@Test
	public void bigBidsDoubleWithEachTrick() {
		RuleSet big = GameMode.HIGH_STAKES.rules(null);
		int[] made = { 8, 12, 16, 24, 40, 72 };
		for (int b = 0; b < made.length; b++) {
			assertEquals("bid " + b, made[b], big.score(b, b));
		}
		assertEquals(-5, big.score(2, 3));
		assertEquals(-10, big.score(0, 2));
	}

	@Test
	public void customRulesAreNamedByTheirCode() {
		RuleSet r = new RuleSet(HandOrder.UP_AND_DOWN, 8, true, true);
		assertEquals(r, RuleSet.parse(r.code()));
		assertEquals(GameMode.CUSTOM, GameMode.of(r));
		assertEquals(GameMode.MARATHON, GameMode.of(RuleSet.parse("UP_AND_DOWN/13")));
		assertEquals(GameMode.CLASSIC, GameMode.of(RuleSet.STANDARD));
		assertEquals(GameMode.POKER_FACE, GameMode.of(RuleSet.parse("UP/8/blind")));
		assertEquals("UP/8/big/blind", RuleSet.STANDARD.withBlindBids(true).withBigBids(true).code());
		for (String bad : new String[] { "UP/8/blind/big", "UP/8/big/big", "UP/8/huge", "UP" }) {
			try {
				RuleSet.parse(bad);
				org.junit.Assert.fail(bad);
			} catch (IllegalArgumentException expected) {
			}
		}
	}
}
