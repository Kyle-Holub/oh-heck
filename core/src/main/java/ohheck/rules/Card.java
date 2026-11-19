package ohheck.rules;

/** A playing card; one instance per suit and rank, so == compares. */
public final class Card {

	public static final int COUNT = 52;
	private static final int PER_SUIT = Rank.values().length;

	private static final Card[][] ALL = new Card[Suit.values().length][PER_SUIT];
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

	public static Card ofIndex(int index) {
		return BY_INDEX[index];
	}

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

	@Override
	public String toString() {
		return rank + " of " + suit;
	}
}
