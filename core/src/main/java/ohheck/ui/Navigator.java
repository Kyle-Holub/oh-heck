package ohheck.ui;

/** The ways between screens. */
public interface Navigator {

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

	/** Back to the game in progress, or the saved one after the app was closed. */
	void continueGame();

	boolean hasGameInProgress();
}
