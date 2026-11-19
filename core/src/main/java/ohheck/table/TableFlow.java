package ohheck.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import ohheck.players.Bot;
import ohheck.players.Player;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.rules.TableView;

/**
 * The card table's flow from the first shuffle to the final scores: which
 * {@link TableState} it's in, how long it stays there, and when the computer
 * players move. No drawing, so it can be tested on its own; {@link PlayScreen}
 * draws it and passes your moves in; it and others hear about events through
 * {@link Listener}s.
 */
final class TableFlow {

	/**
	 * What happens at the table, for whoever wants to know: the screen's effects, the
	 * sounds, the records. Each listener overrides only what it reacts to.
	 */
	interface Listener {
		/** A new hand is about to be shuffled and dealt. */
		default void beforeDeal(Match m) {
		}

		/** The game changed: save it. */
		default void save(Match m) {
		}

		/** A riffle begins during the shuffle. */
		default void shuffling() {
		}

		/** A card was played (yours, or a computer player's). */
		default void played(Match m, int seat, Card c) {
		}

		/** The hand is over: "Bid Tricks!" or "You got set!" is showing. */
		default void handEnded(Match m) {
		}

		/** The last hand is over: final scores are showing. */
		default void gameEnded(Match m) {
		}
	}

	static final int HUMAN = TableLayout.HUMAN;
	/** A computer player's thinking time; how long the hand-end message stays; the trump card's turn. */
	static final float AI_DELAY = 0.8f, HAND_END_SHOW = 2.4f, TRUMP_FLIP_TIME = 0.45f;

	final DealAnimation deal = new DealAnimation();
	private final List<Listener> listeners;
	/** The computer players, one per seat (seat 0, yours, is unused). */
	private final Bot[] bots = new Bot[TableLayout.SEATS];

	private Match match;
	private TableState state = TableState.TURN;
	private float stateT;
	/** Seconds since the deal finished, for the trump card turning over. */
	private float trumpT;
	/** This frame's time step, for the moment's own update. */
	private float dt;

	/** Where the computer players think; at once unless told otherwise. */
	private Thinker thinker = Thinker.AT_ONCE;
	/** The computer player's decision being worked out this turn (a bid or a card), or null. */
	private Future<Move> thinking;

	/** Lets the computer players think on the given thinker (in play: in the background). */
	void thinkWith(Thinker t) {
		thinker = t;
	}

	TableFlow(Listener... listeners) {
		this.listeners = new ArrayList<Listener>(Arrays.asList(listeners));
	}

	/** Tells each listener, in the order given. */
	private void tell(Consumer<Listener> event) {
		for (Listener l : listeners) {
			event.accept(l);
		}
	}

	/** Seats the computer players: players[s - 1] (a {@link Player} id) in seat s. */
	void seat(int[] players, Random rng) {
		for (int s = 1; s < bots.length; s++) {
			bots[s] = Player.byId(players[s - 1]).bot(new Random(rng.nextLong()));
		}
	}

	// ---- starting

	/** Starts a new game: shuffle and deal its first hand. */
	void start(Match m) {
		stopThinking();
		match = m;
		tell(l -> l.save(m));
		startHand();
	}

	/** Carries on a saved game where it was left, without replaying the deal. */
	void resume(Match m) {
		stopThinking();
		match = m;
		trumpT = TRUMP_FLIP_TIME;
		enter(m.phase() == Match.Phase.HAND_OVER ? TableState.HAND_END : TableState.TURN);
	}

	// ---- what's happening

	Match match() {
		return match;
	}

	TableState state() {
		return state;
	}

	/** Seconds in the current state. */
	float stateTime() {
		return stateT;
	}

	float trumpTime() {
		return trumpT;
	}

	boolean isDealing() {
		return state.isDealing();
	}

	boolean trumpFaceUp() {
		return !isDealing() && trumpT >= TRUMP_FLIP_TIME;
	}

	boolean yourTurnToBid() {
		return state == TableState.TURN && match.phase() == Match.Phase.BIDDING && match.currentSeat() == HUMAN;
	}

	boolean yourTurnToPlay() {
		return state == TableState.TURN && match.phase() == Match.Phase.PLAYING && match.currentSeat() == HUMAN;
	}

	/** Is it this seat's turn, with nothing else going on? */
	boolean isTurnOf(int seat) {
		return state == TableState.TURN && match.currentSeat() == seat;
	}

	// ---- time passing

	void update(float dt) {
		this.dt = dt;
		stateT += dt;
		if (!isDealing()) {
			trumpT += dt;
		}
		state.update(this);
	}

	// ---- your actions

	/** Skips the shuffle and deal, or the hand-end message. Returns false if there was nothing to skip. */
	boolean skip() {
		return state.skip(this);
	}

	/** Your bid; false (and nothing happens) if it isn't your turn to bid or the bid isn't allowed. */
	boolean bid(int b) {
		if (!yourTurnToBid() || !match.isLegalBid(b)) {
			return false;
		}
		make(new Move.Bid(HUMAN, b));
		return true;
	}

	/** Your card; false (and nothing happens) if it isn't your turn to play or you must follow suit. */
	boolean play(Card c) {
		if (!yourTurnToPlay() || !match.legalPlays(HUMAN).contains(c)) {
			return false;
		}
		make(new Move.Play(HUMAN, c));
		return true;
	}

	/** From the score table on to the next hand (or the final scores). False if the scores aren't showing. */
	boolean nextHand() {
		if (state != TableState.RESULTS) {
			return false;
		}
		match.nextHand();
		tell(l -> l.save(match));
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER);
		} else {
			startHand();
		}
		return true;
	}

	// ---- internals

	private void startHand() {
		tell(l -> l.beforeDeal(match));
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER); // (the screenshot run can play hands out in beforeDeal)
			return;
		}
		trumpT = 0;
		deal.start(match.handSize(), match.dealer(), match.handIndex() == 0);
		enter(TableState.SHUFFLE);
	}

	/** Drops a decision still being worked out (the table has moved on). */
	private void stopThinking() {
		if (thinking != null) {
			thinking.cancel(false);
			thinking = null;
		}
	}

	/** Moves to another moment, its clock starting at zero. */
	void enter(TableState s) {
		state = s;
		stateT = 0;
		stopThinking();
		s.entered(this);
	}

	void tellHandEnded() {
		tell(l -> l.handEnded(match));
	}

	void tellGameEnded() {
		tell(l -> l.gameEnded(match));
	}

	/** The shuffle and deal playing out; on to the turn once the cards are down. */
	void dealing() {
		if (deal.update(dt)) {
			tell(Listener::shuffling);
		}
		if (deal.isDone()) {
			enter(TableState.TURN);
		} else if (state == TableState.SHUFFLE && !deal.isShuffling()) {
			enter(TableState.DEAL);
		}
	}

	/** A computer player's turn: it thinks, then moves after its pause. */
	void computerTurn() {
		if (match.currentSeat() == HUMAN) {
			return;
		}
		if (thinking == null) {
			thinking = startThinking(match.currentSeat());
		}
		if (stateT >= AI_DELAY && thinking.isDone()) { // its usual pause, and the answer's ready
			botMove(match.currentSeat());
		}
	}

	/** Sets the seat's computer player thinking about its move, from a snapshot of the table. */
	private Future<Move> startThinking(int seat) {
		Bot bot = bots[seat];
		TableView view = match.view(seat);
		return thinker.think(() -> bot.chooseMove(view));
	}

	/** Plays the move the seat's computer player decided on. */
	private void botMove(int seat) {
		Move move;
		try {
			move = thinking.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		} catch (ExecutionException e) {
			throw new IllegalStateException("A computer player failed to decide", e.getCause());
		}
		thinking = null;
		make(move);
	}


	/** Makes a move (yours or a computer player's), saves, and moves the table on. */
	private void make(Move move) {
		int winner = match.apply(move);
		tell(l -> l.save(match));
		if (move instanceof Move.Play) {
			Card c = ((Move.Play) move).card();
			tell(l -> l.played(match, move.seat(), c));
		}
		enter(winner >= 0 ? TableState.TRICK : TableState.TURN);
	}

}
