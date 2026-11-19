package ohheck.records;

import java.util.Random;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Match;
import ohheck.util.Log;

/**
 * A game in progress, saved as one piece of text: the {@link Match} itself, plus the
 * difficulty it started at and who sits at the table, under their own "table." keys (which
 * the match's loader passes over). Older saves kept the last two apart, so loading takes
 * them as a fallback.
 *
 * @param players who sits at the table, or null when unknown (draw a new table)
 */
public record SavedGame(Match match, Difficulty level, Seating players) {

	public String encode() {
		return match.save() + "table.level=" + level.name() + "\n" + "table.players=" + players.encode() + "\n";
	}

	/**
	 * Reads a saved game, or null if it can't be read. legacyLevel and legacyPlayers stand in
	 * for saves made before the table travelled with the game (legacyPlayers may be null).
	 */
	public static SavedGame decode(String saved, Random rng, Difficulty legacyLevel, Seating legacyPlayers) {
		if (saved == null || saved.isEmpty()) {
			return null;
		}
		Match m = Match.load(saved, rng);
		if (m == null) {
			return null;
		}
		String level = value(saved, "table.level"), players = value(saved, "table.players");
		Difficulty d = legacyLevel;
		if (level != null) {
			try {
				d = Difficulty.valueOf(level);
			} catch (IllegalArgumentException e) {
				Log.warn("SavedGame", "Unknown level " + level + "; using " + d, null);
			}
		}
		Seating table = players != null ? Seating.parse(players) : legacyPlayers;
		if (players != null && table == null) {
			Log.warn("SavedGame", "Unreadable table " + players + "; seating a new one", null);
		}
		return new SavedGame(m, d, table != null && table.onRoster() ? table : null);
	}


	private static String value(String saved, String key) {
		for (String line : saved.split("\n")) {
			if (line.startsWith(key + "=")) {
				return line.substring(key.length() + 1).trim();
			}
		}
		return null;
	}
}
