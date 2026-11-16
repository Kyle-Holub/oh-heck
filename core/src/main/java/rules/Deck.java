package rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A standard 52-card deck, dealt from the top.
 */
public final class Deck {

	private final List<Card> cards = new ArrayList<Card>(52);

	/** A deck in suit-then-rank order, unshuffled. */
	public Deck() {
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				cards.add(Card.of(r, s));
			}
		}
	}

	/** A shuffled deck. Pass a seeded Random for repeatable deals. */
	public static Deck shuffled(Random rng) {
		Deck d = new Deck();
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
