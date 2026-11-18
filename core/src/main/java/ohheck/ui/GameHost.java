package ohheck.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.platform.Audio;
import ohheck.platform.GameOptions;
import ohheck.platform.Settings;
import ohheck.records.Records;

/**
 * What a screen needs from the game around it: the shared tools (batch, assets, settings,
 * sound, records) and the way to the other screens. OhHeckGame is the host; screens depend
 * on this, not on it.
 */
public interface GameHost {

	/** "alpha-1.0.9". */
	String version();

	/** The CI build ("dev-0042"), or "" for a local build. */
	String build();

	GameOptions options();

	/** The one sprite batch all screens draw with. */
	SpriteBatch batch();

	Assets assets();

	Settings settings();

	Audio audio();

	PostProcessor post();

	/** Statistics, high scores and the saved game. */
	Records records();

	/** The screenshot run's walk through the menus; null in normal play. */
	Tour tour();

	/** Keeps the viewport matched to what the effects pass draws into. */
	void fitViewportToTarget();

	// ---- going places

	/** Back to the screen before (from the title: quit). */
	void back();

	void showTitle();

	void showTutorial();

	/** "Play": the tutorial the first time, then the table. */
	void play();

	void showOptions();

	void showPlayers();

	/** The high scores, with the entry at newest picked out (-1 for none). */
	void showHighScores(int newest);

	/** A fresh table, at the difficulty now chosen in Options. */
	void newGame();

	void continueGame();

	boolean hasGameInProgress();

	/** The screenshot run's walk through the menus: each menu screen calls step once a frame. */
	interface Tour {
		/** Moves the tour along; true if it has gone on to another screen (so this one stops drawing). */
		boolean step(BaseScreen screen, float shown);
	}

	/** A screen that can describe itself for automated screenshots. */
	interface Tagged {
		String stateTag();
	}
}
