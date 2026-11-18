package rules;

/**
 * An immutable playing card. There is exactly one instance per suit and rank,
 * so cards can be compared with {@code ==} as well as {@code equals}.
 */
public final class Card {

	private static final Card[][] ALL = new Card[Suit.values().length][Rank.values().length];

	static {
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				ALL[s.ordinal()][r.ordinal()] = new Card(s, r);
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
