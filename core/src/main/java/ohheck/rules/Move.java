package ohheck.rules;

/* A bid, a trump suit picked, a swap with the widow or a card played, as a value. */
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

	record PickTrump(int seat, Suit suit) implements Move {
		@Override
		public int applyTo(Match m) {
			m.pickTrump(seat, suit);
			return -1;
		}

		@Override
		public String toString() {
			return "seat " + seat + " picks " + suit;
		}
	}

	record SwapWidow(int seat, Card give, Card take) implements Move {
		@Override
		public int applyTo(Match m) {
			m.swapWidow(seat, give, take);
			return -1;
		}

		@Override
		public String toString() {
			return give == null ? "seat " + seat + " keeps their hand" : "seat " + seat + " swaps " + give + " for " + take;
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
