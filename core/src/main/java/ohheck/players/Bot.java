package ohheck.players;

import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.Move;
import ohheck.rules.TableView;

/**
 * A computer player. It decides from a TableView, so it knows only what a person in its seat
 * would.
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
}
