package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameEndTest {

	@Test
	public void aWinIsCardsThenThePodiumWithFireworksThenScores() {
		GameEnd e = new GameEnd();
		e.start(true, 4, true);
		assertFalse(e.update(0.5f, false));
		assertEquals(-1, e.podiumTime(0.5f), 0);
		assertFalse(e.update(2.0f, true));
		assertEquals(0.5f, e.podiumTime(2.5f), 0.001f);
		float winnerLands = 2.0f + GameEnd.risesAt(1, 4) + GameEnd.STEP;
		assertFalse(e.update(winnerLands - 0.01f, true));
		assertTrue(e.update(winnerLands, true));
		assertFalse("fireworks only once", e.update(winnerLands + 0.1f, true));
		float scores = 2.0f + GameEnd.risesAt(1, 4) + GameEnd.HOLD;
		assertFalse(e.resultsShown(scores - 0.01f));
		assertTrue(e.resultsShown(scores));
	}

	@Test
	public void thePodiumRisesFromLastPlaceWithAPauseBeforeTheWinner() {
		assertTrue(GameEnd.risesAt(4, 4) < GameEnd.risesAt(3, 4));
		assertTrue(GameEnd.risesAt(3, 4) < GameEnd.risesAt(2, 4));
		assertTrue(GameEnd.risesAt(1, 4) - GameEnd.risesAt(2, 4) > GameEnd.STEP);
		assertEquals(0, GameEnd.risesAt(2, 2), 0);
	}

	@Test
	public void aLossHasNoFireworks() {
		GameEnd e = new GameEnd();
		e.start(false, 4, true);
		e.update(1.8f, true);
		assertFalse(e.update(10f, true));
		assertTrue(e.resultsShown(10f));
	}

	@Test
	public void aDuelHasNoPodium() {
		GameEnd e = new GameEnd();
		e.start(true, 2, false);
		assertTrue(e.update(1f, true));
		assertEquals(-1, e.podiumTime(1.5f), 0);
		assertFalse(e.resultsShown(1f + GameEnd.FIREWORKS_FIRST - 0.01f));
		assertTrue(e.resultsShown(1f + GameEnd.FIREWORKS_FIRST));
	}

	@Test
	public void aTapSkipsToTheScores() {
		GameEnd e = new GameEnd();
		e.start(false, 4, true);
		e.update(1f, false);
		e.skip(1.2f);
		assertTrue(e.resultsShown(1.2f));
	}

	@Test
	public void nothingShowsBeforeAGameEnds() {
		GameEnd e = new GameEnd();
		assertFalse(e.resultsShown(100f));
		e.start(true, 4, true);
		e.update(1f, true);
		e.reset();
		assertFalse(e.resultsShown(100f));
	}
}
