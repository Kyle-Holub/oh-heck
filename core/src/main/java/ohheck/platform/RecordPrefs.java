package ohheck.platform;

import java.util.Random;

import com.badlogic.gdx.Preferences;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.records.Records;
import ohheck.records.SavedGame;

/**
 * Records.Store on the device's preferences, including the keys older builds saved the game
 * under.
 */
final class RecordPrefs implements Records.Store {

	private static final String STATS = "stats", HIGH_SCORES = "highScores", SAVED_GAME = "savedGame";
	private static final String LEGACY_SAVED_LEVEL = "savedGameDifficulty", LEGACY_SAVED_PLAYERS = "savedGamePlayers";

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
		Difficulty legacyLevel = Difficulty.fromSaved(null, prefs.getInteger(LEGACY_SAVED_LEVEL, 1));
		String legacyPlayers = prefs.getString(LEGACY_SAVED_PLAYERS, "");
		return SavedGame.decode(s, rng, legacyLevel, legacyPlayers.isEmpty() ? null : Seating.parse(legacyPlayers));
	}

	@Override
	public void saveGame(SavedGame game) {
		prefs.putString(SAVED_GAME, game.encode());
		prefs.remove(LEGACY_SAVED_LEVEL);
		prefs.remove(LEGACY_SAVED_PLAYERS);
		prefs.flush();
	}

	@Override
	public void clearSavedGame() {
		prefs.remove(SAVED_GAME);
		prefs.flush();
	}
}
