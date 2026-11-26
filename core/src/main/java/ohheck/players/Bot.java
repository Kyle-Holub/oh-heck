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
 * every bot's bid passes through neverZeroWithTheTrumpAce.
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
		return v.bidding() ? new Move.Bid(v.seat(), chooseBid(v)) : new Move.Play(v.seat(), choosePlay(v));
	}

	static Bot of(Difficulty d, Random rng) {
		return d.bot(rng);
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
