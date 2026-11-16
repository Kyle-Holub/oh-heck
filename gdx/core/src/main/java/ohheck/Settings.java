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
