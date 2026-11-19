package ohheck.platform;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.records.Records;
import ohheck.records.SavedGame;
import ohheck.rules.HandOrder;

/** Player options, saved on the device between sessions. */
public final class Settings implements Records.Store {

	private final Preferences prefs = Gdx.app.getPreferences("oh-heck");

	public boolean music() {
		return prefs.getBoolean("music", true);
	}

	public boolean sound() {
		return prefs.getBoolean("sound", true);
	}

	public static final String[] SPEEDS = { "Slow", "Medium", "Fast" };
	private static final float[] SPEED_FACTORS = { 0.6f, 0.8f, 1.4f };

	/** Animation and turn speed: 0 = slow (the default), 1 = medium, 2 = fast. */
	public int speed() {
		// earlier versions saved a fast on/off switch
		int legacy = prefs.getBoolean("fast", false) ? 2 : 0;
		return Math.max(0, Math.min(2, prefs.getInteger("speed", legacy)));
	}

	/** How much faster than the base timings everything runs. */
	public float speedFactor() {
		return SPEED_FACTORS[speed()];
	}

	/** The difficulty setting for new games; Normal unless chosen. (Older builds saved its number.) */
	public Difficulty difficulty() {
		return Difficulty.fromSaved(prefs.getString("level", null), prefs.getInteger("difficulty", 1));
	}

	public void setDifficulty(Difficulty d) {
		prefs.putString("level", d.name()).flush();
	}

	/** Which way new games' hands run: one card up to eight (the default), or eight down to one. */
	public HandOrder handOrder() {
		return prefs.getString("handOrder", "").equals(HandOrder.DOWN.name()) ? HandOrder.DOWN : HandOrder.UP;
	}

	public void setHandOrder(HandOrder order) {
		prefs.putString("handOrder", order.name()).flush();
	}

	/** Whether a game in progress is saved. */
	public boolean hasSavedGame() {
		return !prefs.getString("savedGame", "").isEmpty();
	}

	/**
	 * The game in progress, or null if none is saved. Throws IllegalArgumentException if it
	 * can't be read. (Saves from before the table travelled with the game kept its difficulty
	 * and players under keys of their own: read as a fallback.)
	 */
	public SavedGame savedGame(Random rng) {
		String s = prefs.getString("savedGame", "");
		if (s.isEmpty()) {
			return null;
		}
		Difficulty legacyLevel = Difficulty.fromSaved(null, prefs.getInteger("savedGameDifficulty", 1));
		String legacyPlayers = prefs.getString("savedGamePlayers", "");
		return SavedGame.decode(s, rng, legacyLevel, legacyPlayers.isEmpty() ? null : Seating.parse(legacyPlayers));
	}

	/** Saves the game in progress (with its difficulty and table) as one piece of text. */
	public void saveGame(SavedGame game) {
		prefs.putString("savedGame", game.encode());
		prefs.remove("savedGameDifficulty"); // (the old separate keys: now part of the save)
		prefs.remove("savedGamePlayers");
		prefs.flush();
	}

	/** Forgets the saved game (finished, or unreadable). */
	public void clearSavedGame() {
		prefs.remove("savedGame");
		prefs.flush();
	}

	/** Whether you've seen (or skipped) the tutorial, shown before your first game. */
	public boolean tutorialSeen() {
		return prefs.getBoolean("tutorialSeen", false);
	}

	public void setTutorialSeen(boolean seen) {
		prefs.putBoolean("tutorialSeen", seen).flush();
	}

	/** Your statistics, as {@link rules.Stats#encode()} wrote them ("" for none yet). */
	public String stats() {
		return prefs.getString("stats", "");
	}

	public void setStats(String encoded) {
		prefs.putString("stats", encoded).flush();
	}

	/** Your best finished games, as HighScores.encode() writes them. */
	public String highScores() {
		return prefs.getString("highScores", "");
	}

	public void setHighScores(String encoded) {
		prefs.putString("highScores", encoded).flush();
	}

	/** Glow, vignette and grain over the whole screen. */
	public boolean effects() {
		return prefs.getBoolean("effects", true);
	}

	public void setEffects(boolean on) {
		prefs.putBoolean("effects", on).flush();
	}

	public void setMusic(boolean on) {
		prefs.putBoolean("music", on).flush();
	}

	public void setSound(boolean on) {
		prefs.putBoolean("sound", on).flush();
	}

	public void setSpeed(int speed) {
		prefs.putInteger("speed", speed).flush();
	}
}
