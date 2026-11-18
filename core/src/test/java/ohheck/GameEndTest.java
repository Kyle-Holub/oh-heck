package ohheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameEndTest {

	@Test
	public void aWinIsCardsThenFireworksThenScores() {
		GameEnd e = new GameEnd();
		e.start(true);
		assertFalse(e.update(0.5f, false)); // cards still falling
		assertFalse(e.resultsShown(5f)); // however long that takes, no scores yet
		assertTrue(e.update(2.0f, true)); // the last card gone: fireworks, once
		assertFalse(e.update(2.1f, true));
		assertFalse(e.resultsShown(2.0f + GameEnd.FIREWORKS_FIRST - 0.01f));
		assertTrue(e.resultsShown(2.0f + GameEnd.FIREWORKS_FIRST));
	}

	@Test
	public void aLossIsCardsThenScoresWithoutFireworks() {
		GameEnd e = new GameEnd();
		e.start(false);
		assertFalse(e.update(1.8f, true));
		assertFalse(e.resultsShown(1.8f));
		assertTrue(e.resultsShown(1.8f + GameEnd.SCORES_AFTER_CARDS));
	}

	@Test
	public void nothingShowsBeforeAGameEnds() {
		GameEnd e = new GameEnd();
		assertFalse(e.resultsShown(100f));
		e.start(true);
		e.update(1f, true);
		e.reset();
		assertFalse(e.resultsShown(100f));
	}
}
