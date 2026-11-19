package ohheck.players;

import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.TableView;

/** Applies a Style to a level's bot. */
final class StyledBot implements Bot {

	private final Bot bot;
	private final Style style;
	final double lean, habit;
	final Random rng;
	int scoreAtLastBid, lastBidHand = -1;

	StyledBot(Bot bot, Style style, Difficulty level, Random rng) {
		this.bot = bot;
		this.style = style;
		this.lean = level.styleLean;
		this.habit = level.playHabit;
		this.rng = rng;
	}

	boolean leans() {
		return rng.nextDouble() < lean;
	}

	@Override
	public int chooseBid(TableView v) {
		int bid = bot.chooseBid(v);
		int leaned = style.bid(bid, v, this);
		scoreAtLastBid = v.score(v.seat());
		lastBidHand = v.handIndex();
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
