package ohheck.records;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import ohheck.players.Difficulty;
import ohheck.util.Log;

/**
 * Statistics, high scores and the saved game, written to a Store only when persisting. Dates
 * use Calendar because Android 5 has no java.time.
 */
public final class Records {

	public interface Store {
		String stats();

		void setStats(String encoded);

		String highScores();

		void setHighScores(String encoded);

		boolean hasSavedGame();

		SavedGame savedGame(Random rng);

		void saveGame(SavedGame game);

		void clearSavedGame();
	}

	private final Store store;
	private final boolean persist;
	private Stats stats;

	public Records(Store store, boolean persist, Stats stats) {
		this.store = store;
		this.persist = persist;
		this.stats = stats != null ? stats : Stats.decode(persist ? store.stats() : "");
	}

	public Stats stats() {
		return stats;
	}

	public void saveStats() {
		if (persist) {
			store.setStats(stats.encode());
		}
	}

	public void resetStats() {
		stats = new Stats();
		saveStats();
	}

	public HighScores highScores() {
		return HighScores.decode(persist ? store.highScores() : "");
	}

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

	public static String today() {
		Calendar c = Calendar.getInstance();
		return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
				c.get(Calendar.DAY_OF_MONTH));
	}

	public boolean hasSavedGame() {
		return persist && store.hasSavedGame();
	}

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

	public void saveGame(SavedGame game) {
		if (persist) {
			store.saveGame(game);
		}
	}

	public void clearSavedGame() {
		if (persist) {
			store.clearSavedGame();
		}
	}
}
