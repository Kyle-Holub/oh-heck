package ohheck.ui;

public interface Navigator {

	void back();

	void showTitle();

	void showTutorial();

	void play();

	void showOptions();

	void showPlayers();

	void showHighScores(int newest);

	void newGame();

	void continueGame();

	boolean hasGameInProgress();
}
