package ohheck.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Match;

public class RecordsTest {

	/** The device's storage, in memory. */
	static final class MemoryStore implements Records.Store {
		String stats = "", highScores = "", saved = "";

		public String stats() {
			return stats;
		}

		public void setStats(String encoded) {
			stats = encoded;
		}

		public String highScores() {
			return highScores;
		}

		public void setHighScores(String encoded) {
			highScores = encoded;
		}

		public boolean hasSavedGame() {
			return !saved.isEmpty();
		}

		public SavedGame savedGame(Random rng) {
			return saved.isEmpty() ? null : SavedGame.decode(saved, rng, Difficulty.NORMAL, null);
		}

		public void saveGame(SavedGame game) {
			saved = game.encode();
		}

		public void clearSavedGame() {
			saved = "";
		}
	}

	@Test
	public void keepsScoresAndGamesWhenPersisting() {
		MemoryStore store = new MemoryStore();
		Records r = new Records(store, true, null);
		assertEquals(0, r.recordHighScore(50, 1, Difficulty.HARD, "2026-11-18"));
		assertEquals(50, new Records(store, true, null).highScores().entries().get(0).score());

		Match m = Match.random(4, new Random(1));
		r.saveGame(new SavedGame(m, Difficulty.EASY, Seating.of(0, 1, 2)));
		assertTrue(r.hasSavedGame());
		assertEquals(Difficulty.EASY, r.savedGame(new Random(2)).level());
		r.clearSavedGame(); // the game finished
		assertFalse(r.hasSavedGame());
	}

	@Test
	public void writesNothingWhenNotPersisting() {
		MemoryStore store = new MemoryStore();
		Records r = new Records(store, false, null);
		assertEquals(-1, r.recordHighScore(50, 1, Difficulty.HARD, "2026-11-18"));
		r.stats().startGame();
		r.saveStats();
		r.saveGame(new SavedGame(Match.random(4, new Random(1)), Difficulty.EASY, Seating.of(0, 1, 2)));
		assertEquals("", store.stats);
		assertEquals("", store.highScores);
		assertFalse(r.hasSavedGame());
	}

	@Test
	public void throwsAwayAnUnreadableSave() {
		MemoryStore store = new MemoryStore();
		store.saved = "garbage";
		assertNull(new Records(store, true, null).savedGame(new Random()));
		assertEquals("", store.saved);
	}
}
