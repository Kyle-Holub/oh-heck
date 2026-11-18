package rules;

import java.util.Random;

/** A computer player. It decides from a {@link TableView}, so it only knows what a person at the table would. */
public interface Bot {

	/** A legal bid for the player whose turn it is. */
	int chooseBid(TableView v);

	/** A legal card for the player whose turn it is. */
	Card choosePlay(TableView v);

	/** A computer player of the given difficulty; rng drives any randomness (seed it for repeatable games). */
	static Bot of(Difficulty d, Random rng) {
		switch (d) {
		case EASY:
			return new EasyBot(rng);
		case HARD:
			return new HardBot(rng);
		default:
			return new NormalBot();
		}
	}
}
