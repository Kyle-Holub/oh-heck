package ohheck.records;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import ohheck.players.Difficulty;
import ohheck.util.Log;

/**
 * What's kept between games: your statistics, high scores and the game in progress.
 * Written to a {@link Store} (the device's settings) only when persist is on; the
 * screenshot run keeps everything in memory. No libGDX, so it's tested in plain JUnit.
 */
public final class Records {

	/** Where the records are written: the device's preferences (RecordPrefs), a map in the tests. */
	public interface Store {
		String stats();

		void setStats(String encoded);

		String highScores();

		void setHighScores(String encoded);

		boolean hasSavedGame();

		/** The saved game, or null; throws IllegalArgumentException if it can't be read. */
		SavedGame savedGame(Random rng);

		void saveGame(SavedGame game);

		void clearSavedGame();
	}

	private final Store store;
	private final boolean persist;
	private Stats stats;

	/** stats: to start from instead of the stored ones (the screenshot run's made-up history), or null. */
	public Records(Store store, boolean persist, Stats stats) {
		this.store = store;
		this.persist = persist;
		this.stats = stats != null ? stats : Stats.decode(persist ? store.stats() : "");
	}

	// ---- statistics

	public Stats stats() {
		return stats;
	}

	/** Saves the statistics after a change. */
	public void saveStats() {
		if (persist) {
			store.setStats(stats.encode());
		}
	}

	/** Starts the statistics afresh. */
	public void resetStats() {
		stats = new Stats();
		saveStats();
	}

	// ---- high scores

	public HighScores highScores() {
		return HighScores.decode(persist ? store.highScores() : "");
	}

	/** Records your finished game; returns its position in the table, or -1 (or when nothing is kept). */
	public int recordHighScore(int score, int place, Difficulty level, String date) {
		if (!persist) {
			return -1;
		}
		HighScores h = highScores();
		int at = h.add(new HighScores.Entry(score, place, date, level));
		if (at >= 0) {
			store.setHighScores(h.encode());
		}
		return at;
	}

	/** Today as yyyy-mm-dd, for a high score. */
	public static String today() {
		Calendar c = Calendar.getInstance(); // (not java.time: Android 5 doesn't have it)
		return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
				c.get(Calendar.DAY_OF_MONTH));
	}

	// ---- the game in progress

	public boolean hasSavedGame() {
		return persist && store.hasSavedGame();
	}

	/** The saved game, or null if there's none (an unreadable one is thrown away). */
	public SavedGame savedGame(Random rng) {
		if (!persist) {
			return null;
		}
		try {
			return store.savedGame(rng);
		} catch (IllegalArgumentException e) {
			Log.warn("Records", "Threw away a saved game that couldn't be read", e);
			store.clearSavedGame();
			return null;
		}
	}

	/** Keeps the game in progress so it survives the app closing. */
	public void saveGame(SavedGame game) {
		if (persist) {
			store.saveGame(game);
		}
	}

	/** Forgets the saved game (it has finished). */
	public void clearSavedGame() {
		if (persist) {
			store.clearSavedGame();
		}
	}
}
