package ohheck.table;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import ohheck.rules.Card;
import ohheck.rules.Match;

/* The table as its parts read it. */
interface TableScene {

	TableFlow flow();

	default Match match() {
		return flow().match();
	}

	default TableState state() {
		return flow().state();
	}

	default float stateTime() {
		return flow().stateTime();
	}

	float time();

	String name(int seat);

	default String habit(int seat) {
		return "";
	}

	TableLayout layout();

	List<Card> handOrder();

	List<Rectangle> handRects();

	boolean humanCanPlay();

	boolean isMenuOpen();

	default boolean inCampaign() {
		return false;
	}

	default boolean prizesWaiting() {
		return false;
	}

	boolean isDragging();

	Card pressedCard();

	float dragX();

	float dragY();

	boolean resultsShown();

	default float podiumTime() {
		return -1;
	}

	default float sinceResults() {
		return 0;
	}
}
