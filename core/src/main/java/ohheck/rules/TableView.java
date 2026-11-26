package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* What one player can see at the table; all a bot gets. */
public final class TableView {

	private int seat, handIndex, handSize, dealer, numberOfHands;
	private final int players;
	private RuleSet rules = RuleSet.STANDARD;
	private Card trumpCard;
	private Suit pickedTrump;
	private boolean pickingTrump, swapping;
	private final List<Card> widow = new ArrayList<Card>();
	private final List<Card> hand = new ArrayList<Card>();
	private final int[] bids;
	private final int[] tricksWon, scores;
	private final int[] cardsLeft;
	private final List<Card> trick = new ArrayList<Card>();
	private final List<Integer> trickSeats = new ArrayList<Integer>();
	private final List<Card> history = new ArrayList<Card>();
	private final List<Integer> historySeats = new ArrayList<Integer>();
	private final boolean[] played = new boolean[Card.COUNT];
	private final boolean[][] voids;

	private final List<Card> handView = Collections.unmodifiableList(hand), trickView = Collections.unmodifiableList(trick),
			historyView = Collections.unmodifiableList(history);
	private final List<Integer> trickSeatsView = Collections.unmodifiableList(trickSeats),
			historySeatsView = Collections.unmodifiableList(historySeats);

	private TableView(int players) {
		this.players = players;
		bids = new int[players];
		Arrays.fill(bids, Match.NO_BID);
		tricksWon = new int[players];
		scores = new int[players];
		cardsLeft = new int[players];
		voids = new boolean[players][4];
	}

	public Suit trump() {
		return pickedTrump != null ? pickedTrump : trumpCard == null ? null : trumpCard.suit();
	}

	public Suit leadSuit() {
		return trick.isEmpty() ? null : trick.get(0).suit();
	}

	public int bidsSoFar() {
		return Match.total(bids);
	}

	public boolean bidding() {
		return !pickingTrump && !swapping && !hasBid(seat);
	}

	public boolean pickingTrump() {
		return pickingTrump;
	}

	public Suit pickedTrump() {
		return pickedTrump;
	}

	public boolean swapping() {
		return swapping;
	}

	public List<Card> widow() {
		return Collections.unmodifiableList(widow);
	}

	public boolean hasBid(int seat) {
		return bids[seat] != Match.NO_BID;
	}

	public boolean isLegalBid(int bid) {
		return rules.isLegalBid(bid, handSize, bidsSoFar(), seat == dealer);
	}

	public List<Card> legalPlays() {
		return rules.legalPlays(hand, leadSuit());
	}

	public int need() {
		return (hasBid(seat) ? bids[seat] : 0) - tricksWon[seat];
	}

	public int seat() {
		return seat;
	}

	public int players() {
		return players;
	}

	public int handIndex() {
		return handIndex;
	}

	public int numberOfHands() {
		return numberOfHands;
	}

	public int handSize() {
		return handSize;
	}

	public int dealer() {
		return dealer;
	}

	public RuleSet rules() {
		return rules;
	}

	public Card trumpCard() {
		return trumpCard;
	}

	public List<Card> hand() {
		return handView;
	}

	public int bid(int seat) {
		return bids[seat];
	}

	public int tricksWon(int seat) {
		return tricksWon[seat];
	}

	public int score(int seat) {
		return scores[seat];
	}

	public int cardsLeft(int seat) {
		return cardsLeft[seat];
	}

	public List<Card> trick() {
		return trickView;
	}

	public List<Integer> trickSeats() {
		return trickSeatsView;
	}

	public List<Card> history() {
		return historyView;
	}

	public List<Integer> historySeats() {
		return historySeatsView;
	}

	public boolean played(Card c) {
		return played[c.index()];
	}

	public boolean isVoid(int seat, Suit suit) {
		return voids[seat][suit.ordinal()];
	}

	public boolean played(int index) {
		return played[index];
	}

	public static final class Builder {
		private final TableView v;

		public Builder(int players) {
			v = new TableView(players);
		}

		public TableView view() {
			return v;
		}

		public Builder seat(int seat) {
			v.seat = seat;
			return this;
		}

		public Builder hand(int handIndex, int numberOfHands, int handSize, int dealer, Card trumpCard, RuleSet rules) {
			v.handIndex = handIndex;
			v.numberOfHands = numberOfHands;
			v.handSize = handSize;
			v.dealer = dealer;
			v.trumpCard = trumpCard;
			v.rules = rules;
			return this;
		}

		public Builder pickedTrump(Suit suit, boolean picking) {
			v.pickedTrump = suit;
			v.pickingTrump = picking;
			return this;
		}

		public Builder widow(List<Card> cards, boolean swapping) {
			v.widow.clear();
			v.widow.addAll(cards);
			v.swapping = swapping;
			return this;
		}

		public Builder cards(Collection<Card> cards) {
			v.hand.clear();
			v.hand.addAll(cards);
			return this;
		}

		public Builder addCard(Card c) {
			v.hand.add(c);
			return this;
		}

		public Builder seatState(int seat, int bid, int tricksWon, int score, int cardsLeft) {
			v.bids[seat] = bid;
			v.tricksWon[seat] = tricksWon;
			v.scores[seat] = score;
			v.cardsLeft[seat] = cardsLeft;
			return this;
		}

		public Builder voids(int seat, boolean[] bySuit) {
			System.arraycopy(bySuit, 0, v.voids[seat], 0, 4);
			return this;
		}

		public Builder trick(List<Card> cards, List<Integer> seats) {
			v.trick.clear();
			v.trick.addAll(cards);
			v.trickSeats.clear();
			v.trickSeats.addAll(seats);
			return this;
		}

		public Builder history(List<Card> cards, List<Integer> seats) {
			v.history.clear();
			v.history.addAll(cards);
			v.historySeats.clear();
			v.historySeats.addAll(seats);
			Suit lead = null;
			for (int i = 0; i < cards.size(); i++) {
				Card c = cards.get(i);
				v.played[c.index()] = true;
				if (i % v.players == 0) {
					lead = c.suit();
				} else if (c.suit() != lead) {
					v.voids[seats.get(i)][lead.ordinal()] = true;
				}
			}
			return this;
		}

		public Builder played(boolean[] byIndex) {
			System.arraycopy(byIndex, 0, v.played, 0, Card.COUNT);
			return this;
		}
	}
}
