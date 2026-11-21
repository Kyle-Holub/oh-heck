package ohheck.table;

/* Orders the game's end: the cards fall, then fireworks if you won, then the scores. */
final class GameEnd {

	static final float FIREWORKS_FIRST = 1.4f, SCORES_AFTER_CARDS = 0.3f;

	private boolean started, waitingForCards, celebrate;
	private float resultsAt = Float.MAX_VALUE;

	void start(boolean won) {
		started = true;
		waitingForCards = true;
		celebrate = won;
		resultsAt = Float.MAX_VALUE;
	}

	void reset() {
		started = false;
		waitingForCards = false;
		resultsAt = Float.MAX_VALUE;
	}

	boolean update(float stateTime, boolean cardsGone) {
		if (!waitingForCards || !cardsGone) {
			return false;
		}
		waitingForCards = false;
		resultsAt = stateTime + (celebrate ? FIREWORKS_FIRST : SCORES_AFTER_CARDS);
		return celebrate;
	}

	boolean resultsShown(float stateTime) {
		return started && !waitingForCards && stateTime >= resultsAt;
	}
}
