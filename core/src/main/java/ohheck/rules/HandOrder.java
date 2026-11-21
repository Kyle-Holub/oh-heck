package ohheck.rules;

/* Which way the hand sizes run: up from one card, down to it, or up to the biggest hand and back down. */
public enum HandOrder {
	UP, DOWN, UP_AND_DOWN;

	public String label(int most) {
		switch (this) {
		case UP:
			return "1 to " + most;
		case DOWN:
			return most + " to 1";
		default:
			return "1 to " + most + " to 1";
		}
	}

	public int hands(int most) {
		return this == UP_AND_DOWN ? 2 * most - 1 : most;
	}

	public int handSize(int handIndex, int most) {
		if (handIndex < 0 || handIndex >= hands(most)) {
			throw new IllegalArgumentException("No hand " + handIndex);
		}
		switch (this) {
		case UP:
			return handIndex + 1;
		case DOWN:
			return most - handIndex;
		default:
			return handIndex < most ? handIndex + 1 : 2 * most - 1 - handIndex;
		}
	}

	public HandOrder next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
