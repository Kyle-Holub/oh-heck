package ohheck.table;

/* What a placard shows. */
public final class SeatState {

	static final int NO_BID = ohheck.rules.Match.NO_BID;

	String name = "";
	int score, bid = NO_BID, won, handSize = 1;
	boolean turn, dealer, doubled;
	boolean plain;

	public SeatState reset() {
		name = "";
		score = won = 0;
		bid = NO_BID;
		handSize = 1;
		turn = dealer = plain = doubled = false;
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

	public SeatState bid(int b) {
		bid = b;
		return this;
	}

	public SeatState won(int tricks) {
		won = tricks;
		return this;
	}

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

	public SeatState doubled(boolean d) {
		doubled = d;
		return this;
	}

	public SeatState plain(boolean p) {
		plain = p;
		return this;
	}

	boolean hasBid() {
		return bid != NO_BID;
	}

	boolean waiting() {
		return !hasBid() && !plain;
	}
}
