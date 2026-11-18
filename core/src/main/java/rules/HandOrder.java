package rules;

/** Which way a game's eight hands run: one card up to eight (the classic way), or eight down to one. */
public enum HandOrder {
	UP("Ascending"), DOWN("Descending"); // (1 card up to 8, or 8 down to 1)

	/** Hands in a game, and the most cards in any of them. */
	public static final int HANDS = 8;

	/** As shown in the options. */
	public final String label;

	HandOrder(String label) {
		this.label = label;
	}

	/** Cards per player in the given hand (0-based). */
	public int handSize(int handIndex) {
		if (handIndex < 0 || handIndex >= HANDS) {
			throw new IllegalArgumentException("No hand " + handIndex);
		}
		return this == UP ? handIndex + 1 : HANDS - handIndex;
	}

	/** The other way round. */
	public HandOrder flipped() {
		return this == UP ? DOWN : UP;
	}
}
