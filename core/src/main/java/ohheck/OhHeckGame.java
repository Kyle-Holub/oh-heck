package ohheck;

import java.util.ArrayList;
import java.util.List;
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

/** The libGDX entry point shared by Android and desktop: makes the shared things and the screens, and runs the frames. */
public class OhHeckGame extends Game implements GameHost {

	public static final String VERSION = "alpha-1.0.9";

	// ---- how it was launched (set before create)

	private final GameOptions options;
	/** The screenshot run's walk through the menus; null in normal play. */
	private final ScreenshotTour tour;

	// ---- what the screens share (made in create(), so not final)

	/** The CI build ("dev-0042") from assets/build.txt, or "" for a local build. */
	private String build = "";
	/** The one sprite batch all screens draw with. */
	private SpriteBatch batch;
	private Assets assets;
	private Settings settings;
	private Audio audio;
	private PostProcessor post;
	/** Statistics, high scores and the saved game. */
	private Records records;

	// ---- the screens

	/** Which screen is showing, the way back, and the change between them. */
	private final Screens screens = new Screens(this);
	private TitleScreen title;
	private TutorialScreen tutorial;
	private OptionsScreen optionsScreen;
	private HighScoresScreen highScoresScreen;
	private PlayersScreen playersScreen;
	/** The screens made once and kept for the whole run (all but the table), released together. */
	private final List<Screen> keptScreens = new ArrayList<Screen>();
	/** The table, made for each game; null before the first. */
	private PlayScreen play;

	// ---- the frames

	private final FramePacer pacer = new FramePacer();
	/** Whether the effects pass is capturing this frame (so the viewport fits its buffer). */
	private boolean capturing;

	public OhHeckGame() {
		this(new GameOptions());
	}

	public OhHeckGame(GameOptions options) {
		this.options = options;
		this.tour = options.autoplayHuman ? new ScreenshotTour(this) : null;
	}

	@Override
	public void create() {
		Log.to((tag, message, cause) -> { // reports go to the device log
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
		records = new Records(settings.recordStore(), options.persist, options.stats);
		audio = new Audio(settings);
		post = new PostProcessor();
		title = kept(new TitleScreen(this));
		tutorial = kept(new TutorialScreen(this));
		optionsScreen = kept(new OptionsScreen(this));
		highScoresScreen = kept(new HighScoresScreen(this));
		playersScreen = kept(new PlayersScreen(this));
		screens.replaceAll(title);
	}

	private <T extends Screen> T kept(T screen) {
		keptScreens.add(screen);
		return screen;
	}

	@Override
	public void render() {
		pacer.startFrame();
		boolean effects = settings.effects() && post.begin();
		capturing = effects;
		boolean fixed = options.fixedStep > 0; // the screenshot run: fixed time steps
		float delta = fixed ? options.fixedStep : Gdx.graphics.getDeltaTime();
		if (getScreen() != null) {
			getScreen().render(delta);
		}
		if (!fixed) { // (the screenshot run captures screens settled, not mid-change)
			screens.drawChange(batch, assets, delta); // a new screen comes up out of a dark tint
		}
		if (effects) {
			post.end(delta);
		}
		capturing = false;
		Screen s = getScreen();
		pacer.endFrame(!fixed && !screens.changing() && s instanceof BaseScreen && !((BaseScreen) s).animating());
		if (options.frameListener != null && s instanceof GameHost.Tagged) {
			options.frameListener.onFrame(((GameHost.Tagged) s).stateTag());
		}
	}

	// ---- Services

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

	@Override
	public Records records() {
		return records;
	}

	@Override
	public GameHost.Tour tour() {
		return tour;
	}

	/** While the effects pass is capturing a frame, draw into its (smaller) buffer rather than the screen. */
	@Override
	public void fitViewportToTarget() {
		if (capturing) {
			Gdx.gl.glViewport(0, 0, post.sceneWidth(), post.sceneHeight());
		}
	}

	// ---- Navigator

	/** Back to the screen that opened this one, or the title screen. */
	@Override
	public void back() {
		screens.back(title);
	}

	@Override
	public void showTitle() {
		screens.replaceAll(title);
	}

	/** How to play, from Options: the tutorial, with Back. */
	@Override
	public void showTutorial() {
		tutorial.open(false);
		screens.open(tutorial);
	}

	/** Play (a new game): the tutorial first if you've never seen it (it then starts the game). */
	@Override
	public void play() {
		if (!settings.tutorialSeen() && !options.autoplayHuman) {
			tutorial.open(true);
			screens.open(tutorial);
		} else {
			newGame();
		}
	}

	@Override
	public void showOptions() {
		screens.open(optionsScreen);
	}

	@Override
	public void showPlayers() {
		screens.open(playersScreen);
	}

	@Override
	public void showHighScores(int newest) {
		highScoresScreen.highlight(newest);
		screens.open(highScoresScreen);
	}

	@Override
	public void newGame() {
		if (play != null) {
			play.dispose();
		}
		play = PlayScreen.fresh(this);
		screens.replaceAll(play);
	}

	@Override
	public void continueGame() {
		if (play == null) {
			SavedGame saved = records.savedGame(new Random());
			if (saved == null) {
				newGame();
				return;
			}
			play = PlayScreen.resume(this, saved);
		}
		screens.replaceAll(play);
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
