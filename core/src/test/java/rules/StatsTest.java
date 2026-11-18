package rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StatsTest {

	@Test
	public void handsCountForTheirLevelAndForAll() {
		Stats s = new Stats();
		s.startGame();
		s.countHand(0, Difficulty.HARD, 1, 1); // made
		s.countHand(1, Difficulty.HARD, 0, 0); // made zero
		s.countHand(2, Difficulty.HARD, 2, 3); // over
		s.countHand(3, Difficulty.HARD, 3, 1); // under
		for (int which : new int[] { Difficulty.HARD.ordinal(), Stats.ALL }) {
			Stats.Totals t = s.totals(which);
			assertEquals(4, t.hands);
			assertEquals(2, t.made);
			assertEquals(1, t.zeroBids);
			assertEquals(1, t.zeroMade);
			assertEquals(1, t.over);
			assertEquals(1, t.under);
			assertEquals(5, t.tricks);
			assertEquals(Math.max(Rules.score(1, 1), Rules.score(0, 0)), t.bestHand);
			assertEquals(Math.min(Math.min(Rules.score(1, 1), Rules.score(0, 0)), Math.min(Rules.score(2, 3), Rules.score(3, 1))),
					t.worstHand);
		}
		assertEquals(0, s.totals(Difficulty.EASY.ordinal()).hands);
	}

	@Test
	public void aResumedHandIsNotCountedTwice() {
		Stats s = new Stats();
		s.startGame();
		assertTrue(s.countHand(0, Difficulty.NORMAL, 1, 1));
		Stats back = Stats.decode(s.encode());
		assertFalse(back.countHand(0, Difficulty.NORMAL, 1, 1));
		assertEquals(1, back.totals(Stats.ALL).hands);
		assertTrue(back.countHand(1, Difficulty.NORMAL, 0, 0));
	}

	@Test
	public void gamesWinsStreaksAndNemesis() {
		Stats s = new Stats();
		int[][] games = { { 50, 40, 30, 20 }, { 60, 10, 10, 60 }, { 20, 30, 10, 40 }, { 70, 0, 0, 0 } };
		for (int[] g : games) {
			s.startGame();
			for (int h = 0; h < 8; h++) {
				s.countHand(h, Difficulty.EASY, 1, 1);
			}
			assertTrue(s.countGame(Difficulty.EASY, g, 8));
			assertFalse(s.countGame(Difficulty.EASY, g, 8));
		}
		Stats.Totals t = s.totals(Stats.ALL);
		assertEquals(4, t.games);
		assertEquals(3, t.wins); // a tie for first is a win
		assertEquals(1, t.streak);
		assertEquals(2, t.bestStreak);
		assertEquals(70, t.bestFinal);
		assertEquals(20, t.worstFinal);
		assertEquals(50.0, t.averageFinal(), 1e-9);
		assertEquals(4, t.perfect);
		assertEquals(1, t.nemesis()); // Tom and Dan each finished above you once; the first named wins a tie
	}

	@Test
	public void aMissedBidSpoilsAPerfectGame() {
		Stats s = new Stats();
		s.startGame();
		for (int h = 0; h < 8; h++) {
			s.countHand(h, Difficulty.NORMAL, 1, h == 4 ? 0 : 1);
		}
		s.countGame(Difficulty.NORMAL, new int[] { 80, 0, 0, 0 }, 8);
		assertEquals(0, s.totals(Stats.ALL).perfect);
	}

	@Test
	public void unreadableStatsStartAfresh() {
		assertEquals(0, Stats.decode("nonsense").totals(Stats.ALL).games);
		assertEquals(0, Stats.decode("").totals(Stats.ALL).hands);
	}
}
