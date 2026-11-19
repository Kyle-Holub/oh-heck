package ohheck.ui;

/** The screenshot run's walk through the menus: called once a step for the screen showing. */
public interface Tour {
	/**
	 * Moves the tour along, for a screen shown this long (at game speed); true if it has gone
	 * on to another screen (so this one stops).
	 */
	boolean step(Tagged screen, float shown);
}
