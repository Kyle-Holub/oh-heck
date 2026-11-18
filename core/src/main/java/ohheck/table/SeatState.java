package ohheck.table;

/**
 * What a player's placard shows: name, score, whether it's their turn or deal, and their bid
 * and tricks this hand. Built with named setters, so a call reads as what it shows:
 * {@code new SeatState().named("Rupert").bid(1).won(1).cards(3)}. No libGDX.
 */
public final class SeatState {

	/** No bid yet this hand. */
	static final int NO_BID = ohheck.rules.Match.NO_BID;

	String name = "";
	int score, bid = NO_BID, won, handSize = 1;
	boolean turn, dealer;
	/** Shows no bid and no hourglass: while the cards are dealt, or in the tutorial before bidding. */
	boolean plain;

	/** Back to an empty placard (for reuse each frame). */
	SeatState reset() {
		name = "";
		score = won = 0;
		bid = NO_BID;
		handSize = 1;
		turn = dealer = plain = false;
		return this;
	}

	public SeatState named(String n) {
		name = n;
		return this;
	}

	public SeatState score(int points) {
		score = points;
		return this;
	}

	/** Their bid this hand, or NO_BID. */
	public SeatState bid(int b) {
		bid = b;
		return this;
	}

	/** Tricks taken this hand. */
	public SeatState won(int tricks) {
		won = tricks;
		return this;
	}

	/** Cards dealt each this hand (sizes the trick icons). */
	public SeatState cards(int n) {
		handSize = n;
		return this;
	}

	public SeatState turn(boolean t) {
		turn = t;
		return this;
	}

	public SeatState dealer(boolean d) {
		dealer = d;
		return this;
	}

	public SeatState plain(boolean p) {
		plain = p;
		return this;
	}

	boolean hasBid() {
		return bid != NO_BID;
	}

	/** Yet to bid: sunk into the table with an hourglass. */
	boolean waiting() {
		return !hasBid() && !plain;
	}
}
