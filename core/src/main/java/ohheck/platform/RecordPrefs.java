package ohheck.platform;

import java.util.Random;

import com.badlogic.gdx.Preferences;

import ohheck.records.Records;
import ohheck.records.SavedGame;

/* Records.Store on the device's preferences. */
final class RecordPrefs implements Records.Store {

	private static final String STATS = "stats", HIGH_SCORES = "highScores", SAVED_GAME = "savedGame";

	private final Preferences prefs;

	RecordPrefs(Preferences prefs) {
		this.prefs = prefs;
	}

	@Override
	public String stats() {
		return prefs.getString(STATS, "");
	}

	@Override
	public void setStats(String encoded) {
		prefs.putString(STATS, encoded).flush();
	}

	@Override
	public String highScores() {
		return prefs.getString(HIGH_SCORES, "");
	}

	@Override
	public void setHighScores(String encoded) {
		prefs.putString(HIGH_SCORES, encoded).flush();
	}

	@Override
	public boolean hasSavedGame() {
		return !prefs.getString(SAVED_GAME, "").isEmpty();
	}

	@Override
	public SavedGame savedGame(Random rng) {
		String s = prefs.getString(SAVED_GAME, "");
		if (s.isEmpty()) {
			return null;
		}
		return SavedGame.decode(s, rng);
	}

	@Override
	public void saveGame(SavedGame game) {
		prefs.putString(SAVED_GAME, game.encode()).flush();
	}

	@Override
	public void clearSavedGame() {
		prefs.remove(SAVED_GAME);
		prefs.flush();
	}
}
