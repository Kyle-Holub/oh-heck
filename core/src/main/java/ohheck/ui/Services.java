package ohheck.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.platform.Audio;
import ohheck.platform.GameOptions;
import ohheck.platform.Settings;
import ohheck.records.Records;

/** The shared tools a screen draws and plays with: batch, assets, settings, sound, records. */
public interface Services {

	/** "alpha-1.0.9". */
	String version();

	/** The CI build ("dev-0042"), or "" for a local build. */
	String build();

	/** How the launcher runs the game (normal play, or the screenshot run). */
	GameOptions options();

	/** The one sprite batch all screens draw with. */
	SpriteBatch batch();

	Assets assets();

	Settings settings();

	Audio audio();

	PostProcessor post();

	/** Statistics, high scores and the saved game. */
	Records records();

	/** Keeps the viewport matched to what the effects pass draws into. */
	void fitViewportToTarget();

	/** The screenshot run's walk through the menus; null in normal play. */
	GameHost.Tour tour();
}
