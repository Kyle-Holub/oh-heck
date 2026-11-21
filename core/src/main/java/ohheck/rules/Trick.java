package ohheck.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* The trick in progress, shared by Match and the Hard bot's simulations. */
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

	public List<Card> cards() {
		return cardsView;
	}

	public List<Integer> seats() {
		return seatsView;
	}

	public boolean isEmpty() {
		return cards.isEmpty();
	}

	public int size() {
		return cards.size();
	}

	public Suit leadSuit() {
		return cards.isEmpty() ? null : cards.get(0).suit();
	}

	public boolean revealsVoid(Card c) {
		return !cards.isEmpty() && c.suit() != leadSuit();
	}

	public int add(int seat, Card c, Suit trump) {
		cards.add(c);
		seats.add(seat);
		return cards.size() < players ? -1 : seats.get(rules.trickWinner(cards, trump));
	}

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
