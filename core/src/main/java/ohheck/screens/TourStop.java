package ohheck.screens;

/**
 * A screen the screenshot run's {@link ScreenshotTour} stops at: which stop it is, and the
 * little it can be asked to do there. A new stop is a screen that implements this (and a
 * {@link ScreenshotTour.Stop}).
 */
interface TourStop {

	ScreenshotTour.Stop tourStop();

	/** Shows the second half of the stop (on), or the first: the Statistics tab, the end of the Roster. */
	default void secondHalf(boolean on) {
	}

	/** Whether there's no step after this one (the tutorial has several; other stops, one). */
	default boolean lastStep() {
		return true;
	}

	/** On to the stop's next step. */
	default void nextStep() {
	}
}
