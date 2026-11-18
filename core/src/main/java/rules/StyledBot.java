package rules;

import java.util.List;
import java.util.Random;

/**
 * A computer player with a habit: it asks its level's bot for a bid or a card, then leans
 * it the way its {@link Style} goes, some of the time (experts less often). A bid habit
 * carries through the hand, since the bot then plays to make whatever was bid.
 */
final class StyledBot implements Bot {

	/** How often the habit wins out over the cards, by level (easy, normal, hard). */
	static final double[] LEAN = { 0.6, 0.2, 0.12 }; // (measured in the arena's "players": levels still rank)
	/** How often a playing habit wins out, by level: a habit you can see, more so in weaker players. */
	static final double[] PLAY_HABIT = { 0.8, 0.6, 0.4 };

	private final Bot bot;
	private final Style style;
	private final double lean, habit;
	private final Random rng;
	/** Streaky: our score when we last bid, and in which hand (-1 before our first bid). */
	private int scoreAtLastBid, lastBidHand = -1;

	StyledBot(Bot bot, Style style, Difficulty level, Random rng) {
		this.bot = bot;
		this.style = style;
		this.lean = LEAN[level.ordinal()];
		this.habit = PLAY_HABIT[level.ordinal()];
		this.rng = rng;
	}

	@Override
	public int chooseBid(TableView v) {
		int bid = bot.chooseBid(v);
		int leaned = lean(bid, v);
		scoreAtLastBid = v.scores[v.seat];
		lastBidHand = v.handIndex;
		return v.isLegalBid(leaned) ? leaned : bid;
	}

	private int lean(int bid, TableView v) {
		int handSize = v.handSize;
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
		case STREAKY: // bolder after a made bid, warier after a miss (made bids score, misses cost)
			if (lastBidHand >= 0 && lastBidHand < v.handIndex && rng.nextDouble() < lean) {
				return v.scores[v.seat] > scoreAtLastBid ? Math.min(handSize, bid + 1) : Math.max(0, bid - 1);
			}
			return bid;
		case FRONT_RUNNER: // ahead: shade the bid down; behind: reach for more (most of all near the end)
			int best = Integer.MIN_VALUE;
			for (int s = 0; s < v.players; s++) {
				if (s != v.seat) {
					best = Math.max(best, v.scores[s]);
				}
			}
			boolean late = v.handIndex >= v.numberOfHands - 3;
			if (v.handIndex > 0 && rng.nextDouble() < lean * (late ? 3 : 1)) {
				return v.scores[v.seat] > best ? Math.max(0, bid - 1) : Math.min(handSize, bid + 1);
			}
			return bid;
		default: // STEADY, SPOILER (in how it plays) and the playing habits
			return bid;
		}
	}

	@Override
	public Card choosePlay(TableView v) {
		Card card = bot.choosePlay(v);
		if (rng.nextDouble() >= habit) {
			return card;
		}
		List<Card> legal = v.legalPlays();
		Suit trump = v.trump();
		boolean leading = v.trick.isEmpty();
		int tricksLeft = v.hand.size();
		switch (style) {
		case TRUMP_HAPPY: // lead the highest trump while the hand is young
			if (leading && tricksLeft * 2 > v.handSize) {
				Card t = highest(legal, trump, true);
				return t != null ? t : card;
			}
			return card;
		case TRUMP_HOARDER: // play anything but a trump until the last two tricks
			if (card.suit() == trump && tricksLeft > 2) {
				Card other = lowest(legal, trump);
				return other != null ? other : card;
			}
			return card;
		case ACE_LEADER: // lead an ace or king of a plain suit straight away
			if (leading) {
				Card high = highest(legal, trump, false);
				return high != null && high.rank().strength() >= Rank.KING.strength() ? high : card;
			}
			return card;
		default:
			return card;
		}
	}

	/** The highest legal card of the trump suit (trumps true) or of the other suits, or null. */
	private static Card highest(List<Card> legal, Suit trump, boolean trumps) {
		Card best = null;
		for (Card c : legal) {
			if ((c.suit() == trump) == trumps && (best == null || c.rank().strength() > best.rank().strength())) {
				best = c;
			}
		}
		return best;
	}

	/** The lowest legal card that isn't a trump, or null. */
	private static Card lowest(List<Card> legal, Suit trump) {
		Card best = null;
		for (Card c : legal) {
			if (c.suit() != trump && (best == null || c.rank().strength() < best.rank().strength())) {
				best = c;
			}
		}
		return best;
	}
}
