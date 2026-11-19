package ohheck.ui;

/**
 * What a screen needs from the game around it: the shared tools ({@link Services}) and the
 * way to the other screens ({@link Navigator}). OhHeckGame is the host; screens depend on
 * this, not on it, and a helper that needs only one half takes only that.
 */
public interface GameHost extends Services, Navigator {

	/** The screenshot run's walk through the menus: called once a step for the screen showing. */
	interface Tour {
		/**
		 * Moves the tour along, for a screen shown this long (at game speed); true if it has
		 * gone on to another screen (so this one stops).
		 */
		boolean step(Tagged screen, float shown);
	}

	/** A screen that can describe itself for automated screenshots. */
	interface Tagged {
		String stateTag();
	}
}
