package ohheck.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What one player can see at the table: their own hand, the trump card, the
 * bids, tricks and scores, the cards played so far this hand, and which
 * players have shown they're out of a suit. Never anyone else's cards. This is
 * all a bot gets, and it can only read it: views are filled in by {@link Match#view},
 * or by a {@link Builder} for a game a bot imagines (the Hard bot's simulations), and
 * the lists a view hands out are read-only.
 */
public final class TableView {

	int seat, players, handIndex, handSize, dealer, numberOfHands;
	/** The rules this game is played by. */
	RuleSet rules = RuleSet.STANDARD;
	Card trumpCard;
	final List<Card> hand = new ArrayList<Card>();
	/** {@link Match#NO_BID} where a player hasn't bid yet. */
	final int[] bids;
	final int[] tricksWon, scores;
	/** Cards each player still holds. */
	final int[] cardsLeft;
	/** The trick in progress, lead first, and who played each card. */
	final List<Card> trick = new ArrayList<Card>();
	final List<Integer> trickSeats = new ArrayList<Integer>();
	/** Every card played so far this hand, in order, and who played it. */
	final List<Card> history = new ArrayList<Card>();
	final List<Integer> historySeats = new ArrayList<Integer>();
	/** Cards played so far this hand (including the trick in progress), by {@link #index}. */
	final boolean[] played = new boolean[52];
	/** voids[seat][suit]: that player failed to follow that suit this hand, so holds none of it. */
	final boolean[][] voids;

	// read-only views of the lists, made once
	private final List<Card> handView = Collections.unmodifiableList(hand), trickView = Collections.unmodifiableList(trick),
			historyView = Collections.unmodifiableList(history);
	private final List<Integer> trickSeatsView = Collections.unmodifiableList(trickSeats),
			historySeatsView = Collections.unmodifiableList(historySeats);

	TableView(int players) {
		this.players = players;
		bids = new int[players];
		java.util.Arrays.fill(bids, Match.NO_BID);
		tricksWon = new int[players];
		scores = new int[players];
		cardsLeft = new int[players];
		voids = new boolean[players][4];
	}

	/** A number from 0 to 51 for each card. */
	public static int index(Card c) {
		return c.suit().ordinal() * 13 + c.rank().strength();
	}

	public Suit trump() {
		return trumpCard.suit();
	}

	public Suit leadSuit() {
		return trick.isEmpty() ? null : trick.get(0).suit();
	}

	public int bidsSoFar() {
		int t = 0;
		for (int s = 0; s < players; s++) {
			if (hasBid(s)) {
				t += bids[s];
			}
		}
		return t;
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

	/** Tricks this player still wants: their bid minus tricks taken (0 or less: wants no more). */
	public int need() {
		return (hasBid(seat) ? bids[seat] : 0) - tricksWon[seat];
	}

	// ---- reading it

	/** Whose view this is. */
	public int seat() {
		return seat;
	}

	public int players() {
		return players;
	}

	/** Which hand of the game (0-based), and how many there are. */
	public int handIndex() {
		return handIndex;
	}

	public int numberOfHands() {
		return numberOfHands;
	}

	/** Cards dealt to each player this hand. */
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

	/** This player's cards. */
	public List<Card> hand() {
		return handView;
	}

	/** A seat's bid, or {@link Match#NO_BID}. */
	public int bid(int seat) {
		return bids[seat];
	}

	public int tricksWon(int seat) {
		return tricksWon[seat];
	}

	public int score(int seat) {
		return scores[seat];
	}

	/** Cards a seat still holds. */
	public int cardsLeft(int seat) {
		return cardsLeft[seat];
	}

	/** The trick in progress, lead first, and who played each card. */
	public List<Card> trick() {
		return trickView;
	}

	public List<Integer> trickSeats() {
		return trickSeatsView;
	}

	/** Every card played this hand, in order, and who played it. */
	public List<Card> history() {
		return historyView;
	}

	public List<Integer> historySeats() {
		return historySeatsView;
	}

	/** Whether this card has been played this hand (including the trick in progress). */
	public boolean played(Card c) {
		return played[index(c)];
	}

	/** Whether the seat has shown it holds none of this suit (it didn't follow it). */
	public boolean isVoid(int seat, Suit suit) {
		return voids[seat][suit.ordinal()];
	}

	/** Whether the card with this {@link #index} has been played this hand. */
	public boolean played(int index) {
		return played[index];
	}

	/**
	 * Fills in a view of an imagined table, for a bot thinking ahead. It keeps its view, so
	 * one view can be refilled again and again without allocating.
	 */
	public static final class Builder {
		private final TableView v;

		public Builder(int players) {
			v = new TableView(players);
		}

		/** The view being filled. Bots read it; only this builder changes it. */
		public TableView view() {
			return v;
		}

		public Builder seat(int seat) {
			v.seat = seat;
			return this;
		}

		/** The hand: which (0-based), how many in the game, cards each, who dealt, trump, rules. */
		public Builder hand(int handIndex, int numberOfHands, int handSize, int dealer, Card trumpCard, RuleSet rules) {
			v.handIndex = handIndex;
			v.numberOfHands = numberOfHands;
			v.handSize = handSize;
			v.dealer = dealer;
			v.trumpCard = trumpCard;
			v.rules = rules;
			return this;
		}

		/** This player's cards (replacing any before). */
		public Builder cards(java.util.Collection<Card> cards) {
			v.hand.clear();
			v.hand.addAll(cards);
			return this;
		}

		public Builder addCard(Card c) {
			v.hand.add(c);
			return this;
		}

		/** One seat's bid (or Match.NO_BID), tricks, score and cards left. */
		public Builder seatState(int seat, int bid, int tricksWon, int score, int cardsLeft) {
			v.bids[seat] = bid;
			v.tricksWon[seat] = tricksWon;
			v.scores[seat] = score;
			v.cardsLeft[seat] = cardsLeft;
			return this;
		}

		/** Which suits a seat has shown it holds none of (by suit ordinal). */
		public Builder voids(int seat, boolean[] bySuit) {
			System.arraycopy(bySuit, 0, v.voids[seat], 0, 4);
			return this;
		}

		/** The trick in progress (replacing any before). */
		public Builder trick(List<Card> cards, List<Integer> seats) {
			v.trick.clear();
			v.trick.addAll(cards);
			v.trickSeats.clear();
			v.trickSeats.addAll(seats);
			return this;
		}

		/** Which cards have been played, by {@link TableView#index}. */
		public Builder played(boolean[] byIndex) {
			System.arraycopy(byIndex, 0, v.played, 0, 52);
			return this;
		}
	}
}
