package ohheck.table;

import ohheck.rules.Card;

/**
 * What your taps at the table can do ({@link TableInput} calls these): your moves, moving
 * the table on, a hint when a tap can't count, and the ways off the table.
 */
interface TableActions {

	/** Your bid; false (and nothing happens) if it isn't your turn to bid or the bid isn't allowed. */
	boolean bid(int b);

	/** Plays your card (it flies to the trick from wherever it is). */
	void play(Card c);

	/** Hurries the shuffle and deal, or the hand-end message, along; false if there was nothing to skip. */
	boolean skip();

	/** From the hand's scores on to the next hand (or the final scores); false if the scores aren't up. */
	boolean nextHand();

	/** A short note over the table, for why a tap didn't count. */
	void hint(String s);

	/** The high score table, with the game just finished picked out. */
	void showHighScores();

	/** A fresh table, at the difficulty now chosen in Options. */
	void newGame();

	/** Options, over the table (the pause menu stays open for when you come back). */
	void showOptions();

	void showTitle();
}
