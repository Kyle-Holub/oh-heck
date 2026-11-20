package ohheck.players;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;
import ohheck.rules.TableView;
import ohheck.rules.TestCards;

/* A table position for asking a bot what it would do: four seats, its hand, the bids and the play so far. */
final class Position {

	private static final int SEATS = 4, HANDS = 8;

	private final int handSize;
	private final Card trumpCard;
	private int seat, dealer = SEATS - 1;
	private List<Card> hand = new ArrayList<Card>();
	private final int[] bids = { Match.NO_BID, Match.NO_BID, Match.NO_BID, Match.NO_BID }, won = new int[SEATS];
	private final List<Card> history = new ArrayList<Card>(), trick = new ArrayList<Card>();
	private final List<Integer> historySeats = new ArrayList<Integer>(), trickSeats = new ArrayList<Integer>();

	private Position(int handSize, Card trumpCard) {
		this.handSize = handSize;
		this.trumpCard = trumpCard;
	}

	static Position deal(int handSize, String trumpCard) {
		return new Position(handSize, TestCards.c(trumpCard));
	}

	Position me(int seat, String hand) {
		this.seat = seat;
		this.hand = TestCards.cards(hand);
		return this;
	}

	Position dealer(int seat) {
		dealer = seat;
		return this;
	}

	Position bids(int... bids) {
		System.arraycopy(bids, 0, this.bids, 0, SEATS);
		return this;
	}

	Position won(int... tricks) {
		System.arraycopy(tricks, 0, won, 0, SEATS);
		return this;
	}

	Position played(int leader, String cards) {
		add(history, historySeats, leader, cards);
		return this;
	}

	Position trick(int leader, String cards) {
		add(trick, trickSeats, leader, cards);
		add(history, historySeats, leader, cards);
		return this;
	}

	private static void add(List<Card> to, List<Integer> seats, int leader, String cards) {
		List<Card> list = TestCards.cards(cards);
		for (int i = 0; i < list.size(); i++) {
			to.add(list.get(i));
			seats.add((leader + i) % SEATS);
		}
	}

	TableView view() {
		TableView.Builder b = new TableView.Builder(SEATS).seat(seat)
				.hand(handSize - 1, HANDS, handSize, dealer, trumpCard, RuleSet.STANDARD).cards(hand)
				.trick(trick, trickSeats).history(history, historySeats);
		for (int s = 0; s < SEATS; s++) {
			int played = 0;
			for (int who : historySeats) {
				played += who == s ? 1 : 0;
			}
			b.seatState(s, bids[s], won[s], 0, s == seat ? hand.size() : handSize - played);
		}
		return b.view();
	}

	@Override
	public String toString() {
		return "seat " + seat + " holding " + hand + ", trick " + trick + ", bids " + Arrays.toString(bids);
	}
}
