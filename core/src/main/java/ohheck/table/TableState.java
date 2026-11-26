package ohheck.table;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.players.Seating;
import ohheck.rules.Match;

/*
 * The table's moments: how each behaves on entry, over time and on a skip, what it shows
 * over the table and what a tap does.
 */
enum TableState {
	VERSUS {
		@Override
		void update(TableFlow f, float dt) {
			if (f.stateTime() >= TableOverlays.VERSUS_LENGTH) {
				f.enter(PICK_DEALER);
			}
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(PICK_DEALER);
			return true;
		}

		@Override
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			o.drawVersus(s.stateTime());
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
			return "versus";
		}
	},
	PICK_DEALER {
		@Override
		void update(TableFlow f, float dt) {
			if (f.stateTime() >= DealerPick.LENGTH) {
				f.startHand();
			}
		}

		@Override
		boolean skip(TableFlow f) {
			f.startHand();
			return true;
		}

		@Override
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			o.drawDealerPick(s.stateTime());
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
			return "picking";
		}
	},
	SHUFFLE {
		@Override
		void update(TableFlow f, float dt) {
			f.dealing(dt);
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(TURN);
			return true;
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
			return "shuffle:" + hand;
		}
	},
	DEAL {
		@Override
		void update(TableFlow f, float dt) {
			f.dealing(dt);
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(TURN);
			return true;
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
			return "deal:" + hand;
		}
	},
	TURN {
		@Override
		void update(TableFlow f, float dt) {
			f.computerTurn();
		}

		@Override
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			if (s.flow().yourTurnToBid()) {
				o.drawBidPanel();
			} else if (s.flow().yourTurnToPick()) {
				o.drawPickTrump();
			} else if (s.flow().yourTurnToSwap()) {
				o.drawWidow();
			}
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.tapTurn(x, y);
		}

		@Override
		boolean animating(TableScene s) {
			return s.flow().trumpTime() < TableFlow.TRUMP_FLIP_TIME + TableDrawing.PUFF_LEN;
		}

		@Override
		String tag(TableScene s, int hand) {
			Match m = s.match();
			boolean yours = m.currentSeat() == Seating.YOU;
			if (m.pickingTrump()) {
				return (yours ? "pick:" : "picking-trump:") + hand;
			}
			if (m.swapping()) {
				return (yours ? "widow:" : "swapping:") + hand;
			}
			if (m.phase() == Match.Phase.BIDDING) {
				return (yours ? "bid:" : "bidding:") + hand;
			}
			return yours ? (s.isDragging() ? "drag:" : "play:") + hand + ":" + m.currentTrick().size() : "playing:" + hand;
		}
	},
	TRICK {
		@Override
		void update(TableFlow f, float dt) {
			Match m = f.match();
			if (f.stateTime() >= TrickView.showLength(m.lastTrickWinner() == Seating.YOU)) {
				f.enter(m.phase() == Match.Phase.HAND_OVER ? HAND_END : TURN);
			}
		}

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
		void entered(TableFlow f) {
			f.tellHandEnded();
		}

		@Override
		void update(TableFlow f, float dt) {
			if (f.stateTime() >= TableFlow.HAND_END_SHOW) {
				f.enter(RESULTS);
			}
		}

		@Override
		boolean skip(TableFlow f) {
			f.enter(RESULTS);
			return true;
		}

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
			o.drawHandResults(s.stateTime());
			o.drawHandLabels(LABELS_DROP.progress(s.stateTime()), 1f);
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.nextHand();
		}

		@Override
		boolean animating(TableScene s) {
			return s.stateTime() < Math.max(LABEL_DROP, ScoreCard.LENGTH);
		}

		@Override
		String tag(TableScene s, int hand) {
			return "handover:" + hand;
		}
	},
	GAME_OVER {
		@Override
		void entered(TableFlow f) {
			f.tellGameEnded();
		}

		@Override
		void drawOverlay(TableScene s, TableOverlays o, int newHighScore) {
			if (s.resultsShown()) {
				o.drawGameOver(newHighScore, s.sinceResults());
			} else if (s.podiumTime() >= 0) {
				o.drawPodium(s.podiumTime());
			}
		}

		@Override
		void tap(TableInput in, float x, float y) {
			in.tapGameOver(x, y);
		}

		@Override
		boolean animating(TableScene s) {
			return !s.resultsShown() || s.sinceResults() < TableOverlays.NEW_BEST_LENGTH;
		}

		@Override
		String tag(TableScene s, int hand) {
			return s.resultsShown() ? "gameover" : "tossing";
		}
	};


	void entered(TableFlow f) {
	}

	void update(TableFlow f, float dt) {
	}

	boolean skip(TableFlow f) {
		return false;
	}

	boolean isDealing() {
		return this == VERSUS || this == PICK_DEALER || this == SHUFFLE || this == DEAL;
	}


	static final float LABEL_DROP = 0.6f;
	private static final Tween LABELS_DROP = Tween.over(LABEL_DROP, Ease.LINEAR);


	static String describe(TableScene s) {
		int hand = s.match().handIndex() + 1;
		return s.isMenuOpen() ? "menu:" + hand : s.state().tag(s, hand);
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
