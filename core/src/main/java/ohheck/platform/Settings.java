package ohheck.platform;

import java.util.function.UnaryOperator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import ohheck.campaign.Cosmetic;
import ohheck.players.Difficulty;
import ohheck.records.Records;
import ohheck.rules.GameMode;
import ohheck.rules.HandSort;
import ohheck.rules.RuleSet;

/* The player's options, in the device's preferences. */
public final class Settings {

	private static final String MUSIC = "music", SOUND = "sound", EFFECTS = "effects", FAST_GAME = "fastGame", LEVEL = "level",
			MODE = "mode", CUSTOM_RULES = "customRules", CARD_BACK = "cardBack", FELT = "felt", HAND_SORT = "handSort", TUTORIAL_SEEN = "tutorialSeen";

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

	public boolean muted() {
		return !music() && !sound();
	}

	public void setMuted(boolean muted) {
		prefs.putBoolean(MUSIC, !muted).putBoolean(SOUND, !muted).flush();
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

	public GameMode mode() {
		return GameMode.named(prefs.getString(MODE, GameMode.CLASSIC.name()));
	}

	public void setMode(GameMode mode) {
		prefs.putString(MODE, mode.name()).flush();
	}

	public RuleSet customRules() {
		try {
			return RuleSet.parse(prefs.getString(CUSTOM_RULES, RuleSet.STANDARD.code()));
		} catch (IllegalArgumentException e) {
			return RuleSet.STANDARD;
		}
	}

	public RuleSet rules() {
		return mode().rules(customRules());
	}

	public void customize(UnaryOperator<RuleSet> change) {
		prefs.putString(CUSTOM_RULES, change.apply(rules()).code());
		prefs.putString(MODE, GameMode.CUSTOM.name()).flush();
	}

	public HandSort handSort() {
		return prefs.getString(HAND_SORT, "").equals(HandSort.RANK.name()) ? HandSort.RANK : HandSort.SUIT;
	}

	public void setHandSort(HandSort sort) {
		prefs.putString(HAND_SORT, sort.name()).flush();
	}

	public Cosmetic cardBack() {
		return Cosmetic.named(prefs.getString(CARD_BACK, Cosmetic.CLASSIC.name()));
	}

	public void setCardBack(Cosmetic c) {
		prefs.putString(CARD_BACK, c.name()).flush();
	}

	public Cosmetic felt() {
		return Cosmetic.named(prefs.getString(FELT, Cosmetic.CLASSIC.name()));
	}

	public void setFelt(Cosmetic c) {
		prefs.putString(FELT, c.name()).flush();
	}

	public void forgetEverything() {
		prefs.clear();
		prefs.flush();
	}

	public boolean tutorialSeen() {
		return prefs.getBoolean(TUTORIAL_SEEN, false);
	}

	public void setTutorialSeen(boolean seen) {
		prefs.putBoolean(TUTORIAL_SEEN, seen).flush();
	}
}
