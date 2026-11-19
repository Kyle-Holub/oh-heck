package ohheck.rules;

/*
 * Card ranks, aces high. Declaration order follows the card sheet's columns; compare by
 * strength().
 */
public enum Rank {
	ACE(12), TWO(0), THREE(1), FOUR(2), FIVE(3), SIX(4), SEVEN(5), EIGHT(6), NINE(7), TEN(8), JACK(9), QUEEN(10), KING(11);

	private final int strength;

	Rank(int strength) {
		this.strength = strength;
	}

	public int strength() {
		return strength;
	}

	public boolean outranks(Rank other) {
		return strength > other.strength;
	}
}
