package ohheck.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import ohheck.players.Difficulty;
import ohheck.records.Records;
import ohheck.rules.HandOrder;

/** The player's options, in the device's preferences. */
public final class Settings {

	private static final String MUSIC = "music", SOUND = "sound", EFFECTS = "effects", SPEED = "speed", LEVEL = "level",
			HAND_ORDER = "handOrder", TUTORIAL_SEEN = "tutorialSeen";
	private static final String LEGACY_FAST = "fast", LEGACY_DIFFICULTY = "difficulty";

	public static final int SLOW = 0, FASTEST = 2;
	private static final float[] SPEED_FACTORS = { 0.6f, 0.8f, 1.4f };

	private final Preferences prefs = Gdx.app.getPreferences("oh-heck");
	private final Records.Store records = new RecordPrefs(prefs);

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

	public boolean effects() {
		return prefs.getBoolean(EFFECTS, true);
	}

	public void setEffects(boolean on) {
		prefs.putBoolean(EFFECTS, on).flush();
	}

	public int speed() {
		int legacy = prefs.getBoolean(LEGACY_FAST, false) ? FASTEST : SLOW;
		return Math.max(SLOW, Math.min(FASTEST, prefs.getInteger(SPEED, legacy)));
	}

	public void setSpeed(int speed) {
		prefs.putInteger(SPEED, speed).flush();
	}

	public float speedFactor() {
		return SPEED_FACTORS[speed()];
	}

	public Difficulty difficulty() {
		return Difficulty.fromSaved(prefs.getString(LEVEL, null), prefs.getInteger(LEGACY_DIFFICULTY, 1));
	}

	public void setDifficulty(Difficulty d) {
		prefs.putString(LEVEL, d.name()).flush();
	}

	public HandOrder handOrder() {
		return prefs.getString(HAND_ORDER, "").equals(HandOrder.DOWN.name()) ? HandOrder.DOWN : HandOrder.UP;
	}

	public void setHandOrder(HandOrder order) {
		prefs.putString(HAND_ORDER, order.name()).flush();
	}

	public boolean tutorialSeen() {
		return prefs.getBoolean(TUTORIAL_SEEN, false);
	}

	public void setTutorialSeen(boolean seen) {
		prefs.putBoolean(TUTORIAL_SEEN, seen).flush();
	}
}
