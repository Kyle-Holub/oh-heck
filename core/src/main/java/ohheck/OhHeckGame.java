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
	private HighScoresScreen highScoresScreen;
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
		highScoresScreen = new HighScoresScreen(this);
		setScreen(title);
	}

	@Override
	public void render() {
		boolean effects = settings.effects() && post.begin();
		float delta = options.fixedStep > 0 ? options.fixedStep : Gdx.graphics.getDeltaTime();
		if (getScreen() != null) {
			getScreen().render(delta);
		}
		if (effects) {
			post.end(delta);
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

	/** The high score table; newest is the position just added (-1 for none) to pick out. */
	void showHighScores(int newest) {
		highScoresScreen.highlight(newest);
		setScreen(highScoresScreen);
	}

	/** High scores in memory; persisted only for real games. */
	HighScores highScores() {
		return HighScores.decode(options.persist ? settings.highScores() : "");
	}

	/** Records your finished game; returns its place in the table, or -1. */
	int recordHighScore(int score, int place) {
		if (!options.persist) {
			return -1;
		}
		HighScores h = highScores();
		java.util.Calendar c = java.util.Calendar.getInstance();
		String date = String.format(java.util.Locale.US, "%04d-%02d-%02d", c.get(java.util.Calendar.YEAR),
				c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
		int at = h.add(new HighScores.Entry(score, place, date));
		if (at >= 0) {
			settings.setHighScores(h.encode());
		}
		return at;
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
		highScoresScreen.dispose();
		if (play != null) {
			play.dispose();
		}
		audio.dispose();
		post.dispose();
		assets.dispose();
	}
}
