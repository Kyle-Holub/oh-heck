package ohheck.screens;

/* A screen the screenshot tour stops at. */
interface TourStop {

	ScreenshotTour.Stop tourStop();

	default void secondHalf(boolean on) {
	}

	default boolean lastStep() {
		return true;
	}

	default void nextStep() {
	}
}
