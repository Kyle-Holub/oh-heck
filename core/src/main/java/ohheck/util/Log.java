package ohheck.util;

/**
 * Where code reports trouble it recovered from: the device log in the game, standard error
 * elsewhere.
 */
public final class Log {

	public interface Sink {
		void warn(String tag, String message, Throwable cause);
	}

	private static volatile Sink sink = (tag, message, cause) -> System.err
			.println(tag + ": " + message + (cause != null ? " (" + cause + ")" : ""));

	private Log() {
	}

	public static void to(Sink s) {
		sink = s;
	}

	public static void warn(String tag, String message, Throwable cause) {
		sink.warn(tag, message, cause);
	}
}
