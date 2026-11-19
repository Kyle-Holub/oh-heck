package ohheck.rules;

/** Which way the hand sizes run: up from one card, or down to it. */
public enum HandOrder {
	UP, DOWN;

	public String label() {
		int n = Rules.numberOfHands();
		return this == UP ? "1 to " + n : n + " to 1";
	}

	public int handSize(int handIndex, int hands) {
		if (handIndex < 0 || handIndex >= hands) {
			throw new IllegalArgumentException("No hand " + handIndex);
		}
		return this == UP ? handIndex + 1 : hands - handIndex;
	}

	public int handSize(int handIndex) {
		return handSize(handIndex, Rules.numberOfHands());
	}

	public HandOrder flipped() {
		return this == UP ? DOWN : UP;
	}
}
