package ohheck.table;

/*
 * Orders the game's end: the cards fall, then the podium rises a step at a time from last place up,
 * with a pause before the winner (and fireworks as they land, if that's you), then waits for a tap before
 * the scores. A tap while the podium is rising finishes it. A duel has no podium: fireworks if you won,
 * then the scores. A game playing itself goes on after a moment instead of waiting.
 */
final class GameEnd {

	static final float STEP = 1.0f, DRUMROLL = 1.6f, RISE = 0.7f, HOLD = 1.5f, FIREWORKS_FIRST = 1.4f, SCORES_AFTER_CARDS = 0.3f;

	private boolean started, waitingForCards, celebrate, celebrated, podium, waitForTap, tapped;
	private int players;
	private float podiumAt = Float.MAX_VALUE;

	void start(boolean won, int players, boolean podium, boolean waitForTap) {
		started = true;
		waitingForCards = true;
		celebrate = won;
		celebrated = false;
		this.players = players;
		this.podium = podium;
		this.waitForTap = waitForTap;
		tapped = false;
		podiumAt = Float.MAX_VALUE;
	}

	void reset() {
		started = false;
		waitingForCards = false;
		podiumAt = Float.MAX_VALUE;
	}

	static float risesAt(int place, int players) {
		return place == 1 ? (players - 2) * STEP + DRUMROLL : (players - place) * STEP;
	}

	private float podiumDone() {
		return risesAt(1, players) + RISE;
	}

	boolean update(float stateTime, boolean cardsGone) {
		if (waitingForCards && cardsGone) {
			waitingForCards = false;
			podiumAt = stateTime;
		}
		if (celebrate && !celebrated && stateTime >= podiumAt + (podium ? podiumDone() : 0)) {
			celebrated = true;
			return true;
		}
		return false;
	}

	float podiumTime(float stateTime) {
		return started && !waitingForCards && podium ? stateTime - podiumAt : -1;
	}

	boolean podiumDone(float stateTime) {
		return podiumTime(stateTime) >= podiumDone();
	}

	float sinceResults(float stateTime) {
		float wait = podium ? podiumDone() + (waitForTap ? 0 : HOLD) : celebrate ? FIREWORKS_FIRST : SCORES_AFTER_CARDS;
		return stateTime - podiumAt - wait;
	}

	boolean resultsShown(float stateTime) {
		boolean tapNeeded = podium && waitForTap;
		return started && !waitingForCards && sinceResults(stateTime) >= 0 && (!tapNeeded || tapped);
	}

	void tap(float stateTime) {
		if (!started) {
			return;
		}
		if (waitingForCards || podium && !podiumDone(stateTime)) {
			waitingForCards = false;
			podiumAt = stateTime - (podium ? podiumDone() : FIREWORKS_FIRST);
		} else {
			tapped = true;
		}
	}
}
