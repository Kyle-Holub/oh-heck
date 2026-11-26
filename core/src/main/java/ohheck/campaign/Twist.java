package ohheck.campaign;

import ohheck.rules.RuleSet;

/* The rule a chapter's champion brings to the final table and the duel. */
public enum Twist {
	MOMENTUM("Momentum", "x2", "make your bid twice running and your next made bid scores double."),
	PICK_TRUMP("Winner picks trump", "pick", "the highest bid made last hand names the next trump suit."),
	HANDS_WON("Hands won", "1-0", "each made bid scores one, and a tie for first plays on, one card a hand."),
	WIDOW("The widow", "+1", "the highest bid made last hand may swap a card for one of two set aside.");

	public final String label, figure, line;

	Twist(String label, String figure, String line) {
		this.label = label;
		this.figure = figure;
		this.line = line;
	}

	public RuleSet apply(RuleSet r) {
		return switch (this) {
		case MOMENTUM -> r.withMomentum(true);
		case PICK_TRUMP -> r.withPickTrump(true);
		case HANDS_WON -> r.withHandsWon(true);
		case WIDOW -> r.withWidow(true);
		};
	}
}
