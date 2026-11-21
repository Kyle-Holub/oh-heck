package ohheck.rules;

import java.util.List;

/* Applies a log of moves to a game. */
public final class Replay {

	private Replay() {
	}

	public static Match onto(Match m, List<Move> moves) {
		for (Move move : moves) {
			while (m.phase() == Match.Phase.HAND_OVER) {
				m.nextHand();
			}
			m.apply(move);
		}
		return m;
	}
}
