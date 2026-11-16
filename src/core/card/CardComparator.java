package core.card;

import java.util.ArrayList;
import java.util.List;

import rules.Rules;
import rules.Suit;

/**
 * Trick comparisons for on-screen cards. The rules themselves live in
 * {@link rules.Rules}.
 *
 * @author Kyle Holub
 * @version 1.1
 * @since 1.0
 */
public class CardComparator {

	public static boolean isSameSuit(Card card, Suit suit) {
		return card.getSuit() == suit;
	}

	/**
	 * Determines the winning card out of all cards played, considering trump
	 * @param cards ArrayList of cards played, lead first
	 * @param trumpSuit the trump suit
	 * @return the winning card
	 */
	public static PlayerCard determineWinner(ArrayList<PlayerCard> cards, Suit trumpSuit) {
		return cards.get(Rules.trickWinner(toRules(cards), trumpSuit));
	}

	/**
	 * @return true if card1 would take the trick from card2
	 */
	public static boolean isGreaterCard(Card card1, Card card2, Suit trumpSuit) {
		return Rules.beats(card1.rulesCard(), card2.rulesCard(), trumpSuit);
	}

	/** The rules-package cards behind a list of on-screen cards, in the same order. */
	public static List<rules.Card> toRules(List<? extends Card> cards) {
		List<rules.Card> list = new ArrayList<rules.Card>(cards.size());
		for (Card c : cards) {
			list.add(c.rulesCard());
		}
		return list;
	}
}
