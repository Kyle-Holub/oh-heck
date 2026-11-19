package ohheck.rules;

/**
 * An immutable playing card. There is exactly one instance per suit and rank,
 * so cards can be compared with {@code ==} as well as {@code equals}.
 */
public final class Card {

	/** Cards in a deck. */
	public static final int COUNT = 52;
	private static final int PER_SUIT = Rank.values().length;

	private static final Card[][] ALL = new Card[Suit.values().length][PER_SUIT];
	/** Each card by its {@link #index()}. */
	private static final Card[] BY_INDEX = new Card[COUNT];

	static {
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				Card c = new Card(s, r);
				ALL[s.ordinal()][r.ordinal()] = c;
				BY_INDEX[c.index()] = c;
			}
		}
	}

	private final Suit suit;
	private final Rank rank;

	private Card(Suit suit, Rank rank) {
		this.suit = suit;
		this.rank = rank;
	}

	public static Card of(Rank rank, Suit suit) {
		return ALL[suit.ordinal()][rank.ordinal()];
	}

	/** The card with this {@link #index()}. */
	public static Card ofIndex(int index) {
		return BY_INDEX[index];
	}

	/** A number from 0 to 51 for each card: by suit, then by strength. */
	public int index() {
		return suit.ordinal() * PER_SUIT + rank.strength();
	}

	public Suit suit() {
		return suit;
	}

	public Rank rank() {
		return rank;
	}

	public boolean is(Suit s) {
		return suit == s;
	}

	/** For example "ACE of SPADES". */
	@Override
	public String toString() {
		return rank + " of " + suit;
	}
}
