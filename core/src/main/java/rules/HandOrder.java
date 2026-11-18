package rules;

/** Which way a game's hands run: one card up to the most (the classic way), or the most down to one. */
public enum HandOrder {
	UP, DOWN;

	/** As shown in the options: "1 to 8", "8 to 1". */
	public String label() {
		int n = Rules.numberOfHands();
		return this == UP ? "1 to " + n : n + " to 1";
	}

	/** Cards per player in the given hand (0-based) of a game of this many hands. */
	public int handSize(int handIndex, int hands) {
		if (handIndex < 0 || handIndex >= hands) {
			throw new IllegalArgumentException("No hand " + handIndex);
		}
		return this == UP ? handIndex + 1 : hands - handIndex;
	}

	/** Cards per player in the given hand (0-based) of a standard game. */
	public int handSize(int handIndex) {
		return handSize(handIndex, Rules.numberOfHands());
	}

	/** The other way round. */
	public HandOrder flipped() {
		return this == UP ? DOWN : UP;
	}
}
