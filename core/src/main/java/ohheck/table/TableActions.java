package ohheck.table;

import ohheck.rules.Card;

/* What taps at the table can do. */
interface TableActions {

	boolean bid(int b);

	void play(Card c);

	boolean skip();

	boolean nextHand();

	void hint(String s);

	void showHighScores();

	void newGame();

	void showOptions();

	void showTitle();

	default void forceWin() {
	}

	default void answerTutorialOffer(boolean take) {
	}
}
