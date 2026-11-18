package ohheck.table;

/**
 * The game's end, in turn: the table's cards are thrown off; once they've all fallen,
 * fireworks if you won; then the scores (a little after the fireworks start, or just after
 * the cards if you didn't win). Only timing, with nothing drawn, so it's tested on its own.
 */
final class GameEnd {

	/** After the cards: seconds of fireworks (if you won) before the scores, or a short pause before them. */
	static final float FIREWORKS_FIRST = 1.4f, SCORES_AFTER_CARDS = 0.3f;

	private boolean started, waitingForCards, celebrate;
	/** When (in game-over seconds) the scores appear, once known. */
	private float resultsAt = Float.MAX_VALUE;

	/** The game has just ended; won if you won (or tied for the win). */
	void start(boolean won) {
		started = true;
		waitingForCards = true;
		celebrate = won;
		resultsAt = Float.MAX_VALUE;
	}

	/** A new game: nothing ending. */
	void reset() {
		started = false;
		waitingForCards = false;
		resultsAt = Float.MAX_VALUE;
	}

	/**
	 * Each frame of the game-over state: stateTime seconds in, cardsGone once the last tossed
	 * card has fallen. Returns true at the moment the fireworks should start.
	 */
	boolean update(float stateTime, boolean cardsGone) {
		if (!waitingForCards || !cardsGone) {
			return false;
		}
		waitingForCards = false;
		resultsAt = stateTime + (celebrate ? FIREWORKS_FIRST : SCORES_AFTER_CARDS);
		return celebrate;
	}

	/** Whether the scores are up, stateTime seconds into the game-over state. */
	boolean resultsShown(float stateTime) {
		return started && !waitingForCards && stateTime >= resultsAt;
	}
}
