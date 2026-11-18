package rules;

import java.util.Random;

/**
 * A computer player with a habit: it asks its level's bot for a bid, then leans it the way
 * its {@link Style} goes, some of the time (experts lean less often). Play is the bot's own,
 * aimed at whatever was bid.
 */
final class StyledBot implements Bot {

	/** How often the habit wins out over the cards, by level (easy, normal, hard). */
	static final double[] LEAN = { 0.6, 0.2, 0.12 }; // (measured in the arena's "players": levels still rank)

	private final Bot bot;
	private final Style style;
	private final double lean;
	private final Random rng;

	StyledBot(Bot bot, Style style, Difficulty level, Random rng) {
		this.bot = bot;
		this.style = style;
		this.lean = LEAN[level.ordinal()];
		this.rng = rng;
	}

	@Override
	public int chooseBid(TableView v) {
		int bid = bot.chooseBid(v);
		int leaned = lean(bid, v.handSize);
		return v.isLegalBid(leaned) ? leaned : bid;
	}

	private int lean(int bid, int handSize) {
		switch (style) {
		case BOLD:
			return rng.nextDouble() < lean ? Math.min(handSize, bid + 1) : bid;
		case TIMID:
			return rng.nextDouble() < lean ? Math.max(0, bid - 1) : bid;
		case FLUFFER: // a hand worth one trick (or, now and then, two) gets bid at zero
			double r = rng.nextDouble();
			return (bid == 1 && r < 2 * lean) || (bid == 2 && r < lean / 2) ? 0 : bid;
		case GAMBLER:
			if (rng.nextDouble() < lean) {
				return Math.max(0, Math.min(handSize, bid + (rng.nextBoolean() ? 1 : -1)));
			}
			return bid;
		default: // STEADY, and SPOILER (whose habit is in how it plays)
			return bid;
		}
	}

	@Override
	public Card choosePlay(TableView v) {
		return bot.choosePlay(v);
	}
}
