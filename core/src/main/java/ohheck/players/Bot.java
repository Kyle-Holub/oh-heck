package ohheck.players;

import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.Move;
import ohheck.rules.TableView;

/** A computer player. It decides from a {@link TableView}, so it only knows what a person at the table would. */
public interface Bot {

	/** A legal bid for the player whose turn it is. */
	int chooseBid(TableView v);

	/** A legal card for the player whose turn it is. */
	Card choosePlay(TableView v);

	/** This player's move, whichever is due: a bid or a card. */
	default Move chooseMove(TableView v) {
		return v.bidding() ? new Move.Bid(v.seat(), chooseBid(v)) : new Move.Play(v.seat(), choosePlay(v));
	}

	/** A computer player of the given difficulty (see {@link Difficulty#bot}). */
	static Bot of(Difficulty d, Random rng) {
		return d.bot(rng);
	}
}
