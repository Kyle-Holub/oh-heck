package rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static rules.TestCards.c;
import static rules.TestCards.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

/**
 * Pins the bot to the behaviour of the original ComputerPlayer, case by case.
 */
public class BotTest {

	private final Bot bot = new Bot();

	// ---- Bidding ----

	@Test
	public void bidsTrumpsPlusAces() {
		assertEquals(2, bot.chooseBid(cards("AS 2C 5H"), Suit.CLUBS, 0));
	}

	@Test
	public void trumpAceCountsOnce() {
		assertEquals(1, bot.chooseBid(cards("AC 5H"), Suit.CLUBS, 5));
	}

	@Test
	public void bidsOneLessToAvoidHook() {
		// 3 cards, 1 bid so far: bidding 2 would total 3
		assertEquals(1, bot.chooseBid(cards("AS 2C 5H"), Suit.CLUBS, 1));
	}

	@Test
	public void zeroBidGoesUpToAvoidHook() {
		assertEquals(1, bot.chooseBid(cards("2H"), Suit.CLUBS, 1));
	}

	// ---- Leading ----

	@Test
	public void leadsHighestNonTrumpWhenWantingTricks() {
		assertEquals(c("KH"), bot.choosePlay(cards("2C KH 9S"), none(), Suit.CLUBS, 1, 0));
	}

	@Test
	public void leadsHighestTrumpWhenOnlyTrumps() {
		assertEquals(c("9C"), bot.choosePlay(cards("2C 9C"), none(), Suit.CLUBS, 1, 0));
	}

	@Test
	public void leadsLowestTrumpWhenDoneTakingTricks() {
		assertEquals(c("2C"), bot.choosePlay(cards("KH 2C 9C"), none(), Suit.CLUBS, 0, 0));
	}

	@Test
	public void leadsLowestCardWhenDoneAndNoTrumps() {
		assertEquals(c("3D"), bot.choosePlay(cards("KH 9S 3D"), none(), Suit.CLUBS, 1, 1));
	}

	// ---- Following suit ----

	@Test
	public void playsHighestFollowerWhenItCanWin() {
		assertEquals(c("KH"), bot.choosePlay(cards("KH 2H 9S"), cards("5H"), Suit.CLUBS, 1, 0));
	}

	@Test
	public void savesHighCardWhenItCannotWin() {
		assertEquals(c("2H"), bot.choosePlay(cards("KH 2H"), cards("AH"), Suit.CLUBS, 1, 0));
	}

	@Test
	public void savesHighCardWhenTrickIsTrumped() {
		assertEquals(c("3H"), bot.choosePlay(cards("KH 3H"), cards("5H 2C"), Suit.CLUBS, 1, 0));
	}

	@Test
	public void onlyFollowerIsPlayedEvenIfItLoses() {
		assertEquals(c("KH"), bot.choosePlay(cards("KH 9S"), cards("AH"), Suit.CLUBS, 1, 0));
	}

	@Test
	public void ducksWithLowestFollowerWhenDone() {
		assertEquals(c("2H"), bot.choosePlay(cards("KH 2H 9H"), cards("5H"), Suit.CLUBS, 0, 0));
	}

	// ---- Void in the lead suit (original quirk: no deliberate trumping) ----

	@Test
	public void voidAndWantingTricksPlaysLowestCard() {
		assertEquals(c("3D"), bot.choosePlay(cards("9S 3D AC"), cards("5H"), Suit.CLUBS, 2, 0));
	}

	@Test
	public void voidAndDonePlaysHighestCardEvenATrump() {
		assertEquals(c("AC"), bot.choosePlay(cards("9S 3D AC"), cards("5H"), Suit.CLUBS, 0, 0));
	}

	// ---- Small cases ----

	@Test
	public void singleCardIsPlayed() {
		assertEquals(c("2D"), bot.choosePlay(cards("2D"), cards("AH"), Suit.CLUBS, 1, 0));
	}

	@Test
	public void tiesBreakLikeTheOriginal() {
		assertEquals(c("KS"), Bot.highest(cards("KS KD 2H")));
		assertEquals(c("3D"), Bot.lowest(cards("3S 3D KH")));
	}

	// ---- Never breaks the rules ----

	@Test
	public void alwaysPlaysALegalCard() {
		Random rng = new Random(7);
		for (int i = 0; i < 20000; i++) {
			List<Card> all = new ArrayList<Card>(new Deck().deal(52));
			Collections.shuffle(all, rng);
			int handSize = 1 + rng.nextInt(8);
			List<Card> hand = all.subList(0, handSize);
			List<Card> trick = all.subList(handSize, handSize + rng.nextInt(4));
			Suit trump = Suit.values()[rng.nextInt(4)];
			int bid = rng.nextInt(handSize + 1);
			int won = rng.nextInt(handSize + 1);
			Card played = bot.choosePlay(hand, trick, trump, bid, won);
			Suit lead = trick.isEmpty() ? null : trick.get(0).suit();
			assertTrue(played + " from " + hand + " on " + trick, Rules.isLegalPlay(hand, played, lead));
		}
	}

	@Test
	public void alwaysBidsInRangeAndNeverHooks() {
		Random rng = new Random(11);
		for (int i = 0; i < 20000; i++) {
			int handSize = 1 + rng.nextInt(8);
			List<Card> hand = Deck.shuffled(rng).deal(handSize);
			int bidsSoFar = rng.nextInt(3 * handSize + 1);
			int b = bot.chooseBid(hand, Suit.values()[rng.nextInt(4)], bidsSoFar);
			assertTrue(b >= 0 && b <= handSize);
			assertNotEquals(handSize - bidsSoFar, b);
		}
	}

	private static List<Card> none() {
		return Collections.emptyList();
	}
}
