package ohheck.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Deck {

	private final List<Card> cards = new ArrayList<Card>(Card.COUNT);

	public Deck() {
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				cards.add(Card.of(r, s));
			}
		}
	}

	public static Deck shuffled(Random rng) {
		return shuffled(rng, RuleSet.STANDARD);
	}

	public static Deck shuffled(Random rng, RuleSet rules) {
		Deck d = new Deck();
		d.cards.removeIf(c -> !rules.inDeck(c));
		Collections.shuffle(d.cards, rng);
		return d;
	}

	public Card deal() {
		if (cards.isEmpty()) {
			throw new IllegalStateException("The deck is empty");
		}
		return cards.remove(0);
	}

	public List<Card> deal(int count) {
		List<Card> dealt = new ArrayList<Card>(count);
		for (int i = 0; i < count; i++) {
			dealt.add(deal());
		}
		return dealt;
	}

	public int size() {
		return cards.size();
	}
}
