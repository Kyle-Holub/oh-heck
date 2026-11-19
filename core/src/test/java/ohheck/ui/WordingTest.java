package ohheck.ui;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import ohheck.players.Difficulty;
import ohheck.records.Stats;
import ohheck.rules.Suit;

public class WordingTest {

	@Test
	public void winnersReadRight() {
		assertEquals("You win the game!", Wording.winners(Arrays.asList("You"), true));
		assertEquals("Rupert wins the game!", Wording.winners(Arrays.asList("Rupert"), false));
		assertEquals("You and Rupert tie!", Wording.winners(Arrays.asList("You", "Rupert"), false));
		assertEquals("You, Rupert and Oswald tie!", Wording.winners(Arrays.asList("You", "Rupert", "Oswald"), false));
	}

	@Test
	public void hintsAndPlaces() {
		assertEquals("You must follow suit: play a heart", Wording.mustFollow(Suit.HEARTS));
		assertEquals("As dealer you can't bid 2: the bids would add up to 3", Wording.dealerCantBid(2, 3));
		assertEquals("1st", Wording.ordinal(1));
		assertEquals("2nd", Wording.ordinal(2));
		assertEquals("3rd", Wording.ordinal(3));
		assertEquals("4th", Wording.ordinal(4));
		assertEquals("Set!", Wording.handResult(false));
	}

	@Test
	public void statisticsWithNothingYetShowADash() {
		String[][] rows = Wording.gameRows(new Stats().totals(0));
		assertEquals("0", rows[0][1]);
		for (int i = 1; i < rows.length; i++) {
			assertEquals(rows[i][0], Wording.NONE, rows[i][1]);
		}
		assertEquals(Wording.NONE, Wording.handRows(new Stats().totals(0))[1][1]);
	}

	@Test
	public void theOptionsDescribeTheOddsTruly() {
		assertEquals("Mostly easy-going players, some solid players; never an expert.",
				Wording.tableDescription(Difficulty.EASY));
		assertEquals("Mostly solid players, some easy-going players, some experts.",
				Wording.tableDescription(Difficulty.NORMAL));
		assertEquals("Mostly experts, some solid players; never a beginner.", Wording.tableDescription(Difficulty.HARD));
	}
}
