package ohheck.table;

/* Who plays your seat: you, or Autoplay. */
interface YourSeat extends TableFlow.Listener {

	YourSeat PERSON = new YourSeat() {
	};

	default void update(float dt) {
	}

	default void whilePaused(float dt) {
	}

	default boolean menuOnPause() {
		return true;
	}

	default boolean thinkAtOnce() {
		return false;
	}
}
