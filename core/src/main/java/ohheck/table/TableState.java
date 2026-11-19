package ohheck.table;

import ohheck.rules.Match;

/** The table's moments and how each behaves on entry, over time and on a skip. */
enum TableState {
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
	TURN {
		@Override
		void update(TableFlow f) {
			f.computerTurn();
		}
	},
	TRICK {
		@Override
		void update(TableFlow f) {
			Match m = f.match();
			if (f.stateTime() >= TrickView.showLength(m.lastTrickWinner() == TableFlow.HUMAN)) {
				f.enter(m.phase() == Match.Phase.HAND_OVER ? HAND_END : TURN);
			}
		}
	},
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
	RESULTS,
	GAME_OVER {
		@Override
		void entered(TableFlow f) {
			f.tellGameEnded();
		}
	};

	void entered(TableFlow f) {
	}

	void update(TableFlow f) {
	}

	boolean skip(TableFlow f) {
		return false;
	}

	boolean isDealing() {
		return this == SHUFFLE || this == DEAL;
	}
}
