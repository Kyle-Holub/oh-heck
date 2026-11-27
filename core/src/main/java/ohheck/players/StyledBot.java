package ohheck.players;

import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.TableView;

/* Applies a Style to a level's bot. */
final class StyledBot implements Bot {

	private final Bot bot;
	private final Style style;
	private final double bidLean, playLean;
	private final Random rng;
	private int scoreAtLastBid, lastBidHand = -1;

	StyledBot(Bot bot, Style style, Difficulty level, Random rng) {
		this.bot = bot;
		this.style = style;
		this.bidLean = level.bidLean;
		this.playLean = level.playLean;
		this.rng = rng;
	}

	boolean leans() {
		return leans(1);
	}

	boolean leans(double strength) {
		return rng.nextDouble() < bidLean * strength;
	}

	boolean hasBidBeforeThisHand(TableView v) {
		return lastBidHand >= 0 && lastBidHand < v.handIndex();
	}

	boolean scoreRoseSinceLastBid(TableView v) {
		return v.score(v.seat()) > scoreAtLastBid;
	}

	@Override
	public int chooseBid(TableView v) {
		int bid = bot.chooseBid(v);
		int leaned = style.bid(bid, v, this);
		scoreAtLastBid = v.score(v.seat());
		lastBidHand = v.handIndex();
		return Bot.neverZeroWithTheTrumpAce(v, v.isLegalBid(leaned) && Bot.believable(v, leaned) ? leaned : bid);
	}

	@Override
	public Card choosePlay(TableView v) {
		Card card = bot.choosePlay(v);
		if (rng.nextDouble() >= playLean) {
			return card;
		}
		return style.play(card, v, v.legalPlays());
	}
}
