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

	/** The difficulty the saved game was started at (its high score badge and statistics level). */
	public int savedGameDifficulty() {
		return Math.max(0, Math.min(2, prefs.getInteger("savedGameDifficulty", 1)));
	}

	/** Who sits at the saved game's table (Player ids for seats 1-3), or null if not saved (older saves). */
	public int[] savedGamePlayers() {
		String[] f = prefs.getString("savedGamePlayers", "").split(",");
		if (f.length != 3) {
			return null;
		}
		try {
			int[] ids = new int[3];
			for (int i = 0; i < 3; i++) {
				ids[i] = Integer.parseInt(f[i].trim());
				if (ids[i] < 0 || ids[i] >= rules.Player.ALL.size()) {
					return null;
				}
			}
			return ids;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** The game in progress, as saved by rules.Match.save(), or null. */
	public String savedGame() {
		String s = prefs.getString("savedGame", "");
		return s.isEmpty() ? null : s;
	}

	/** Saves the game in progress, the difficulty it started at, and who's at the table. */
	public void saveGame(String saved, int difficulty, int[] players) {
		prefs.putString("savedGame", saved);
		prefs.putInteger("savedGameDifficulty", difficulty);
		prefs.putString("savedGamePlayers", players[0] + "," + players[1] + "," + players[2]);
		prefs.flush();
	}

	/** Forgets the saved game (finished, or unreadable). */
	public void clearSavedGame() {
		prefs.remove("savedGame");
		prefs.flush();
	}

	/** Your best finished games, as HighScores.encode() writes them. */
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
