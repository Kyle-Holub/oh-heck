package ohheck.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import ohheck.players.Bot;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.rules.TableView;

/** The table's state machine and timing; asks the bots and tells its listeners what happens. */
final class TableFlow implements FlowView {

	interface Listener {
		default void beforeDeal(Match m) {
		}

		default void changed(Match m) {
		}

		default void shuffling() {
		}

		default void played(Match m, int seat, Card c) {
		}

		default void handEnded(Match m) {
		}

		default void gameEnded(Match m) {
		}
	}

	static final float AI_DELAY = 0.8f, HAND_END_SHOW = 2.4f, TRUMP_FLIP_TIME = 0.45f;

	private final DealAnimation deal = new DealAnimation();
	private final List<Listener> listeners;
	private final Bot[] bots = new Bot[Seating.SEATS];

	private Match match;
	private TableState state = TableState.TURN;
	private float stateTime;
	private float trumpTime;

	private Thinker thinker = Thinker.AT_ONCE;
	private Future<Move> thinking;

	void thinkWith(Thinker t) {
		thinker = t;
	}

	TableFlow(Listener... listeners) {
		this.listeners = new ArrayList<Listener>(Arrays.asList(listeners));
	}

	private void tell(Consumer<Listener> event) {
		for (Listener l : listeners) {
			event.accept(l);
		}
	}

	void seat(Seating players, Random rng) {
		for (int s = 0; s < bots.length; s++) {
			if (s != Seating.YOU) {
				bots[s] = players.at(s).bot(new Random(rng.nextLong()));
			}
		}
	}

	void start(Match m) {
		stopThinking();
		match = m;
		tell(l -> l.changed(m));
		startHand();
	}

	void resume(Match m) {
		stopThinking();
		match = m;
		trumpTime = TRUMP_FLIP_TIME;
		enter(m.phase() == Match.Phase.HAND_OVER ? TableState.HAND_END : TableState.TURN);
	}

	@Override
	public Match match() {
		return match;
	}

	@Override
	public TableState state() {
		return state;
	}

	@Override
	public float stateTime() {
		return stateTime;
	}

	@Override
	public float trumpTime() {
		return trumpTime;
	}

	@Override
	public boolean isDealing() {
		return state.isDealing();
	}

	@Override
	public boolean trumpFaceUp() {
		return !isDealing() && trumpTime >= TRUMP_FLIP_TIME;
	}

	@Override
	public FlowView.Deal deal() {
		return deal;
	}

	@Override
	public boolean yourTurnToBid() {
		return state == TableState.TURN && match.phase() == Match.Phase.BIDDING && match.currentSeat() == Seating.YOU;
	}

	@Override
	public boolean yourTurnToPlay() {
		return state == TableState.TURN && match.phase() == Match.Phase.PLAYING && match.currentSeat() == Seating.YOU;
	}

	boolean isTurnOf(int seat) {
		return state == TableState.TURN && match.currentSeat() == seat;
	}

	void update(float dt) {
		stateTime += dt;
		if (!isDealing()) {
			trumpTime += dt;
		}
		state.update(this, dt);
	}

	boolean skip() {
		return state.skip(this);
	}

	boolean bid(int b) {
		if (!yourTurnToBid() || !match.isLegalBid(b)) {
			return false;
		}
		make(new Move.Bid(Seating.YOU, b));
		return true;
	}

	boolean play(Card c) {
		if (!yourTurnToPlay() || !match.legalPlays(Seating.YOU).contains(c)) {
			return false;
		}
		make(new Move.Play(Seating.YOU, c));
		return true;
	}

	boolean nextHand() {
		if (state != TableState.RESULTS) {
			return false;
		}
		match.nextHand();
		tell(l -> l.changed(match));
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER);
		} else {
			startHand();
		}
		return true;
	}

	private void startHand() {
		tell(l -> l.beforeDeal(match));
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER);
			return;
		}
		trumpTime = 0;
		deal.start(match.handSize(), match.dealer(), match.handIndex() == 0);
		enter(TableState.SHUFFLE);
	}

	private void stopThinking() {
		if (thinking != null) {
			thinking.cancel(false);
			thinking = null;
		}
	}

	void enter(TableState s) {
		state = s;
		stateTime = 0;
		stopThinking();
		s.entered(this);
	}

	void tellHandEnded() {
		tell(l -> l.handEnded(match));
	}

	void tellGameEnded() {
		tell(l -> l.gameEnded(match));
	}

	void dealing(float dt) {
		if (deal.update(dt)) {
			tell(Listener::shuffling);
		}
		if (deal.isDone()) {
			enter(TableState.TURN);
		} else if (state == TableState.SHUFFLE && !deal.isShuffling()) {
			enter(TableState.DEAL);
		}
	}

	void computerTurn() {
		if (match.currentSeat() == Seating.YOU) {
			return;
		}
		if (thinking == null) {
			thinking = startThinking(match.currentSeat());
		}
		if (stateTime >= AI_DELAY && thinking.isDone()) {
			botMove(match.currentSeat());
		}
	}

	private Future<Move> startThinking(int seat) {
		Bot bot = bots[seat];
		TableView view = match.view(seat);
		return thinker.think(() -> bot.chooseMove(view));
	}

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

	private void make(Move move) {
		int winner = match.apply(move);
		tell(l -> l.changed(match));
		if (move instanceof Move.Play) {
			Card c = ((Move.Play) move).card();
			tell(l -> l.played(match, move.seat(), c));
		}
		enter(winner >= 0 ? TableState.TRICK : TableState.TURN);
	}
}
