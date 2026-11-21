package ohheck.records;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohheck.players.Difficulty;
import ohheck.util.Log;

/* The ten best finished games. */
public final class HighScores {

	static final int KEEP = 10;

	public record Entry(int score, int place, String date, Difficulty level) {
	}

	private final List<Entry> entries = new ArrayList<Entry>();

	public List<Entry> entries() {
		return Collections.unmodifiableList(entries);
	}

	int add(Entry e) {
		int at = 0;
		while (at < entries.size() && entries.get(at).score() >= e.score()) {
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

	String encode() {
		StringBuilder b = new StringBuilder();
		for (Entry e : entries) {
			if (b.length() > 0) {
				b.append(';');
			}
			String level = e.level() != null ? e.level().name() : "-";
			b.append("score:").append(e.score()).append(",place:").append(e.place()).append(",date:").append(e.date())
					.append(",level:").append(level);
		}
		return b.toString();
	}

	static HighScores decode(String s) {
		HighScores h = new HighScores();
		if (s == null || s.isEmpty()) {
			return h;
		}
		for (String part : s.split(";")) {
			try {
				Entry e = part.contains(":") ? named(part) : numbered(part);
				if (e != null) {
					h.add(e);
				}
			} catch (RuntimeException e) {
				Log.warn("HighScores", "Skipped a damaged entry: " + part, e);
			}
		}
		return h;
	}

	private static Entry named(String part) {
		Map<String, String> f = new HashMap<String, String>();
		for (String kv : part.split(",")) {
			int c = kv.indexOf(':');
			if (c > 0) {
				f.put(kv.substring(0, c).trim(), kv.substring(c + 1).trim());
			}
		}
		if (!f.containsKey("score") || !f.containsKey("place") || !f.containsKey("date")) {
			return null;
		}
		String l = f.get("level");
		Difficulty level = l == null || l.equals("-") ? null : Difficulty.valueOf(l);
		return new Entry(Integer.parseInt(f.get("score")), Integer.parseInt(f.get("place")), f.get("date"), level);
	}

	private static Entry numbered(String part) {
		String[] f = part.split(",");
		if (f.length != 3 && f.length != 4) {
			return null;
		}
		Difficulty level = f.length == 4 ? byOrdinal(Integer.parseInt(f[3].trim())) : null;
		return new Entry(Integer.parseInt(f[0].trim()), Integer.parseInt(f[1].trim()), f[2].trim(), level);
	}

	private static Difficulty byOrdinal(int ordinal) {
		Difficulty[] all = Difficulty.values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
	}
}
