package rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

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
			int[] t = Player.draw(Difficulty.values()[i % 3], rng);
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
				for (int id : Player.draw(d, rng)) {
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
			for (int id : Player.draw(Difficulty.EASY, rng)) {
				assertNotEquals(Difficulty.HARD, Player.byId(id).level);
			}
			for (int id : Player.draw(Difficulty.HARD, rng)) {
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
			while (m.phase() != Match.Phase.GAME_OVER) { // Match throws on anything illegal
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
}
