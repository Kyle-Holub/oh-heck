package ohheck.table;

/**
 * What the card table is showing. {@link PlayScreen} is always in exactly one
 * of these, with one clock for how long it has been there; the pause menu sits
 * on top of any of them.
 *
 * <pre>
 * SHUFFLE -> DEAL -> TURN <-> TRICK -> HAND_END -> RESULTS -> SHUFFLE (next hand)
 *                                                          -> GAME_OVER
 * </pre>
 * A tap skips the shuffle and deal, and the hand-end message.
 */
enum TableState {
	/** The deck comes to the middle and is riffled. */
	SHUFFLE,
	/** Cards fly to each seat. */
	DEAL,
	/** Someone is bidding or choosing a card; a computer player moves after a short pause. */
	TURN,
	/** A completed trick stays on the table, then the winner gathers it in. */
	TRICK,
	/** "Bid Tricks!" or "You got set!" */
	HAND_END,
	/** The hand's score table. */
	RESULTS,
	/** Final scores. */
	GAME_OVER
}
