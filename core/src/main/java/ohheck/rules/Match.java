package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * One game of Oh Heck from the first deal to the final scores, with no
 * graphics or timing. A screen asks it whose turn it is, shows the state, and
 * calls {@link #placeBid} and {@link #playCard} as players act.
 * <p>
 * Seats are numbered 0 to {@code players - 1} clockwise. The deal moves one
 * seat clockwise each hand; the player left of the dealer bids first and
 * leads the first trick, and the dealer bids last. Whoever wins a trick leads
 * the next.
 */
public final class Match {

	public enum Phase {
		/** Players bid in turn, dealer last. */
		BIDDING,
		/** Players play cards in turn. */
		PLAYING,
		/** The hand's tricks are all played and scored; call {@link #nextHand()}. */
		HAND_OVER,
		/** All hands are played. */
		GAME_OVER
	}

	final int players;
	/** A seat's bid before it has bid. */
	public static final int NO_BID = -1;

	/** The moves made so far (see {@link #moves}). */
	private final List<Move> moves = new ArrayList<Move>();

	private final Random rng;
	private final RuleSet rules;

	// The game's state. Package-visible so MatchCodec can write and read it; nothing else touches it.

	Phase phase;
	int handIndex;
	/** Which way the hands run (set once, when the game starts or loads). */
	HandOrder order = HandOrder.UP;
	int dealer;
	int current;
	Card trumpCard;

	final List<List<Card>> hands = new ArrayList<List<Card>>();
	final int[] bids;
	final int[] tricks;
	final int[] scores;
	final int[] lastHandPoints;

	/** The trick in progress. */
	final Trick trick;
	/** Every card played this hand, in order, and who played it: what players remember. */
	final List<Card> history = new ArrayList<Card>();
	final List<Integer> historySeats = new ArrayList<Integer>();
	List<Card> lastTrick = Collections.emptyList();
	List<Integer> lastTrickSeats = Collections.emptyList();
	int lastTrickWinner = -1;

	/** A game by the standard rules, with a random first dealer, its hands running one card up to eight. */
	public static Match random(int players, Random rng) {
		return random(players, rng, HandOrder.UP, RuleSet.STANDARD);
	}

	/** A game by the given rules, with a random first dealer, its hands running the given way. */
	public static Match random(int players, Random rng, HandOrder order, RuleSet rules) {
		return new Match(players, rng, rng.nextInt(players), order, rules);
	}

	/** A game by the standard rules, its hands running one card up to eight, firstDealer dealing first. */
	public Match(int players, Random rng, int firstDealer) {
		this(players, rng, firstDealer, HandOrder.UP, RuleSet.STANDARD);
	}

	/** A game by the given rules, its hands running the given way, firstDealer dealing first. */
	public Match(int players, Random rng, int firstDealer, HandOrder order, RuleSet rules) {
		this(players, rng, rules);
		this.order = order;
		this.dealer = firstDealer;
		startHand();
	}

	/** Allocates an empty game, for the constructors above and {@link MatchCodec}. */
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

	// ---- Actions ----

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

	/** Makes a move (a bid or a card); returns the seat that won a trick it completed, else -1. */
	public int apply(Move move) {
		return move.applyTo(this);
	}

	/**
	 * Every move made in this game since it was started (or loaded: the log isn't saved), in
	 * order. Replayed onto a game dealt from the same seed, it reaches this position.
	 */
	public List<Move> moves() {
		return Collections.unmodifiableList(moves);
	}

	/**
	 * Plays a card. Returns the seat that won the trick if this card completed
	 * it, else -1. A completed trick stays visible through {@link #lastTrick()}.
	 */
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

	/** Deal the next hand, or end the game after the last one. */
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

	// ---- State ----

	public Phase phase() {
		return phase;
	}

	public int players() {
		return players;
	}

	/** Whose turn it is to bid or play. */
	public int currentSeat() {
		return current;
	}

	public int dealer() {
		return dealer;
	}

	/** Which way the hands run. */
	public HandOrder handOrder() {
		return order;
	}

	/** 0-based index of the current hand. */
	public int handIndex() {
		return handIndex;
	}

	/** The rules this game is played by. */
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

	/** The seat's cards, in the order they were dealt. */
	public List<Card> hand(int seat) {
		return Collections.unmodifiableList(hands.get(seat));
	}

	/** The seat's bid this hand, or {@link #NO_BID} if it hasn't bid yet. */
	public int bid(int seat) {
		return bids[seat];
	}

	public boolean hasBid(int seat) {
		return bids[seat] != NO_BID;
	}

	public int bidsSoFar() {
		return total(bids);
	}

	/** The bids made so far, added up ({@link #NO_BID}s left out). */
	static int total(int[] bids) {
		int total = 0;
		for (int b : bids) {
			if (b != NO_BID) {
				total += b;
			}
		}
		return total;
	}

	/** Can the player whose turn it is make this bid? */
	public boolean isLegalBid(int bid) {
		return phase == Phase.BIDDING && rules.isLegalBid(bid, handSize(), bidsSoFar(), current == dealer);
	}

	/** The bid the current player may not make, or -1 if every bid is allowed. */
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

	/** Points each seat scored in the most recently finished hand. */
	public int lastHandPoints(int seat) {
		return lastHandPoints[seat];
	}

	/** Did the seat take exactly its bid? Meaningful once the hand is over. */
	public boolean madeBid(int seat) {
		return hasBid(seat) && bids[seat] == tricks[seat];
	}

	/** Cards played so far in the trick in progress, lead first. */
	public List<Card> currentTrick() {
		return trick.cards();
	}

	/** Seats that played the cards in {@link #currentTrick()}, in the same order. */
	public List<Integer> currentTrickSeats() {
		return trick.seats();
	}

	public Suit leadSuit() {
		return trick.leadSuit();
	}

	public List<Card> legalPlays(int seat) {
		return rules.legalPlays(hands.get(seat), seat == current ? leadSuit() : null);
	}

	/** Every card played so far this hand (the whole hand once it's over), in order. */
	public List<Card> playedThisHand() {
		return Collections.unmodifiableList(history);
	}

	/** Who played each card in {@link #playedThisHand()}. */
	public List<Integer> playedThisHandSeats() {
		return Collections.unmodifiableList(historySeats);
	}

	public List<Card> lastTrick() {
		return lastTrick;
	}

	public List<Integer> lastTrickSeats() {
		return lastTrickSeats;
	}

	/** Seat that won the most recent completed trick, or -1 if none yet this game. */
	public int lastTrickWinner() {
		return lastTrickWinner;
	}

	/** Where this seat stands: 1 for the lead, and players on the same score share a place. */
	public int placeOf(int seat) {
		int p = 1;
		for (int s = 0; s < players; s++) {
			if (scores[s] > scores[seat]) {
				p++;
			}
		}
		return p;
	}

	/** Seats with the highest score; more than one means a tie. */
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

	/** What the given player can see, for a computer player to decide from. */
	public TableView view(int seat) {
		TableView v = new TableView(players);
		v.seat = seat;
		v.handIndex = handIndex;
		v.handSize = handSize();
		v.dealer = dealer;
		v.numberOfHands = rules.numberOfHands();
		v.rules = rules;
		v.trumpCard = trumpCard;
		v.hand.addAll(hands.get(seat));
		for (int s = 0; s < players; s++) {
			v.bids[s] = bids[s];
			v.tricksWon[s] = tricks[s];
			v.scores[s] = scores[s];
			v.cardsLeft[s] = hands.get(s).size();
		}
		v.trick.addAll(trick.cards);
		v.trickSeats.addAll(trick.seats);
		v.history.addAll(history);
		v.historySeats.addAll(historySeats);
		Suit lead = null;
		for (int i = 0; i < history.size(); i++) {
			Card c = history.get(i);
			v.played[c.index()] = true;
			if (i % players == 0) {
				lead = c.suit();
			} else if (c.suit() != lead) {
				v.voids[historySeats.get(i)][lead.ordinal()] = true;
			}
		}
		return v;
	}

	// ---- Saving (the text format is MatchCodec's) ----

	/**
	 * The whole game state as a short text, for saving to storage. The random
	 * number generator is not saved: a restored game deals future hands from
	 * the Random given to {@link #load}.
	 */
	public String save() {
		return MatchCodec.encode(this);
	}

	/**
	 * Rebuilds a game saved with {@link #save} (or by an older build).
	 *
	 * @throws IllegalArgumentException if the text is not a valid saved game
	 */
	public static Match load(String saved, Random rng) {
		return MatchCodec.decode(saved, rng);
	}

	/** Sanity checks after loading, so a damaged save is rejected rather than played. */
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

	// ---- Internals ----

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
