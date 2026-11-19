package ohheck.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import ohheck.util.Log;

/** The game's music and sound effects, respecting the player's settings. */
public final class Audio implements Disposable {

	public enum Effect {
		SHUFFLE("sfx/shuffle.mp3", 1f),
		PLAY_CARD("sfx/playCard.mp3", 1f),
		WIN("sfx/win.mp3", 0.25f), // a quarter of full volume: it was too loud even at half
		LOSE("sfx/lose.mp3", 1f);

		final String path;
		final float volume;

		Effect(String path, float volume) {
			this.path = path;
			this.volume = volume;
		}
	}

	private final Settings settings;
	private final Sound[] sounds = new Sound[Effect.values().length];
	private Music music;

	public Audio(Settings settings) {
		this.settings = settings;
		for (Effect e : Effect.values()) {
			try {
				sounds[e.ordinal()] = Gdx.audio.newSound(Gdx.files.internal(e.path));
			} catch (RuntimeException ex) {
				Log.warn("Audio", "Couldn't load " + e.path, ex);
			}
		}
		try {
			music = Gdx.audio.newMusic(Gdx.files.internal("music/backTrack.mp3"));
			music.setLooping(true);
			music.setVolume(0.6f);
		} catch (RuntimeException ex) {
			Log.warn("Audio", "Couldn't load music", ex);
		}
	}

	public void play(Effect e) {
		Sound s = sounds[e.ordinal()];
		if (s != null && settings.sound()) {
			s.play(e.volume);
		}
	}

	/** Starts or stops the music to match the setting. */
	public void startMusic() {
		if (music == null) {
			return;
		}
		if (settings.music()) {
			if (!music.isPlaying()) {
				music.play();
			}
		} else {
			music.pause();
		}
	}

	@Override
	public void dispose() {
		for (Sound s : sounds) {
			if (s != null) {
				s.dispose();
			}
		}
		if (music != null) {
			music.dispose();
		}
	}
}
