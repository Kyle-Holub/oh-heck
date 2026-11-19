package ohheck.players;

import java.util.List;
import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.Suit;
import ohheck.rules.TableView;

/*
 * A player's habit, leaning the bids or cards its level's bot chooses. A new habit is one more
 * constant.
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
		int bid(int bid, TableView v, StyledBot player) {
			return player.leans() ? up(bid, v) : bid;
		}
	},
	TIMID("Underbids") {
		@Override
		int bid(int bid, TableView v, StyledBot player) {
			return player.leans() ? down(bid) : bid;
		}
	},
	FLUFFER("Fluffs") {
		@Override
		int bid(int bid, TableView v, StyledBot player) {
			double strength = bid == 1 ? 2 : bid == 2 ? 0.5 : 0;
			return player.leans(strength) ? 0 : bid;
		}
	},
	GAMBLER("Unpredictable") {
		@Override
		int bid(int bid, TableView v, StyledBot player) {
			if (!player.leans()) {
				return bid;
			}
			return player.coinFlip() ? up(bid, v) : down(bid);
		}
	},
	SPOILER("Spoiler") {
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
	STREAKY("Streaky") {
		@Override
		int bid(int bid, TableView v, StyledBot player) {
			if (player.hasBidBeforeThisHand(v) && player.leans()) {
				return player.scoreRoseSinceLastBid(v) ? up(bid, v) : down(bid);
			}
			return bid;
		}
	},
	FRONT_RUNNER("Front-runner") {
		@Override
		int bid(int bid, TableView v, StyledBot player) {
			boolean late = v.handIndex() >= v.numberOfHands() - LATE_HANDS;
			if (v.handIndex() > 0 && player.leans(late ? LATE_STRENGTH : 1)) {
				return v.score(v.seat()) > bestOtherScore(v) ? down(bid) : up(bid, v);
			}
			return bid;
		}
	},
	TRUMP_HAPPY("Trump-happy") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (v.trick().isEmpty() && v.hand().size() * 2 > v.handSize()) {
				Card t = highest(legal, v.trump(), true);
				return t != null ? t : card;
			}
			return card;
		}
	},
	TRUMP_HOARDER("Trump hoarder") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (card.suit() == v.trump() && v.hand().size() > 2) {
				Card other = lowest(legal, v.trump());
				return other != null ? other : card;
			}
			return card;
		}
	},
	ACE_LEADER("Ace leader") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (v.trick().isEmpty()) {
				Card high = highest(legal, v.trump(), false);
				return high != null && high.rank().strength() >= Rank.KING.strength() ? high : card;
			}
			return card;
		}
	};

	public final String label;

	Style(String label) {
		this.label = label;
	}

	Bot bot(Difficulty level, Random rng) {
		Bot base = base(level, rng);
		return habitual() ? new StyledBot(base, this, level, rng) : base;
	}

	Bot base(Difficulty level, Random rng) {
		return level.bot(rng);
	}

	boolean habitual() {
		return true;
	}

	int bid(int bid, TableView v, StyledBot player) {
		return bid;
	}

	Card play(Card card, TableView v, List<Card> legal) {
		return card;
	}

	private static final int LATE_HANDS = 3, LATE_STRENGTH = 3;

	static int up(int bid, TableView v) {
		return Math.min(v.handSize(), bid + 1);
	}

	static int down(int bid) {
		return Math.max(0, bid - 1);
	}

	static int bestOtherScore(TableView v) {
		int best = Integer.MIN_VALUE;
		for (int s = 0; s < v.players(); s++) {
			if (s != v.seat()) {
				best = Math.max(best, v.score(s));
			}
		}
		return best;
	}

	static Card highest(List<Card> legal, Suit trump, boolean trumps) {
		Card best = null;
		for (Card c : legal) {
			if ((c.suit() == trump) == trumps && (best == null || c.rank().strength() > best.rank().strength())) {
				best = c;
			}
		}
		return best;
	}

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
