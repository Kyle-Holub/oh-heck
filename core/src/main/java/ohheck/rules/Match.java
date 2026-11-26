package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/*
 * One game from the first deal to the final scores. Seats run clockwise from 0; the deal moves
 * one seat each hand; the player after the dealer leads, and the dealer bids last. Each hand is dealt
 * from a shuffled deck, or from a fixed list of deals for a scripted game such as the practice round.
 * When the winner picks trump, the hand opens with PICKING_TRUMP: whoever made the highest bid last hand
 * (ties to the first from the dealer's left) names the trump suit, which overrides the turned card. When hands
 * won are counted, a tie for first after the last hand plays on in one-card sudden-death hands until it breaks.
 * With the widow, that same player then sees two face-down cards set aside and may swap one of theirs for one
 * of them (SWAPPING); the widow is dealt only when the deck has two cards to spare.
 */
public final class Match {

	public enum Phase {
		PICKING_TRUMP,
		SWAPPING,
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
	int dealer;
	int current;
	Card trumpCard;
	Suit pickedTrump;
	final List<Card> widow = new ArrayList<Card>();

	final List<List<Card>> hands = new ArrayList<List<Card>>();
	final int[] bids;
	final int[] tricks;
	final int[] scores;
	final int[] lastHandPoints;
	final int[] streaks;

	final Trick trick;
	final List<Card> history = new ArrayList<Card>();
	final List<Integer> historySeats = new ArrayList<Integer>();
	List<Card> lastTrick = Collections.emptyList();
	List<Integer> lastTrickSeats = Collections.emptyList();
	int lastTrickWinner = -1;
	private List<Deal> script;

	public static Match random(int players, Random rng) {
		return random(players, rng, RuleSet.STANDARD);
	}

	public static Match random(int players, Random rng, RuleSet rules) {
		return new Match(players, rng, rng.nextInt(players), rules);
	}

	public Match(int players, Random rng, int firstDealer) {
		this(players, rng, firstDealer, RuleSet.STANDARD);
	}

	public Match(int players, Random rng, int firstDealer, RuleSet rules) {
		this(players, rng, rules);
		this.dealer = firstDealer;
		startHand();
	}

	public static Match scripted(RuleSet rules, int firstDealer, List<Deal> deals) {
		if (deals.size() != rules.numberOfHands()) {
			throw new IllegalArgumentException("A deal is needed for each of the " + rules.numberOfHands() + " hands");
		}
		Match m = new Match(deals.get(0).hands().size(), new Random(0), rules);
		m.script = deals;
		m.dealer = firstDealer;
		m.startHand();
		return m;
	}

	public static Match dealt(RuleSet rules, int dealer, List<List<Card>> hands, Card trumpCard) {
		Match m = new Match(hands.size(), new Random(0), rules);
		int size = hands.get(0).size();
		while (m.handIndex < rules.numberOfHands() - 1 && rules.handSize(m.handIndex) != size) {
			m.handIndex++;
		}
		for (int s = 0; s < hands.size(); s++) {
			m.hands.set(s, new ArrayList<Card>(hands.get(s)));
		}
		m.trumpCard = trumpCard;
		m.dealer = dealer;
		m.phase = Phase.BIDDING;
		m.current = m.next(dealer);
		if (m.handSize() != size) {
			throw new IllegalArgumentException("No hand of " + size + " in " + rules.code());
		}
		m.check();
		return m;
	}

	Match(int players, Random rng, RuleSet rules) {
		if (players < 2 || players * rules.mostCards > rules.deckSize()) {
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
		this.streaks = new int[players];
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

	public void pickTrump(int seat, Suit suit) {
		requirePhase(Phase.PICKING_TRUMP);
		requireTurn(seat);
		moves.add(new Move.PickTrump(seat, suit));
		pickedTrump = suit;
		if (widow.isEmpty()) {
			startBidding();
		} else {
			phase = Phase.SWAPPING;
		}
	}

	public void swapWidow(int seat, Card give, Card take) {
		requirePhase(Phase.SWAPPING);
		requireTurn(seat);
		if (give != null && (!hands.get(seat).contains(give) || !widow.contains(take))) {
			throw new IllegalArgumentException("Seat " + seat + " may not swap " + give + " for " + take);
		}
		moves.add(new Move.SwapWidow(seat, give, take));
		if (give != null) {
			hands.get(seat).remove(give);
			hands.get(seat).add(take);
			widow.set(widow.indexOf(take), give);
		}
		startBidding();
	}

	public boolean swapping() {
		return phase == Phase.SWAPPING;
	}

	public List<Card> widow() {
		return Collections.unmodifiableList(widow);
	}

	private void startBidding() {
		phase = Phase.BIDDING;
		current = next(dealer);
	}

	public boolean pickingTrump() {
		return phase == Phase.PICKING_TRUMP;
	}

	public Suit pickedTrump() {
		return pickedTrump;
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
		int winner = trick.add(seat, card, trump());
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
				boolean made = bids[i] == tricks[i];
				lastHandPoints[i] = rules.score(bids[i], tricks[i]) * (made && onMomentum(i) ? 2 : 1);
				scores[i] += lastHandPoints[i];
				streaks[i] = made ? streaks[i] + 1 : 0;
			}
			phase = Phase.HAND_OVER;
		}
		return winner;
	}

	public void nextHand() {
		requirePhase(Phase.HAND_OVER);
		if (handIndex + 1 >= rules.numberOfHands() && !(rules.handsWon && leaders().size() > 1)) {
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

	public int handIndex() {
		return handIndex;
	}

	public RuleSet rules() {
		return rules;
	}

	public int handSize() {
		return rules.handSize(handIndex);
	}

	public boolean suddenDeath() {
		return handIndex >= rules.numberOfHands();
	}

	public Card trumpCard() {
		return trumpCard;
	}

	public Suit trump() {
		return pickedTrump != null ? pickedTrump : trumpCard == null ? null : trumpCard.suit();
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

	public int bidSeenBy(int seat, int viewer) {
		return rules.blindBids && phase == Phase.BIDDING && seat != viewer ? NO_BID : bids[seat];
	}

	public int bidsSeenBy(int viewer) {
		int total = 0;
		for (int s = 0; s < players; s++) {
			total += Math.max(0, bidSeenBy(s, viewer));
		}
		return total;
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
		if (phase != Phase.BIDDING || current != dealer || rules.blindBids) {
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

	public int streak(int seat) {
		return streaks[seat];
	}

	public boolean onMomentum(int seat) {
		return rules.momentum && streaks[seat] >= 2;
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

	public void winningCards(List<Card> into) {
		into.clear();
		for (int start = 0; start + players <= history.size(); start += players) {
			List<Card> played = history.subList(start, start + players);
			into.add(played.get(rules.trickWinner(played, trump())));
		}
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
				.hand(handIndex, rules.numberOfHands(), handSize(), dealer, trumpCard, rules)
				.pickedTrump(pickedTrump, pickingTrump()).widow(swapping() && seat == current ? widow : Collections.<Card>emptyList(), swapping())
				.cards(hands.get(seat))
				.trick(trick.cards(), trick.seats()).history(history, historySeats);
		for (int s = 0; s < players; s++) {
			v.seatState(s, bidSeenBy(s, seat), tricks[s], scores[s], hands.get(s).size());
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
		if (handIndex < 0 || handIndex >= rules.numberOfHands() && !rules.handsWon || dealer < 0 || dealer >= players || current < 0
				|| current >= players || trick.cards.size() != trick.seats.size() || trick.size() >= players
				|| history.size() != historySeats.size()) {
			throw new IllegalArgumentException("Inconsistent game state");
		}
		Set<Card> seen = new HashSet<Card>();
		if (trumpCard != null) {
			seen.add(trumpCard);
		}
		for (Card c : widow) {
			if (!seen.add(c)) {
				throw new IllegalArgumentException("Card appears twice: " + c);
			}
		}
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

	private int trumpPicker() {
		if (!rules.pickTrump && !rules.widow || handIndex == 0) {
			return -1;
		}
		int picker = -1;
		for (int i = 0; i < players; i++) {
			int seat = (dealer + 1 + i) % players;
			if (bids[seat] != NO_BID && bids[seat] == tricks[seat] && (picker < 0 || bids[seat] > bids[picker])) {
				picker = seat;
			}
		}
		return picker;
	}

	private void startHand() {
		int picker = trumpPicker();
		Deck deck = script == null ? Deck.shuffled(rng, rules) : null;
		for (int i = 0; i < players; i++) {
			int seat = (dealer + 1 + i) % players;
			hands.set(seat, deck != null ? deck.deal(handSize()) : new ArrayList<Card>(script.get(handIndex).hands().get(seat)));
			bids[seat] = NO_BID;
			tricks[seat] = 0;
		}
		trumpCard = deck == null ? script.get(handIndex).trump() : deck.size() > 0 ? deck.deal() : null;
		pickedTrump = null;
		widow.clear();
		if (rules.widow && picker >= 0 && deck != null && deck.size() >= 2) {
			widow.add(deck.deal());
			widow.add(deck.deal());
		}
		trick.clear();
		history.clear();
		historySeats.clear();
		current = picker;
		if (rules.pickTrump && picker >= 0) {
			phase = Phase.PICKING_TRUMP;
		} else if (!widow.isEmpty()) {
			phase = Phase.SWAPPING;
		} else {
			startBidding();
		}
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
