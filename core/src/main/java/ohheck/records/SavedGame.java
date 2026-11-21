package ohheck.records;

import java.util.Random;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Match;

/* A game in progress with its difficulty and seating, saved as one text. */
public record SavedGame(Match match, Difficulty level, Seating players) {

	public String encode() {
		return match.save() + "table.level=" + level.name() + "\n" + "table.players=" + players.encode() + "\n";
	}

	public static SavedGame decode(String saved, Random rng) {
		Match m = Match.load(saved, rng);
		Difficulty level = Difficulty.valueOf(value(saved, "table.level"));
		Seating table = Seating.parse(value(saved, "table.players"));
		if (table == null || !table.onRoster()) {
			throw new IllegalArgumentException("No such table: " + value(saved, "table.players"));
		}
		return new SavedGame(m, level, table);
	}

	private static String value(String saved, String key) {
		for (String line : saved.split("\n")) {
			if (line.startsWith(key + "=")) {
				return line.substring(key.length() + 1).trim();
			}
		}
		throw new IllegalArgumentException("Missing " + key);
	}
}
