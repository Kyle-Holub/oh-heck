package ohheck.table;

import ohheck.rules.Match;

/**
 * What the card table is showing, and how each moment behaves: what happens on entering
 * it, as time passes, and on a skip. {@link TableFlow} is always in exactly one of these,
 * with one clock for how long it has been there; the pause menu sits on top of any of them.
 * How each looks and answers a tap is its {@link StateView}.
 *
 * <pre>
 * SHUFFLE -> DEAL -> TURN <-> TRICK -> HAND_END -> RESULTS -> SHUFFLE (next hand)
 *                                                          -> GAME_OVER
 * </pre>
 * To add a moment: a constant here (its flow) and one in StateView (its look).
 */
enum TableState {
	/** The deck comes to the middle and is riffled. */
	SHUFFLE {
		@Override
		void update(TableFlow f) {
			f.dealing();
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(TURN);
			return true;
		}
	},
	/** Cards fly to each seat. */
	DEAL {
		@Override
		void update(TableFlow f) {
			f.dealing();
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(TURN);
			return true;
		}
	},
	/** Someone is bidding or choosing a card; a computer player moves after a short pause. */
	TURN {
		@Override
		void update(TableFlow f) {
			f.computerTurn();
		}
	},
	/** A completed trick stays on the table, then the winner gathers it in. */
	TRICK {
		@Override
		void update(TableFlow f) {
			Match m = f.match();
			if (f.stateTime() >= TrickView.showLength(m.lastTrickWinner() == TableFlow.HUMAN)) {
				f.enter(m.phase() == Match.Phase.HAND_OVER ? HAND_END : TURN);
			}
		}
	},
	/** "Bid Tricks!" or "You got set!" */
	HAND_END {
		@Override
		void entered(TableFlow f) {
			f.tellHandEnded();
		}

		@Override
		void update(TableFlow f) {
			if (f.stateTime() >= TableFlow.HAND_END_SHOW) {
				f.enter(RESULTS);
			}
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(RESULTS);
			return true;
		}
	},
	/** The hand's score table, until you tap on. */
	RESULTS,
	/** Final scores. */
	GAME_OVER {
		@Override
		void entered(TableFlow f) {
			f.tellGameEnded();
		}
	};

	/** On arriving in this moment. */
	void entered(TableFlow f) {
	}

	/** Each frame, after the clock has moved on. */
	void update(TableFlow f) {
	}

	/** A tap that hurries the moment along; false if there's nothing to skip. */
	boolean skip(TableFlow f) {
		return false;
	}

	boolean isDealing() {
		return this == SHUFFLE || this == DEAL;
	}
}
