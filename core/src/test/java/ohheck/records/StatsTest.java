package ohheck.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ohheck.players.Difficulty;
import ohheck.rules.Rules;

public class StatsTest {

	@Test
	public void handsCountForTheirLevelAndForAll() {
		Stats s = new Stats();
		s.startGame();
		s.countHand(0, Difficulty.HARD, 1, 1, Rules.score(1, 1)); // made
		s.countHand(1, Difficulty.HARD, 0, 0, Rules.score(0, 0)); // made zero
		s.countHand(2, Difficulty.HARD, 2, 3, Rules.score(2, 3)); // over
		s.countHand(3, Difficulty.HARD, 3, 1, Rules.score(3, 1)); // under
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
		assertTrue(s.countHand(0, Difficulty.NORMAL, 1, 1, Rules.score(1, 1)));
		Stats back = Stats.decode(s.encode());
		assertFalse(back.countHand(0, Difficulty.NORMAL, 1, 1, Rules.score(1, 1)));
		assertEquals(1, back.totals(Stats.ALL).hands);
		assertTrue(back.countHand(1, Difficulty.NORMAL, 0, 0, Rules.score(0, 0)));
	}

	@Test
	public void gamesWinsStreaksAndNemesis() {
		Stats s = new Stats();
		int[][] games = { { 50, 40, 30, 20 }, { 60, 10, 10, 60 }, { 20, 30, 10, 40 }, { 70, 0, 0, 0 } };
		for (int[] g : games) {
			s.startGame();
			for (int h = 0; h < 8; h++) {
				s.countHand(h, Difficulty.EASY, 1, 1, Rules.score(1, 1));
			}
			assertTrue(s.countGame(Difficulty.EASY, g, 8, new int[] { 4, 6, 9 }));
			assertFalse(s.countGame(Difficulty.EASY, g, 8, new int[] { 4, 6, 9 }));
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
		assertEquals(4, t.nemesis()); // players 4 and 9 each finished above you once; the first wins a tie
		assertEquals(4, Stats.decode(s.encode()).totals(Stats.ALL).nemesis());
	}

	@Test
	public void aMissedBidSpoilsAPerfectGame() {
		Stats s = new Stats();
		s.startGame();
		for (int h = 0; h < 8; h++) {
			s.countHand(h, Difficulty.NORMAL, 1, h == 4 ? 0 : 1, Rules.score(1, h == 4 ? 0 : 1));
		}
		s.countGame(Difficulty.NORMAL, new int[] { 80, 0, 0, 0 }, 8, new int[] { 0, 1, 2 });
		assertEquals(0, s.totals(Stats.ALL).perfect);
	}

	@Test
	public void unreadableStatsStartAfresh() {
		assertEquals(0, Stats.decode("nonsense").totals(Stats.ALL).games);
		assertEquals(0, Stats.decode("").totals(Stats.ALL).hands);
	}

	@Test
	public void savedByNameAndReadBack() {
		Stats s = new Stats();
		s.startGame();
		s.countHand(0, Difficulty.HARD, 2, 2, Rules.score(2, 2));
		s.countGame(Difficulty.HARD, new int[] { 10, 30, 0, 0 }, 1, new int[] { 9, 4, 1 });
		String saved = s.encode();
		assertTrue(saved.contains("hard.games=1"));
		assertTrue(saved.contains("all.beatenBy.9=1"));
		Stats back = Stats.decode(saved);
		assertEquals(1, back.totals(Difficulty.HARD.ordinal()).made);
		assertEquals(9, back.totals(Stats.ALL).nemesis());
		assertEquals(saved, back.encode());
		// a number this build doesn't know (from a newer one) is passed over
		assertEquals(1, Stats.decode(saved + "all.someday=5\n").totals(Stats.ALL).games);
	}

	@Test
	public void readsTheFirstBuildsNumberedLines() {
		StringBuilder line = new StringBuilder("3,2,1,2,0,150,60,40,24,15,4,3,5,4,30,20,-10");
		for (int id = 0; id < 12; id++) {
			line.append(',').append(id == 7 ? 2 : 0); // Oswald (7) beat you twice
		}
		Stats s = Stats.decode("game=0,0,1\n3=" + line + "\n");
		Stats.Totals t = s.totals(Stats.ALL);
		assertEquals(3, t.games);
		assertEquals(150 / 3.0, t.averageFinal(), 1e-9);
		assertEquals(-10, t.worstHand);
		assertEquals(7, t.nemesis());
		// the very first, with three counts by seat: the counts are dropped, the rest kept
		Stats seats = Stats.decode("game=0,0,1\n3=3,2,1,2,0,150,60,40,24,15,4,3,5,4,30,20,-10,1,0,0\n");
		assertEquals(3, seats.totals(Stats.ALL).games);
		assertEquals(-1, seats.totals(Stats.ALL).nemesis());
	}

	/** Every number in Totals is saved, and comes back. */
	@Test
	public void everyStatisticIsSavedAndReadBack() throws Exception {
		java.util.List<String> saved = Stats.fieldNames();
		for (java.lang.reflect.Field f : Stats.Totals.class.getDeclaredFields()) {
			if (f.getType() == int.class || f.getType() == long.class) {
				assertTrue(f.getName() + " isn't saved", saved.contains(f.getName()));
			}
		}
		Stats s = new Stats();
		long n = 1;
		for (java.lang.reflect.Field f : Stats.Totals.class.getDeclaredFields()) {
			f.setAccessible(true);
			if (f.getType() == int.class) {
				f.setInt(s.totals(Difficulty.HARD.ordinal()), (int) n++);
			} else if (f.getType() == long.class) {
				f.setLong(s.totals(Difficulty.HARD.ordinal()), n++);
			}
		}
		Stats back = Stats.decode(s.encode());
		for (java.lang.reflect.Field f : Stats.Totals.class.getDeclaredFields()) {
			if (f.getType() == int.class || f.getType() == long.class) {
				assertEquals(f.getName(), f.get(s.totals(Difficulty.HARD.ordinal())), f.get(back.totals(Difficulty.HARD.ordinal())));
			}
		}
	}
}
