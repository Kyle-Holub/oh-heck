package ohheck.rules;

/**
 * A player's move, as a value: a {@link Bid} or a {@link Play}. Whoever decides it (you,
 * a computer player, a replay) hands it to {@link Match#apply}, and the match keeps a log of
 * every move made ({@link Match#moves}), so a game can be played back exactly
 * ({@link Replay}).
 */
public interface Move {

	/** Who's moving. */
	int seat();

	/** Makes the move (it must be legal); returns the seat that won a trick it completed, else -1. */
	int applyTo(Match m);

	/** A bid of so many tricks. */
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

	/** A card played to the trick. */
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
