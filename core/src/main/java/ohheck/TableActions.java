package ohheck;

import rules.Card;

/** What your taps at the table can do beyond the table itself ({@link TableInput} calls these). */
interface TableActions {

	/** Plays your card, flying in from (fromX, fromY). */
	void humanPlay(Card c, float fromX, float fromY);

	/** The high score table, with the game just finished picked out. */
	void showHighScores();

	/** A fresh table, at the difficulty now chosen in Options. */
	void newGame();

	/** Options, over the table (the pause menu stays open for when you come back). */
	void showOptions();

	void showTitle();
}
