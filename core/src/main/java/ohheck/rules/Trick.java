package ohheck.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The trick in progress: the cards played to it, lead first, and who played each. One
 * place decides when a trick is complete and who takes it, for the real game
 * ({@link Match}) and for the games a bot imagines (the Hard bot's simulations), so both
 * follow the same {@link RuleSet}.
 */
public final class Trick {

	private final int players;
	private final RuleSet rules;
	final List<Card> cards = new ArrayList<Card>();
	final List<Integer> seats = new ArrayList<Integer>();
	private final List<Card> cardsView = Collections.unmodifiableList(cards);
	private final List<Integer> seatsView = Collections.unmodifiableList(seats);

	public Trick(int players, RuleSet rules) {
		this.players = players;
		this.rules = rules;
	}

	/** The cards played so far, lead first (read-only, and it follows the trick). */
	public List<Card> cards() {
		return cardsView;
	}

	/** Who played each of {@link #cards()}, in the same order. */
	public List<Integer> seats() {
		return seatsView;
	}

	public boolean isEmpty() {
		return cards.isEmpty();
	}

	public int size() {
		return cards.size();
	}

	/** The suit led, or null before the lead. */
	public Suit leadSuit() {
		return cards.isEmpty() ? null : cards.get(0).suit();
	}

	/** Whether this card, played now, shows its player holds none of the suit led. */
	public boolean revealsVoid(Card c) {
		return !cards.isEmpty() && c.suit() != leadSuit();
	}

	/**
	 * Adds the seat's card. Returns the seat that takes the trick if this card completed it,
	 * else -1. A completed trick keeps its cards until {@link #clear()}, so the caller can
	 * look at it first.
	 */
	public int add(int seat, Card c, Suit trump) {
		cards.add(c);
		seats.add(seat);
		return cards.size() < players ? -1 : seats.get(rules.trickWinner(cards, trump));
	}

	/** Replaces the trick with these cards and seats. */
	public void set(List<Card> cards, List<Integer> seats) {
		clear();
		this.cards.addAll(cards);
		this.seats.addAll(seats);
	}

	public void clear() {
		cards.clear();
		seats.clear();
	}
}
