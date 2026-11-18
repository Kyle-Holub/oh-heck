package rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The rules a game is played by: how many hands and how big, which card wins a trick, what
 * may be bid and played, and how a hand scores. {@link #STANDARD} is Oh Heck as it's always
 * been played here. A variant extends this, overrides what it changes, and is listed in
 * {@link #ALL} so saved games can find it again by {@link #name}.
 * <p>
 * A {@link Match} holds its rules, and the bots read them from their {@link TableView}.
 * The bots' learned bid odds ({@link BidTable}) are for the standard rules.
 */
public class RuleSet {

	public static final RuleSet STANDARD = new RuleSet("standard");

	/** Every rule set, for reading saved games back. Add a variant here. */
	public static final List<RuleSet> ALL = Collections.unmodifiableList(new ArrayList<RuleSet>(Arrays.asList(STANDARD)));

	/** Written into saved games: keep it once a variant ships. */
	public final String name;

	protected RuleSet(String name) {
		this.name = name;
	}

	/** The rule set saved under this name; throws IllegalArgumentException for one this build doesn't know. */
	public static RuleSet named(String name) {
		for (RuleSet r : ALL) {
			if (r.name.equals(name)) {
				return r;
			}
		}
		throw new IllegalArgumentException("Unknown rules: " + name);
	}

	// ---- The hands ----

	/** Hands in a game, and the most cards in any of them. */
	public int numberOfHands() {
		return 8;
	}

	/** Cards per player in the given hand (0-based), when the hands run the given way. */
	public int handSize(HandOrder order, int handIndex) {
		return order.handSize(handIndex, numberOfHands());
	}

	// ---- Tricks ----

	/**
	 * Does {@code challenger} take the trick from the card currently winning it?
	 * A higher card of the same suit wins, and a trump beats any non-trump.
	 * An off-suit, non-trump card never wins.
	 */
	public boolean beats(Card challenger, Card currentlyWinning, Suit trump) {
		if (challenger.suit() == currentlyWinning.suit()) {
			return challenger.rank().outranks(currentlyWinning.rank());
		}
		return challenger.suit() == trump;
	}

	/** Index into {@code plays} of the card that wins the trick. The first card is the lead. */
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

	// ---- Following suit ----

	/**
	 * Can {@code card} be played from {@code hand}? Players must follow the lead
	 * suit if they can; otherwise, or when leading, any card is allowed.
	 *
	 * @param leadSuit the suit led this trick, or null if this player is leading
	 */
	public boolean isLegalPlay(List<Card> hand, Card card, Suit leadSuit) {
		if (!hand.contains(card)) {
			return false;
		}
		if (leadSuit == null || card.suit() == leadSuit) {
			return true;
		}
		return !hasSuit(hand, leadSuit);
	}

	/** The cards in {@code hand} that may be played, in hand order. */
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

	// ---- Bidding ----

	/**
	 * The one bid the dealer may not make: the bid that would make the total
	 * equal the number of tricks, so that someone must miss. Negative when the
	 * others have already bid more than that, meaning nothing is forbidden.
	 */
	public int forbiddenDealerBid(int handSize, int bidsSoFar) {
		return handSize - bidsSoFar;
	}

	public boolean isLegalBid(int bid, int handSize, int bidsSoFar, boolean isDealer) {
		if (bid < 0 || bid > handSize) {
			return false;
		}
		return !(isDealer && bid == forbiddenDealerBid(handSize, bidsSoFar));
	}

	// ---- Scoring ----

	/** For making your bid exactly: this, plus {@link #perTrickBid} for each trick bid. */
	public int made() {
		return 10;
	}

	public int perTrickBid() {
		return 2;
	}

	/** For missing it: this many points off for each trick over or under. */
	public int perTrickOff() {
		return 5;
	}

	/** Points for one hand: made() + perTrickBid() per trick for making the bid exactly, else -perTrickOff() per trick off. */
	public int score(int bid, int tricksTaken) {
		if (bid == tricksTaken) {
			return made() + perTrickBid() * bid;
		}
		return -perTrickOff() * Math.abs(bid - tricksTaken);
	}
}
