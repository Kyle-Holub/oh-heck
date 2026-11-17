package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import rules.HandOrder;

/** Player options, saved on the device between sessions. */
public final class Settings {

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

	/** Computer players' difficulty for new games (a rules.Difficulty ordinal); Normal unless chosen. */
	public int difficulty() {
		return Math.max(0, Math.min(2, prefs.getInteger("difficulty", 1)));
	}

	public void setDifficulty(int d) {
		prefs.putInteger("difficulty", d).flush();
	}

	/** Which way new games' hands run: one card up to eight (the default), or eight down to one. */
	public HandOrder handOrder() {
		return prefs.getString("handOrder", "").equals(HandOrder.DOWN.name()) ? HandOrder.DOWN : HandOrder.UP;
	}

	public void setHandOrder(HandOrder order) {
		prefs.putString("handOrder", order.name()).flush();
	}

	/** The easiest difficulty the saved game has been played at (its high score badge). */
	public int savedGameLowestDifficulty() {
		return Math.max(0, Math.min(2, prefs.getInteger("savedGameDifficulty", 1)));
	}

	/** The game in progress, as saved by rules.Match.save(), or null. */
	public String savedGame() {
		String s = prefs.getString("savedGame", "");
		return s.isEmpty() ? null : s;
	}

	public void saveGame(String saved, int lowestDifficulty) {
		if (saved == null) {
			prefs.remove("savedGame");
		} else {
			prefs.putString("savedGame", saved);
			prefs.putInteger("savedGameDifficulty", lowestDifficulty);
		}
		prefs.flush();
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
