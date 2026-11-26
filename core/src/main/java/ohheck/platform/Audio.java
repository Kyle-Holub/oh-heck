package ohheck.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;

import ohheck.util.Log;

public final class Audio implements Disposable {

	public enum Effect {
		SHUFFLE("sfx/shuffle.mp3", 1f),
		PLAY_CARD("sfx/playCard.mp3", 1f),
		WIN("sfx/win.mp3", 0.25f),
		LOSE("sfx/lose.mp3", 1f),
		CLINK("sfx/clink.wav", 0.4f),
		TANKARD("sfx/tankard.wav", 0.4f),
		THUMP("sfx/thump.wav", 0.6f),
		KNOCK("sfx/knock.wav", 0.6f),
		THUP("sfx/thup.wav", 0.6f),
		WHUMP("sfx/whump.wav", 0.6f),
		SWEEP("sfx/sweep.wav", 0.6f),
		COIN("sfx/coin.wav", 0.6f),
		CRACKLE("sfx/crackle.wav", 0.6f),
		STAMP("sfx/stamp.wav", 0.6f),
		RIFFLE("sfx/riffle.wav", 0.6f),
		PAPER("sfx/paper.wav", 0.6f),
		DRUM("sfx/drum.wav", 0.6f),
		TICK("sfx/tick.wav", 0.6f),
		CREAK("sfx/creak.wav", 0.6f),
		MEDAL("sfx/medal.wav", 0.6f),
		WHOOSH("sfx/whoosh.wav", 0.6f),
		LATCH("sfx/latch.wav", 0.6f);

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
			music.setVolume(MUSIC_VOLUME);
		} catch (RuntimeException ex) {
			Log.warn("Audio", "Couldn't load music", ex);
		}
	}

	public enum Buzz {
		TICK(8, 0, 0), TAP(15, 0, 0), THUD(35, 0, 0), DOUBLE(20, 90, 20), HEARTBEAT(45, 170, 35);

		final int first, gap, second;

		Buzz(int first, int gap, int second) {
			this.first = first;
			this.gap = gap;
			this.second = second;
		}
	}

	public void play(Effect e) {
		play(e, 1f);
	}

	public void play(Effect e, float loudness) {
		Sound s = sounds[e.ordinal()];
		if (s != null && settings.sound()) {
			s.play(e.volume * loudness);
		}
	}

	public void buzz(Buzz b) {
		if (!settings.vibration()) {
			return;
		}
		Gdx.input.vibrate(b.first);
		if (b.second > 0) {
			Timer.schedule(new Timer.Task() {
				@Override
				public void run() {
					Gdx.input.vibrate(b.second);
				}
			}, b.gap / 1000f);
		}
	}

	public void duck(boolean on) {
		if (music != null) {
			music.setVolume(on ? MUSIC_DUCKED : MUSIC_VOLUME);
		}
	}

	public void press(boolean main) {
		if (settings.buttonSounds()) {
			play(main ? Effect.TANKARD : Effect.CLINK);
		}
		buzz(Buzz.TAP);
	}

	private static final float MUSIC_VOLUME = 0.6f, MUSIC_DUCKED = 0.25f;

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
