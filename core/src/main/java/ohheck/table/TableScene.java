package ohheck.table;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import ohheck.rules.Card;
import ohheck.rules.Match;

/**
 * The table as its helpers read it ({@link TableDrawing}, {@link CardSprites},
 * {@link TableOverlays}, {@link TableInput}, {@link Autoplay}): the game and its timing,
 * who's who, your hand's layout and what your finger is doing. Nothing here changes the
 * table; that goes through {@link TableActions}. {@link PlayScreen} is the table.
 */
interface TableScene {

	/** The game, the moment it's in and the deal: read only. */
	FlowView flow();

	default Match match() {
		return flow().match();
	}

	default TableState state() {
		return flow().state();
	}

	default float stateTime() {
		return flow().stateTime();
	}

	/** Game time, for animations; stops while paused. */
	float time();

	/** "You", or the name of the computer player in that seat. */
	String name(int seat);

	/** Where things go on the table, for the current screen shape. */
	TableLayout layout();

	/** Your hand in display order, and each card's tap area (rebuilt when drawn). */
	List<Card> handOrder();

	List<Rectangle> handRects();

	/** Your turn to play a card, and the menu's not open. */
	boolean humanCanPlay();

	boolean isMenuOpen();

	boolean isDragging();

	/** The card under your finger, or null. */
	Card pressedCard();

	/** Where your finger is while dragging a card. */
	float dragX();

	float dragY();

	/** Game over and the final scores are up. */
	boolean resultsShown();
}
