package ohheck.campaign;

/* A feat worth a badge on Records. Beating a chapter's champion earns that champion's badge too, read from the chapter. */
public enum Badge {
	FIRST_DUEL("First Duel", "Win a duel"),
	CLEAN_SWEEP("Clean Sweep", "Make every bid in a game"),
	BIG_SPENDER("Big Spender", "Make a bid of 5 or more with big bids"),
	POKER_FACE("Poker Face", "Win a blind-bid game without a miss"),
	MARATHONER("Marathoner", "Finish a Marathon");

	public final String title, how;

	Badge(String title, String how) {
		this.title = title;
		this.how = how;
	}
}
