package rules;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of Oh Heck, with no state of their own.
 */
public final class Rules {

	private Rules() {
	}

	/** Hands in a game; how many cards each holds depends on the game's {@link HandOrder}. */
	public static int numberOfHands() {
		return HandOrder.HANDS;
	}

	// ---- Tricks ----

	/**
	 * Does {@code challenger} take the trick from the card currently winning it?
	 * A higher card of the same suit wins, and a trump beats any non-trump.
	 * An off-suit, non-trump card never wins.
	 */
	public static boolean beats(Card challenger, Card currentlyWinning, Suit trump) {
		if (challenger.suit() == currentlyWinning.suit()) {
			return challenger.rank().outranks(currentlyWinning.rank());
		}
		return challenger.suit() == trump;
	}

	/** Index into {@code plays} of the card that wins the trick. The first card is the lead. */
	public static int trickWinner(List<Card> plays, Suit trump) {
		if (plays.isEmpty()) {
			throw new IllegalArgumentException("No cards played");
		}
		int winner = 0;
		for (int i = 1; i < plays.size(); i++) {
			if (beats(plays.get(i), plays.get(winner), trump)) {
				winner = i;
			}
		}
		return winner;
	}

	// ---- Following suit ----

	/**
	 * Can {@code card} be played from {@code hand}? Players must follow the lead
	 * suit if they can; otherwise, or when leading, any card is allowed.
	 *
	 * @param leadSuit the suit led this trick, or null if this player is leading
	 */
	public static boolean isLegalPlay(List<Card> hand, Card card, Suit leadSuit) {
		if (!hand.contains(card)) {
			return false;
		}
		if (leadSuit == null || card.suit() == leadSuit) {
			return true;
		}
		return !hasSuit(hand, leadSuit);
	}

	/** The cards in {@code hand} that may be played, in hand order. */
	public static List<Card> legalPlays(List<Card> hand, Suit leadSuit) {
		List<Card> legal = new ArrayList<Card>();
		for (Card c : hand) {
			if (isLegalPlay(hand, c, leadSuit)) {
				legal.add(c);
			}
		}
		return legal;
	}

	public static boolean hasSuit(List<Card> hand, Suit suit) {
		for (Card c : hand) {
			if (c.suit() == suit) {
				return true;
			}
		}
		return false;
	}

	// ---- Bidding ----

	/**
	 * The one bid the dealer may not make: the bid that would make the total
	 * equal the number of tricks, so that someone must miss. Negative when the
	 * others have already bid more than that, meaning nothing is forbidden.
	 */
	public static int forbiddenDealerBid(int handSize, int bidsSoFar) {
		return handSize - bidsSoFar;
	}

	public static boolean isLegalBid(int bid, int handSize, int bidsSoFar, boolean isDealer) {
		if (bid < 0 || bid > handSize) {
			return false;
		}
		return !(isDealer && bid == forbiddenDealerBid(handSize, bidsSoFar));
	}

	// ---- Scoring ----

	/** Points for one hand: 10 + 2 per trick for making the bid exactly, else -5 per trick off. */
	public static int score(int bid, int tricksTaken) {
		if (bid == tricksTaken) {
			return 10 + 2 * bid;
		}
		return -5 * Math.abs(bid - tricksTaken);
	}
}
