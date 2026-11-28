package ohheck.records;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.junit.Test;

import ohheck.campaign.Chapter;
import ohheck.campaign.Stage;
import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;

public class SavedGameTest {

	@Test
	public void theTableTravelsWithTheGame() {
		Match m = new Match(4, new Random(3), 1, RuleSet.STANDARD.withOrder(HandOrder.DOWN));
		String saved = new SavedGame(m, Difficulty.HARD, Seating.of(8, 9, 5), null, null).encode();
		SavedGame back = SavedGame.decode(saved, new Random(1));
		assertEquals(Difficulty.HARD, back.level());
		assertEquals(Seating.of(8, 9, 5), back.players());
		assertEquals(m.save(), back.match().save());
	}

	@Test
	public void aCampaignGameKeepsItsChapterAndStage() {
		Match m = new Match(2, new Random(3), 1, Chapter.ONE.rules(Stage.DUEL));
		SavedGame back = SavedGame.decode(new SavedGame(m, Difficulty.NORMAL, Chapter.ONE.seating(Stage.DUEL), Chapter.ONE, Stage.DUEL).encode(),
				new Random(1));
		assertEquals(Chapter.ONE, back.chapter());
		assertEquals(Stage.DUEL, back.stage());
		assertEquals(m.save(), back.match().save());
	}

	@Test
	public void aCustomGameHasNoChapter() {
		SavedGame back = SavedGame.decode(new SavedGame(new Match(4, new Random(3), 1), Difficulty.EASY, Seating.of(0, 1, 2), null, null).encode(),
				new Random(1));
		assertNull(back.chapter());
		assertNull(back.stage());
	}

	@Test(expected = IllegalArgumentException.class)
	public void aSaveWithoutItsTableIsUnreadable() {
		SavedGame.decode(new Match(4, new Random(3), 1).save(), new Random(1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void aPlayerNoLongerOnTheRosterMakesItUnreadable() {
		SavedGame.decode(new SavedGame(new Match(4, new Random(3), 1), Difficulty.NORMAL, Seating.of(4, 99, 1), null, null).encode(),
				new Random(1));
	}
}
