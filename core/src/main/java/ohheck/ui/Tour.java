package ohheck.ui;

/** The screenshot run's walk through the screens. */
public interface Tour {
	boolean step(Tagged screen, float shown);
}
