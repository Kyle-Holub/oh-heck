package ohheck.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import ohheck.players.Bot;
import ohheck.players.Difficulty;

public class MatchTest {

	private final Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));

	@Test
	public void thousandsOfBotGamesFinishAndObeyTheRules() {
		for (long seed = 0; seed < 3000; seed++) {
			playWholeGame(new Random(seed));
		}
	}

	private void playWholeGame(Random rng) {
		Match m = Match.random(4, rng);
		int[] expectedScores = new int[4];
		int expectedDealer = m.dealer();

		for (int hand = 0; hand < 8; hand++) {
			assertEquals(hand, m.handIndex());
			assertEquals(Match.Phase.BIDDING, m.phase());
			assertEquals("deal moves clockwise", expectedDealer, m.dealer());

			Set<Card> seen = new HashSet<Card>();
			for (int s = 0; s < 4; s++) {
				assertEquals(hand + 1, m.hand(s).size());
				seen.addAll(m.hand(s));
				assertFalse(m.hasBid(s));
			}
			assertTrue(seen.add(m.trumpCard()));
			assertEquals(4 * (hand + 1) + 1, seen.size());

			for (int i = 1; i <= 4; i++) {
				int seat = (m.dealer() + i) % 4;
				assertEquals(seat, m.currentSeat());
				int b = bot.chooseBid(m.view(seat));
				assertTrue(m.isLegalBid(b));
				m.placeBid(seat, b);
			}
			assertTrue("dealer can't make bids add up", m.bidsSoFar() != hand + 1);
			assertEquals(Match.Phase.PLAYING, m.phase());
			assertEquals("left of dealer leads", (m.dealer() + 1) % 4, m.currentSeat());

			for (int t = 0; t <= hand; t++) {
				int leader = m.currentSeat();
				List<Card> plays = new ArrayList<Card>();
				int winner = -1;
				for (int i = 0; i < 4; i++) {
					int seat = m.currentSeat();
					assertEquals((leader + i) % 4, seat);
					Card c = bot.choosePlay(m.view(seat));
					assertTrue(m.legalPlays(seat).contains(c));
					plays.add(c);
					winner = m.playCard(seat, c);
					assertEquals(i < 3 ? -1 : winner, winner);
				}
				assertEquals(plays, m.lastTrick());
				assertEquals((leader + m.rules().trickWinner(plays, m.trump())) % 4, winner);
				if (t < hand) {
					assertEquals("winner leads next", winner, m.currentSeat());
				}
			}

			assertEquals(Match.Phase.HAND_OVER, m.phase());
			int totalTricks = 0;
			for (int s = 0; s < 4; s++) {
				totalTricks += m.tricksWon(s);
				assertTrue(m.hand(s).isEmpty());
				int pts = m.rules().score(m.bid(s), m.tricksWon(s));
				assertEquals(pts, m.lastHandPoints(s));
				assertEquals(m.bid(s) == m.tricksWon(s), m.madeBid(s));
				expectedScores[s] += pts;
				assertEquals(expectedScores[s], m.score(s));
			}
			assertEquals(hand + 1, totalTricks);

			m.nextHand();
			expectedDealer = (expectedDealer + 1) % 4;
		}
		assertEquals(Match.Phase.GAME_OVER, m.phase());
		assertFalse(m.leaders().isEmpty());
		for (int s : m.leaders()) {
			for (int o = 0; o < 4; o++) {
				assertTrue(m.score(s) >= m.score(o));
			}
		}
	}

	@Test
	public void sameSeedSameGame() {
		assertEquals(transcript(new Random(5)), transcript(new Random(5)));
	}

	private String transcript(Random rng) {
		Match m = Match.random(4, rng);
		StringBuilder sb = new StringBuilder();
		while (m.phase() != Match.Phase.GAME_OVER) {
			int s = m.currentSeat();
			switch (m.phase()) {
			case BIDDING:
				int b = bot.chooseBid(m.view(s));
				m.placeBid(s, b);
				sb.append(s).append(" bids ").append(b).append('\n');
				break;
			case PLAYING:
				Card c = bot.choosePlay(m.view(s));
				m.playCard(s, c);
				sb.append(s).append(" plays ").append(c).append('\n');
				break;
			default:
				m.nextHand();
			}
		}
		return sb.toString();
	}

	@Test
	public void refusesOutOfTurnBid() {
		Match m = new Match(4, new Random(1), 0);
		try {
			m.placeBid(2, 0);
			fail();
		} catch (IllegalStateException expected) {
		}
		assertEquals(1, m.currentSeat());
	}

	@Test
	public void refusesDealerHook() {
		Match m = new Match(4, new Random(1), 0);
		m.placeBid(1, 1);
		m.placeBid(2, 0);
		m.placeBid(3, 0);
		assertEquals(0, m.forbiddenBid());
		assertFalse(m.isLegalBid(0));
		try {
			m.placeBid(0, 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}
		m.placeBid(0, 1);
		assertEquals(Match.Phase.PLAYING, m.phase());
	}

	@Test
	public void refusesCardNotFollowingSuit() {
		for (long seed = 0; seed < 500; seed++) {
			Match m = new Match(4, new Random(seed), 0);
			bidAll(m);
			m.playCard(1, m.hand(1).get(0));
			m.playCard(2, m.hand(2).get(0));
			m.playCard(3, m.hand(3).get(0));
			m.playCard(0, m.hand(0).get(0));
			m.nextHand();
			bidAll(m);
			int leader = m.currentSeat();
			Card lead = m.hand(leader).get(0);
			m.playCard(leader, lead);
			int next = m.currentSeat();
			List<Card> hand = m.hand(next);
			if (hand.get(0).is(lead.suit()) != hand.get(1).is(lead.suit())) {
				Card offSuit = hand.get(0).is(lead.suit()) ? hand.get(1) : hand.get(0);
				try {
					m.playCard(next, offSuit);
					fail("allowed " + offSuit + " on a " + lead.suit() + " lead with " + hand);
				} catch (IllegalArgumentException expected) {
				}
				assertEquals(2, m.hand(next).size());
				return;
			}
		}
		fail("no suitable deal found");
	}

	@Test
	public void handSizeAndDealerProgress() {
		Match m = new Match(4, new Random(3), 2);
		assertEquals(1, m.handSize());
		assertEquals(3, m.currentSeat());
		bidAll(m);
		for (int i = 0; i < 4; i++) {
			int s = m.currentSeat();
			m.playCard(s, m.legalPlays(s).get(0));
		}
		m.nextHand();
		assertEquals(2, m.handSize());
		assertEquals(3, m.dealer());
		assertEquals(0, m.currentSeat());
	}

	@Test
	public void handsCanCountDownFromEight() {
		Match m = new Match(4, new Random(3), 2, HandOrder.DOWN, RuleSet.STANDARD);
		assertEquals(8, m.handSize());
		for (int s = 0; s < 4; s++) {
			assertEquals(8, m.hand(s).size());
		}
		bidAll(m);
		while (m.phase() == Match.Phase.PLAYING) {
			int s = m.currentSeat();
			m.playCard(s, m.legalPlays(s).get(0));
		}
		m.nextHand();
		assertEquals(7, m.handSize());
		assertEquals(7, m.hand(0).size());
	}

	@Test
	public void theHandOrderIsSaved() {
		Match m = new Match(4, new Random(3), 2, HandOrder.DOWN, RuleSet.STANDARD);
		Match back = Match.load(m.save(), new Random(1));
		assertEquals(HandOrder.DOWN, back.handOrder());
		assertEquals(8, back.handSize());
		String old = new Match(4, new Random(3), 2).save().replaceAll("order=UP\n", "");
		assertEquals(HandOrder.UP, Match.load(old, new Random(1)).handOrder());
	}

	private void bidAll(Match m) {
		while (m.phase() == Match.Phase.BIDDING) {
			int s = m.currentSeat();
			m.placeBid(s, bot.chooseBid(m.view(s)));
		}
	}

	@Test
	public void placesShareOnATie() {
		Match m = new Match(4, new Random(3), 0);
		m.scores[0] = 30;
		m.scores[1] = 50;
		m.scores[2] = 30;
		m.scores[3] = 10;
		assertEquals(1, m.placeOf(1));
		assertEquals(2, m.placeOf(0));
		assertEquals(2, m.placeOf(2));
		assertEquals(4, m.placeOf(3));
	}
}
