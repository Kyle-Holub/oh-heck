package rules;

import java.util.List;

/**
 * Plays like a decent human.
 * <p>
 * Bidding: estimates each card's chance of taking a trick (how many higher
 * cards are still out, whether its suit is likely to be trumped), works out
 * the chance of taking 0, 1, 2... tricks, and bids whatever scores best on
 * average, since an exact bid is all that counts.
 * <p>
 * Playing: remembers what's been played and who is out of which suit; when it
 * wants tricks it cashes cards nobody can beat, wins as cheaply as possible
 * and trumps in; when it doesn't, it ducks under the winning card and sheds
 * its most dangerous high cards while it safely can.
 */
final class NormalBot implements Bot {

	// ---- bidding

	@Override
	public int chooseBid(TableView v) {
		double[] p = new double[v.hand.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = winChance(v, v.hand.get(i));
		}
		double[] dist = trickDistribution(p);
		int best = -1;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int b = 0; b <= v.handSize; b++) {
			if (!v.isLegalBid(b)) {
				continue;
			}
			double expected = 0;
			for (int k = 0; k < dist.length; k++) {
				expected += dist[k] * Rules.score(b, k);
			}
			if (expected > bestScore) {
				bestScore = expected;
				best = b;
			}
		}
		return best;
	}

	/** Rough chance that this card takes a trick, judged before play starts. */
	static double winChance(TableView v, Card c) {
		Suit trump = v.trump();
		int unseen = 52 - v.handSize - 1; // not in my hand, not the face-up trump
		double others = (double) (v.players - 1) * v.handSize;
		double held = Math.min(1, others / unseen); // chance a given unseen card is in someone's hand
		int higher = 0, mySuit = 0, myTrumps = 0;
		for (Rank r : Rank.values()) {
			Card o = Card.of(r, c.suit());
			if (r.outranks(c.rank()) && !v.hand.contains(o) && o != v.trumpCard) {
				higher++;
			}
		}
		for (Card h : v.hand) {
			if (h.suit() == c.suit()) {
				mySuit++;
			}
			if (h.suit() == trump) {
				myTrumps++;
			}
		}
		double noneHigher = Math.pow(1 - held, higher);
		if (c.suit() == trump) {
			// long trumps tend to win late even when small
			double length = 0.12 * Math.max(0, myTrumps - 2);
			return Math.min(0.98, noneHigher + (1 - noneHigher) * length);
		}
		// a side card can be trumped by someone out of its suit
		int suitOut = 13 - mySuit - (v.trumpCard.suit() == c.suit() ? 1 : 0);
		int trumpsOut = 13 - myTrumps - 1;
		double voidChance = Math.pow(1 - (double) suitOut / unseen, v.handSize);
		double hasTrump = 1 - Math.pow(1 - (double) trumpsOut / unseen, v.handSize);
		double safe = Math.pow(1 - voidChance * hasTrump, v.players - 1);
		return noneHigher * safe;
	}

	/** Chance of taking exactly k tricks, k = 0..n, if each card wins independently with chance p[i]. */
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

	// ---- playing

	@Override
	public Card choosePlay(TableView v) {
		List<Card> legal = v.legalPlays();
		if (legal.size() == 1) {
			return legal.get(0);
		}
		boolean want = v.need() > 0;
		return v.trick.isEmpty() ? lead(v, legal, want) : follow(v, legal, want);
	}

	private Card lead(TableView v, List<Card> legal, boolean want) {
		Suit trump = v.trump();
		if (want) {
			// cash a side-suit card nobody can beat, then a top trump
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
			if (v.need() >= v.hand.size()) {
				return highest(legal, trump, false); // every trick needed: lead strength
			}
			// otherwise feel out a side suit with a low card, keeping the high ones for later
			return lowest(legal, trump, true);
		}
		// no more tricks wanted: lead the card least likely to win
		return lowest(legal, trump, true);
	}

	private Card follow(TableView v, List<Card> legal, boolean want) {
		Suit trump = v.trump();
		Card winning = v.trick.get(Rules.trickWinner(v.trick, trump));
		boolean last = v.trick.size() == v.players - 1;
		Card cheapestWinner = null, highestWinner = null, highestLoser = null, lowestLoser = null;
		for (Card c : legal) {
			if (Rules.beats(c, winning, trump)) {
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
		if (want) {
			if (cheapestWinner == null) {
				return lowestLoser; // can't win: throw the least useful card
			}
			if (last) {
				return cheapestWinner; // the trick is ours for the least
			}
			// others still to play: a top card of the led suit holds best; a trump, the lowest that wins
			if (highestWinner.suit() != trump && isBoss(v, highestWinner)) {
				return highestWinner;
			}
			return cheapestWinner.suit() == trump ? cheapestWinner : highestWinner;
		}
		if (highestLoser != null) {
			return highestLoser; // duck, shedding the most dangerous card that still loses
		}
		// every card wins: if last, unload the biggest; otherwise play low and hope to be overtaken
		return last ? highestWinner : cheapestWinner;
	}

	/** Is c the highest card of its suit still out (not played, not the face-up trump, not otherwise in my hand)? */
	static boolean isBoss(TableView v, Card c) {
		for (Rank r : Rank.values()) {
			if (r.outranks(c.rank())) {
				Card o = Card.of(r, c.suit());
				if (!v.played[TableView.index(o)] && o != v.trumpCard && !v.hand.contains(o)) {
					return false;
				}
			}
		}
		return true;
	}

	/** Is a "cheaper" to give up than b? Non-trumps before trumps, then lower ranks. */
	static boolean cheaper(Card a, Card b, Suit trump) {
		boolean at = a.suit() == trump, bt = b.suit() == trump;
		if (at != bt) {
			return !at;
		}
		return b.rank().outranks(a.rank());
	}

	/** The cheapest card (non-trumps first when preferSide). */
	static Card lowest(List<Card> cards, Suit trump, boolean preferSide) {
		Card best = cards.get(0);
		for (Card c : cards) {
			if (preferSide ? cheaper(c, best, trump) : best.rank().outranks(c.rank())) {
				best = c;
			}
		}
		return best;
	}

	/** The strongest card (trumps first unless preferSide). */
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
