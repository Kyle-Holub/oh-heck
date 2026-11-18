package rules;

import java.util.Random;

/**
 * A game in progress, saved as one piece of text: the {@link Match} itself, plus the
 * difficulty it started at and who sits at the table, under their own "table." keys (which
 * the match's loader passes over). Older saves kept the last two apart, so loading takes
 * them as a fallback.
 *
 * @param players who sits in seats 1-3 ({@link Player} ids), or null when unknown (draw a new table)
 */
public record SavedGame(Match match, Difficulty level, int[] players) {

	public String encode() {
		return match.save() + "table.level=" + level.name() + "\n" + "table.players=" + players[0] + "," + players[1]
				+ "," + players[2] + "\n";
	}

	/**
	 * Reads a saved game, or null if it can't be read. legacyLevel and legacyPlayers stand in
	 * for saves made before the table travelled with the game (legacyPlayers may be null).
	 */
	public static SavedGame decode(String saved, Random rng, Difficulty legacyLevel, int[] legacyPlayers) {
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
		int[] ids = players != null ? parsePlayers(players) : legacyPlayers;
		return new SavedGame(m, d, valid(ids) ? ids : null);
	}

	/** Three players who are still on the roster (a removed player's id means a new table). */
	static boolean valid(int[] ids) {
		if (ids == null || ids.length != 3) {
			return false;
		}
		for (int id : ids) {
			if (Player.byId(id) == null) {
				return false;
			}
		}
		return true;
	}

	/** "4,10,1" as ids, or null if it isn't three numbers. */
	public static int[] parsePlayers(String s) {
		String[] f = s.split(",");
		if (f.length != 3) {
			return null;
		}
		try {
			int[] ids = new int[3];
			for (int i = 0; i < 3; i++) {
				ids[i] = Integer.parseInt(f[i].trim());
			}
			return ids;
		} catch (NumberFormatException e) {
			Log.warn("SavedGame", "Unreadable table " + s + "; seating a new one", e);
			return null;
		}
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
