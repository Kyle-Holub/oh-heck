package ohheck.records;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.junit.Test;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;

public class SavedGameTest {

	@Test
	public void theTableTravelsWithTheGame() {
		Match m = new Match(4, new Random(3), 1, HandOrder.DOWN, RuleSet.STANDARD);
		String saved = new SavedGame(m, Difficulty.HARD, Seating.of(8, 9, 5)).encode();
		SavedGame back = SavedGame.decode(saved, new Random(1), Difficulty.EASY, null);
		assertEquals(Difficulty.HARD, back.level());
		assertEquals(Seating.of(8, 9, 5), back.players());
		assertEquals(m.save(), back.match().save());
	}

	@Test
	public void olderSavesTakeTheTableFromTheirOldKeys() {
		String old = new Match(4, new Random(3), 1).save(); // just the match, as before
		SavedGame back = SavedGame.decode(old, new Random(1), Difficulty.NORMAL, Seating.of(4, 6, 1));
		assertEquals(Difficulty.NORMAL, back.level());
		assertEquals(Seating.of(4, 6, 1), back.players());
	}

	@Test
	public void aPlayerNoLongerOnTheRosterMeansANewTable() {
		Match m = new Match(4, new Random(3), 1);
		String saved = new SavedGame(m, Difficulty.NORMAL, Seating.of(4, 99, 1)).encode();
		assertNull(SavedGame.decode(saved, new Random(1), Difficulty.NORMAL, null).players());
	}
}
