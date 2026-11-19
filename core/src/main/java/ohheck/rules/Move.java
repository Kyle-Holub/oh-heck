package ohheck.rules;

/** A bid or a card played, as a value. */
public interface Move {

	int seat();

	int applyTo(Match m);

	record Bid(int seat, int tricks) implements Move {
		@Override
		public int applyTo(Match m) {
			m.placeBid(seat, tricks);
			return -1;
		}

		@Override
		public String toString() {
			return "seat " + seat + " bids " + tricks;
		}
	}

	record Play(int seat, Card card) implements Move {
		@Override
		public int applyTo(Match m) {
			return m.playCard(seat, card);
		}

		@Override
		public String toString() {
			return "seat " + seat + " plays " + card;
		}
	}
}
