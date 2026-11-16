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

	/** Shorter pauses between moves. */
	public boolean fast() {
		return prefs.getBoolean("fast", false);
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

	public void setMusic(boolean on) {
		prefs.putBoolean("music", on).flush();
	}

	public void setSound(boolean on) {
		prefs.putBoolean("sound", on).flush();
	}

	public void setFast(boolean on) {
		prefs.putBoolean("fast", on).flush();
	}
}
