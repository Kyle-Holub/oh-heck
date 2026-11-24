package ohheck.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.campaign.Chapter;
import ohheck.campaign.TrickShot;
import ohheck.platform.Audio;
import ohheck.platform.GameOptions;
import ohheck.platform.Settings;
import ohheck.records.Records;

/* Everything a screen needs from the game: the shared services and the ways between screens. */
public interface GameHost {

	String version();

	String build();

	GameOptions options();

	SpriteBatch batch();

	Assets assets();

	Settings settings();

	Audio audio();

	PostProcessor post();

	Records records();

	void fitViewportToTarget();

	void back();

	void showTitle();

	void showTutorial();

	void play();

	void showPlayMenu();

	void showGameSetup();

	void showOptions();

	void showPlayers();

	void showHighScores(int newest);

	void showCampaign();

	void startCampaign();

	void playCampaign(Chapter chapter, boolean duel);

	void playTrickShot(TrickShot shot);

	void newGame();

	void continueGame();

	boolean hasGameInProgress();

	void startFresh();
}
