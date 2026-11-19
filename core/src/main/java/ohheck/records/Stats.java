package ohheck.records;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ObjLongConsumer;
import java.util.function.ToLongFunction;

import ohheck.players.Difficulty;
import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.util.Log;

/** Statistics per difficulty and overall. */
public final class Stats {

	public static final class Totals {
		public int games, wins, streak, bestStreak, perfect;
		long finalScores;
		public int bestFinal = Integer.MIN_VALUE, worstFinal = Integer.MAX_VALUE;
		public int hands, made, zeroBids, zeroMade, over, under, tricks;
		public int bestHand = Integer.MIN_VALUE, worstHand = Integer.MAX_VALUE;
		public final int[] beatenBy = new int[Player.idLimit()];

		public boolean anyGames() {
			return games > 0;
		}

		public boolean anyHands() {
			return hands > 0;
		}

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

	public static final int ALL = Difficulty.values().length;

	private final Totals[] totals = new Totals[ALL + 1];

	private int handsCounted;
	private boolean gameCounted, perfectSoFar = true;

	public Stats() {
		for (int i = 0; i < totals.length; i++) {
			totals[i] = new Totals();
		}
	}

	public Totals totals(int which) {
		return totals[which];
	}

	public void startGame() {
		handsCounted = 0;
		gameCounted = false;
		perfectSoFar = true;
	}

	public boolean countHand(int handIndex, Difficulty level, int bid, int won, int points) {
		if (handIndex < handsCounted) {
			return false;
		}
		handsCounted = handIndex + 1;
		boolean madeIt = bid == won;
		perfectSoFar &= madeIt;
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

	public boolean countGame(Difficulty level, int[] finalScores, int hands, Seating players) {
		if (gameCounted) {
			return false;
		}
		gameCounted = true;
		int mine = finalScores[Seating.YOU];
		boolean won = true;
		for (int s = 0; s < finalScores.length; s++) {
			won &= s == Seating.YOU || finalScores[s] <= mine;
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
			for (int s = 0; s < finalScores.length; s++) {
				if (s != Seating.YOU && finalScores[s] > mine) {
					t.beatenBy[players.idAt(s)]++;
				}
			}
		}
		return true;
	}

	private Totals[] both(Difficulty level) {
		return new Totals[] { totals[level.ordinal()], totals[ALL] };
	}

	private static String bucket(int which) {
		return which == ALL ? "all" : Difficulty.values()[which].key();
	}

	private record Field(String name, ToLongFunction<Totals> get, ObjLongConsumer<Totals> set) {
	}

	private static final List<Field> FIELDS = Arrays.asList(
			new Field("games", t -> t.games, (t, v) -> t.games = (int) v),
			new Field("wins", t -> t.wins, (t, v) -> t.wins = (int) v),
			new Field("streak", t -> t.streak, (t, v) -> t.streak = (int) v),
			new Field("bestStreak", t -> t.bestStreak, (t, v) -> t.bestStreak = (int) v),
			new Field("perfect", t -> t.perfect, (t, v) -> t.perfect = (int) v),
			new Field("finalScores", t -> t.finalScores, (t, v) -> t.finalScores = v),
			new Field("bestFinal", t -> t.bestFinal, (t, v) -> t.bestFinal = (int) v),
			new Field("worstFinal", t -> t.worstFinal, (t, v) -> t.worstFinal = (int) v),
			new Field("hands", t -> t.hands, (t, v) -> t.hands = (int) v),
			new Field("made", t -> t.made, (t, v) -> t.made = (int) v),
			new Field("zeroBids", t -> t.zeroBids, (t, v) -> t.zeroBids = (int) v),
			new Field("zeroMade", t -> t.zeroMade, (t, v) -> t.zeroMade = (int) v),
			new Field("over", t -> t.over, (t, v) -> t.over = (int) v),
			new Field("under", t -> t.under, (t, v) -> t.under = (int) v),
			new Field("tricks", t -> t.tricks, (t, v) -> t.tricks = (int) v),
			new Field("bestHand", t -> t.bestHand, (t, v) -> t.bestHand = (int) v),
			new Field("worstHand", t -> t.worstHand, (t, v) -> t.worstHand = (int) v));

	static List<String> fieldNames() {
		List<String> names = new ArrayList<String>();
		for (Field f : FIELDS) {
			names.add(f.name());
		}
		return names;
	}

	private static void set(Totals t, String name, long v) {
		for (Field f : FIELDS) {
			if (f.name().equals(name)) {
				f.set().accept(t, v);
				return;
			}
		}
		if (name.startsWith("beatenBy.")) {
			int id = Integer.parseInt(name.substring("beatenBy.".length()));
			if (id >= 0 && id < t.beatenBy.length) {
				t.beatenBy[id] = (int) v;
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
			for (Field f : FIELDS) {
				sb.append(b).append('.').append(f.name()).append('=').append(f.get().applyAsLong(totals[i])).append('\n');
			}
			for (int id = 0; id < totals[i].beatenBy.length; id++) {
				if (totals[i].beatenBy[id] > 0) {
					sb.append(b).append(".beatenBy.").append(id).append('=').append(totals[i].beatenBy[id]).append('\n');
				}
			}
		}
		return sb.toString();
	}

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
				return decodeNumbered(lines);
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
			Log.warn("Stats", "Unreadable statistics; starting afresh", e);
			return new Stats();
		}
	}

	private static final int LEGACY_PLAYERS = 12;
	private static final List<String> NUMBERED_FORMAT = Arrays.asList("games", "wins", "streak", "bestStreak",
			"perfect", "finalScores", "bestFinal", "worstFinal", "hands", "made", "zeroBids", "zeroMade", "over", "under",
			"tricks", "bestHand", "worstHand");

	private static Stats decodeNumbered(Map<String, String> lines) {
		Stats s = new Stats();
		String[] g = lines.get("game").split(",");
		s.handsCounted = Integer.parseInt(g[0]);
		s.gameCounted = g[1].equals("1");
		s.perfectSoFar = g[2].equals("1");
		String[] names = NUMBERED_FORMAT.toArray(new String[0]);
		for (int i = 0; i < s.totals.length; i++) {
			String line = lines.get(String.valueOf(i));
			if (line == null) {
				continue;
			}
			String[] f = line.split(",");
			for (int j = 0; j < names.length && j < f.length; j++) {
				set(s.totals[i], names[j], Long.parseLong(f[j]));
			}
			if (f.length == names.length + LEGACY_PLAYERS) {
				for (int id = 0; id < LEGACY_PLAYERS; id++) {
					set(s.totals[i], "beatenBy." + id, Long.parseLong(f[names.length + id]));
				}
			}
		}
		return s;
	}
}
