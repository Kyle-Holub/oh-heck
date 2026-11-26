package ohheck.campaign;

import java.util.List;
import java.util.Random;

import ohheck.players.Bot;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Deal;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.rules.RuleSet;
import ohheck.rules.TableView;

/*
 * The practice round that teaches a new player: four Classic hands of 1 to 4 cards, dealt the same every time.
 * In the first two hands every move is scripted, yours and the rivals', so each lesson happens as planned;
 * in the last two the cards are fixed but you play freely against careful bots. Each scripted line is a
 * seat's bid and then its cards in the order played.
 */
public final class PracticeRound {

	public static final RuleSet RULES = new RuleSet(HandOrder.UP, 4, false, false);
	public static final int FIRST_DEALER = 1, SCRIPTED_HANDS = 2;
	public static final Seating RIVALS = Seating.of(4, 5, 6);
	public static final long BOT_SEED = 20261126L;

	static final List<Deal> DEALS = List.of(
			Deal.of("7S", "AS", "4H", "QH", "9H"),
			Deal.of("6D", "5H 2D", "9H 8C", "AH AC", "KH 3C"),
			Deal.of("9S", "2C 3H 4D", "AC KH JS", "KC AH QD", "QC AD 10S"),
			Deal.of("5H", "AH KS 9C 4D", "KH AS 3C 7D", "QH QS AC 8D", "2H JS KC AD"));

	private static final String[][] SCRIPT = {
			{ "1 AS", "1 4H", "0 QH", "0 9H" },
			{ "1 5H 2D", "0 9H 8C", "2 AH AC", "0 KH 3C" } };

	private static final String[] POSITIONS = { "you", "the player to your left", "the player across", "the player to your right" };

	private PracticeRound() {
	}

	public static Match match() {
		return Match.scripted(RULES, FIRST_DEALER, DEALS);
	}

	public static Bot[] bots() {
		Random rng = new Random(BOT_SEED);
		Bot[] bots = new Bot[Seating.SEATS];
		for (int s = 1; s < Seating.SEATS; s++) {
			bots[s] = new Scripted(s, RIVALS.at(s).bot(new Random(rng.nextLong())));
		}
		return bots;
	}

	public static String position(int seat) {
		return POSITIONS[seat];
	}

	public static boolean allowed(Match m, Move move) {
		if (m.handIndex() >= SCRIPTED_HANDS) {
			return true;
		}
		if (move instanceof Move.Bid bid) {
			return bid.tricks() == scriptedBid(m.handIndex(), move.seat());
		}
		return ((Move.Play) move).card().equals(scriptedPlay(m.handIndex(), move.seat(), m.hand(move.seat())));
	}

	static int scriptedBid(int hand, int seat) {
		return Integer.parseInt(SCRIPT[hand][seat].split(" ")[0]);
	}

	static Card scriptedPlay(int hand, int seat, List<Card> held) {
		String[] line = SCRIPT[hand][seat].split(" ");
		for (int i = 1; i < line.length; i++) {
			Card c = Card.parse(line[i]);
			if (held.contains(c)) {
				return c;
			}
		}
		throw new IllegalStateException("Seat " + seat + " has no scripted card left in hand " + (hand + 1));
	}

	private static final class Scripted implements Bot {

		private final int seat;
		private final Bot free;

		Scripted(int seat, Bot free) {
			this.seat = seat;
			this.free = free;
		}

		@Override
		public int chooseBid(TableView v) {
			return v.handIndex() < SCRIPTED_HANDS ? scriptedBid(v.handIndex(), seat) : free.chooseBid(v);
		}

		@Override
		public Card choosePlay(TableView v) {
			return v.handIndex() < SCRIPTED_HANDS ? scriptedPlay(v.handIndex(), seat, v.hand()) : free.choosePlay(v);
		}
	}
}
