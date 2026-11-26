package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameEndTest {

	@Test
	public void aWinIsCardsThenThePodiumWithFireworksThenATapForScores() {
		GameEnd e = new GameEnd();
		e.start(true, 4, true, true);
		assertFalse(e.update(0.5f, false));
		assertEquals(-1, e.podiumTime(0.5f), 0);
		assertFalse(e.update(2.0f, true));
		float winnerLands = 2.0f + GameEnd.risesAt(1, 4) + GameEnd.RISE;
		assertFalse(e.update(winnerLands - 0.01f, true));
		assertTrue(e.update(winnerLands, true));
		assertFalse("fireworks only once", e.update(winnerLands + 0.1f, true));
		assertFalse("the scores wait for a tap", e.resultsShown(winnerLands + 60));
		e.tap(winnerLands + 60);
		assertTrue(e.resultsShown(winnerLands + 60));
	}

	@Test
	public void thePodiumRisesFromLastPlaceWithAPauseBeforeTheWinner() {
		assertTrue(GameEnd.risesAt(4, 4) < GameEnd.risesAt(3, 4));
		assertTrue(GameEnd.risesAt(3, 4) < GameEnd.risesAt(2, 4));
		assertTrue(GameEnd.risesAt(1, 4) - GameEnd.risesAt(2, 4) > GameEnd.STEP);
	}

	@Test
	public void aTapDuringThePodiumFinishesItAndASecondShowsTheScores() {
		GameEnd e = new GameEnd();
		e.start(false, 4, true, true);
		e.update(1f, true);
		e.tap(1.5f);
		assertTrue(e.podiumDone(1.5f));
		assertFalse(e.resultsShown(1.5f));
		e.tap(1.6f);
		assertTrue(e.resultsShown(1.6f));
	}

	@Test
	public void aGamePlayingItselfGoesOnWithoutATap() {
		GameEnd e = new GameEnd();
		e.start(false, 4, true, false);
		e.update(1f, true);
		assertTrue(e.resultsShown(1f + GameEnd.risesAt(1, 4) + GameEnd.RISE + GameEnd.HOLD));
	}

	@Test
	public void aDuelHasNoPodium() {
		GameEnd e = new GameEnd();
		e.start(true, 2, false, true);
		assertTrue(e.update(1f, true));
		assertEquals(-1, e.podiumTime(1.5f), 0);
		assertFalse(e.resultsShown(1f + GameEnd.FIREWORKS_FIRST - 0.01f));
		assertTrue(e.resultsShown(1f + GameEnd.FIREWORKS_FIRST));
	}

	@Test
	public void nothingShowsBeforeAGameEnds() {
		GameEnd e = new GameEnd();
		assertFalse(e.resultsShown(100f));
		e.start(true, 4, true, true);
		e.update(1f, true);
		e.reset();
		assertFalse(e.resultsShown(100f));
	}
}
