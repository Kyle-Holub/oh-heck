package ohheck.util;

/**
 * Where the game reports trouble it recovered from: a damaged save thrown away, a high
 * score that wouldn't read, a sound that wouldn't load, effects the device can't draw.
 * The game sends it to the device log; anywhere else (tests, the arena) it goes to
 * standard error.
 */
public final class Log {

	/** Takes a report: a short tag (the class), what happened, and the cause if there was one. */
	public interface Sink {
		void warn(String tag, String message, Throwable cause);
	}

	private static volatile Sink sink = (tag, message, cause) -> System.err
			.println(tag + ": " + message + (cause != null ? " (" + cause + ")" : ""));

	private Log() {
	}

	/** Sends reports here from now on. */
	public static void to(Sink s) {
		sink = s;
	}

	public static void warn(String tag, String message, Throwable cause) {
		sink.warn(tag, message, cause);
	}
}
