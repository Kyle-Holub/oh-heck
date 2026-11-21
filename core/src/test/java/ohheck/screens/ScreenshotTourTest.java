package ohheck.screens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.Test;

import ohheck.screens.ScreenshotTour.Move;
import ohheck.screens.ScreenshotTour.Stop;
import ohheck.ui.Navigator;
import ohheck.ui.Tagged;

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
		assertEquals(Move.SECOND_HALF, ScreenshotTour.plan(Stop.HIGH_SCORES, 2f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.HIGH_SCORES, 3.1f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.OPTIONS, 1.6f, false));
		assertEquals(Move.SECOND_HALF, ScreenshotTour.plan(Stop.ROSTER, 1.5f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.ROSTER, 2.3f, false));
	}

	@Test
	public void theTutorialStepsThroughThenStartsAGame() {
		assertEquals(Move.STAY, ScreenshotTour.plan(Stop.TUTORIAL, 3.9f, false));
		assertEquals(Move.NEXT_STEP, ScreenshotTour.plan(Stop.TUTORIAL, 4.1f, false));
		assertEquals(Move.LEAVE, ScreenshotTour.plan(Stop.TUTORIAL, 4.1f, true));
	}

	private static final class FakeStop implements TourStop, Tagged {
		final Stop stop;
		final StringBuilder did = new StringBuilder();
		boolean last;

		FakeStop(Stop stop) {
			this.stop = stop;
		}

		@Override
		public Stop tourStop() {
			return stop;
		}

		@Override
		public void secondHalf(boolean on) {
			if (on) {
				did.append("second ");
			}
		}

		@Override
		public boolean lastStep() {
			return last;
		}

		@Override
		public void nextStep() {
			did.append("next ");
		}

		@Override
		public String stateTag() {
			return stop.name();
		}
	}

	private static Navigator recorder(StringBuilder went) {
		return (Navigator) Proxy.newProxyInstance(Navigator.class.getClassLoader(), new Class<?>[] { Navigator.class },
				(proxy, method, args) -> {
					went.append(method.getName()).append(' ');
					return method.getReturnType() == boolean.class ? false : null;
				});
	}

	@Test
	public void eachStopIsAskedWhatItsScreenDoes() {
		StringBuilder went = new StringBuilder();
		ScreenshotTour tour = new ScreenshotTour(recorder(went));
		FakeStop scores = new FakeStop(Stop.HIGH_SCORES);
		assertFalse(tour.step(scores, 1f));
		assertFalse(tour.step(scores, 2f));
		assertTrue(tour.step(scores, 3.1f));
		assertEquals("second second ", scores.did.toString());
		assertEquals("showOptions ", went.toString());

		FakeStop tutorial = new FakeStop(Stop.TUTORIAL);
		assertFalse(tour.step(tutorial, 4.1f));
		tutorial.last = true;
		assertTrue(tour.step(tutorial, 4.1f));
		assertEquals("next second ", tutorial.did.toString());
		assertEquals("showOptions newGame ", went.toString());
	}

	@Test
	public void aScreenThatIsntAStopIsLeftAlone() {
		StringBuilder went = new StringBuilder();
		assertFalse(new ScreenshotTour(recorder(went)).step(() -> "play:1:0", 99f));
		assertEquals("", went.toString());
	}
}
