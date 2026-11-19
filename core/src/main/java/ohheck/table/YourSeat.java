package ohheck.table;

/**
 * Who plays your seat: you, through the screen's taps ({@link #PERSON}), or {@link Autoplay}
 * in the scripted screenshot run, through the same taps. The table asks it the few things
 * that differ, so it never checks which it has.
 */
interface YourSeat extends TableFlow.Listener {

	/** You: your taps come from the screen; nothing to do on the table's clock. */
	YourSeat PERSON = new YourSeat() {
	};

	/** One step of game time while the table is moving. */
	default void update(float dt) {
	}

	/** One step of game time while the pause menu is open. */
	default void whilePaused(float dt) {
	}

	/** Whether leaving the app opens the pause menu, for when you come back. */
	default boolean menuOnPause() {
		return true;
	}

	/** Whether the computer players must think at once, so the game repeats exactly. */
	default boolean thinkAtOnce() {
		return false;
	}
}
