package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * One game from the first deal to the final scores. Seats run clockwise from 0; the deal moves
 * one seat each hand; the player after the dealer leads, and the dealer bids last.
 */
public final class Match {

	public enum Phase {
		BIDDING,
		PLAYING,
		HAND_OVER,
		GAME_OVER
	}

	final int players;
	public static final int NO_BID = -1;

	private final List<Move> moves = new ArrayList<Move>();

	private final Random rng;
	private final RuleSet rules;

	Phase phase;
	int handIndex;
	HandOrder order = HandOrder.UP;
	int dealer;
	int current;
	Card trumpCard;

	final List<List<Card>> hands = new ArrayList<List<Card>>();
	final int[] bids;
	final int[] tricks;
	final int[] scores;
	final int[] lastHandPoints;

	final Trick trick;
	final List<Card> history = new ArrayList<Card>();
	final List<Integer> historySeats = new ArrayList<Integer>();
	List<Card> lastTrick = Collections.emptyList();
	List<Integer> lastTrickSeats = Collections.emptyList();
	int lastTrickWinner = -1;

	public static Match random(int players, Random rng) {
		return random(players, rng, HandOrder.UP, RuleSet.STANDARD);
	}

	public static Match random(int players, Random rng, HandOrder order, RuleSet rules) {
		return new Match(players, rng, rng.nextInt(players), order, rules);
	}

	public Match(int players, Random rng, int firstDealer) {
		this(players, rng, firstDealer, HandOrder.UP, RuleSet.STANDARD);
	}

	public Match(int players, Random rng, int firstDealer, HandOrder order, RuleSet rules) {
		this(players, rng, rules);
		this.order = order;
		this.dealer = firstDealer;
		startHand();
	}

	Match(int players, Random rng, RuleSet rules) {
		if (players < 2 || players * rules.numberOfHands() >= Card.COUNT) {
			throw new IllegalArgumentException("Unsupported number of players: " + players);
		}
		this.players = players;
		this.rng = rng;
		this.rules = rules;
		this.trick = new Trick(players, rules);
		this.bids = new int[players];
		Arrays.fill(bids, NO_BID);
		this.tricks = new int[players];
		this.scores = new int[players];
		this.lastHandPoints = new int[players];
		for (int i = 0; i < players; i++) {
			hands.add(new ArrayList<Card>());
		}
	}

	public void placeBid(int seat, int bid) {
		requirePhase(Phase.BIDDING);
		requireTurn(seat);
		if (!isLegalBid(bid)) {
			throw new IllegalArgumentException("Seat " + seat + " may not bid " + bid);
		}
		moves.add(new Move.Bid(seat, bid));
		bids[seat] = bid;
		if (seat == dealer) {
			phase = Phase.PLAYING;
			current = next(dealer);
		} else {
			current = next(seat);
		}
	}

	public int apply(Move move) {
		return move.applyTo(this);
	}

	public List<Move> moves() {
		return Collections.unmodifiableList(moves);
	}

	public int playCard(int seat, Card card) {
		requirePhase(Phase.PLAYING);
		requireTurn(seat);
		List<Card> hand = hands.get(seat);
		if (!rules.isLegalPlay(hand, card, leadSuit())) {
			throw new IllegalArgumentException("Seat " + seat + " may not play " + card);
		}
		moves.add(new Move.Play(seat, card));
		hand.remove(card);
		history.add(card);
		historySeats.add(seat);
		int winner = trick.add(seat, card, trumpCard.suit());
		if (winner < 0) {
			current = next(seat);
			return -1;
		}

		tricks[winner]++;
		lastTrick = Collections.unmodifiableList(new ArrayList<Card>(trick.cards()));
		lastTrickSeats = Collections.unmodifiableList(new ArrayList<Integer>(trick.seats()));
		lastTrickWinner = winner;
		trick.clear();
		current = winner;

		if (hand.isEmpty()) {
			for (int i = 0; i < players; i++) {
				lastHandPoints[i] = rules.score(bids[i], tricks[i]);
				scores[i] += lastHandPoints[i];
			}
			phase = Phase.HAND_OVER;
		}
		return winner;
	}

	public void nextHand() {
		requirePhase(Phase.HAND_OVER);
		if (handIndex + 1 >= rules.numberOfHands()) {
			phase = Phase.GAME_OVER;
			return;
		}
		handIndex++;
		dealer = next(dealer);
		startHand();
	}

	public Phase phase() {
		return phase;
	}

	public int players() {
		return players;
	}

	public int currentSeat() {
		return current;
	}

	public int dealer() {
		return dealer;
	}

	public HandOrder handOrder() {
		return order;
	}

	public int handIndex() {
		return handIndex;
	}

	public RuleSet rules() {
		return rules;
	}

	public int handSize() {
		return rules.handSize(order, handIndex);
	}

	public Card trumpCard() {
		return trumpCard;
	}

	public Suit trump() {
		return trumpCard.suit();
	}

	public List<Card> hand(int seat) {
		return Collections.unmodifiableList(hands.get(seat));
	}

	public int bid(int seat) {
		return bids[seat];
	}

	public boolean hasBid(int seat) {
		return bids[seat] != NO_BID;
	}

	public int bidsSoFar() {
		return total(bids);
	}

	static int total(int[] bids) {
		int total = 0;
		for (int b : bids) {
			if (b != NO_BID) {
				total += b;
			}
		}
		return total;
	}

	public boolean isLegalBid(int bid) {
		return phase == Phase.BIDDING && rules.isLegalBid(bid, handSize(), bidsSoFar(), current == dealer);
	}

	public int forbiddenBid() {
		if (phase != Phase.BIDDING || current != dealer) {
			return -1;
		}
		int f = rules.forbiddenDealerBid(handSize(), bidsSoFar());
		return f >= 0 && f <= handSize() ? f : -1;
	}

	public int tricksWon(int seat) {
		return tricks[seat];
	}

	public int score(int seat) {
		return scores[seat];
	}

	public int lastHandPoints(int seat) {
		return lastHandPoints[seat];
	}

	public boolean madeBid(int seat) {
		return hasBid(seat) && bids[seat] == tricks[seat];
	}

	public List<Card> currentTrick() {
		return trick.cards();
	}

	public List<Integer> currentTrickSeats() {
		return trick.seats();
	}

	public Suit leadSuit() {
		return trick.leadSuit();
	}

	public List<Card> legalPlays(int seat) {
		return rules.legalPlays(hands.get(seat), seat == current ? leadSuit() : null);
	}

	public List<Card> playedThisHand() {
		return Collections.unmodifiableList(history);
	}

	public List<Integer> playedThisHandSeats() {
		return Collections.unmodifiableList(historySeats);
	}

	public List<Card> lastTrick() {
		return lastTrick;
	}

	public List<Integer> lastTrickSeats() {
		return lastTrickSeats;
	}

	public int lastTrickWinner() {
		return lastTrickWinner;
	}

	public int placeOf(int seat) {
		int p = 1;
		for (int s = 0; s < players; s++) {
			if (scores[s] > scores[seat]) {
				p++;
			}
		}
		return p;
	}

	public List<Integer> leaders() {
		int best = Integer.MIN_VALUE;
		for (int s : scores) {
			best = Math.max(best, s);
		}
		List<Integer> seats = new ArrayList<Integer>();
		for (int i = 0; i < players; i++) {
			if (scores[i] == best) {
				seats.add(i);
			}
		}
		return seats;
	}

	public TableView view(int seat) {
		TableView.Builder v = new TableView.Builder(players).seat(seat)
				.hand(handIndex, rules.numberOfHands(), handSize(), dealer, trumpCard, rules).cards(hands.get(seat))
				.trick(trick.cards(), trick.seats()).history(history, historySeats);
		for (int s = 0; s < players; s++) {
			v.seatState(s, bids[s], tricks[s], scores[s], hands.get(s).size());
		}
		return v.view();
	}

	public String save() {
		return MatchCodec.encode(this);
	}

	public static Match load(String saved, Random rng) {
		return MatchCodec.decode(saved, rng);
	}

	void check() {
		if (handIndex < 0 || handIndex >= rules.numberOfHands() || dealer < 0 || dealer >= players || current < 0
				|| current >= players || trick.cards.size() != trick.seats.size() || trick.size() >= players
				|| history.size() != historySeats.size()) {
			throw new IllegalArgumentException("Inconsistent game state");
		}
		Set<Card> seen = new HashSet<Card>();
		seen.add(trumpCard);
		for (List<Card> h : hands) {
			for (Card c : h) {
				if (!seen.add(c)) {
					throw new IllegalArgumentException("Card appears twice: " + c);
				}
			}
		}
		for (Card c : trick.cards) {
			if (!seen.add(c)) {
				throw new IllegalArgumentException("Card appears twice: " + c);
			}
		}
	}

	private void startHand() {
		Deck deck = Deck.shuffled(rng);
		for (int i = 0; i < players; i++) {
			int seat = (dealer + 1 + i) % players;
			hands.set(seat, deck.deal(handSize()));
			bids[seat] = NO_BID;
			tricks[seat] = 0;
		}
		trumpCard = deck.deal();
		trick.clear();
		history.clear();
		historySeats.clear();
		phase = Phase.BIDDING;
		current = next(dealer);
	}

	private int next(int seat) {
		return rules.nextSeat(seat, players);
	}

	private void requirePhase(Phase p) {
		if (phase != p) {
			throw new IllegalStateException("Not allowed during " + phase);
		}
	}

	private void requireTurn(int seat) {
		if (seat != current) {
			throw new IllegalStateException("Not seat " + seat + "'s turn (it is seat " + current + "'s)");
		}
	}
}
