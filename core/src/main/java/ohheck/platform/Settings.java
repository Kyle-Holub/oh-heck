package ohheck.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import ohheck.players.Difficulty;
import ohheck.records.Records;
import ohheck.rules.HandOrder;
import ohheck.rules.HandSort;

/* The player's options, in the device's preferences. */
public final class Settings {

	private static final String MUSIC = "music", SOUND = "sound", EFFECTS = "effects", FAST_GAME = "fastGame", LEVEL = "level",
			HAND_ORDER = "handOrder", HAND_SORT = "handSort", TUTORIAL_SEEN = "tutorialSeen";

	private static final float STANDARD_PACE = 0.6f, FAST_PACE = 1.4f;

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

	public boolean fastGame() {
		return prefs.getBoolean(FAST_GAME, false);
	}

	public void setFastGame(boolean fast) {
		prefs.putBoolean(FAST_GAME, fast).flush();
	}

	public float speedFactor() {
		return fastGame() ? FAST_PACE : STANDARD_PACE;
	}

	public Difficulty difficulty() {
		return Difficulty.named(prefs.getString(LEVEL, Difficulty.NORMAL.name()));
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

	public HandSort handSort() {
		return prefs.getString(HAND_SORT, "").equals(HandSort.RANK.name()) ? HandSort.RANK : HandSort.SUIT;
	}

	public void setHandSort(HandSort sort) {
		prefs.putString(HAND_SORT, sort.name()).flush();
	}

	public boolean tutorialSeen() {
		return prefs.getBoolean(TUTORIAL_SEEN, false);
	}

	public void setTutorialSeen(boolean seen) {
		prefs.putBoolean(TUTORIAL_SEEN, seen).flush();
	}
}
