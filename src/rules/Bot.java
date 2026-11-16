package rules;

import java.util.ArrayList;
import java.util.List;

/**
 * The computer players' decisions.
 * <p>
 * This is a straight port of the original ComputerPlayer logic and reproduces
 * its behaviour exactly, quirks included (see the notes on
 * {@link #choosePlay}), so the desktop game plays the same as before.
 * Ties between equal ranks are broken the same way too: the highest pick is
 * the first such card in hand order, the lowest pick is the last.
 */
public final class Bot {

	/**
	 * Bid one trick per trump plus one per non-trump ace. If that bid would make
	 * the total equal the hand size, bid one fewer (or one more if it is zero).
	 */
	public int chooseBid(List<Card> hand, Suit trump, int bidsSoFar) {
		int bid = 0;
		for (Card c : hand) {
			if (c.suit() == trump || c.rank() == Rank.ACE) {
				bid++;
			}
		}
		if (bid == hand.size() - bidsSoFar) {
			bid = bid > 0 ? bid - 1 : bid + 1;
		}
		return bid;
	}

	/**
	 * Choose a card to play.
	 * <p>
	 * Quirk kept from the original: when the bot cannot follow suit it never
	 * deliberately trumps. It plays its lowest card if it still wants tricks and
	 * its highest card if it doesn't, ignoring suit (so that card may happen to
	 * be a trump).
	 *
	 * @param trickSoFar cards already played this trick, lead first; empty when leading
	 * @param bid        this player's bid for the hand
	 * @param tricksWon  tricks this player has taken so far this hand
	 */
	public Card choosePlay(List<Card> hand, List<Card> trickSoFar, Suit trump, int bid, int tricksWon) {
		if (hand.isEmpty()) {
			throw new IllegalArgumentException("Empty hand");
		}
		if (hand.size() == 1) {
			return hand.get(0);
		}
		boolean wantTrick = bid > tricksWon;

		if (trickSoFar.isEmpty()) {
			// leading
			List<Card> trumps = new ArrayList<Card>();
			List<Card> others = new ArrayList<Card>();
			for (Card c : hand) {
				(c.suit() == trump ? trumps : others).add(c);
			}
			if (wantTrick) {
				return others.isEmpty() ? highest(trumps) : highest(others);
			}
			return trumps.isEmpty() ? lowest(others) : lowest(trumps);
		}

		Suit lead = trickSoFar.get(0).suit();
		List<Card> followers = new ArrayList<Card>();
		for (Card c : hand) {
			if (c.suit() == lead) {
				followers.add(c);
			}
		}

		if (followers.isEmpty()) {
			// can't follow suit (original quirk: no deliberate trumping)
			return wantTrick ? lowest(hand) : highest(hand);
		}

		if (!wantTrick) {
			return lowest(followers);
		}
		Card best = trickSoFar.get(Rules.trickWinner(trickSoFar, trump));
		Card high = highest(followers);
		if (Rules.beats(best, high, trump) && followers.size() > 1) {
			// can't win this one, so save the high card
			return lowest(followers);
		}
		return high;
	}

	/** First card of the highest rank, in list order. */
	static Card highest(List<Card> cards) {
		Card best = cards.get(0);
		for (Card c : cards) {
			if (c.rank().outranks(best.rank())) {
				best = c;
			}
		}
		return best;
	}

	/** Last card of the lowest rank, in list order. */
	static Card lowest(List<Card> cards) {
		Card low = cards.get(0);
		for (Card c : cards) {
			if (!c.rank().outranks(low.rank())) {
				low = c;
			}
		}
		return low;
	}
}
