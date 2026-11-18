package ohheck;

import java.util.Random;

import rules.Difficulty;
import rules.Log;
import rules.SavedGame;
import rules.Stats;

/**
 * What's kept between games: your statistics, high scores and the game in progress.
 * Written to a {@link Store} (the device's settings) only when persist is on; the
 * screenshot run keeps everything in memory. No libGDX, so it's tested in plain JUnit.
 */
final class Records {

	/** Where the records are written: Settings on the device, a map in the tests. */
	interface Store {
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
	Records(Store store, boolean persist, Stats stats) {
		this.store = store;
		this.persist = persist;
		this.stats = stats != null ? stats : Stats.decode(persist ? store.stats() : "");
	}

	// ---- statistics

	Stats stats() {
		return stats;
	}

	/** Saves the statistics after a change (for convenience, when changed is true). */
	void saveStats(boolean changed) {
		if (changed && persist) {
			store.setStats(stats.encode());
		}
	}

	/** Starts the statistics afresh. */
	void resetStats() {
		stats = new Stats();
		saveStats(true);
	}

	// ---- high scores

	HighScores highScores() {
		return HighScores.decode(persist ? store.highScores() : "");
	}

	/** Records your finished game; returns its position in the table, or -1 (or when nothing is kept). */
	int recordHighScore(int score, int place, Difficulty level, String date) {
		if (!persist) {
			return -1;
		}
		HighScores h = highScores();
		int at = h.add(new HighScores.Entry(score, place, date, level.ordinal()));
		if (at >= 0) {
			store.setHighScores(h.encode());
		}
		return at;
	}

	/** Today as yyyy-mm-dd, for a high score. */
	static String today() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		return String.format(java.util.Locale.US, "%04d-%02d-%02d", c.get(java.util.Calendar.YEAR),
				c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
	}

	// ---- the game in progress

	boolean hasSavedGame() {
		return persist && store.hasSavedGame();
	}

	/** The saved game, or null if there's none (an unreadable one is thrown away). */
	SavedGame savedGame(Random rng) {
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

	/** Keeps the game in progress so it survives the app closing; a finished one is forgotten. */
	void saveGame(SavedGame game, boolean over) {
		if (!persist) {
			return;
		}
		if (over) {
			store.clearSavedGame();
		} else {
			store.saveGame(game);
		}
	}
}
