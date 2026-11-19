package ohheck.ui;

/**
 * What a screen needs from the game around it: the shared tools ({@link Services}) and the
 * way to the other screens ({@link Navigator}). OhHeckGame is the host; screens depend on
 * this, not on it, and a helper that needs only one half takes only that.
 */
public interface GameHost extends Services, Navigator {
}
