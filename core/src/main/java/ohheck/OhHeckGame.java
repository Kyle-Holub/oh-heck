package ohheck;

import java.util.Random;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.platform.Audio;
import ohheck.platform.GameOptions;
import ohheck.platform.Settings;
import ohheck.records.Records;
import ohheck.records.SavedGame;
import ohheck.screens.HighScoresScreen;
import ohheck.screens.OptionsScreen;
import ohheck.screens.PlayersScreen;
import ohheck.screens.ScreenshotTour;
import ohheck.screens.TitleScreen;
import ohheck.screens.TutorialScreen;
import ohheck.table.PlayScreen;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.PostProcessor;
import ohheck.util.Log;

/** The libGDX entry point shared by Android and desktop; switches between screens. */
public class OhHeckGame extends Game implements GameHost {

	public static final String VERSION = "alpha-1.0.9";
	/** The CI build ("dev-0042") from assets/build.txt, or "" for a local build. */
	private String build = "";


	private final GameOptions options;
	/** The one sprite batch all screens draw with. */
	private SpriteBatch batch;
	private Assets assets;
	private Settings settings;
	private Audio audio;
	private PostProcessor post;

	// ---- what the screens share (made in create(), so not final)

	/** The CI build ("dev-0042"), or "" for a local build. */
	@Override
	public String version() {
		return VERSION;
	}

	@Override
	public String build() {
		return build;
	}

	@Override
	public GameOptions options() {
		return options;
	}

	@Override
	public SpriteBatch batch() {
		return batch;
	}

	@Override
	public Assets assets() {
		return assets;
	}

	@Override
	public Settings settings() {
		return settings;
	}

	@Override
	public Audio audio() {
		return audio;
	}

	@Override
	public PostProcessor post() {
		return post;
	}

	/** Statistics, high scores and the saved game. */
	@Override
	public Records records() {
		return records;
	}

	/** The screenshot run's walk through the menus; null in normal play. */
	@Override
	public GameHost.Tour tour() {
		return tour;
	}

	private TitleScreen title;
	private TutorialScreen tutorial;
	private OptionsScreen optionsScreen;
	private HighScoresScreen highScoresScreen;
	private PlayersScreen playersScreen;
	/** The screens made once and kept for the whole run (all but the table), released together. */
	private final java.util.List<Screen> keptScreens = new java.util.ArrayList<Screen>();

	private <T extends Screen> T kept(T screen) {
		keptScreens.add(screen);
		return screen;
	}
	/** Statistics, high scores and the saved game. */
	private Records records;
	private PlayScreen play;

	public OhHeckGame() {
		this(new GameOptions());
	}

	public OhHeckGame(GameOptions options) {
		this.options = options;
		tour = options.autoplayHuman ? new ScreenshotTour(this) : null;
	}

	/** The screenshot run's walk through the menus; null in normal play. */
	private final ScreenshotTour tour;

	/** Screens to return to with Back: Options, How to play and High scores open over where you were. */
	private final java.util.ArrayDeque<Screen> backStack = new java.util.ArrayDeque<Screen>();

	@Override
	public void create() {
		Log.to((tag, message, cause) -> { // the rules' reports go to the device log
			if (cause != null) {
				Gdx.app.error(tag, message, cause);
			} else {
				Gdx.app.error(tag, message);
			}
		});
		batch = new SpriteBatch();
		if (Gdx.files.internal("build.txt").exists()) {
			build = Gdx.files.internal("build.txt").readString("UTF-8").trim();
		}
		assets = new Assets();
		settings = new Settings();
		records = new Records(settings, options.persist, options.stats);
		audio = new Audio(settings);
		post = new PostProcessor();
		title = kept(new TitleScreen(this));
		tutorial = kept(new TutorialScreen(this));
		optionsScreen = kept(new OptionsScreen(this));
		highScoresScreen = kept(new HighScoresScreen(this));
		playersScreen = kept(new PlayersScreen(this));
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
		if (options.frameListener != null && s instanceof GameHost.Tagged) {
			options.frameListener.onFrame(((GameHost.Tagged) s).stateTag());
		}
	}

	/** While the effects pass is capturing a frame, draw into its (smaller) buffer rather than the screen. */
	@Override
	public void fitViewportToTarget() {
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
	@Override
	public void back() {
		setScreen(backStack.isEmpty() ? title : backStack.pop());
	}

	@Override
	public void showTitle() {
		backStack.clear();
		setScreen(title);
	}

	/** How to play, from Options: the tutorial, with Back. */
	@Override
	public void showTutorial() {
		tutorial.open(false);
		open(tutorial);
	}

	/** Play (a new game): the tutorial first if you've never seen it (it then starts the game). */
	@Override
	public void play() {
		if (!settings.tutorialSeen() && !options.autoplayHuman) {
			tutorial.open(true);
			open(tutorial);
		} else {
			newGame();
		}
	}

	@Override
	public void showOptions() {
		open(optionsScreen);
	}

	@Override
	public void showPlayers() {
		open(playersScreen);
	}

	/** The high score table; newest is the position just added (-1 for none) to pick out. */
	@Override
	public void showHighScores(int newest) {
		highScoresScreen.highlight(newest);
		open(highScoresScreen);
	}

	@Override
	public void newGame() {
		if (play != null) {
			play.dispose();
		}
		play = new PlayScreen(this, null, null, null);
		backStack.clear();
		setScreen(play);
	}

	/** Back to the game in progress, or the saved one after the app was closed. */
	@Override
	public void continueGame() {
		if (play == null) {
			SavedGame saved = records.savedGame(new Random());
			if (saved == null) {
				newGame();
				return;
			}
			play = new PlayScreen(this, saved.match(), saved.level(), saved.players());
		}
		backStack.clear();
		setScreen(play);
	}

	@Override
	public boolean hasGameInProgress() {
		if (play != null) {
			return !play.isGameOver();
		}
		return records.hasSavedGame();
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Screen s : keptScreens) {
			s.dispose();
		}
		if (play != null) {
			play.dispose();
		}
		audio.dispose();
		post.dispose();
		assets.dispose();
		batch.dispose();
	}
}
