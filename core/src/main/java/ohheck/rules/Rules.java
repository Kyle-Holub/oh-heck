package ohheck.rules;

/**
 * The standard rules' numbers ({@link RuleSet#STANDARD}), for code that isn't holding a game:
 * the tutorial's captions and its little table. Anything playing a
 * {@link Match} asks the match's own {@link Match#rules()} instead, so it follows a variant
 * too; anything else asks {@link RuleSet#STANDARD} directly.
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

	/** Points for one hand: 10 + 2 per trick for making the bid exactly, else -5 per trick off. */
	public static int score(int bid, int tricksTaken) {
		return STANDARD.score(bid, tricksTaken);
	}
}
