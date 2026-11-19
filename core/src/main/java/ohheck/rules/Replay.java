package ohheck.rules;

import java.util.List;

/** Plays a log of moves onto a game, moving on to each next hand as it comes. */
public final class Replay {

	private Replay() {
	}

	/**
	 * Applies the moves to m in order. Started on a game dealt from the same seed as the
	 * one they came from, it reaches exactly the same position.
	 */
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
