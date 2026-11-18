package rules;

import java.util.List;
import java.util.Random;

/** A Normal player that misjudges its hand now and then and sometimes plays the wrong card. */
final class EasyBot implements Bot {

	/** How often the bid is off by one, and how often a card is chosen at random. */
	private static final double BID_SLIP = 0.6, PLAY_SLIP = 0.5;

	private final NormalBot normal = new NormalBot();
	private final Random rng;

	EasyBot(Random rng) {
		this.rng = rng;
	}

	@Override
	public int chooseBid(TableView v) {
		int b = normal.chooseBid(v);
		if (rng.nextDouble() < BID_SLIP) {
			int off = b + (rng.nextBoolean() ? 1 : -1);
			if (off >= 0 && off <= v.handSize() && v.isLegalBid(off)) {
				return off;
			}
		}
		return b;
	}

	@Override
	public Card choosePlay(TableView v) {
		if (rng.nextDouble() < PLAY_SLIP) {
			List<Card> legal = v.legalPlays();
			return legal.get(rng.nextInt(legal.size()));
		}
		return normal.choosePlay(v);
	}
}
