package ohheck.table;

import ohheck.rules.Card;
import ohheck.rules.Suit;

/* What taps at the table can do. */
interface TableActions {

	boolean bid(int b);

	void play(Card c);

	boolean pickTrump(Suit s);

	boolean swapWidow(Card give, Card take);

	boolean skip();

	boolean nextHand();

	void hint(String s);

	void showHighScores();

	void newGame();

	void showOptions();

	void showTitle();

	default void forceWin() {
	}

	default void refuseBid(int b) {
	}

	default void refuseCard(Card c) {
	}

	default void touchedCard() {
	}

	default void skipGameEnd() {
	}

	default void dismissTip() {
	}
}
