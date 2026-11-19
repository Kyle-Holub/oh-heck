package ohheck.ui;

/* Numbers as cached strings, so drawing them makes no garbage. */
public final class Numbers {

	private static final int LOW = -999, HIGH = 9999;
	private static final String[] CACHE = new String[HIGH - LOW + 1];

	private Numbers() {
	}

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

	public static String signed(int n) {
		return n > 0 ? Plus.text(n) : text(n);
	}

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
