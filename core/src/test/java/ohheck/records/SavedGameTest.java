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
		Match m = new Match(4, new Random(3), 1, RuleSet.STANDARD.withOrder(HandOrder.DOWN));
		String saved = new SavedGame(m, Difficulty.HARD, Seating.of(8, 9, 5)).encode();
		SavedGame back = SavedGame.decode(saved, new Random(1));
		assertEquals(Difficulty.HARD, back.level());
		assertEquals(Seating.of(8, 9, 5), back.players());
		assertEquals(m.save(), back.match().save());
	}

	@Test(expected = IllegalArgumentException.class)
	public void aSaveWithoutItsTableIsUnreadable() {
		SavedGame.decode(new Match(4, new Random(3), 1).save(), new Random(1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void aPlayerNoLongerOnTheRosterMakesItUnreadable() {
		SavedGame.decode(new SavedGame(new Match(4, new Random(3), 1), Difficulty.NORMAL, Seating.of(4, 99, 1)).encode(),
				new Random(1));
	}
}
