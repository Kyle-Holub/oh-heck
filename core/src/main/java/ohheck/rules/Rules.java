package ohheck.rules;

/* The standard rules' numbers, for code without a game in hand. */
public final class Rules {

	private Rules() {
	}

	private static final RuleSet STANDARD = RuleSet.STANDARD;

	public static final int MADE = STANDARD.made(), PER_TRICK_BID = STANDARD.perTrickBid();
	public static final int PER_TRICK_OFF = STANDARD.perTrickOff();

	public static int numberOfHands() {
		return STANDARD.numberOfHands();
	}

	public static int score(int bid, int tricksTaken) {
		return STANDARD.score(bid, tricksTaken);
	}
}
