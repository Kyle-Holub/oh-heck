package ohheck;

import java.util.Random;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import rules.Match;

/** The libGDX entry point shared by Android and desktop; switches between screens. */
public class OhHeckGame extends Game {

	public static final String VERSION = "alpha-1.0.8";
	/** The CI build ("dev-0042") from assets/build.txt, or "" for a local build. */
	String build = "";

	/** A screen that can describe itself for automated screenshots. */
	public interface Tagged {
		String stateTag();
	}

	final GameOptions options;
	/** The one sprite batch all screens draw with. */
	SpriteBatch batch;
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

	/** Screens to return to with Back: Options, How to play and High scores open over where you were. */
	private final java.util.ArrayDeque<Screen> backStack = new java.util.ArrayDeque<Screen>();

	@Override
	public void create() {
		batch = new SpriteBatch();
		if (Gdx.files.internal("build.txt").exists()) {
			build = Gdx.files.internal("build.txt").readString("UTF-8").trim();
		}
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

	/** Frame length when nothing is moving: 30 fps, to save battery. */
	private static final long IDLE_FRAME_NS = 1000000000L / 30;
	private long frameStart;
	private boolean idle, capturing;

	@Override
	public void render() {
		long now = System.nanoTime();
		if (idle) {
			long wait = frameStart + IDLE_FRAME_NS - now;
			if (wait > 0) {
				try {
					Thread.sleep(wait / 1000000, (int) (wait % 1000000));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				now = System.nanoTime();
			}
		}
		frameStart = now;
		boolean effects = settings.effects() && post.begin();
		capturing = effects;
		float delta = options.fixedStep > 0 ? options.fixedStep : Gdx.graphics.getDeltaTime();
		if (getScreen() != null) {
			getScreen().render(delta);
		}
		if (effects) {
			post.end(delta);
		}
		capturing = false;
		Screen s = getScreen();
		idle = options.fixedStep <= 0 && s instanceof BaseScreen && !((BaseScreen) s).animating();
		if (options.frameListener != null && s instanceof Tagged) {
			options.frameListener.onFrame(((Tagged) s).stateTag());
		}
	}

	/** While the effects pass is capturing a frame, draw into its (smaller) buffer rather than the screen. */
	void fitViewportToTarget() {
		if (capturing) {
			Gdx.gl.glViewport(0, 0, post.sceneWidth(), post.sceneHeight());
		}
	}

	/** Opens a screen over the current one; {@link #back()} returns to it. */
	private void open(Screen s) {
		if (getScreen() != null) {
			backStack.push(getScreen());
		}
		setScreen(s);
	}

	/** Back to the screen that opened this one, or the title screen. */
	void back() {
		setScreen(backStack.isEmpty() ? title : backStack.pop());
	}

	void showTitle() {
		backStack.clear();
		setScreen(title);
	}

	void showRules() {
		open(rules);
	}

	void showOptions() {
		open(optionsScreen);
	}

	/** The high score table; newest is the position just added (-1 for none) to pick out. */
	void showHighScores(int newest) {
		highScoresScreen.highlight(newest);
		open(highScoresScreen);
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
		backStack.clear();
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
		backStack.clear();
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
		batch.dispose();
	}
}
