package ohheck.table;

import com.badlogic.gdx.math.MathUtils;

import ohheck.rules.Match;

/**
 * How each moment of the table ({@link TableState}) looks and answers you: what it shows
 * over the table, what a tap does, whether it's moving (so the frame rate stays up), and
 * how the screenshot run names it. One constant per moment, in the same order.
 */
enum StateView {
	SHUFFLE {
		@Override
		void tap(TableInput in, float x, float y) {
			in.skip();
		}

		@Override
		boolean animating(TableScene s) {
			return true;
		}

		@Override
		String tag(TableScene s, int hand) {
			return "shuffle:" + hand;
		}
	},
	DEAL {
		@Override
		void tap(TableInput in, float x, float y) {
			in.skip();
		}

		@Override
		boolean animating(TableScene s) {
			return true;
		}

		@Override
		String tag(TableScene s, int hand) {
			return "deal:" + hand;
		}
	},
	TURN {
		@Override
		void drawOverlay(TableScene s, int newHighScore) {
			if (s.flow().yourTurnToBid()) {
				s.overlays().drawBidPanel();
			}
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.tapTurn(x, y);
		}

		@Override
		boolean animating(TableScene s) {
			return s.flow().trumpTime() < TableFlow.TRUMP_FLIP_TIME;
		}

		@Override
		String tag(TableScene s, int hand) {
			Match m = s.match();
			boolean yours = m.currentSeat() == TableLayout.HUMAN;
			if (m.phase() == Match.Phase.BIDDING) {
				return (yours ? "bid:" : "bidding:") + hand;
			}
			return yours ? (s.isDragging() ? "drag:" : "play:") + hand + ":" + m.currentTrick().size() : "playing:" + hand;
		}
	},
	TRICK {
		@Override
		boolean animating(TableScene s) {
			return true;
		}

		@Override
		String tag(TableScene s, int hand) {
			float t = s.stateTime();
			return (t >= TrickView.TRICK_PAUSE ? "trickwon:"
					: t > TrickView.TRICK_PAUSE - TrickView.COLLECT_TIME ? "collect:" : "trick:") + hand;
		}
	},
	HAND_END {
		@Override
		void drawOverlay(TableScene s, int newHighScore) {
			s.overlays().drawHandEnd(s.stateTime());
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.skip();
		}

		@Override
		boolean animating(TableScene s) {
			return true;
		}

		@Override
		String tag(TableScene s, int hand) {
			return "handend:" + hand;
		}
	},
	RESULTS {
		@Override
		void drawOverlay(TableScene s, int newHighScore) {
			s.overlays().drawHandResults();
			s.overlays().drawHandLabels(MathUtils.clamp(s.stateTime() / LABEL_DROP, 0, 1), 1f);
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.nextHand();
		}

		@Override
		boolean animating(TableScene s) {
			return s.stateTime() < LABEL_DROP;
		}

		@Override
		String tag(TableScene s, int hand) {
			return "handover:" + hand;
		}
	},
	GAME_OVER {
		@Override
		void drawOverlay(TableScene s, int newHighScore) {
			if (s.resultsShown()) {
				s.overlays().drawGameOver(newHighScore);
			}
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.tapGameOver(x, y);
		}

		@Override
		String tag(TableScene s, int hand) {
			// the final hand's cards thrown off the table, then the scores
			return s.resultsShown() ? "gameover" : "tossing";
		}
	};

	/** Your "Bid tricks!"/"Set!" takes this long to glide beside your panel. */
	static final float LABEL_DROP = 0.6f;

	private static final StateView[] BY_STATE = new StateView[TableState.values().length];
	static {
		for (TableState s : TableState.values()) {
			BY_STATE[s.ordinal()] = valueOf(s.name()); // (fails at once if a moment has no view)
		}
	}

	/** The view of a moment. */
	static StateView of(TableState s) {
		return BY_STATE[s.ordinal()];
	}

	/** What the moment shows over the table (nothing, unless overridden). */
	void drawOverlay(TableScene s, int newHighScore) {
	}

	/** A tap anywhere but the menu button (nothing, unless overridden). */
	void tap(TableInput in, float x, float y) {
	}

	/** Whether the moment itself is moving. */
	boolean animating(TableScene s) {
		return false;
	}

	/** The screenshot run's name for what's on screen. */
	abstract String tag(TableScene s, int hand);
}
