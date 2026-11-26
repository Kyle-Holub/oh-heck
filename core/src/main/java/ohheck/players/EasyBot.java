package ohheck.players;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.TableView;

/*
 * A Normal player that loses count, misjudges its bid now and then, and plays loosely: often
 * another card that does the same job (another winner, another loser), never a careless one.
 */
final class EasyBot implements Bot {

	private static final double BID_SLIP = 0.7, LOOSE_PLAY = 0.5;
	private static final int TRICKS_REMEMBERED = 0;

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
				return Bot.neverZeroWithTheTrumpAce(v, off);
			}
		}
		return b;
	}

	@Override
	public Card choosePlay(TableView v) {
		TableView remembered = forgetful(v);
		Card card = normal.choosePlay(remembered);
		if (rng.nextDouble() >= LOOSE_PLAY || NormalBot.isSure(remembered, card)) {
			return card;
		}
		List<Card> alike = alike(remembered, card);
		return alike.get(rng.nextInt(alike.size()));
	}

	static TableView forgetful(TableView v) {
		int players = v.players(), inTrick = v.trick().size();
		int completed = (v.history().size() - inTrick) / players;
		int from = Math.max(0, completed - TRICKS_REMEMBERED) * players;
		TableView.Builder b = new TableView.Builder(players).seat(v.seat())
				.hand(v.handIndex(), v.numberOfHands(), v.handSize(), v.dealer(), v.trumpCard(), v.rules())
				.pickedTrump(v.pickedTrump(), false).cards(v.hand())
				.trick(v.trick(), v.trickSeats())
				.history(v.history().subList(from, v.history().size()), v.historySeats().subList(from, v.history().size()));
		for (int s = 0; s < players; s++) {
			b.seatState(s, v.bid(s), v.tricksWon(s), v.score(s), v.cardsLeft(s));
		}
		return b.view();
	}

	static List<Card> alike(TableView v, Card chosen) {
		List<Card> alike = new ArrayList<Card>();
		for (Card c : v.legalPlays()) {
			if (sameJob(v, c, chosen)) {
				alike.add(c);
			}
		}
		return alike;
	}

	private static boolean sameJob(TableView v, Card c, Card chosen) {
		if (v.trick().isEmpty()) {
			boolean wantsTricks = v.need() > 0;
			return wantsTricks ? c.rank().strength() >= chosen.rank().strength()
					: c.rank().strength() <= chosen.rank().strength();
		}
		return winsNow(v, c) == winsNow(v, chosen);
	}

	static boolean winsNow(TableView v, Card c) {
		Card winning = v.trick().get(v.rules().trickWinner(v.trick(), v.trump()));
		return v.rules().beats(c, winning, v.trump());
	}
}
