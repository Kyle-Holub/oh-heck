package rules;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Your statistics over every game you've played: kept for each difficulty and for all of
 * them together. A game counts at the easiest level it was played at (as its high score
 * does); a hand at the level being played when it ended. Hands count even in a game you
 * never finish; the game itself only counts once it ends.
 */
public final class Stats {

	/** Totals for one difficulty, or for all of them. */
	public static final class Totals {
		public int games, wins, streak, bestStreak, perfect;
		long finalScores;
		public int bestFinal = Integer.MIN_VALUE, worstFinal = Integer.MAX_VALUE;
		public int hands, made, zeroBids, zeroMade, over, under, tricks;
		public int bestHand = Integer.MIN_VALUE, worstHand = Integer.MAX_VALUE;
		/** Games each computer player (by {@link Player} id) finished above you. */
		public final int[] beatenBy = new int[Player.idLimit()];

		public boolean anyGames() {
			return games > 0;
		}

		public boolean anyHands() {
			return hands > 0;
		}

		/** 0-1, or 0 before any game. */
		public double winRate() {
			return games == 0 ? 0 : (double) wins / games;
		}

		public double averageFinal() {
			return games == 0 ? 0 : (double) finalScores / games;
		}

		public double madeRate() {
			return hands == 0 ? 0 : (double) made / hands;
		}

		public double tricksPerHand() {
			return hands == 0 ? 0 : (double) tricks / hands;
		}

		/** The player (a {@link Player} id) who has finished above you most often, or -1 if none has yet. */
		public int nemesis() {
			int best = -1;
			for (int s = 0; s < beatenBy.length; s++) {
				if (beatenBy[s] > 0 && (best < 0 || beatenBy[s] > beatenBy[best])) {
					best = s;
				}
			}
			return best;
		}
	}

	/** The index of the totals over every difficulty (the others are by {@link Difficulty#ordinal()}). */
	public static final int ALL = Difficulty.values().length;

	private final Totals[] totals = new Totals[ALL + 1];

	// the game in progress: hands already counted (so a resumed hand isn't counted twice), and so on
	private int handsCounted;
	private boolean gameCounted, perfectSoFar = true;

	public Stats() {
		for (int i = 0; i < totals.length; i++) {
			totals[i] = new Totals();
		}
	}

	/** For one difficulty, or {@link #ALL}. */
	public Totals totals(int which) {
		return totals[which];
	}

	/** A new game has begun. */
	public void startGame() {
		handsCounted = 0;
		gameCounted = false;
		perfectSoFar = true;
	}

	/**
	 * Counts your hand, just ended (once: false if it was already counted, as when a game is
	 * resumed at the end of a hand).
	 */
	public boolean countHand(int handIndex, Difficulty level, int bid, int won) {
		if (handIndex < handsCounted) {
			return false;
		}
		handsCounted = handIndex + 1;
		boolean madeIt = bid == won;
		perfectSoFar &= madeIt;
		int points = Rules.score(bid, won);
		for (Totals t : both(level)) {
			t.hands++;
			t.tricks += won;
			if (madeIt) {
				t.made++;
			} else if (won > bid) {
				t.over++;
			} else {
				t.under++;
			}
			if (bid == 0) {
				t.zeroBids++;
				if (madeIt) {
					t.zeroMade++;
				}
			}
			t.bestHand = Math.max(t.bestHand, points);
			t.worstHand = Math.min(t.worstHand, points);
		}
		return true;
	}

	/**
	 * Counts the game, just finished (once): seat 0 is you, seats 1-3 the given players
	 * ({@link Player} ids); level is the difficulty it was started at.
	 */
	public boolean countGame(Difficulty level, int[] finalScores, int hands, int[] players) {
		if (gameCounted) {
			return false;
		}
		gameCounted = true;
		int mine = finalScores[0];
		boolean won = true;
		for (int s = 1; s < finalScores.length; s++) {
			won &= finalScores[s] <= mine;
		}
		boolean perfect = perfectSoFar && handsCounted == hands;
		for (Totals t : both(level)) {
			t.games++;
			t.finalScores += mine;
			t.bestFinal = Math.max(t.bestFinal, mine);
			t.worstFinal = Math.min(t.worstFinal, mine);
			if (won) {
				t.wins++;
				t.streak++;
				t.bestStreak = Math.max(t.bestStreak, t.streak);
			} else {
				t.streak = 0;
			}
			if (perfect) {
				t.perfect++;
			}
			for (int s = 1; s < finalScores.length; s++) {
				if (finalScores[s] > mine) {
					t.beatenBy[players[s - 1]]++;
				}
			}
		}
		return true;
	}

	private Totals[] both(Difficulty level) {
		return new Totals[] { totals[level.ordinal()], totals[ALL] };
	}

	// ---- saving: one "name=value" line per number, e.g. "hard.games=12", "all.beatenBy.9=3"

	/** Each totals' name in the save: the difficulties, then all of them. */
	private static String bucket(int which) {
		return which == ALL ? "all" : Difficulty.values()[which].key();
	}

	/** A totals' numbers by name (the per-player counts are added separately). */
	private static java.util.Map<String, Long> fields(Totals t) {
		java.util.Map<String, Long> f = new LinkedHashMap<String, Long>();
		f.put("games", (long) t.games);
		f.put("wins", (long) t.wins);
		f.put("streak", (long) t.streak);
		f.put("bestStreak", (long) t.bestStreak);
		f.put("perfect", (long) t.perfect);
		f.put("finalScores", t.finalScores);
		f.put("bestFinal", (long) t.bestFinal);
		f.put("worstFinal", (long) t.worstFinal);
		f.put("hands", (long) t.hands);
		f.put("made", (long) t.made);
		f.put("zeroBids", (long) t.zeroBids);
		f.put("zeroMade", (long) t.zeroMade);
		f.put("over", (long) t.over);
		f.put("under", (long) t.under);
		f.put("tricks", (long) t.tricks);
		f.put("bestHand", (long) t.bestHand);
		f.put("worstHand", (long) t.worstHand);
		return f;
	}

	/** Sets one number by its name; an unknown name is ignored (from a newer or older save). */
	private static void set(Totals t, String name, long v) {
		int i = (int) v;
		if (name.equals("games")) t.games = i;
		else if (name.equals("wins")) t.wins = i;
		else if (name.equals("streak")) t.streak = i;
		else if (name.equals("bestStreak")) t.bestStreak = i;
		else if (name.equals("perfect")) t.perfect = i;
		else if (name.equals("finalScores")) t.finalScores = v;
		else if (name.equals("bestFinal")) t.bestFinal = i;
		else if (name.equals("worstFinal")) t.worstFinal = i;
		else if (name.equals("hands")) t.hands = i;
		else if (name.equals("made")) t.made = i;
		else if (name.equals("zeroBids")) t.zeroBids = i;
		else if (name.equals("zeroMade")) t.zeroMade = i;
		else if (name.equals("over")) t.over = i;
		else if (name.equals("under")) t.under = i;
		else if (name.equals("tricks")) t.tricks = i;
		else if (name.equals("bestHand")) t.bestHand = i;
		else if (name.equals("worstHand")) t.worstHand = i;
		else if (name.startsWith("beatenBy.")) {
			int id = Integer.parseInt(name.substring("beatenBy.".length()));
			if (id >= 0 && id < t.beatenBy.length) { // (a removed player's count is dropped)
				t.beatenBy[id] = i;
			}
		}
	}

	public String encode() {
		StringBuilder sb = new StringBuilder();
		sb.append("game.handsCounted=").append(handsCounted).append('\n');
		sb.append("game.counted=").append(gameCounted ? 1 : 0).append('\n');
		sb.append("game.perfectSoFar=").append(perfectSoFar ? 1 : 0).append('\n');
		for (int i = 0; i < totals.length; i++) {
			String b = bucket(i);
			for (java.util.Map.Entry<String, Long> e : fields(totals[i]).entrySet()) {
				sb.append(b).append('.').append(e.getKey()).append('=').append(e.getValue()).append('\n');
			}
			for (int id = 0; id < totals[i].beatenBy.length; id++) {
				if (totals[i].beatenBy[id] > 0) {
					sb.append(b).append(".beatenBy.").append(id).append('=').append(totals[i].beatenBy[id]).append('\n');
				}
			}
		}
		return sb.toString();
	}

	/** Reads what {@link #encode()} wrote (or the first builds' numbered lines); anything unreadable starts afresh. */
	public static Stats decode(String saved) {
		Stats s = new Stats();
		if (saved == null || saved.isEmpty()) {
			return s;
		}
		try {
			Map<String, String> lines = new LinkedHashMap<String, String>();
			for (String line : saved.split("\n")) {
				int eq = line.indexOf('=');
				if (eq > 0) {
					lines.put(line.substring(0, eq), line.substring(eq + 1).trim());
				}
			}
			if (lines.containsKey("game")) {
				return decodeNumbered(lines); // the first builds' format
			}
			if (!lines.containsKey("game.handsCounted")) {
				return new Stats();
			}
			s.handsCounted = Integer.parseInt(lines.get("game.handsCounted"));
			s.gameCounted = "1".equals(lines.get("game.counted"));
			s.perfectSoFar = "1".equals(lines.get("game.perfectSoFar"));
			for (int i = 0; i < s.totals.length; i++) {
				String prefix = bucket(i) + ".";
				for (Map.Entry<String, String> e : lines.entrySet()) {
					if (e.getKey().startsWith(prefix)) {
						set(s.totals[i], e.getKey().substring(prefix.length()), Long.parseLong(e.getValue()));
					}
				}
			}
			return s;
		} catch (RuntimeException e) {
			return new Stats();
		}
	}

	/** Players on the roster when statistics were kept by position. */
	private static final int LEGACY_PLAYERS = 12;

	/** The first builds' format: "game=" then one line of numbers per totals, by position. */
	private static Stats decodeNumbered(Map<String, String> lines) {
		Stats s = new Stats();
		String[] g = lines.get("game").split(",");
		s.handsCounted = Integer.parseInt(g[0]);
		s.gameCounted = g[1].equals("1");
		s.perfectSoFar = g[2].equals("1");
		String[] names = fields(new Totals()).keySet().toArray(new String[0]);
		for (int i = 0; i < s.totals.length; i++) {
			String line = lines.get(String.valueOf(i));
			if (line == null) {
				continue;
			}
			String[] f = line.split(",");
			for (int j = 0; j < names.length && j < f.length; j++) {
				set(s.totals[i], names[j], Long.parseLong(f[j]));
			}
			// then, from the build with twelve players, one count per player by id (the very first
			// builds kept three, by seat: those can't be matched to players, so they're dropped)
			if (f.length == names.length + LEGACY_PLAYERS) {
				for (int id = 0; id < LEGACY_PLAYERS; id++) {
					set(s.totals[i], "beatenBy." + id, Long.parseLong(f[names.length + id]));
				}
			}
		}
		return s;
	}
}
