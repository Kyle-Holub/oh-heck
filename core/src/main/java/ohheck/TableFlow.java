package ohheck;

import java.util.Random;

import rules.Bot;
import rules.Card;
import rules.Player;
import rules.Match;

/**
 * The card table's flow from the first shuffle to the final scores: which
 * {@link TableState} it's in, how long it stays there, and when the computer
 * players move. No drawing, so it can be tested on its own; {@link PlayScreen}
 * draws it and passes your moves in, and hears about events through
 * {@link Listener}.
 */
final class TableFlow {

	/** What the screen needs to hear about. */
	interface Listener {
		/** A new hand is about to be shuffled and dealt. */
		void beforeDeal(Match m);

		/** The game changed: save it. */
		void save(Match m);

		/** A riffle begins during the shuffle. */
		void shuffleSound();

		/** A card was played (yours, or a computer player's). */
		void played(int seat, Card c);

		/** The hand is over: "Bid Tricks!" or "You got set!" is showing. */
		void handEnded();

		/** The last hand is over: final scores are showing. */
		void gameEnded();
	}

	static final int HUMAN = TableLayout.HUMAN;
	/** A computer player's thinking time; how long the hand-end message stays; the trump card's turn. */
	static final float AI_DELAY = 0.8f, HAND_END_SHOW = 2.4f, TRUMP_FLIP_TIME = 0.45f;

	final DealAnimation deal = new DealAnimation();
	private final Listener listener;
	/** The computer players, one per seat (seat 0, yours, is unused). */
	private final Bot[] bots = new Bot[4];

	private Match match;
	private TableState state = TableState.TURN;
	private float stateT;
	/** Seconds since the deal finished, for the trump card turning over. */
	private float trumpT;

	TableFlow(Listener listener) {
		this.listener = listener;
	}

	/** Seats the computer players: players[s - 1] (a {@link Player} id) in seat s. */
	void seat(int[] players, Random rng) {
		for (int s = 1; s < bots.length; s++) {
			bots[s] = Player.ALL.get(players[s - 1]).bot(new Random(rng.nextLong()));
		}
	}

	// ---- starting

	/** Starts a new game: shuffle and deal its first hand. */
	void start(Match m) {
		match = m;
		listener.save(m);
		startHand();
	}

	/** Carries on a saved game where it was left, without replaying the deal. */
	void resume(Match m) {
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
		return state == TableState.SHUFFLE || state == TableState.DEAL;
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
		stateT += dt;
		if (!isDealing()) {
			trumpT += dt;
		}
		switch (state) {
		case SHUFFLE:
		case DEAL:
			if (deal.update(dt)) {
				listener.shuffleSound();
			}
			if (deal.isDone()) {
				enter(TableState.TURN);
			} else if (state == TableState.SHUFFLE && !deal.isShuffling()) {
				enter(TableState.DEAL);
			}
			break;
		case TURN:
			if (match.currentSeat() != HUMAN && stateT >= AI_DELAY) {
				botMove(match.currentSeat());
			}
			break;
		case TRICK:
			if (stateT >= TrickView.showLength(match.lastTrickWinner() == HUMAN)) {
				enter(match.phase() == Match.Phase.HAND_OVER ? TableState.HAND_END : TableState.TURN);
			}
			break;
		case HAND_END:
			if (stateT >= HAND_END_SHOW) {
				enter(TableState.RESULTS);
			}
			break;
		default:
			break;
		}
	}

	// ---- your actions

	/** Skips the shuffle and deal, or the hand-end message. Returns false if there was nothing to skip. */
	boolean skip() {
		if (isDealing()) {
			enter(TableState.TURN);
			return true;
		}
		if (state == TableState.HAND_END) {
			enter(TableState.RESULTS);
			return true;
		}
		return false;
	}

	/** Your bid; false (and nothing happens) if it isn't your turn to bid or the bid isn't allowed. */
	boolean bid(int b) {
		if (!yourTurnToBid() || !match.isLegalBid(b)) {
			return false;
		}
		placeBid(HUMAN, b);
		return true;
	}

	/** Your card; false (and nothing happens) if it isn't your turn to play or you must follow suit. */
	boolean play(Card c) {
		if (!yourTurnToPlay() || !match.legalPlays(HUMAN).contains(c)) {
			return false;
		}
		playCard(HUMAN, c);
		return true;
	}

	/** From the score table on to the next hand (or the final scores). False if the scores aren't showing. */
	boolean nextHand() {
		if (state != TableState.RESULTS) {
			return false;
		}
		match.nextHand();
		listener.save(match);
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER);
		} else {
			startHand();
		}
		return true;
	}

	// ---- internals

	private void startHand() {
		listener.beforeDeal(match);
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER); // (the screenshot run can play hands out in beforeDeal)
			return;
		}
		trumpT = 0;
		deal.start(match.handSize(), match.dealer(), match.handIndex() == 0);
		enter(TableState.SHUFFLE);
	}

	private void enter(TableState s) {
		state = s;
		stateT = 0;
		if (s == TableState.HAND_END) {
			listener.handEnded();
		} else if (s == TableState.GAME_OVER) {
			listener.gameEnded();
		}
	}

	private void botMove(int seat) {
		if (match.phase() == Match.Phase.BIDDING) {
			placeBid(seat, bots[seat].chooseBid(match.view(seat)));
		} else {
			playCard(seat, bots[seat].choosePlay(match.view(seat)));
		}
	}

	private void placeBid(int seat, int bid) {
		match.placeBid(seat, bid);
		listener.save(match);
		enter(TableState.TURN);
	}

	private void playCard(int seat, Card c) {
		int winner = match.playCard(seat, c);
		listener.save(match);
		listener.played(seat, c);
		enter(winner >= 0 ? TableState.TRICK : TableState.TURN);
	}
}
