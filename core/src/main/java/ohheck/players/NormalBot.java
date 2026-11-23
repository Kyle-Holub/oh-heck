package ohheck.players;

import java.util.List;

import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.Suit;
import ohheck.rules.TableView;

/*
 * Bids the best expected score from BidTable's per-card odds, adjusted for the bids before it.
 * Plays with card memory: cashes winners and trumps in when it wants tricks, ducks and sheds
 * high cards when it doesn't. Top trumps in hand are sure tricks: once they cover what it still
 * needs, it keeps out of every other trick and takes its last ones with them. The tuning
 * constants were measured in the arena.
 */
final class NormalBot implements Bot {

	@Override
	public int chooseBid(TableView v) {
		double[] expected = expectedScores(v);
		int best = -1;
		for (int b = 0; b < expected.length; b++) {
			if (!Double.isNaN(expected[b]) && (best < 0 || expected[b] > expected[best])) {
				best = b;
			}
		}
		return Bot.neverZeroWithTheTrumpAce(v, best);
	}

	static double[] expectedScores(TableView v) {
		double[] p = new double[v.hand().size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = BidTable.chance(v, v.hand().get(i));
		}
		adjustForTheTable(v, p);
		double[] dist = trickDistribution(p);
		double[] expected = new double[v.handSize() + 1];
		for (int b = 0; b <= v.handSize(); b++) {
			if (!v.isLegalBid(b)) {
				expected[b] = Double.NaN;
				continue;
			}
			for (int k = 0; k < dist.length; k++) {
				expected[b] += dist[k] * v.rules().score(b, k);
			}
		}
		return expected;
	}

	static final double TABLE_PULL = 0.6;

	static void adjustForTheTable(TableView v, double[] p) {
		int before = ((v.seat() - v.dealer() - 1) % v.players() + v.players()) % v.players();
		if (before == 0 || p.length == 0 || v.rules().blindBids) {
			return;
		}
		double over = v.bidsSoFar() - before * (double) v.handSize() / v.players();
		double shift = Math.max(-0.5, Math.min(0.6, -TABLE_PULL * over / v.handSize()));
		double expected = 0;
		for (double x : p) {
			expected += x;
		}
		if (shift < 0 && expected > 0) {
			double f = Math.max(0, (expected + shift) / expected);
			for (int i = 0; i < p.length; i++) {
				p[i] *= f;
			}
		} else if (shift > 0 && expected < p.length) {
			double f = Math.max(0, (p.length - expected - shift) / (p.length - expected));
			for (int i = 0; i < p.length; i++) {
				p[i] = 1 - (1 - p[i]) * f;
			}
		}
	}

	static double[] trickDistribution(double[] p) {
		double[] d = new double[p.length + 1];
		d[0] = 1;
		for (int i = 0; i < p.length; i++) {
			for (int k = i + 1; k > 0; k--) {
				d[k] = d[k] * (1 - p[i]) + d[k - 1] * p[i];
			}
			d[0] *= 1 - p[i];
		}
		return d;
	}

	@Override
	public Card choosePlay(TableView v) {
		List<Card> legal = v.legalPlays();
		if (legal.size() == 1) {
			return legal.get(0);
		}
		boolean want = v.need() > 0;
		Card sure = lowestSure(legal, v);
		if (want && sure != null && v.need() <= sureTricks(v)) {
			return v.trick().isEmpty() ? sure : coverWithSureTricks(v, legal, sure);
		}
		return v.trick().isEmpty() ? lead(v, legal, want) : follow(v, legal, want);
	}

	static boolean isSure(TableView v, Card c) {
		return c.suit() == v.trump() && isBoss(v, c);
	}

	static int sureTricks(TableView v) {
		int n = 0;
		for (Card c : v.hand()) {
			n += isSure(v, c) ? 1 : 0;
		}
		return n;
	}

	private static Card lowestSure(List<Card> legal, TableView v) {
		Card best = null;
		for (Card c : legal) {
			if (isSure(v, c) && (best == null || best.rank().outranks(c.rank()))) {
				best = c;
			}
		}
		return best;
	}

	private Card coverWithSureTricks(TableView v, List<Card> legal, Card sure) {
		Suit trump = v.trump();
		Card winning = v.trick().get(v.rules().trickWinner(v.trick(), trump));
		Card highestLoser = null;
		for (Card c : legal) {
			if (!v.rules().beats(c, winning, trump) && (highestLoser == null || cheaper(highestLoser, c, trump))) {
				highestLoser = c;
			}
		}
		return highestLoser != null ? highestLoser : sure;
	}

	private Card lead(TableView v, List<Card> legal, boolean want) {
		Suit trump = v.trump();
		if (want) {
			Card boss = null;
			for (Card c : legal) {
				if (c.suit() != trump && isBoss(v, c) && (boss == null || c.rank().outranks(boss.rank()))) {
					boss = c;
				}
			}
			if (boss != null) {
				return boss;
			}
			for (Card c : legal) {
				if (c.suit() == trump && isBoss(v, c)) {
					return c;
				}
			}
			if (v.need() >= v.hand().size()) {
				return highest(legal, trump, false);
			}
			return lowest(legal, trump, true);
		}
		return lowest(legal, trump, true);
	}

	private Card follow(TableView v, List<Card> legal, boolean want) {
		Suit trump = v.trump();
		Card winning = v.trick().get(v.rules().trickWinner(v.trick(), trump));
		boolean last = v.trick().size() == v.players() - 1;
		Card cheapestWinner = null, highestWinner = null, highestLoser = null, lowestLoser = null;
		for (Card c : legal) {
			if (v.rules().beats(c, winning, trump)) {
				if (cheapestWinner == null || cheaper(c, cheapestWinner, trump)) {
					cheapestWinner = c;
				}
				if (highestWinner == null || cheaper(highestWinner, c, trump)) {
					highestWinner = c;
				}
			} else {
				if (highestLoser == null || cheaper(highestLoser, c, trump)) {
					highestLoser = c;
				}
				if (lowestLoser == null || cheaper(c, lowestLoser, trump)) {
					lowestLoser = c;
				}
			}
		}
		return want ? toWin(v, trump, last, cheapestWinner, highestWinner, lowestLoser)
				: toLose(last, cheapestWinner, highestWinner, highestLoser);
	}

	private Card toWin(TableView v, Suit trump, boolean last, Card cheapestWinner, Card highestWinner, Card lowestLoser) {
		if (cheapestWinner == null) {
			return lowestLoser;
		}
		if (last) {
			return cheapestWinner;
		}
		if (highestWinner.suit() != trump && isBoss(v, highestWinner)) {
			return highestWinner;
		}
		return cheapestWinner.suit() == trump ? cheapestWinner : highestWinner;
	}

	private static Card toLose(boolean last, Card cheapestWinner, Card highestWinner, Card highestLoser) {
		if (highestLoser != null) {
			return highestLoser;
		}
		return last ? highestWinner : cheapestWinner;
	}

	static boolean isBoss(TableView v, Card c) {
		for (Rank r : Rank.values()) {
			if (r.outranks(c.rank())) {
				Card o = Card.of(r, c.suit());
				if (!v.played(o) && o != v.trumpCard() && !v.hand().contains(o)) {
					return false;
				}
			}
		}
		return true;
	}

	static boolean cheaper(Card a, Card b, Suit trump) {
		boolean at = a.suit() == trump, bt = b.suit() == trump;
		if (at != bt) {
			return !at;
		}
		return b.rank().outranks(a.rank());
	}

	static Card lowest(List<Card> cards, Suit trump, boolean preferSide) {
		Card best = cards.get(0);
		for (Card c : cards) {
			if (preferSide ? cheaper(c, best, trump) : best.rank().outranks(c.rank())) {
				best = c;
			}
		}
		return best;
	}

	static Card highest(List<Card> cards, Suit trump, boolean preferSide) {
		Card best = cards.get(0);
		for (Card c : cards) {
			if (preferSide ? c.rank().outranks(best.rank()) : cheaper(best, c, trump)) {
				best = c;
			}
		}
		return best;
	}
}
