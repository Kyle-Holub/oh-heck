package ohheck.records;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohheck.players.Difficulty;
import ohheck.util.Log;

/**
 * Your ten best finished games: final score, where you placed, and the date.
 * Pure Java so it can be tested without libGDX; Settings stores the text form.
 */
public final class HighScores {

	static final int KEEP = 10;

	/**
	 * One finished game: its score, place (1 = won; ties share a place, up to 4), date
	 * (yyyy-mm-dd) and the difficulty setting (a Difficulty ordinal, or -1 for games from
	 * before there was a choice).
	 */
	public record Entry(int score, int place, String date, int level) {
	}

	private final List<Entry> entries = new ArrayList<Entry>();

	public List<Entry> entries() {
		return Collections.unmodifiableList(entries);
	}

	/**
	 * Adds a finished game, keeping the best ten. Earlier games stay ahead of
	 * later ones on the same score. Returns its position (0 = best), or -1 if
	 * it didn't make the list.
	 */
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

	/** "score:72,place:1,date:2026-11-16,level:NORMAL;..." (level "-" for games from before there was a choice) */
	String encode() {
		StringBuilder b = new StringBuilder();
		for (Entry e : entries) {
			if (b.length() > 0) {
				b.append(';');
			}
			String level = e.level() >= 0 && e.level() < Difficulty.values().length ? Difficulty.values()[e.level()].name() : "-";
			b.append("score:").append(e.score()).append(",place:").append(e.place()).append(",date:").append(e.date())
					.append(",level:").append(level);
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
			try {
				Entry e = part.contains(":") ? named(part) : numbered(part);
				if (e != null) {
					h.add(e);
				}
			} catch (RuntimeException e) {
				Log.warn("HighScores", "Skipped a damaged entry: " + part, e); // (the rest still load)
			}
		}
		return h;
	}

	/** "score:72,place:1,date:...,level:NORMAL" (unknown fields are passed over). */
	private static Entry named(String part) {
		java.util.Map<String, String> f = new java.util.HashMap<String, String>();
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
		int level = l == null || l.equals("-") ? -1 : Difficulty.valueOf(l).ordinal();
		return new Entry(Integer.parseInt(f.get("score")), Integer.parseInt(f.get("place")), f.get("date"), level);
	}

	/** The first builds' form: "score,place,date[,level ordinal]". */
	private static Entry numbered(String part) {
		String[] f = part.split(",");
		if (f.length != 3 && f.length != 4) {
			return null;
		}
		int level = f.length == 4 ? Integer.parseInt(f[3].trim()) : -1;
		return new Entry(Integer.parseInt(f[0].trim()), Integer.parseInt(f[1].trim()), f[2].trim(), level);
	}
}
