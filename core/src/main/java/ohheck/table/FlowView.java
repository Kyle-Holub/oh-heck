package ohheck.table;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;

/**
 * The table's flow ({@link TableFlow}) as the parts that draw it read it: the game, the
 * moment it's in and for how long, and the deal. It can't be moved on from here; your
 * moves go through {@link TableActions}.
 */
interface FlowView {

	Match match();

	TableState state();

	/** Seconds in the current state. */
	float stateTime();

	/** Seconds since the deal finished, for the trump card turning over. */
	float trumpTime();

	boolean isDealing();

	boolean trumpFaceUp();

	boolean yourTurnToBid();

	boolean yourTurnToPlay();

	/** The shuffle and deal of the current hand. */
	Deal deal();

	/** The shuffle and deal, as drawn: who deals, and when each card leaves. */
	interface Deal {
		int dealer();

		boolean isShuffling();

		/** Seconds since the dealing began. */
		float dealTime();

		/** When the j-th card to a seat leaves the dealer, in deal time. */
		float startOf(int seat, int j);

		/** The deck being riffled in the middle of the table, while shuffling. */
		void drawShuffle(SpriteBatch batch, Assets assets, CardArt art, TableLayout table);
	}
}
