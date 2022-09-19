package core.card;

import java.util.ArrayList;

import core.card.Card.Suit;

/**
 * Class for comparing cards and lists of cards.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class CardComparator {
	
	/**
	 * Determines if two specified cards are of the same suit.
	 * @param card1 a Card
	 * @param card2 the card to be compared to
	 * @return true if card1 is of the same suit as card2, else false.
	 */
	public static boolean isSameSuit(Card card1, Suit suit) {
		if (card1.getSuit().equals(suit)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Determines if a card has a greater face value than another
	 * @param card1 a Card
	 * @param card2 the Card to be compared to
	 * @return true if card1 is greater than card2, else false
	 */
	public static boolean isGreaterFaceValue(Card card1, Card card2) {
		if (card1.getFaceValue().getValue() > card2.getFaceValue().getValue()) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Determines the winning card out of all cards played, considering trump card
	 * @param cards ArrayList of cards played
	 * @param trumpCard
	 * @return the winning card
	 */
	public static PlayerCard determineWinner(ArrayList<PlayerCard> cards, Suit trumpSuit) {
		PlayerCard startingCard = cards.get(0);
		PlayerCard winner = startingCard;
		for (int i = 1; i < cards.size(); i++) {
			if (isGreaterCard(cards.get(i), winner, trumpSuit)) {
				winner = cards.get(i);
			}
		}
		return winner;
	}
	
	/**
	 * Gets the greater card
	 * @param card1 a Card
	 * @param card2 the card to be compared to
	 * @return true if card1 is more valuable than card2
	 */
	public static boolean isGreaterCard(Card card1, Card card2, Suit trumpSuit) {
		// if card1 is the same suit as card2 and if card1 has a greater face value
		if (isSameSuit(card1, card2.getSuit()) && isGreaterFaceValue(card1, card2)) {
			return true;
		} 
		// else if card1 is not the same as card2, and is a trump card
		else if (!isSameSuit(card1, card2.getSuit()) && isSameSuit(card1, trumpSuit)){
			return true;
		} 
		// else card1 is not greater
		else {
			return false;
		}
	}
}
