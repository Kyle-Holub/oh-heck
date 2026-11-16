package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

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

	/** Animation and turn speed: 0 = slow, 1 = medium (the default), 2 = fast. */
	public int speed() {
		// earlier versions saved a fast on/off switch
		int legacy = prefs.getBoolean("fast", false) ? 2 : 1;
		return Math.max(0, Math.min(2, prefs.getInteger("speed", legacy)));
	}

	/** How much faster than the base timings everything runs. */
	public float speedFactor() {
		return SPEED_FACTORS[speed()];
	}

	/** The game in progress, as saved by rules.Match.save(), or null. */
	public String savedGame() {
		String s = prefs.getString("savedGame", "");
		return s.isEmpty() ? null : s;
	}

	public void saveGame(String saved) {
		if (saved == null) {
			prefs.remove("savedGame");
		} else {
			prefs.putString("savedGame", saved);
		}
		prefs.flush();
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
