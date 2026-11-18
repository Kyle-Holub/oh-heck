package rules;

import java.util.List;
import java.util.Random;

/**
 * A computer player's habit: a lean on top of how well they play. Most are about bidding
 * (and since every player plays to make whatever they bid, the habit carries through the
 * hand); the trump and ace habits are about which card to play.
 * <p>
 * Each habit is written in its own constant: {@link #bid} for a bidding lean, {@link #play}
 * for a playing one. A new habit is one more constant (and a player who has it).
 */
public enum Style {
	STEADY("By the book") {
		@Override
		boolean habitual() {
			return false;
		}
	},
	BOLD("Overbids") {
		@Override
		int bid(int bid, TableView v, StyledBot h) {
			return h.leans() ? Math.min(v.handSize, bid + 1) : bid;
		}
	},
	TIMID("Underbids") {
		@Override
		int bid(int bid, TableView v, StyledBot h) {
			return h.leans() ? Math.max(0, bid - 1) : bid;
		}
	},
	/** A hand worth one trick (or, now and then, two) gets bid at zero. */
	FLUFFER("Fluffs") {
		@Override
		int bid(int bid, TableView v, StyledBot h) {
			double r = h.rng.nextDouble();
			return (bid == 1 && r < 2 * h.lean) || (bid == 2 && r < h.lean / 2) ? 0 : bid;
		}
	},
	GAMBLER("Unpredictable") {
		@Override
		int bid(int bid, TableView v, StyledBot h) {
			if (h.leans()) {
				return Math.max(0, Math.min(v.handSize, bid + (h.rng.nextBoolean() ? 1 : -1)));
			}
			return bid;
		}
	},
	/** An expert who weighs spoiling the leader's hand more than other experts do (in how it plays). */
	SPOILER("Spoiler") {
		/** How much more a spoiler weighs the leader's points than other experts do. */
		static final double SPOIL = 1.2;

		@Override
		boolean habitual() {
			return false;
		}

		@Override
		Bot base(Difficulty level, Random rng) {
			return level == Difficulty.HARD ? new HardBot(rng, SPOIL) : level.bot(rng);
		}
	},
	/** Bolder after making a bid, warier after missing one (made bids score, misses cost). */
	STREAKY("Streaky") {
		@Override
		int bid(int bid, TableView v, StyledBot h) {
			if (h.lastBidHand >= 0 && h.lastBidHand < v.handIndex && h.leans()) {
				return v.scores[v.seat] > h.scoreAtLastBid ? Math.min(v.handSize, bid + 1) : Math.max(0, bid - 1);
			}
			return bid;
		}
	},
	/** Plays safe when ahead in the game, takes risks when behind (most of all near the end). */
	FRONT_RUNNER("Front-runner") {
		@Override
		int bid(int bid, TableView v, StyledBot h) {
			int best = Integer.MIN_VALUE;
			for (int s = 0; s < v.players; s++) {
				if (s != v.seat) {
					best = Math.max(best, v.scores[s]);
				}
			}
			boolean late = v.handIndex >= v.numberOfHands - 3;
			if (v.handIndex > 0 && h.rng.nextDouble() < h.lean * (late ? 3 : 1)) {
				return v.scores[v.seat] > best ? Math.max(0, bid - 1) : Math.min(v.handSize, bid + 1);
			}
			return bid;
		}
	},
	/** Leads its highest trump while the hand is young, to draw everyone else's. */
	TRUMP_HAPPY("Trump-happy") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (v.trick.isEmpty() && v.hand.size() * 2 > v.handSize) {
				Card t = highest(legal, v.trump(), true);
				return t != null ? t : card;
			}
			return card;
		}
	},
	/** Plays anything but a trump until the last two tricks. */
	TRUMP_HOARDER("Trump hoarder") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (card.suit() == v.trump() && v.hand.size() > 2) {
				Card other = lowest(legal, v.trump());
				return other != null ? other : card;
			}
			return card;
		}
	},
	/** Leads an ace or king of a plain suit straight away. */
	ACE_LEADER("Ace leader") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (v.trick.isEmpty()) {
				Card high = highest(legal, v.trump(), false);
				return high != null && high.rank().strength() >= Rank.KING.strength() ? high : card;
			}
			return card;
		}
	};

	/** As shown on the Players screen. */
	public final String label;

	Style(String label) {
		this.label = label;
	}

	/** A computer player at this level with this habit; rng drives its randomness. */
	Bot bot(Difficulty level, Random rng) {
		Bot base = base(level, rng);
		return habitual() ? new StyledBot(base, this, level, rng) : base;
	}

	/** The player under the habit: its level's bot, unless the habit changes how it thinks. */
	Bot base(Difficulty level, Random rng) {
		return level.bot(rng);
	}

	/** Whether the habit leans on the bot's choices (false: it's in how the bot itself thinks, or there's none). */
	boolean habitual() {
		return true;
	}

	/** The bid, leaned this habit's way (h: the player's chances and memory). Unchanged unless overridden. */
	int bid(int bid, TableView v, StyledBot h) {
		return bid;
	}

	/** The card, swapped for this habit's choice (when the habit wins out). Unchanged unless overridden. */
	Card play(Card card, TableView v, List<Card> legal) {
		return card;
	}

	/** The highest legal card of the trump suit (trumps true) or of the other suits, or null. */
	static Card highest(List<Card> legal, Suit trump, boolean trumps) {
		Card best = null;
		for (Card c : legal) {
			if ((c.suit() == trump) == trumps && (best == null || c.rank().strength() > best.rank().strength())) {
				best = c;
			}
		}
		return best;
	}

	/** The lowest legal card that isn't a trump, or null. */
	static Card lowest(List<Card> legal, Suit trump) {
		Card best = null;
		for (Card c : legal) {
			if (c.suit() != trump && (best == null || c.rank().strength() < best.rank().strength())) {
				best = c;
			}
		}
		return best;
	}
}
