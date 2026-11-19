package ohheck.platform;

import java.util.Random;

import com.badlogic.gdx.Preferences;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.records.Records;
import ohheck.records.SavedGame;

/**
 * Where {@link Records} keeps your statistics, high scores and the game in progress on the
 * device: the same preferences as {@link Settings}, under keys of their own.
 */
final class RecordPrefs implements Records.Store {

	private static final String STATS = "stats", HIGH_SCORES = "highScores", SAVED_GAME = "savedGame";
	/** Saves from before the table travelled with the game kept its difficulty and players apart. */
	private static final String LEGACY_SAVED_LEVEL = "savedGameDifficulty", LEGACY_SAVED_PLAYERS = "savedGamePlayers";

	private final Preferences prefs;

	RecordPrefs(Preferences prefs) {
		this.prefs = prefs;
	}

	/** Your statistics, as {@code Stats.encode()} wrote them ("" for none yet). */
	@Override
	public String stats() {
		return prefs.getString(STATS, "");
	}

	@Override
	public void setStats(String encoded) {
		prefs.putString(STATS, encoded).flush();
	}

	/** Your best finished games, as {@code HighScores.encode()} writes them. */
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

	/**
	 * The game in progress, or null if none is saved. Throws IllegalArgumentException if it
	 * can't be read. (Older saves' difficulty and players are read from their own keys as a
	 * fallback.)
	 */
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

	/** Saves the game in progress (with its difficulty and table) as one piece of text. */
	@Override
	public void saveGame(SavedGame game) {
		prefs.putString(SAVED_GAME, game.encode());
		prefs.remove(LEGACY_SAVED_LEVEL); // (now part of the save)
		prefs.remove(LEGACY_SAVED_PLAYERS);
		prefs.flush();
	}

	/** Forgets the saved game (finished, or unreadable). */
	@Override
	public void clearSavedGame() {
		prefs.remove(SAVED_GAME);
		prefs.flush();
	}
}
