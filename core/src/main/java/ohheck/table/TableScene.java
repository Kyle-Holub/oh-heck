package ohheck.table;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import ohheck.campaign.PracticeCoach;
import ohheck.players.Seating;
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

	default boolean finishing() {
		return false;
	}

	default boolean inPractice() {
		return false;
	}

	default String placardName(int seat) {
		return name(seat);
	}

	default boolean playable(Card c) {
		return match().legalPlays(Seating.YOU).contains(c);
	}

	default boolean bidAllowed(int b) {
		return true;
	}

	default PracticeCoach.Tip tip() {
		return null;
	}

	default boolean inCampaign() {
		return false;
	}

	default boolean prizesWaiting() {
		return false;
	}

	boolean isDragging();

	Card pressedCard();

	default Card hoveredCard() {
		return null;
	}

	default Card refusedCard() {
		return null;
	}

	default float refusedFor() {
		return 99;
	}

	float dragX();

	float dragY();

	boolean resultsShown();

	default float podiumTime() {
		return -1;
	}

	default boolean podiumDone() {
		return false;
	}

	default float sinceResults() {
		return 0;
	}
}
