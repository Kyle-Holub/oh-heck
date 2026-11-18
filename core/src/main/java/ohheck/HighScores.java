package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your ten best finished games: final score, where you placed, and the date.
 * Pure Java so it can be tested without libGDX; Settings stores the text form.
 */
final class HighScores {

	static final int KEEP = 10;

	static final class Entry {
		final int score;
		/** 1 = won (ties share a place), up to 4. */
		final int place;
		/** yyyy-mm-dd */
		final String date;
		/** The computer players' difficulty (a Difficulty ordinal), or -1 for games from before there was a choice. */
		final int level;

		Entry(int score, int place, String date, int level) {
			this.score = score;
			this.place = place;
			this.date = date;
			this.level = level;
		}
	}

	private final List<Entry> entries = new ArrayList<Entry>();

	List<Entry> entries() {
		return Collections.unmodifiableList(entries);
	}

	/**
	 * Adds a finished game, keeping the best ten. Earlier games stay ahead of
	 * later ones on the same score. Returns its position (0 = best), or -1 if
	 * it didn't make the list.
	 */
	int add(Entry e) {
		int at = 0;
		while (at < entries.size() && entries.get(at).score >= e.score) {
			at++;
		}
		if (at >= KEEP) {
			return -1;
		}
		entries.add(at, e);
		while (entries.size() > KEEP) {
			entries.remove(entries.size() - 1);
		}
		return at;
	}

	/** "score,place,date,level;..." (older entries have no level) */
	String encode() {
		StringBuilder b = new StringBuilder();
		for (Entry e : entries) {
			if (b.length() > 0) {
				b.append(';');
			}
			b.append(e.score).append(',').append(e.place).append(',').append(e.date).append(',').append(e.level);
		}
		return b.toString();
	}

	/** Reads encode()'s form; anything unreadable is skipped. */
	static HighScores decode(String s) {
		HighScores h = new HighScores();
		if (s == null || s.isEmpty()) {
			return h;
		}
		for (String part : s.split(";")) {
			String[] f = part.split(",");
			if (f.length != 3 && f.length != 4) {
				continue;
			}
			try {
				int level = f.length == 4 ? Integer.parseInt(f[3].trim()) : -1;
				h.add(new Entry(Integer.parseInt(f[0].trim()), Integer.parseInt(f[1].trim()), f[2].trim(), level));
			} catch (NumberFormatException ignored) {
				// skip a damaged entry
			}
		}
		return h;
	}

	/** "1st", "2nd"... */
	static String ordinal(int n) {
		return n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
	}
}
