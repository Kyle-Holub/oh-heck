package ohheck.records;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohheck.players.Difficulty;
import ohheck.rules.GameMode;
import ohheck.rules.RuleSet;
import ohheck.util.Log;

/* The ten best finished games of each game mode. */
public final class HighScores {

	static final int KEEP = 10;

	public record Entry(int score, int place, String date, Difficulty level, RuleSet rules) {
		public GameMode mode() {
			return GameMode.of(rules);
		}
	}

	private final List<Entry> entries = new ArrayList<Entry>();

	public List<Entry> entries() {
		return Collections.unmodifiableList(entries);
	}

	public List<Entry> of(GameMode mode) {
		List<Entry> mine = new ArrayList<Entry>();
		for (Entry e : entries) {
			if (e.mode() == mode) {
				mine.add(e);
			}
		}
		return mine;
	}

	int add(Entry e) {
		int at = 0;
		while (at < entries.size() && entries.get(at).score() >= e.score()) {
			at++;
		}
		entries.add(at, e);
		List<Entry> same = of(e.mode());
		if (same.size() > KEEP) {
			entries.remove(same.get(same.size() - 1));
		}
		return entries.indexOf(e);
	}

	String encode() {
		StringBuilder b = new StringBuilder();
		for (Entry e : entries) {
			if (b.length() > 0) {
				b.append(';');
			}
			b.append("score:").append(e.score()).append(",place:").append(e.place()).append(",date:").append(e.date())
					.append(",level:").append(e.level().name()).append(",rules:").append(e.rules().code());
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
				Entry e = named(part);
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
		if (!f.containsKey("score") || !f.containsKey("place") || !f.containsKey("date") || !f.containsKey("level")
				|| !f.containsKey("rules")) {
			return null;
		}
		return new Entry(Integer.parseInt(f.get("score")), Integer.parseInt(f.get("place")), f.get("date"),
				Difficulty.valueOf(f.get("level")), RuleSet.parse(f.get("rules")));
	}
}
