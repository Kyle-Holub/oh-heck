package ohheck.table;

import java.util.Random;

/**
 * Separate random streams for seating, the bots and the deck, so drawing more from one leaves
 * the screenshot run's seeded game unchanged.
 */
final class TableStreams {

	final Random seating, bots, deck;

	private TableStreams(Long seed) {
		seating = stream(seed, 0x5EA7L);
		bots = stream(seed, 0xB075L);
		deck = stream(seed, 0xDEC4L);
	}

	static TableStreams of(Long seed) {
		return new TableStreams(seed);
	}

	static Random stream(Long seed, long salt) {
		return seed == null ? new Random() : new Random(seed * 0x9E3779B97F4A7C15L ^ salt);
	}
}
