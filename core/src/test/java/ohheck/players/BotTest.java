package ohheck.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Rank;
import ohheck.rules.Suit;
import ohheck.rules.TableView;

public class BotTest {

	static int[] play(long seed, Difficulty... seats) {
		Bot[] bots = new Bot[4];
		for (int i = 0; i < 4; i++) {
			bots[i] = Bot.of(seats[i], new Random(seed * 7 + i));
		}
		Match m = Match.random(4, new Random(seed));
		while (m.phase() != Match.Phase.GAME_OVER) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				m.nextHand();
				continue;
			}
			int s = m.currentSeat();
			if (m.phase() == Match.Phase.BIDDING) {
				int b = bots[s].chooseBid(m.view(s));
				assertTrue(seats[s] + " bid " + b, m.isLegalBid(b));
				m.placeBid(s, b);
			} else {
				Card c = bots[s].choosePlay(m.view(s));
				assertTrue(seats[s] + " played " + c, m.legalPlays(s).contains(c));
				m.playCard(s, c);
			}
		}
		int[] scores = new int[4];
		for (int i = 0; i < 4; i++) {
			scores[i] = m.score(i);
		}
		return scores;
	}

	@Test
	public void everyLevelPlaysLegally() {
		for (long seed = 0; seed < 300; seed++) {
			play(seed, Difficulty.EASY, Difficulty.NORMAL, Difficulty.EASY, Difficulty.NORMAL);
		}
		for (long seed = 0; seed < 30; seed++) {
			play(seed, Difficulty.HARD, Difficulty.EASY, Difficulty.HARD, Difficulty.NORMAL);
		}
	}

	private static double edge(int games, Difficulty me, Difficulty others) {
		double mine = 0, theirs = 0;
		for (long seed = 0; seed < games; seed++) {
			int[] s = play(1000 + seed, me, others, others, others);
			mine += s[0];
			theirs += (s[1] + s[2] + s[3]) / 3.0;
		}
		return (mine - theirs) / games;
	}

	@Test
	public void normalBeatsEasy() {
		assertTrue(edge(400, Difficulty.NORMAL, Difficulty.EASY) > 5);
	}

	@Test
	public void hardBeatsNormal() {
		assertTrue(edge(60, Difficulty.HARD, Difficulty.NORMAL) > 10);
	}

	@Test
	public void sameSeedSamePlay() {
		assertTrue(java.util.Arrays.equals(play(5, Difficulty.HARD, Difficulty.EASY, Difficulty.NORMAL, Difficulty.EASY),
				play(5, Difficulty.HARD, Difficulty.EASY, Difficulty.NORMAL, Difficulty.EASY)));
	}

	@Test
	public void hardRacesTheLeaderOrSecondPlace() {
		TableView.Builder b = new TableView.Builder(4).seat(1);
		int[] scores = { 50, 20, 70, 10 };
		for (int s = 0; s < 4; s++) {
			b.seatState(s, Match.NO_BID, 0, scores[s], 0);
		}
		assertEquals("behind: the leader", 2, HardBot.rival(b.view()));
		b.seatState(1, Match.NO_BID, 0, 90, 0);
		assertEquals("ahead: second place", 2, HardBot.rival(b.view()));
		b.seatState(0, Match.NO_BID, 0, 80, 0);
		assertEquals(0, HardBot.rival(b.view()));
	}

	@Test
	public void viewsRememberPlayedCardsAndVoids() {
		Match m = Match.random(4, new Random(11));
		Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));
		while (m.handIndex() < 1 || m.phase() == Match.Phase.BIDDING) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				m.nextHand();
			} else if (m.phase() == Match.Phase.BIDDING) {
				m.placeBid(m.currentSeat(), bot.chooseBid(m.view(m.currentSeat())));
			} else {
				m.playCard(m.currentSeat(), bot.choosePlay(m.view(m.currentSeat())));
			}
		}
		for (int i = 0; i < 4; i++) {
			int s = m.currentSeat();
			m.playCard(s, bot.choosePlay(m.view(s)));
		}
		TableView v = m.view(0);
		int played = 0;
		for (int i = 0; i < 52; i++) {
			played += v.played(i) ? 1 : 0;
		}
		assertEquals("one trick of hand 2", 4, played);
		Suit lead = m.lastTrick().get(0).suit();
		for (int i = 1; i < 4; i++) {
			int seat = m.lastTrickSeats().get(i);
			boolean followed = m.lastTrick().get(i).suit() == lead;
			assertEquals(!followed, v.isVoid(seat, lead));
		}
		assertFalse(v.played(m.hand(0).get(0)));
		TableView again = Match.load(m.save(), new Random()).view(0);
		for (int i = 0; i < 52; i++) {
			assertEquals(v.played(i), again.played(i));
		}
	}

	@Test
	public void bidChancesAddUp() {
		double[] d = NormalBot.trickDistribution(new double[] { 0.5, 0.5 });
		assertEquals(0.25, d[0], 1e-9);
		assertEquals(0.5, d[1], 1e-9);
		assertEquals(0.25, d[2], 1e-9);
	}

	@Test
	public void theBidTableIsSane() {
		Match m = Match.random(4, new Random(3));
		for (int hand = 0; hand < 8; hand++) {
			TableView v = m.view(0);
			for (Suit s : Suit.values()) {
				for (Rank r : Rank.values()) {
					double p = BidTable.chance(v, Card.of(r, s));
					assertTrue(p >= 0 && p <= 1);
				}
			}
			assertEquals("the ace of trumps always wins", 1.0, BidTable.chance(v, Card.of(Rank.ACE, v.trump())), 1e-9);
			while (m.phase() != Match.Phase.HAND_OVER) {
				int s = m.currentSeat();
				Bot b = Bot.of(Difficulty.NORMAL, new Random(1));
				if (m.phase() == Match.Phase.BIDDING) {
					m.placeBid(s, b.chooseBid(m.view(s)));
				} else {
					m.playCard(s, b.choosePlay(m.view(s)));
				}
			}
			if (hand < 7) {
				m.nextHand();
			}
		}
	}
}
