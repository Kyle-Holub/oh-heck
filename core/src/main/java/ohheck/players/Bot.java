package ohheck.players;

import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.Move;
import ohheck.rules.Rank;
import ohheck.rules.TableView;

/*
 * A computer player. It decides from a TableView, so it knows only what a person in its seat
 * would. Whatever its level or habit, it never bids zero holding the trump ace, a sure trick:
 * every bot's bid passes through neverZeroWithTheTrumpAce.
 */
public interface Bot {

	int chooseBid(TableView v);

	Card choosePlay(TableView v);

	default Move chooseMove(TableView v) {
		return v.bidding() ? new Move.Bid(v.seat(), chooseBid(v)) : new Move.Play(v.seat(), choosePlay(v));
	}

	static Bot of(Difficulty d, Random rng) {
		return d.bot(rng);
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
