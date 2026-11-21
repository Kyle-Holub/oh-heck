package ohheck.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import ohheck.rules.Match;

public class PlayerTest {

	@Test
	public void twelvePlayersFourAtEachLevel() {
		assertEquals(12, Player.ALL.size());
		int[] perLevel = new int[3];
		java.util.Set<Integer> ids = new java.util.HashSet<Integer>();
		for (Player p : Player.ALL) {
			assertTrue("ids are permanent and never shared", ids.add(p.id));
			assertEquals(p, Player.byId(p.id));
			perLevel[p.level.ordinal()]++;
		}
		assertEquals(4, perLevel[0]);
		assertEquals(4, perLevel[1]);
		assertEquals(4, perLevel[2]);
	}

	@Test
	public void aTableIsThreeDifferentPlayers() {
		Random rng = new Random(5);
		for (int i = 0; i < 500; i++) {
			int[] t = Player.draw(Difficulty.values()[i % 3], rng).ids();
			assertNotEquals(t[0], t[1]);
			assertNotEquals(t[0], t[2]);
			assertNotEquals(t[1], t[2]);
		}
	}

	@Test
	public void theSettingShiftsTheOdds() {
		Random rng = new Random(9);
		int[][] experts = new int[3][1];
		for (Difficulty d : Difficulty.values()) {
			for (int i = 0; i < 2000; i++) {
				for (int id : Player.draw(d, rng).ids()) {
					if (Player.byId(id).level == Difficulty.HARD) {
						experts[d.ordinal()][0]++;
					}
				}
			}
		}
		assertTrue(experts[0][0] < experts[1][0]);
		assertTrue(experts[1][0] < experts[2][0]);
	}

	@Test
	public void easyNeverSeatsAnExpertAndHardNeverABeginner() {
		Random rng = new Random(3);
		for (int i = 0; i < 2000; i++) {
			for (int id : Player.draw(Difficulty.EASY, rng).ids()) {
				assertNotEquals(Difficulty.HARD, Player.byId(id).level);
			}
			for (int id : Player.draw(Difficulty.HARD, rng).ids()) {
				assertNotEquals(Difficulty.EASY, Player.byId(id).level);
			}
		}
	}

	@Test
	public void everyonePlaysLegally() {
		for (Player p : Player.ALL) {
			Random rng = new Random(p.id);
			Bot[] bots = { p.bot(rng), p.bot(rng), p.bot(rng), p.bot(rng) };
			Match m = new Match(4, rng, 0);
			while (m.phase() != Match.Phase.GAME_OVER) {
				int s = m.currentSeat();
				if (m.phase() == Match.Phase.BIDDING) {
					m.placeBid(s, bots[s].chooseBid(m.view(s)));
				} else if (m.phase() == Match.Phase.PLAYING) {
					m.playCard(s, bots[s].choosePlay(m.view(s)));
				} else {
					m.nextHand();
				}
			}
		}
	}

	private static int[] levels(Seating table) {
		int[] count = new int[Difficulty.values().length];
		for (int id : table.ids()) {
			count[Player.byId(id).level.ordinal()]++;
		}
		return count;
	}

	@Test
	public void easySeatsTwoBeginnersAndSometimesASolidPlayer() {
		Random rng = new Random(11);
		int solid = 0, tables = 4000;
		for (int i = 0; i < tables; i++) {
			int[] count = levels(Player.draw(Difficulty.EASY, rng));
			assertTrue(count[Difficulty.EASY.ordinal()] >= 2);
			assertEquals(0, count[Difficulty.HARD.ordinal()]);
			solid += count[Difficulty.NORMAL.ordinal()];
		}
		assertEquals(0.4, (double) solid / tables, 0.03);
	}

	@Test
	public void normalSeatsTwoSolidPlayersAndAThirdOfAnyLevel() {
		Random rng = new Random(12);
		int easy = 0, hard = 0, tables = 4000;
		for (int i = 0; i < tables; i++) {
			int[] count = levels(Player.draw(Difficulty.NORMAL, rng));
			assertTrue(count[Difficulty.NORMAL.ordinal()] >= 2);
			easy += count[Difficulty.EASY.ordinal()];
			hard += count[Difficulty.HARD.ordinal()];
		}
		assertEquals(0.2, (double) easy / tables, 0.03);
		assertEquals(0.2, (double) hard / tables, 0.03);
	}

	@Test
	public void hardSeatsThreeExperts() {
		Random rng = new Random(13);
		for (int i = 0; i < 500; i++) {
			assertEquals(3, levels(Player.draw(Difficulty.HARD, rng))[Difficulty.HARD.ordinal()]);
		}
	}

	@Test
	public void theOddPlayerOutSitsInAnySeat() {
		Random rng = new Random(14);
		boolean[] seen = new boolean[Seating.SEATS];
		for (int i = 0; i < 500; i++) {
			Seating table = Player.draw(Difficulty.EASY, rng);
			for (int seat = 1; seat < Seating.SEATS; seat++) {
				seen[seat] |= table.at(seat).level == Difficulty.NORMAL;
			}
		}
		assertTrue(seen[1] && seen[2] && seen[3]);
	}
}
