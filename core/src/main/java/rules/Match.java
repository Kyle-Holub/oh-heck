package rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

	private final int players;
	private final Random rng;

	private Phase phase;
	private int handIndex;
	private int dealer;
	private int current;
	private Card trumpCard;

	private final List<List<Card>> hands = new ArrayList<List<Card>>();
	private final Integer[] bids;
	private final int[] tricks;
	private final int[] scores;
	private final int[] lastHandPoints;

	private final List<Card> trick = new ArrayList<Card>();
	private final List<Integer> trickSeats = new ArrayList<Integer>();
	/** Every card played this hand, in order, and who played it: what players remember. */
	private final List<Card> history = new ArrayList<Card>();
	private final List<Integer> historySeats = new ArrayList<Integer>();
	private List<Card> lastTrick = Collections.emptyList();
	private List<Integer> lastTrickSeats = Collections.emptyList();
	private int lastTrickWinner = -1;

	/** A game with a random first dealer. */
	public static Match random(int players, Random rng) {
		return new Match(players, rng, rng.nextInt(players));
	}

	public Match(int players, Random rng, int firstDealer) {
		this(players, rng);
		this.dealer = firstDealer;
		startHand();
	}

	/** Allocates an empty game, for the constructors above and {@link #load}. */
	private Match(int players, Random rng) {
		if (players < 2 || players * Rules.handSize(Rules.numberOfHands() - 1) >= 52) {
			throw new IllegalArgumentException("Unsupported number of players: " + players);
		}
		this.players = players;
		this.rng = rng;
		this.bids = new Integer[players];
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
		bids[seat] = bid;
		if (seat == dealer) {
			phase = Phase.PLAYING;
			current = next(dealer);
		} else {
			current = next(seat);
		}
	}

	/**
	 * Play a card. Returns the seat that won the trick if this card completed
	 * it, else -1. A completed trick stays visible through {@link #lastTrick()}.
	 */
	public int playCard(int seat, Card card) {
		requirePhase(Phase.PLAYING);
		requireTurn(seat);
		List<Card> hand = hands.get(seat);
		if (!Rules.isLegalPlay(hand, card, leadSuit())) {
			throw new IllegalArgumentException("Seat " + seat + " may not play " + card);
		}
		hand.remove(card);
		trick.add(card);
		trickSeats.add(seat);
		history.add(card);
		historySeats.add(seat);

		if (trick.size() < players) {
			current = next(seat);
			return -1;
		}

		int winner = trickSeats.get(Rules.trickWinner(trick, trumpCard.suit()));
		tricks[winner]++;
		lastTrick = Collections.unmodifiableList(new ArrayList<Card>(trick));
		lastTrickSeats = Collections.unmodifiableList(new ArrayList<Integer>(trickSeats));
		lastTrickWinner = winner;
		trick.clear();
		trickSeats.clear();
		current = winner;

		if (hand.isEmpty()) {
			for (int i = 0; i < players; i++) {
				lastHandPoints[i] = Rules.score(bids[i], tricks[i]);
				scores[i] += lastHandPoints[i];
			}
			phase = Phase.HAND_OVER;
		}
		return winner;
	}

	/** Deal the next hand, or end the game after the last one. */
	public void nextHand() {
		requirePhase(Phase.HAND_OVER);
		if (handIndex + 1 >= Rules.numberOfHands()) {
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

	/** 0-based index of the current hand. */
	public int handIndex() {
		return handIndex;
	}

	public int handSize() {
		return Rules.handSize(handIndex);
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

	/** The seat's bid this hand, or null if it hasn't bid yet. */
	public Integer bid(int seat) {
		return bids[seat];
	}

	public int bidsSoFar() {
		int total = 0;
		for (Integer b : bids) {
			if (b != null) {
				total += b;
			}
		}
		return total;
	}

	/** Can the player whose turn it is make this bid? */
	public boolean isLegalBid(int bid) {
		return phase == Phase.BIDDING && Rules.isLegalBid(bid, handSize(), bidsSoFar(), current == dealer);
	}

	/** The bid the current player may not make, or -1 if every bid is allowed. */
	public int forbiddenBid() {
		if (phase != Phase.BIDDING || current != dealer) {
			return -1;
		}
		int f = Rules.forbiddenDealerBid(handSize(), bidsSoFar());
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
		return bids[seat] != null && bids[seat] == tricks[seat];
	}

	/** Cards played so far in the trick in progress, lead first. */
	public List<Card> currentTrick() {
		return Collections.unmodifiableList(trick);
	}

	/** Seats that played the cards in {@link #currentTrick()}, in the same order. */
	public List<Integer> currentTrickSeats() {
		return Collections.unmodifiableList(trickSeats);
	}

	public Suit leadSuit() {
		return trick.isEmpty() ? null : trick.get(0).suit();
	}

	public List<Card> legalPlays(int seat) {
		return Rules.legalPlays(hands.get(seat), seat == current ? leadSuit() : null);
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
		v.numberOfHands = Rules.numberOfHands();
		v.trumpCard = trumpCard;
		v.hand.addAll(hands.get(seat));
		for (int s = 0; s < players; s++) {
			v.bids[s] = bids[s];
			v.tricksWon[s] = tricks[s];
			v.scores[s] = scores[s];
			v.cardsLeft[s] = hands.get(s).size();
		}
		v.trick.addAll(trick);
		v.trickSeats.addAll(trickSeats);
		v.history.addAll(history);
		v.historySeats.addAll(historySeats);
		Suit lead = null;
		for (int i = 0; i < history.size(); i++) {
			Card c = history.get(i);
			v.played[TableView.index(c)] = true;
			if (i % players == 0) {
				lead = c.suit();
			} else if (c.suit() != lead) {
				v.voids[historySeats.get(i)][lead.ordinal()] = true;
			}
		}
		return v;
	}

	// ---- Saving ----

	/** Cards written as text ("AS", "10H"). */
	private static final String FORMAT = "oh-heck-match-2";
	/** The first format, which wrote cards as numbers from the enums' declaration order; still read. */
	private static final String FORMAT_1 = "oh-heck-match-1";

	/**
	 * The whole game state as a short text, for saving to storage. The random
	 * number generator is not saved: a restored game deals future hands from
	 * the Random given to {@link #load}.
	 */
	public String save() {
		StringBuilder sb = new StringBuilder(FORMAT).append('\n');
		line(sb, "players", String.valueOf(players));
		line(sb, "phase", phase.name());
		line(sb, "hand", String.valueOf(handIndex));
		line(sb, "dealer", String.valueOf(dealer));
		line(sb, "current", String.valueOf(current));
		line(sb, "trump", code(trumpCard));
		for (int s = 0; s < players; s++) {
			line(sb, "hand" + s, cards(hands.get(s)));
		}
		StringBuilder b = new StringBuilder();
		for (int s = 0; s < players; s++) {
			b.append(s > 0 ? "," : "").append(bids[s] == null ? "-" : bids[s].toString());
		}
		line(sb, "bids", b.toString());
		line(sb, "tricks", ints(tricks));
		line(sb, "scores", ints(scores));
		line(sb, "points", ints(lastHandPoints));
		line(sb, "trick", cards(trick));
		line(sb, "trickSeats", seats(trickSeats));
		line(sb, "last", cards(lastTrick));
		line(sb, "lastSeats", seats(lastTrickSeats));
		line(sb, "lastWinner", String.valueOf(lastTrickWinner));
		line(sb, "history", cards(history));
		line(sb, "historySeats", seats(historySeats));
		return sb.toString();
	}

	/**
	 * Rebuilds a game saved with {@link #save}.
	 *
	 * @throws IllegalArgumentException if the text is not a valid saved game
	 */
	public static Match load(String saved, Random rng) {
		try {
			java.util.Map<String, String> v = new java.util.HashMap<String, String>();
			String[] lines = saved.split("\n");
			String format = lines[0].trim();
			if (!format.equals(FORMAT) && !format.equals(FORMAT_1)) {
				throw new IllegalArgumentException("Not a saved game");
			}
			boolean legacy = format.equals(FORMAT_1);
			for (int i = 1; i < lines.length; i++) {
				int eq = lines[i].indexOf('=');
				if (eq > 0) {
					v.put(lines[i].substring(0, eq), lines[i].substring(eq + 1).trim());
				}
			}
			Match m = new Match(Integer.parseInt(req(v, "players")), rng);
			m.phase = Phase.valueOf(req(v, "phase"));
			m.handIndex = Integer.parseInt(req(v, "hand"));
			m.dealer = Integer.parseInt(req(v, "dealer"));
			m.current = Integer.parseInt(req(v, "current"));
			m.trumpCard = card(req(v, "trump"), legacy);
			for (int s = 0; s < m.players; s++) {
				m.hands.set(s, parseCards(req(v, "hand" + s), legacy));
			}
			String[] b = req(v, "bids").split(",");
			int[] t = parseInts(req(v, "tricks"), m.players), sc = parseInts(req(v, "scores"), m.players),
					pt = parseInts(req(v, "points"), m.players);
			for (int s = 0; s < m.players; s++) {
				m.bids[s] = b[s].equals("-") ? null : Integer.valueOf(b[s]);
				m.tricks[s] = t[s];
				m.scores[s] = sc[s];
				m.lastHandPoints[s] = pt[s];
			}
			m.trick.addAll(parseCards(req(v, "trick"), legacy));
			m.trickSeats.addAll(parseSeats(req(v, "trickSeats")));
			m.lastTrick = Collections.unmodifiableList(parseCards(req(v, "last"), legacy));
			m.lastTrickSeats = Collections.unmodifiableList(parseSeats(req(v, "lastSeats")));
			m.lastTrickWinner = Integer.parseInt(req(v, "lastWinner"));
			// (games saved before alpha-1.0.8's smarter players have no history: they just start forgetful)
			if (v.containsKey("history") && v.containsKey("historySeats")) {
				m.history.addAll(parseCards(v.get("history"), legacy));
				m.historySeats.addAll(parseSeats(v.get("historySeats")));
			}
			m.check();
			return m;
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Corrupt saved game: " + e.getMessage(), e);
		}
	}

	/** Sanity checks after loading, so a damaged save is rejected rather than played. */
	private void check() {
		if (handIndex < 0 || handIndex >= Rules.numberOfHands() || dealer < 0 || dealer >= players || current < 0
				|| current >= players || trick.size() != trickSeats.size() || trick.size() >= players
				|| history.size() != historySeats.size()) {
			throw new IllegalArgumentException("Inconsistent game state");
		}
		java.util.Set<Card> seen = new java.util.HashSet<Card>();
		seen.add(trumpCard);
		for (List<Card> h : hands) {
			for (Card c : h) {
				if (!seen.add(c)) {
					throw new IllegalArgumentException("Card appears twice: " + c);
				}
			}
		}
		for (Card c : trick) {
			if (!seen.add(c)) {
				throw new IllegalArgumentException("Card appears twice: " + c);
			}
		}
	}

	private static void line(StringBuilder sb, String key, String value) {
		sb.append(key).append('=').append(value).append('\n');
	}

	private static String req(java.util.Map<String, String> v, String key) {
		String s = v.get(key);
		if (s == null) {
			throw new IllegalArgumentException("Missing " + key);
		}
		return s;
	}

	// Card codes: rank then suit letter, e.g. "AS", "10H", "QD". Independent of the enums' order.
	private static final String[] RANK_CODES = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
	private static final String SUIT_CODES = "CDHS";
	private static final Suit[] SUITS_BY_CODE = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };
	// The first format's numbering: suit * 13 + rank, in these orders
	private static final Rank[] LEGACY_RANKS = { Rank.ACE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX,
			Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING };
	private static final Suit[] LEGACY_SUITS = { Suit.CLUBS, Suit.SPADES, Suit.HEARTS, Suit.DIAMONDS };

	private static String code(Card c) {
		return RANK_CODES[c.rank().strength()] + SUIT_CODES.charAt(java.util.Arrays.asList(SUITS_BY_CODE).indexOf(c.suit()));
	}

	private static Card card(String code, boolean legacy) {
		if (legacy) {
			int n = Integer.parseInt(code);
			return Card.of(LEGACY_RANKS[n % 13], LEGACY_SUITS[n / 13]);
		}
		int suit = SUIT_CODES.indexOf(code.charAt(code.length() - 1));
		String rank = code.substring(0, code.length() - 1);
		for (Rank r : Rank.values()) {
			if (RANK_CODES[r.strength()].equals(rank) && suit >= 0) {
				return Card.of(r, SUITS_BY_CODE[suit]);
			}
		}
		throw new IllegalArgumentException("Not a card: " + code);
	}

	private static String cards(List<Card> cs) {
		StringBuilder sb = new StringBuilder();
		for (Card c : cs) {
			sb.append(sb.length() > 0 ? "," : "").append(code(c));
		}
		return sb.toString();
	}

	private static List<Card> parseCards(String s, boolean legacy) {
		List<Card> list = new ArrayList<Card>();
		for (String p : s.isEmpty() ? new String[0] : s.split(",")) {
			list.add(card(p.trim(), legacy));
		}
		return list;
	}

	private static String seats(List<Integer> seats) {
		StringBuilder sb = new StringBuilder();
		for (Integer i : seats) {
			sb.append(sb.length() > 0 ? "," : "").append(i);
		}
		return sb.toString();
	}

	private static List<Integer> parseSeats(String s) {
		List<Integer> list = new ArrayList<Integer>();
		for (String p : s.isEmpty() ? new String[0] : s.split(",")) {
			list.add(Integer.valueOf(p));
		}
		return list;
	}

	private static String ints(int[] a) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			sb.append(i > 0 ? "," : "").append(a[i]);
		}
		return sb.toString();
	}

	private static int[] parseInts(String s, int n) {
		String[] p = s.split(",");
		if (p.length != n) {
			throw new IllegalArgumentException("Expected " + n + " values");
		}
		int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = Integer.parseInt(p[i]);
		}
		return a;
	}

	// ---- Internals ----

	private void startHand() {
		Deck deck = Deck.shuffled(rng);
		for (int i = 0; i < players; i++) {
			int seat = (dealer + 1 + i) % players;
			hands.set(seat, deck.deal(handSize()));
			bids[seat] = null;
			tricks[seat] = 0;
		}
		trumpCard = deck.deal();
		trick.clear();
		trickSeats.clear();
		history.clear();
		historySeats.clear();
		phase = Phase.BIDDING;
		current = next(dealer);
	}

	private int next(int seat) {
		return (seat + 1) % players;
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
