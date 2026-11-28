package ohheck.players;

import java.util.List;
import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.Move;
import ohheck.rules.Rank;
import ohheck.rules.Suit;
import ohheck.rules.TableView;

/*
 * A computer player. It decides from a TableView, so it knows only what a person in its seat
 * would. Whatever its level or habit, it never bids zero holding the trump ace, a sure trick:
 * every bot's bid passes through neverZeroWithTheTrumpAce. A misjudged or habit-leaned bid stays believable:
 * within reach of what the cards are worth, never claiming tricks a hand plainly can't take, or
 * giving up ones it plainly holds.
 */
public interface Bot {

	int chooseBid(TableView v);

	Card choosePlay(TableView v);

	default Suit chooseTrump(TableView v) {
		return strongestSuit(v.hand());
	}

	default Move chooseMove(TableView v) {
		if (v.pickingTrump()) {
			return new Move.PickTrump(v.seat(), chooseTrump(v));
		}
		if (v.swapping()) {
			return chooseSwap(v);
		}
		return v.bidding() ? new Move.Bid(v.seat(), chooseBid(v)) : new Move.Play(v.seat(), choosePlay(v));
	}

	static Bot of(Difficulty d, Random rng) {
		return d.bot(rng);
	}

	default Move.SwapWidow chooseSwap(TableView v) {
		Card take = null, give = null;
		for (Card c : v.widow()) {
			if (take == null || worth(c, v.trump()) > worth(take, v.trump())) {
				take = c;
			}
		}
		for (Card c : v.hand()) {
			if (give == null || worth(c, v.trump()) < worth(give, v.trump())) {
				give = c;
			}
		}
		boolean better = take != null && give != null && worth(take, v.trump()) > worth(give, v.trump());
		return better ? new Move.SwapWidow(v.seat(), give, take) : new Move.SwapWidow(v.seat(), null, null);
	}

	static int worth(Card c, Suit trump) {
		return c.rank().strength() + (c.suit() == trump ? Rank.ACE.strength() + 1 : 0);
	}

	static Suit strongestSuit(List<Card> hand) {
		Suit best = Suit.SPADES;
		int bestWorth = -1;
		for (Suit s : Suit.values()) {
			int worth = 0;
			for (Card c : hand) {
				if (c.suit() == s) {
					worth += 10 + c.rank().strength();
				}
			}
			if (worth > bestWorth) {
				best = s;
				bestWorth = worth;
			}
		}
		return best;
	}

	double BELIEVABLE_REACH = 1.5;

	static boolean believable(TableView v, int bid) {
		double worth = 0;
		for (Card c : v.hand()) {
			worth += BidTable.chance(v, c);
		}
		return Math.abs(bid - worth) < BELIEVABLE_REACH;
	}

	static int neverZeroWithTheTrumpAce(TableView v, int bid) {
		if (bid != 0 || v.trump() == null || !v.hand().contains(Card.of(Rank.ACE, v.trump()))) {
			return bid;
		}
		for (int b = 1; b <= v.handSize(); b++) {
			if (v.isLegalBid(b)) {
				return b;
			}
		}
		return bid;
	}
}
