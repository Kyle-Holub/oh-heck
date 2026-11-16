package ohheck;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

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
		title = new TitleScreen(this);
		rules = new RulesScreen(this);
		optionsScreen = new OptionsScreen(this);
		setScreen(title);
	}

	@Override
	public void render() {
		super.render();
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
		play = new PlayScreen(this);
		setScreen(play);
	}

	void continueGame() {
		if (play == null) {
			newGame();
		} else {
			setScreen(play);
		}
	}

	boolean hasGameInProgress() {
		return play != null && !play.isGameOver();
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
		assets.dispose();
	}
}
