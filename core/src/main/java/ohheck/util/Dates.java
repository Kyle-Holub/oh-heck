package ohheck.util;

import java.util.Calendar;
import java.util.Locale;

/** Dates as text. Uses Calendar because Android 5 has no java.time. */
public final class Dates {

	private Dates() {
	}

	public static String today() {
		Calendar c = Calendar.getInstance();
		return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
				c.get(Calendar.DAY_OF_MONTH));
	}
}
