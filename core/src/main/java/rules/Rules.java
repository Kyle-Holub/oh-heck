package rules;

import java.util.List;

/**
 * The standard rules ({@link RuleSet#STANDARD}), for code that isn't holding a game: the
 * tutorial, the statistics, the tests. Anything playing a {@link Match} asks the match's
 * own {@link Match#rules()} instead, so it follows a variant too.
 */
public final class Rules {

	private Rules() {
	}

	private static final RuleSet STANDARD = RuleSet.STANDARD;

	/** For making your bid exactly: this, plus PER_TRICK_BID for each trick bid. */
	public static final int MADE = STANDARD.made(), PER_TRICK_BID = STANDARD.perTrickBid();
	/** For missing it: this many points off for each trick over or under. */
	public static final int PER_TRICK_OFF = STANDARD.perTrickOff();

	public static int numberOfHands() {
		return STANDARD.numberOfHands();
	}

	public static boolean beats(Card challenger, Card currentlyWinning, Suit trump) {
		return STANDARD.beats(challenger, currentlyWinning, trump);
	}

	public static int trickWinner(List<Card> plays, Suit trump) {
		return STANDARD.trickWinner(plays, trump);
	}

	public static boolean isLegalPlay(List<Card> hand, Card card, Suit leadSuit) {
		return STANDARD.isLegalPlay(hand, card, leadSuit);
	}

	public static List<Card> legalPlays(List<Card> hand, Suit leadSuit) {
		return STANDARD.legalPlays(hand, leadSuit);
	}

	public static boolean hasSuit(List<Card> hand, Suit suit) {
		return RuleSet.hasSuit(hand, suit);
	}

	public static int forbiddenDealerBid(int handSize, int bidsSoFar) {
		return STANDARD.forbiddenDealerBid(handSize, bidsSoFar);
	}

	public static boolean isLegalBid(int bid, int handSize, int bidsSoFar, boolean isDealer) {
		return STANDARD.isLegalBid(bid, handSize, bidsSoFar, isDealer);
	}

	/** Points for one hand: 10 + 2 per trick for making the bid exactly, else -5 per trick off. */
	public static int score(int bid, int tricksTaken) {
		return STANDARD.score(bid, tricksTaken);
	}
}
