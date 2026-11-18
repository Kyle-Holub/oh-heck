package ohheck;

import java.util.List;
import java.util.Locale;

import rules.Player;
import rules.Stats;
import rules.Suit;

/**
 * The game's sentences, built from what happened: who won, why a tap didn't count, how a hand
 * went, the statistics rows. Kept out of the drawing code so the grammar is in one place and
 * tested (WordingTest). No libGDX.
 */
final class Wording {

	/** Shown for a statistic with nothing to show yet. */
	static final String NONE = "—";

	private Wording() {
	}

	/** "You win the game!", "Rupert wins the game!", "You and Rupert tie!", "You, Rupert and Oswald tie!" */
	static String winners(List<String> names, boolean youAlone) {
		if (names.size() == 1) {
			return names.get(0) + (youAlone ? " win the game!" : " wins the game!");
		}
		return list(names) + " tie!";
	}

	/** "A", "A and B", "A, B and C". */
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

	/** Under the bid buttons, when you're the dealer. */
	static String dealerCantBid(int forbidden) {
		return "As dealer you can't bid " + forbidden;
	}

	/** When you tap the forbidden bid. */
	static String dealerCantBid(int bid, int tricks) {
		return "As dealer you can't bid " + bid + ": the bids would add up to " + tricks;
	}

	/** When you tap a card that doesn't follow suit. */
	static String mustFollow(Suit led) {
		return "You must follow suit: play a " + singular(led);
	}

	/** By each player at the end of a hand. */
	static String handResult(boolean made) {
		return made ? "Bid tricks!" : "Set!";
	}

	/** "1st", "2nd", "3rd", "4th"... (to 20th, which is as far as places go). */
	static String ordinal(int n) {
		return n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
	}

	/** "heart", "spade"... */
	static String singular(Suit suit) {
		String n = suit.name().toLowerCase(Locale.US);
		return n.substring(0, n.length() - 1);
	}

	static String percent(double share) {
		return Math.round(share * 100) + "%";
	}

	/** The Statistics tab's Games panel: a label and a value per row. */
	static String[][] gameRows(Stats.Totals t) {
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

	/** The Statistics tab's Hands panel. */
	static String[][] handRows(Stats.Totals t) {
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
