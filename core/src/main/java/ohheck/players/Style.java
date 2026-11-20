package ohheck.players;

import java.util.ArrayList;
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
	THINKER("Thinks it through") {
		static final double EFFORT = 3;

		@Override
		boolean habitual() {
			return false;
		}

		@Override
		Bot base(Difficulty level, Random rng) {
			return level == Difficulty.HARD ? new HardBot(rng, HardBot.SPOIL, EFFORT, true) : level.bot(rng);
		}
	},
	BOLD("Overbids") {
		static final double STRENGTH = 0.25;

		@Override
		int bid(int bid, TableView v, StyledBot player) {
			return player.leans(STRENGTH) ? up(bid, v) : bid;
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
	SPOILER("Spoiler") {
		static final double SPOIL = 0.8;

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
	TRUMP_COUNTER("Trump counter") {
		@Override
		int bid(int bid, TableView v, StyledBot player) {
			return player.leans() ? Math.min(v.handSize(), trumpsAndAces(v)) : bid;
		}
	},
	SPENDTHRIFT("Spendthrift") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			if (v.need() <= 0) {
				return card;
			}
			if (v.trick().isEmpty()) {
				return strongest(legal, v);
			}
			List<Card> winners = winners(legal, v);
			return winners.contains(card) ? strongest(winners, v) : card;
		}
	},
	GREEDY("Greedy") {
		static final double CLOSE = 4;

		@Override
		int bid(int bid, TableView v, StyledBot player) {
			double[] expected = NormalBot.expectedScores(v);
			double best = Double.NEGATIVE_INFINITY;
			for (double e : expected) {
				best = Double.isNaN(e) ? best : Math.max(best, e);
			}
			for (int b = expected.length - 1; b > bid; b--) {
				if (!Double.isNaN(expected[b]) && expected[b] >= best - CLOSE) {
					return b;
				}
			}
			return bid;
		}
	},
	VOID_MAKER("Void maker") {
		@Override
		Card play(Card card, TableView v, List<Card> legal) {
			Suit led = v.leadSuit();
			if (led == null || card.suit() == led || card.suit() == v.trump()) {
				return card;
			}
			Card discard = null;
			int shortest = Integer.MAX_VALUE;
			for (Card c : legal) {
				int length = suitLength(v.hand(), c.suit());
				if (c.suit() != v.trump() && (length < shortest
						|| length == shortest && c.rank().strength() < discard.rank().strength())) {
					shortest = length;
					discard = c;
				}
			}
			return discard != null ? discard : card;
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

	static int trumpsAndAces(TableView v) {
		int n = 0;
		for (Card c : v.hand()) {
			if (c.suit() == v.trump() || c.rank() == Rank.ACE) {
				n++;
			}
		}
		return n;
	}

	static int suitLength(List<Card> hand, Suit suit) {
		int n = 0;
		for (Card c : hand) {
			if (c.suit() == suit) {
				n++;
			}
		}
		return n;
	}

	static List<Card> winners(List<Card> legal, TableView v) {
		Card winning = v.trick().get(v.rules().trickWinner(v.trick(), v.trump()));
		List<Card> winners = new ArrayList<Card>();
		for (Card c : legal) {
			if (v.rules().beats(c, winning, v.trump())) {
				winners.add(c);
			}
		}
		return winners;
	}

	static Card strongest(List<Card> cards, TableView v) {
		Card best = cards.get(0);
		for (Card c : cards) {
			if (NormalBot.cheaper(best, c, v.trump())) {
				best = c;
			}
		}
		return best;
	}
}
