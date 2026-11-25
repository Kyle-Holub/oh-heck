package ohheck.table;

/*
 * Orders the game's end: the cards fall, then the podium rises a step at a time from last place up,
 * with a pause before the winner (and fireworks as they land, if that's you), then the scores.
 */
final class GameEnd {

	static final float STEP = 0.55f, DRUMROLL = 0.9f, HOLD = 1.5f;

	private boolean started, waitingForCards, celebrate, celebrated;
	private int players;
	private float podiumAt = Float.MAX_VALUE;

	void start(boolean won, int players) {
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
		if (celebrate && !celebrated && stateTime >= podiumAt + risesAt(1, players) + STEP) {
			celebrated = true;
			return true;
		}
		return false;
	}

	float podiumTime(float stateTime) {
		return started && !waitingForCards ? stateTime - podiumAt : -1;
	}

	float sinceResults(float stateTime) {
		return podiumTime(stateTime) - risesAt(1, players) - HOLD;
	}

	boolean resultsShown(float stateTime) {
		return started && !waitingForCards && sinceResults(stateTime) >= 0;
	}
}
