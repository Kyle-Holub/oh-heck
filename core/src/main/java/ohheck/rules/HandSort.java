package ohheck.rules;

import java.util.Comparator;

/*
 * How your hand is laid out, left to right: trumps first, then the other suits in turn (their
 * colours alternating), or every card by rank. Low to high either way.
 */
public enum HandSort {
	SUIT("Suit"), RANK("Rank");

	private static final Suit[] COLOURS_ALTERNATING = { Suit.CLUBS, Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS };
	private static final int NO_TRUMP = Suit.values().length;
	private static final Comparator<Card>[][] ORDERS = orders();

	public final String label;

	HandSort(String label) {
		this.label = label;
	}

	public HandSort next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public Comparator<Card> order(Suit trump) {
		return ORDERS[ordinal()][trump == null ? NO_TRUMP : trump.ordinal()];
	}

	@SuppressWarnings("unchecked")
	private static Comparator<Card>[][] orders() {
		Comparator<Card>[][] orders = new Comparator[2][NO_TRUMP + 1];
		for (int t = 0; t <= NO_TRUMP; t++) {
			Suit trump = t == NO_TRUMP ? null : Suit.values()[t];
			orders[SUIT.ordinal()][t] = (a, b) -> {
				int bySuit = place(a.suit(), trump) - place(b.suit(), trump);
				return bySuit != 0 ? bySuit : a.rank().strength() - b.rank().strength();
			};
			orders[RANK.ordinal()][t] = (a, b) -> {
				boolean at = a.suit() == trump, bt = b.suit() == trump;
				if (at != bt) {
					return at ? -1 : 1;
				}
				int byRank = a.rank().strength() - b.rank().strength();
				return byRank != 0 ? byRank : place(a.suit(), trump) - place(b.suit(), trump);
			};
		}
		return orders;
	}

	private static int place(Suit suit, Suit trump) {
		int start = trump == null ? 0 : indexOf(trump), i = indexOf(suit);
		return (i - start + COLOURS_ALTERNATING.length) % COLOURS_ALTERNATING.length;
	}

	private static int indexOf(Suit suit) {
		for (int i = 0; i < COLOURS_ALTERNATING.length; i++) {
			if (COLOURS_ALTERNATING[i] == suit) {
				return i;
			}
		}
		throw new IllegalArgumentException(String.valueOf(suit));
	}
}
