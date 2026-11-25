package ohheck.table;

/*
 * Orders the game's end: the cards fall, then the podium rises a step at a time from last place up,
 * with a pause before the winner (and fireworks as they land, if that's you), then the scores. A duel has
 * no podium: fireworks if you won, then the scores. A tap skips straight to the scores.
 */
final class GameEnd {

	static final float STEP = 0.55f, DRUMROLL = 0.9f, HOLD = 1.5f, FIREWORKS_FIRST = 1.4f, SCORES_AFTER_CARDS = 0.3f;

	private boolean started, waitingForCards, celebrate, celebrated, podium;
	private int players;
	private float podiumAt = Float.MAX_VALUE;

	void start(boolean won, int players, boolean podium) {
		this.podium = podium;
		started = true;
		waitingForCards = true;
		celebrate = won;
		celebrated = false;
		this.players = players;
		podiumAt = Float.MAX_VALUE;
	}

	void reset() {
		started = false;
		waitingForCards = false;
		podiumAt = Float.MAX_VALUE;
	}

	static float risesAt(int place, int players) {
		return place == 1 ? (players - 1) * STEP + DRUMROLL : (players - place) * STEP;
	}

	boolean update(float stateTime, boolean cardsGone) {
		if (waitingForCards && cardsGone) {
			waitingForCards = false;
			podiumAt = stateTime;
		}
		if (celebrate && !celebrated && stateTime >= podiumAt + (podium ? risesAt(1, players) + STEP : 0)) {
			celebrated = true;
			return true;
		}
		return false;
	}

	float podiumTime(float stateTime) {
		return started && !waitingForCards && podium ? stateTime - podiumAt : -1;
	}

	float sinceResults(float stateTime) {
		return stateTime - podiumAt - (podium ? risesAt(1, players) + HOLD : celebrate ? FIREWORKS_FIRST : SCORES_AFTER_CARDS);
	}

	void skip(float stateTime) {
		if (!started) {
			return;
		}
		waitingForCards = false;
		celebrated = true;
		podiumAt = stateTime - (podium ? risesAt(1, players) + HOLD : FIREWORKS_FIRST);
	}

	boolean resultsShown(float stateTime) {
		return started && !waitingForCards && sinceResults(stateTime) >= 0;
	}
}
