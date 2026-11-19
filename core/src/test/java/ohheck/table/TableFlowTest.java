package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;

public class TableFlowTest {

	private static final float STEP = 1 / 30f;

	static final class Events implements TableFlow.Listener {
		int deals, saves, shuffleSounds, handsEnded, gamesEnded;
		final List<Integer> playedBy = new ArrayList<Integer>();

		@Override
		public void beforeDeal(Match m) {
			deals++;
		}

		@Override
		public void changed(Match m) {
			saves++;
		}

		@Override
		public void shuffling() {
			shuffleSounds++;
		}

		@Override
		public void played(Match m, int seat, Card c) {
			playedBy.add(seat);
		}

		@Override
		public void handEnded(Match m) {
			handsEnded++;
		}

		@Override
		public void gameEnded(Match m) {
			gamesEnded++;
		}
	}

	private static final Map<TableState, Set<TableState>> NEXT = new EnumMap<TableState, Set<TableState>>(TableState.class);
	static {
		NEXT.put(TableState.SHUFFLE, EnumSet.of(TableState.DEAL, TableState.TURN));
		NEXT.put(TableState.DEAL, EnumSet.of(TableState.TURN));
		NEXT.put(TableState.TURN, EnumSet.of(TableState.TURN, TableState.TRICK));
		NEXT.put(TableState.TRICK, EnumSet.of(TableState.TURN, TableState.HAND_END));
		NEXT.put(TableState.HAND_END, EnumSet.of(TableState.RESULTS));
		NEXT.put(TableState.RESULTS, EnumSet.of(TableState.SHUFFLE, TableState.GAME_OVER));
		NEXT.put(TableState.GAME_OVER, EnumSet.noneOf(TableState.class));
	}

	private final Events events = new Events();
	private final TableFlow flow = seated(events, Seating.of(4, 5, 6), 1);

	private static TableFlow seated(TableFlow.Listener listener, Seating players, long seed) {
		TableFlow f = new TableFlow(listener);
		f.seat(players, new Random(seed));
		return f;
	}

	private Match newGame(long seed) {
		Match m = Match.random(4, new Random(seed));
		flow.start(m);
		return m;
	}

	private TableState waitForChange() {
		TableState from = flow.state();
		for (int i = 0; i < 60 / STEP; i++) {
			flow.update(STEP);
			if (flow.state() != from) {
				assertTrue(from + " -> " + flow.state(), NEXT.get(from).contains(flow.state()));
				return flow.state();
			}
		}
		fail("stuck in " + from);
		return null;
	}

	@Test
	public void aNewGameShufflesTwiceThenDeals() {
		newGame(1);
		assertEquals(TableState.SHUFFLE, flow.state());
		assertEquals(1, events.deals);
		assertEquals(TableState.DEAL, waitForChange());
		assertEquals(2, events.shuffleSounds);
		assertFalse(flow.trumpFaceUp());
		assertEquals(TableState.TURN, waitForChange());
	}

	@Test
	public void aTapSkipsTheShuffleAndDeal() {
		newGame(2);
		flow.update(0.2f);
		assertTrue(flow.skip());
		assertEquals(TableState.TURN, flow.state());
		assertFalse(flow.trumpFaceUp());
		flow.update(TableFlow.TRUMP_FLIP_TIME + 0.01f);
		assertTrue(flow.trumpFaceUp());
		assertFalse("nothing to skip now", flow.skip());
	}

	@Test
	public void computerPlayersTakeTheirTime() {
		Match m = newGame(3);
		flow.skip();
		while (m.currentSeat() == Seating.YOU) {
			newGame(m.dealer() + 10);
			m = flow.match();
			flow.skip();
		}
		int seat = m.currentSeat();
		flow.update(TableFlow.AI_DELAY - 0.05f);
		assertEquals("still thinking", seat, m.currentSeat());
		assertEquals(TableState.TURN, flow.state());
		assertEquals(seat, flow.match().currentSeat());
		flow.update(0.1f);
		assertTrue("has bid", m.hasBid(seat));
	}

	@Test
	public void yourMovesAreCheckedFirst() {
		Match m = newGame(4);
		flow.skip();
		assertFalse("not your turn to play", flow.play(m.hand(Seating.YOU).get(0)));
		while (!flow.yourTurnToBid()) {
			flow.update(STEP);
		}
		int forbidden = m.forbiddenBid();
		if (forbidden >= 0) {
			assertFalse(flow.bid(forbidden));
		}
		assertFalse(flow.bid(m.handSize() + 1));
		int saves = events.saves;
		assertTrue(flow.bid(forbidden == 0 ? 1 : 0));
		assertEquals(saves + 1, events.saves);
		assertFalse("only once", flow.bid(0));
	}

	@Test
	public void aCompletedTrickStaysOnShowBeforePlayGoesOn() {
		Match m = newGame(5);
		playUntil(TableState.TRICK);
		int hand = m.handIndex();
		float shown = 0;
		while (flow.state() == TableState.TRICK) {
			flow.update(STEP);
			shown += STEP;
		}
		boolean youWon = m.lastTrickWinner() == Seating.YOU;
		assertEquals(TrickView.showLength(youWon), shown, 2 * STEP);
		assertEquals(hand, m.handIndex());
		assertTrue(flow.state() == TableState.TURN || flow.state() == TableState.HAND_END);
	}

	@Test
	public void aWholeGame() {
		Match m = newGame(6);
		TableState s = flow.state();
		int results = 0;
		while (s != TableState.GAME_OVER) {
			if (flow.yourTurnToBid()) {
				int b = 0;
				while (!m.isLegalBid(b)) {
					b++;
				}
				assertTrue(flow.bid(b));
			} else if (flow.yourTurnToPlay()) {
				assertTrue(flow.play(m.legalPlays(Seating.YOU).get(0)));
			} else if (s == TableState.RESULTS) {
				results++;
				assertTrue(flow.nextHand());
			} else {
				flow.update(STEP);
			}
			TableState now = flow.state();
			assertTrue(s + " -> " + now, now == s || NEXT.get(s).contains(now));
			s = now;
		}
		assertEquals(8, results);
		assertEquals(8, events.handsEnded);
		assertEquals(8, events.deals);
		assertEquals(1, events.gamesEnded);
		assertEquals(Match.Phase.GAME_OVER, m.phase());
		assertEquals(4 * 36, events.playedBy.size());
		assertFalse(flow.nextHand());
	}

	@Test
	public void theHandEndMessageCanBeSkippedOrTimesOut() {
		newGame(7);
		playUntil(TableState.HAND_END);
		assertTrue(flow.skip());
		assertEquals(TableState.RESULTS, flow.state());

		newGame(8);
		playUntil(TableState.HAND_END);
		float shown = 0;
		while (flow.state() == TableState.HAND_END) {
			flow.update(STEP);
			shown += STEP;
		}
		assertEquals(TableFlow.HAND_END_SHOW, shown, 2 * STEP);
		assertEquals(TableState.RESULTS, flow.state());
	}

	@Test
	public void aSavedGameResumesWithoutTheDeal() {
		Match m = newGame(9);
		playUntil(TableState.RESULTS);
		flow.nextHand();
		playUntil(TableState.TRICK);
		assertEquals(Match.Phase.PLAYING, m.phase());
		Match saved = Match.load(m.save(), new Random(1));
		TableFlow resumed = seated(new Events(), Seating.of(8, 9, 10), 2);
		resumed.resume(saved);
		assertEquals(TableState.TURN, resumed.state());
		assertTrue(resumed.trumpFaceUp());

		playUntil(TableState.HAND_END);
		Match over = Match.load(m.save(), new Random(1));
		resumed.resume(over);
		assertEquals(TableState.HAND_END, resumed.state());
	}

	private void playUntil(TableState target) {
		Match m = flow.match();
		for (int i = 0; i < 100000 && flow.state() != target; i++) {
			if (flow.yourTurnToBid()) {
				int b = 0;
				while (!m.isLegalBid(b)) {
					b++;
				}
				flow.bid(b);
			} else if (flow.yourTurnToPlay()) {
				flow.play(m.legalPlays(Seating.YOU).get(0));
			} else if (flow.state() == TableState.RESULTS) {
				flow.nextHand();
			} else {
				flow.update(STEP);
			}
		}
		assertEquals(target, flow.state());
	}

	static final class SlowThinker implements Thinker {
		final List<java.util.concurrent.FutureTask<?>> waiting = new ArrayList<java.util.concurrent.FutureTask<?>>();

		@Override
		public <T> java.util.concurrent.Future<T> think(java.util.concurrent.Callable<T> decision) {
			java.util.concurrent.FutureTask<T> f = new java.util.concurrent.FutureTask<T>(decision);
			waiting.add(f);
			return f;
		}

		void finish() {
			for (java.util.concurrent.FutureTask<?> f : waiting) {
				f.run();
			}
			waiting.clear();
		}
	}

	@Test
	public void theTableWaitsForAComputerPlayerStillThinking() {
		SlowThinker slow = new SlowThinker();
		flow.thinkWith(slow);
		Match m = newGame(3);
		flow.skip();
		while (m.currentSeat() == Seating.YOU) {
			newGame(m.dealer() + 10);
			m = flow.match();
			flow.skip();
		}
		int seat = m.currentSeat();
		flow.update(TableFlow.AI_DELAY + 1f);
		assertFalse("no answer yet: still its turn", m.hasBid(seat));
		assertEquals(1, slow.waiting.size());
		slow.finish();
		flow.update(STEP);
		assertTrue("played as soon as it was ready", m.hasBid(seat));
	}
}
