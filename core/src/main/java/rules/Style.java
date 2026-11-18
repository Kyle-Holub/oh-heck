package rules;

/**
 * A computer player's habit: a lean on top of how well they play. Most are about bidding
 * (and since every player plays to make whatever they bid, the habit carries through the
 * hand); the trump and ace habits are about which card to play.
 */
public enum Style {
	STEADY("By the book"),
	BOLD("Overbids"),
	TIMID("Underbids"),
	FLUFFER("Fluffs"),
	GAMBLER("Unpredictable"),
	SPOILER("Spoiler"),
	/** Bolder after making a bid, warier after missing one. */
	STREAKY("Streaky"),
	/** Plays safe when ahead in the game, takes risks when behind. */
	FRONT_RUNNER("Front-runner"),
	/** Leads trumps early, to draw everyone else's. */
	TRUMP_HAPPY("Trump-happy"),
	/** Keeps its trumps for the last tricks. */
	TRUMP_HOARDER("Trump hoarder"),
	/** Leads its aces and kings straight away. */
	ACE_LEADER("Ace leader");

	/** As shown on the Players screen. */
	public final String label;

	Style(String label) {
		this.label = label;
	}
}
