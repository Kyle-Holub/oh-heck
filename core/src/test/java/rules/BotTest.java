package rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/** The computer players at each difficulty: they play legally, and the harder ones score more. */
public class BotTest {

	/** Plays one game with the given level in each seat; returns the final scores. */
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

	/** Average score of seat 0 (the level under test) against three of another level, minus their average. */
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
		TableView v = new TableView(4);
		v.seat = 1;
		v.scores[0] = 50;
		v.scores[1] = 20;
		v.scores[2] = 70;
		v.scores[3] = 10;
		assertEquals("behind: the leader", 2, HardBot.rival(v));
		v.scores[1] = 90;
		assertEquals("ahead: second place", 2, HardBot.rival(v));
		v.scores[0] = 80;
		assertEquals(0, HardBot.rival(v));
	}

	@Test
	public void viewsRememberPlayedCardsAndVoids() {
		// play hand 1 out and into hand 2's first trick, then check what seat 0 knows
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
		for (boolean p : v.played) {
			played += p ? 1 : 0;
		}
		assertEquals("one trick of hand 2", 4, played);
		Suit lead = m.lastTrick().get(0).suit();
		for (int i = 1; i < 4; i++) {
			int seat = m.lastTrickSeats().get(i);
			boolean followed = m.lastTrick().get(i).suit() == lead;
			assertEquals(!followed, v.voids[seat][lead.ordinal()]);
		}
		assertFalse(v.played[TableView.index(m.hand(0).get(0))]);
		// and it survives saving
		TableView again = Match.load(m.save(), new Random()).view(0);
		assertTrue(java.util.Arrays.equals(v.played, again.played));
	}

	@Test
	public void bidChancesAddUp() {
		double[] d = NormalBot.trickDistribution(new double[] { 0.5, 0.5 });
		assertEquals(0.25, d[0], 1e-9);
		assertEquals(0.5, d[1], 1e-9);
		assertEquals(0.25, d[2], 1e-9);
	}
}
