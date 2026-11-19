package ohheck.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import ohheck.players.Difficulty;
import ohheck.records.Records;
import ohheck.rules.HandOrder;

/** Player options, saved on the device between sessions. */
public final class Settings {

	private static final String MUSIC = "music", SOUND = "sound", EFFECTS = "effects", SPEED = "speed", LEVEL = "level",
			HAND_ORDER = "handOrder", TUTORIAL_SEEN = "tutorialSeen";
	/** Older builds' keys, still read: a fast on/off switch, and the difficulty by number. */
	private static final String LEGACY_FAST = "fast", LEGACY_DIFFICULTY = "difficulty";

	/** Game speeds: 0 = slow (the default), 1 = medium, 2 = fast; how much faster than the base timings each runs. */
	public static final int SLOW = 0, FASTEST = 2;
	private static final float[] SPEED_FACTORS = { 0.6f, 0.8f, 1.4f };

	private final Preferences prefs = Gdx.app.getPreferences("oh-heck");
	/** Statistics, high scores and the saved game, kept in the same preferences. */
	private final Records.Store records = new RecordPrefs(prefs);

	/** Where {@link Records} keeps what it keeps between games. */
	public Records.Store recordStore() {
		return records;
	}

	public boolean music() {
		return prefs.getBoolean(MUSIC, true);
	}

	public void setMusic(boolean on) {
		prefs.putBoolean(MUSIC, on).flush();
	}

	public boolean sound() {
		return prefs.getBoolean(SOUND, true);
	}

	public void setSound(boolean on) {
		prefs.putBoolean(SOUND, on).flush();
	}

	/** Glow, vignette and grain over the whole screen. */
	public boolean effects() {
		return prefs.getBoolean(EFFECTS, true);
	}

	public void setEffects(boolean on) {
		prefs.putBoolean(EFFECTS, on).flush();
	}

	/** Animation and turn speed, {@link #SLOW} to {@link #FASTEST}. */
	public int speed() {
		int legacy = prefs.getBoolean(LEGACY_FAST, false) ? FASTEST : SLOW;
		return Math.max(SLOW, Math.min(FASTEST, prefs.getInteger(SPEED, legacy)));
	}

	public void setSpeed(int speed) {
		prefs.putInteger(SPEED, speed).flush();
	}

	/** How much faster than the base timings everything runs. */
	public float speedFactor() {
		return SPEED_FACTORS[speed()];
	}

	/** The difficulty setting for new games; Normal unless chosen. (Older builds saved its number.) */
	public Difficulty difficulty() {
		return Difficulty.fromSaved(prefs.getString(LEVEL, null), prefs.getInteger(LEGACY_DIFFICULTY, 1));
	}

	public void setDifficulty(Difficulty d) {
		prefs.putString(LEVEL, d.name()).flush();
	}

	/** Which way new games' hands run: one card up to eight (the default), or eight down to one. */
	public HandOrder handOrder() {
		return prefs.getString(HAND_ORDER, "").equals(HandOrder.DOWN.name()) ? HandOrder.DOWN : HandOrder.UP;
	}

	public void setHandOrder(HandOrder order) {
		prefs.putString(HAND_ORDER, order.name()).flush();
	}

	/** Whether you've seen (or skipped) the tutorial, shown before your first game. */
	public boolean tutorialSeen() {
		return prefs.getBoolean(TUTORIAL_SEEN, false);
	}

	public void setTutorialSeen(boolean seen) {
		prefs.putBoolean(TUTORIAL_SEEN, seen).flush();
	}
}
