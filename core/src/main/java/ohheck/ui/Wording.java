package ohheck.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ohheck.players.Difficulty;
import ohheck.players.Player;
import ohheck.records.Stats;
import ohheck.rules.Suit;

/* The game's sentences. */
public final class Wording {

	static final String NONE = "—";

	private Wording() {
	}

	private static final Map<Difficulty, String[]> LEVEL_WORDS = new EnumMap<Difficulty, String[]>(Difficulty.class);
	static {
		LEVEL_WORDS.put(Difficulty.EASY, new String[] { "easy-going players", "easy-going player", "a beginner" });
		LEVEL_WORDS.put(Difficulty.NORMAL, new String[] { "solid players", "solid player", "a solid player" });
		LEVEL_WORDS.put(Difficulty.HARD, new String[] { "experts", "expert", "an expert" });
		for (Difficulty d : Difficulty.values()) {
			if (!LEVEL_WORDS.containsKey(d)) {
				throw new IllegalStateException("No words for " + d);
			}
		}
	}
	private static final int MANY = 0, ODD_ONE = 1, NEVER = 2;

	public static String tableDescription(Difficulty setting) {
		List<Difficulty> byChance = new ArrayList<Difficulty>(Arrays.asList(Difficulty.values()));
		byChance.sort((x, y) -> Double.compare(setting.chanceOf(y), setting.chanceOf(x)));
		StringBuilder sb = new StringBuilder("Mostly ").append(LEVEL_WORDS.get(byChance.get(0))[MANY]);
		for (Difficulty l : byChance.subList(1, byChance.size())) {
			double p = setting.chanceOf(l);
			if (p > 0) {
				sb.append(p < 0.2 ? ", the odd " + LEVEL_WORDS.get(l)[ODD_ONE] : ", some " + LEVEL_WORDS.get(l)[MANY]);
			}
		}
		for (Difficulty l : Difficulty.values()) {
			if (setting.chanceOf(l) == 0) {
				sb.append("; never ").append(LEVEL_WORDS.get(l)[NEVER]);
			}
		}
		return sb.append('.').toString();
	}

	public static String winners(List<String> names, boolean youAlone) {
		if (names.size() == 1) {
			return names.get(0) + (youAlone ? " win the game!" : " wins the game!");
		}
		return list(names) + " tie!";
	}

	static String list(List<String> names) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < names.size(); i++) {
			if (i > 0) {
				sb.append(i == names.size() - 1 ? " and " : ", ");
			}
			sb.append(names.get(i));
		}
		return sb.toString();
	}

	public static String dealerCantBid(int forbidden) {
		return "As dealer you can't bid " + forbidden;
	}

	public static String dealerCantBid(int bid, int tricks) {
		return "As dealer you can't bid " + bid + ": the bids would add up to " + tricks;
	}

	public static String mustFollow(Suit led) {
		return "You must follow suit: play a " + singular(led);
	}

	public static String handResult(boolean made) {
		return made ? "Bid tricks!" : "Set!";
	}

	public static String ordinal(int n) {
		if (n >= 0 && n < ORDINALS.length) {
			if (ORDINALS[n] == null) {
				ORDINALS[n] = n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
			}
			return ORDINALS[n];
		}
		return n + "th";
	}

	private static final String[] ORDINALS = new String[21];

	public static String handResults(int hand) {
		if (hand >= 0 && hand < HAND_RESULTS.length) {
			if (HAND_RESULTS[hand] == null) {
				HAND_RESULTS[hand] = "Hand " + hand + " results";
			}
			return HAND_RESULTS[hand];
		}
		return "Hand " + hand + " results";
	}

	private static final String[] HAND_RESULTS = new String[64];

	static String singular(Suit suit) {
		String n = suit.name().toLowerCase(Locale.US);
		return n.substring(0, n.length() - 1);
	}

	static String percent(double share) {
		return Math.round(share * 100) + "%";
	}

	public static String[][] gameRows(Stats.Totals t) {
		boolean g = t.anyGames();
		return new String[][] {
				{ "Games played", String.valueOf(t.games) },
				{ "Games won", g ? t.wins + "  (" + percent(t.winRate()) + ")" : NONE },
				{ "Winning streak", g ? t.streak + "  (best " + t.bestStreak + ")" : NONE },
				{ "Average score", g ? String.valueOf(Math.round(t.averageFinal())) : NONE },
				{ "Best / worst score", g ? t.bestFinal + " / " + t.worstFinal : NONE },
				{ "Perfect games", g ? String.valueOf(t.perfect) : NONE },
				{ "Nemesis", t.nemesis() >= 0 ? Player.byId(t.nemesis()).name : NONE } };
	}

	public static String[][] handRows(Stats.Totals t) {
		boolean hd = t.anyHands();
		int misses = t.over + t.under;
		return new String[][] {
				{ "Hands played", String.valueOf(t.hands) },
				{ "Bids made", hd ? percent(t.madeRate()) + "  (" + t.made + ")" : NONE },
				{ "Zero bids made", t.zeroBids > 0 ? t.zeroMade + " of " + t.zeroBids : NONE },
				{ "Misses: over / under", misses > 0 ? t.over + " / " + t.under : NONE },
				{ "Tricks won", hd ? t.tricks + "  (" + String.format(Locale.US, "%.1f", t.tricksPerHand()) + " a hand)" : NONE },
				{ "Best hand", hd ? t.bestHand + " points" : NONE },
				{ "Worst hand", hd ? t.worstHand + " points" : NONE } };
	}
}
