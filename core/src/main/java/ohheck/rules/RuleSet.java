package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The rules a game is played by. A variant overrides what it changes and is listed in ALL. Its
 * name is written into saves, so it never changes once shipped.
 */
public class RuleSet {

	public static final RuleSet STANDARD = new RuleSet("standard");

	public static final List<RuleSet> ALL = Collections.unmodifiableList(new ArrayList<RuleSet>(Arrays.asList(STANDARD)));

	public final String name;

	protected RuleSet(String name) {
		this.name = name;
	}

	public static RuleSet named(String name) {
		for (RuleSet r : ALL) {
			if (r.name.equals(name)) {
				return r;
			}
		}
		throw new IllegalArgumentException("Unknown rules: " + name);
	}

	public int numberOfHands() {
		return 8;
	}

	public int handSize(HandOrder order, int handIndex) {
		return order.handSize(handIndex, numberOfHands());
	}

	public int nextSeat(int seat, int players) {
		return (seat + 1) % players;
	}

	public boolean beats(Card challenger, Card currentlyWinning, Suit trump) {
		if (challenger.suit() == currentlyWinning.suit()) {
			return challenger.rank().outranks(currentlyWinning.rank());
		}
		return challenger.suit() == trump;
	}

	public int trickWinner(List<Card> plays, Suit trump) {
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

	public boolean isLegalPlay(List<Card> hand, Card card, Suit leadSuit) {
		if (!hand.contains(card)) {
			return false;
		}
		if (leadSuit == null || card.suit() == leadSuit) {
			return true;
		}
		return !hasSuit(hand, leadSuit);
	}

	public List<Card> legalPlays(List<Card> hand, Suit leadSuit) {
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

	public int forbiddenDealerBid(int handSize, int bidsSoFar) {
		return handSize - bidsSoFar;
	}

	public boolean isLegalBid(int bid, int handSize, int bidsSoFar, boolean isDealer) {
		if (bid < 0 || bid > handSize) {
			return false;
		}
		return !(isDealer && bid == forbiddenDealerBid(handSize, bidsSoFar));
	}

	public int made() {
		return 10;
	}

	public int perTrickBid() {
		return 2;
	}

	public int perTrickOff() {
		return 5;
	}

	public int score(int bid, int tricksTaken) {
		if (bid == tricksTaken) {
			return made() + perTrickBid() * bid;
		}
		return -perTrickOff() * Math.abs(bid - tricksTaken);
	}
}
