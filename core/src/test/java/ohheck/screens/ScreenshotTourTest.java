package ohheck.screens;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ohheck.screens.ScreenshotTour.Move;
import ohheck.screens.ScreenshotTour.Stop;

/** The screenshot run's route and timing (what CI's screenshots rely on). */
public class ScreenshotTourTest {

	@Test
	public void theRouteRunsFromTheTitleIntoAGame() {
		Stop s = Stop.TITLE;
		StringBuilder route = new StringBuilder(s.name());
		while (s != Stop.GAME) {
			s = ScreenshotTour.next(s);
			route.append(" > ").append(s.name());
		}
		assertEquals("TITLE > HIGH_SCORES > OPTIONS > ROSTER > TUTORIAL > GAME", route.toString());
	}

	@Test
	public void eachStopWaitsThenShowsItsSecondHalfThenLeaves() {
		assertEquals(Move.STAY, ScreenshotTour.plan(Stop.TITLE, 3.4f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.TITLE, 3.6f, false));
		assertEquals(Move.STAY, ScreenshotTour.plan(Stop.HIGH_SCORES, 1f, false));
		assertEquals(Move.SECOND_HALF, ScreenshotTour.plan(Stop.HIGH_SCORES, 2f, false)); // the Statistics tab
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.HIGH_SCORES, 3.1f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.OPTIONS, 1.6f, false));
		assertEquals(Move.SECOND_HALF, ScreenshotTour.plan(Stop.ROSTER, 1.5f, false)); // the end of the list
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.ROSTER, 2.3f, false));
	}

	@Test
	public void theTutorialStepsThroughThenStartsAGame() {
		assertEquals(Move.STAY, ScreenshotTour.plan(Stop.TUTORIAL, 3.9f, false));
		assertEquals(Move.NEXT_STEP, ScreenshotTour.plan(Stop.TUTORIAL, 4.1f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.TUTORIAL, 4.1f, true));
	}
}
