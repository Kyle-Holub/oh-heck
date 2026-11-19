package ohheck.table;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.players.Seating;
import ohheck.rules.Match;

/* How each TableState looks and answers taps. */
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
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			if (s.flow().yourTurnToBid()) {
				o.drawBidPanel();
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
			boolean yours = m.currentSeat() == Seating.YOU;
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
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			o.drawHandEnd(s.stateTime());
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
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			o.drawHandResults();
			o.drawHandLabels(LABELS_DROP.progress(s.stateTime()), 1f);
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
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			if (s.resultsShown()) {
				o.drawGameOver(newHighScore);
			}
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.tapGameOver(x, y);
		}

		@Override
		String tag(TableScene s, int hand) {
			return s.resultsShown() ? "gameover" : "tossing";
		}
	};

	static final float LABEL_DROP = 0.6f;
	private static final Tween LABELS_DROP = Tween.over(LABEL_DROP, Ease.LINEAR);

	private static final StateView[] BY_STATE = new StateView[TableState.values().length];
	static {
		for (TableState s : TableState.values()) {
			BY_STATE[s.ordinal()] = valueOf(s.name());
		}
	}

	static String describe(TableScene s) {
		int hand = s.match().handIndex() + 1;
		return s.isMenuOpen() ? "menu:" + hand : of(s.state()).tag(s, hand);
	}

	static StateView of(TableState s) {
		return BY_STATE[s.ordinal()];
	}

	void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
	}

	void tap(TableInput in, float x, float y) {
	}

	boolean animating(TableScene s) {
		return false;
	}

	abstract String tag(TableScene s, int hand);
}
