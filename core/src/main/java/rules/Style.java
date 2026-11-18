package rules;

/**
 * A computer player's habit: a lean on top of how well they play. Most are about bidding;
 * since every player then plays to make whatever they bid, the habit carries through the hand.
 */
public enum Style {
	STEADY("By the book"),
	BOLD("Overbids"),
	TIMID("Underbids"),
	FLUFFER("Fluffs"),
	GAMBLER("Unpredictable"),
	SPOILER("Spoiler");

	/** As shown on the Players screen. */
	public final String label;

	Style(String label) {
		this.label = label;
	}
}
