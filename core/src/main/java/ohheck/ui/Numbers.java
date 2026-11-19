package ohheck.ui;

/**
 * Numbers as text, made once and kept: scores, bids and places are drawn every frame, and
 * turning them into new strings each time leaves garbage that makes Android pause to clear.
 */
public final class Numbers {

	private static final int LOW = -999, HIGH = 9999;
	private static final String[] CACHE = new String[HIGH - LOW + 1];

	private Numbers() {
	}

	/** n as text ("42", "-5"); the same String object each time. */
	public static String text(int n) {
		if (n < LOW || n > HIGH) {
			return String.valueOf(n);
		}
		String s = CACHE[n - LOW];
		if (s == null) {
			s = String.valueOf(n);
			CACHE[n - LOW] = s;
		}
		return s;
	}

	/** n with its sign ("+14", "-5", "0"). */
	public static String signed(int n) {
		return n > 0 ? Plus.text(n) : text(n);
	}

	/** "+1", "+2"... made once. */
	private static final class Plus {
		private static final String[] CACHE = new String[HIGH + 1];

		static String text(int n) {
			if (n > HIGH) {
				return "+" + n;
			}
			String s = CACHE[n];
			if (s == null) {
				s = "+" + n;
				CACHE[n] = s;
			}
			return s;
		}
	}
}
