package rules;

import java.util.Random;

/**
 * A computer player with a habit: it asks its level's bot for a bid or a card, then leans
 * it the way its {@link Style} goes, some of the time (experts less often, see
 * {@link Difficulty}). A bid habit carries through the hand, since the bot then plays to
 * make whatever was bid. The habits themselves are written in {@link Style}.
 */
final class StyledBot implements Bot {

	private final Bot bot;
	private final Style style;
	/** How often a bidding habit wins out over the cards, and a playing one. */
	final double lean, habit;
	final Random rng;
	/** Our score when we last bid, and in which hand (-1 before our first bid): for habits with memory. */
	int scoreAtLastBid, lastBidHand = -1;

	StyledBot(Bot bot, Style style, Difficulty level, Random rng) {
		this.bot = bot;
		this.style = style;
		this.lean = level.styleLean;
		this.habit = level.playHabit;
		this.rng = rng;
	}

	/** Whether the bidding habit wins out this time. */
	boolean leans() {
		return rng.nextDouble() < lean;
	}

	@Override
	public int chooseBid(TableView v) {
		int bid = bot.chooseBid(v);
		int leaned = style.bid(bid, v, this);
		scoreAtLastBid = v.scores[v.seat];
		lastBidHand = v.handIndex;
		return v.isLegalBid(leaned) ? leaned : bid;
	}

	@Override
	public Card choosePlay(TableView v) {
		Card card = bot.choosePlay(v);
		if (rng.nextDouble() >= habit) {
			return card;
		}
		return style.play(card, v, v.legalPlays());
	}
}
