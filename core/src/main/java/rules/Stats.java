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
		public final int[] beatenBy = new int[Player.ALL.size()];

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

	// ---- saving

	/** Totals saved before the per-player counts. */
	private static final int FIELDS = 17;

	public String encode() {
		StringBuilder sb = new StringBuilder();
		sb.append("game=").append(handsCounted).append(',').append(gameCounted ? 1 : 0).append(',')
				.append(perfectSoFar ? 1 : 0).append('\n');
		for (int i = 0; i < totals.length; i++) {
			Totals t = totals[i];
			sb.append(i).append('=');
			long[] v = { t.games, t.wins, t.streak, t.bestStreak, t.perfect, t.finalScores, t.bestFinal, t.worstFinal,
					t.hands, t.made, t.zeroBids, t.zeroMade, t.over, t.under, t.tricks, t.bestHand, t.worstHand };
			for (int j = 0; j < v.length; j++) {
				sb.append(j == 0 ? "" : ",").append(v[j]);
			}
			for (int b : t.beatenBy) { // then one count per player
				sb.append(',').append(b);
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	/** Reads what {@link #encode()} wrote; anything unreadable starts afresh. */
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
					lines.put(line.substring(0, eq), line.substring(eq + 1));
				}
			}
			String[] g = lines.get("game").split(",");
			s.handsCounted = Integer.parseInt(g[0]);
			s.gameCounted = g[1].equals("1");
			s.perfectSoFar = g[2].equals("1");
			for (int i = 0; i < s.totals.length; i++) {
				String line = lines.get(String.valueOf(i));
				if (line == null) {
					continue;
				}
				String[] f = line.split(",");
				Totals t = s.totals[i];
				t.games = Integer.parseInt(f[0]);
				t.wins = Integer.parseInt(f[1]);
				t.streak = Integer.parseInt(f[2]);
				t.bestStreak = Integer.parseInt(f[3]);
				t.perfect = Integer.parseInt(f[4]);
				t.finalScores = Long.parseLong(f[5]);
				t.bestFinal = Integer.parseInt(f[6]);
				t.worstFinal = Integer.parseInt(f[7]);
				t.hands = Integer.parseInt(f[8]);
				t.made = Integer.parseInt(f[9]);
				t.zeroBids = Integer.parseInt(f[10]);
				t.zeroMade = Integer.parseInt(f[11]);
				t.over = Integer.parseInt(f[12]);
				t.under = Integer.parseInt(f[13]);
				t.tricks = Integer.parseInt(f[14]);
				t.bestHand = Integer.parseInt(f[15]);
				t.worstHand = Integer.parseInt(f[16]);
				// (one count per player after the 17 totals; the first test builds kept three, by seat: dropped)
				if (f.length == FIELDS + t.beatenBy.length) {
					for (int b = 0; b < t.beatenBy.length; b++) {
						t.beatenBy[b] = Integer.parseInt(f[FIELDS + b]);
					}
				}
			}
			return s;
		} catch (RuntimeException e) {
			return new Stats();
		}
	}
}
