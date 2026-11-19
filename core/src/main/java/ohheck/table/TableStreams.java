package ohheck.table;

import java.util.Random;

/**
 * Separate random streams for who sits down, the bots' own randomness and the deck, so a
 * change that draws on one (a new player, a new habit) doesn't reshuffle the others: the
 * screenshot run's seeded game stays the same game.
 */
final class TableStreams {

	final Random seating, bots, deck;

	private TableStreams(Long seed) {
		seating = stream(seed, 0x5EA7L);
		bots = stream(seed, 0xB075L);
		deck = stream(seed, 0xDEC4L);
	}

	/** Unseeded normally (seed null); from the run's seed for the screenshot run. */
	static TableStreams of(Long seed) {
		return new TableStreams(seed);
	}

	/** A random stream: unseeded normally; from the run's seed and a salt for the screenshot run. */
	static Random stream(Long seed, long salt) {
		return seed == null ? new Random() : new Random(seed * 0x9E3779B97F4A7C15L ^ salt);
	}
}
