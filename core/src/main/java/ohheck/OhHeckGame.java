package ohheck;

import java.util.Random;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

import rules.Match;

/** The libGDX entry point shared by Android and desktop; switches between screens. */
public class OhHeckGame extends Game {

	public static final String VERSION = "alpha-1.0.7";

	/** A screen that can describe itself for automated screenshots. */
	public interface Tagged {
		String stateTag();
	}

	final GameOptions options;
	Assets assets;
	Settings settings;
	Audio audio;
	private PostProcessor post;

	private TitleScreen title;
	private RulesScreen rules;
	private OptionsScreen optionsScreen;
	private PlayScreen play;

	public OhHeckGame() {
		this(new GameOptions());
	}

	public OhHeckGame(GameOptions options) {
		this.options = options;
	}

	@Override
	public void create() {
		assets = new Assets();
		settings = new Settings();
		audio = new Audio(settings);
		post = new PostProcessor();
		title = new TitleScreen(this);
		rules = new RulesScreen(this);
		optionsScreen = new OptionsScreen(this);
		setScreen(title);
	}

	@Override
	public void render() {
		boolean effects = settings.effects() && post.begin();
		super.render();
		if (effects) {
			post.end(Gdx.graphics.getDeltaTime());
		}
		Screen s = getScreen();
		if (options.frameListener != null && s instanceof Tagged) {
			options.frameListener.onFrame(((Tagged) s).stateTag());
		}
	}

	void showTitle() {
		setScreen(title);
	}

	void showRules() {
		setScreen(rules);
	}

	void showOptions() {
		setScreen(optionsScreen);
	}

	void newGame() {
		if (play != null) {
			play.dispose();
		}
		play = new PlayScreen(this, null);
		setScreen(play);
	}

	/** Back to the game in progress, or the saved one after the app was closed. */
	void continueGame() {
		if (play == null) {
			Match saved = loadSavedGame();
			if (saved == null) {
				newGame();
				return;
			}
			play = new PlayScreen(this, saved);
		}
		setScreen(play);
	}

	boolean hasGameInProgress() {
		if (play != null) {
			return !play.isGameOver();
		}
		return options.persist && settings.savedGame() != null;
	}

	private Match loadSavedGame() {
		String s = options.persist ? settings.savedGame() : null;
		if (s == null) {
			return null;
		}
		try {
			return Match.load(s, new Random());
		} catch (IllegalArgumentException e) {
			Gdx.app.error("OhHeck", "Discarding unreadable saved game", e);
			settings.saveGame(null);
			return null;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		title.dispose();
		rules.dispose();
		optionsScreen.dispose();
		if (play != null) {
			play.dispose();
		}
		audio.dispose();
		post.dispose();
		assets.dispose();
	}
}
