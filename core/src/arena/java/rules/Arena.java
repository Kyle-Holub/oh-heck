package rules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Experiments on the computer players, for when changing them: who beats whom, how well
 * each level bids by round, and the learned bidding table.
 * <p>
 * Games are played in duplicate: each deal is played four times with the player under
 * test in a different seat each time, so who got the good cards cancels out and what's
 * left is strategy. With 400 games a win rate is good to about 2 points either way.
 * <p>
 * Run through Gradle (see core/build.gradle), e.g. {@code ./gradlew core:arena -Parena="vs 400"}.
 */
public final class Arena {

	/** Makes a fresh computer player (a new one per seat per game). */
	interface Maker {
		Bot make(long seed);
	}

	static Maker level(final Difficulty d) {
		return new Maker() {
			@Override
			public Bot make(long seed) {
				return Bot.of(d, new Random(seed));
			}
		};
	}

	/** What the player under test did. */
	static final class Stats {
		long games, hands, exact;
		double points, miss, wins;
		final long[] handsBy = new long[9], exactBy = new long[9], overBy = new long[9], underBy = new long[9];
		final double[] pointsBy = new double[9], expectedBy = new double[9], wonBy = new double[9];
	}

	/** One game with the tested player in seat {@code seat} and the field everywhere else. */
	static void game(long deal, int seat, Maker test, Maker field, Stats st) {
		Bot[] bots = new Bot[4];
		for (int i = 0; i < 4; i++) {
			bots[i] = (i == seat ? test : field).make(deal * 4 + i);
		}
		Match m = Match.random(4, new Random(deal));
		double expected = 0;
		while (m.phase() != Match.Phase.GAME_OVER) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				int n = m.handSize(), b = m.bid(seat), w = m.tricksWon(seat);
				st.hands++;
				st.handsBy[n]++;
				if (b == w) {
					st.exact++;
					st.exactBy[n]++;
				} else if (b > w) {
					st.overBy[n]++;
				} else {
					st.underBy[n]++;
				}
				st.miss += Math.abs(b - w);
				st.points += Rules.score(b, w);
				st.pointsBy[n] += Rules.score(b, w);
				st.expectedBy[n] += expected;
				st.wonBy[n] += w;
				m.nextHand();
				continue;
			}
			int s = m.currentSeat();
			TableView v = m.view(s);
			if (m.phase() == Match.Phase.BIDDING) {
				if (s == seat) {
					expected = 0;
					for (Card c : v.hand) {
						expected += BidTable.chance(v, c);
					}
				}
				m.placeBid(s, bots[s].chooseBid(v));
			} else {
				m.playCard(s, bots[s].choosePlay(v));
			}
		}
		st.games++;
		List<Integer> leaders = m.leaders();
		if (leaders.contains(seat)) {
			st.wins += 1.0 / leaders.size();
		}
	}

	/** {@code deals} deals, each played with the tested player in all four seats. */
	static Stats run(int deals, Maker test, Maker field) {
		Stats st = new Stats();
		for (int d = 0; d < deals; d++) {
			for (int seat = 0; seat < 4; seat++) {
				game(5000 + d, seat, test, field, st);
			}
		}
		return st;
	}

	static void report(String name, Stats s) {
		System.out.printf("%-30s %5.2f pts/hand   %4.1f%% exact   %4.2f off   %4.1f%% wins   (%d games)%n", name,
				s.points / s.hands, 100.0 * s.exact / s.hands, s.miss / s.hands, 100 * s.wins / s.games, s.games);
	}

	static void byRound(Stats s) {
		System.out.println("   hand  exact  over  under  pts/hand  tricks expected  taken");
		for (int n = 1; n <= 8; n++) {
			if (s.handsBy[n] > 0) {
				double h = s.handsBy[n];
				System.out.printf("   %d     %3.0f%%   %3.0f%%  %3.0f%%    %5.2f       %4.2f        %4.2f%n", n,
						100 * s.exactBy[n] / h, 100 * s.overBy[n] / h, 100 * s.underBy[n] / h, s.pointsBy[n] / h,
						s.expectedBy[n] / h, s.wonBy[n] / h);
			}
		}
	}

	// ---- learning the bidding table

	/** Smoothing: a detailed cell counts as this many games' worth of the coarse estimate. */
	private static final double PRIOR = 40;

	/**
	 * Plays {@code games} games with Normal players and counts, for every card dealt, whether
	 * it took a trick, by: hand size, trump or not, rank, how many cards of its suit its
	 * holder had (1, 2, 3 or more), and its holder's place in the bidding (first to bid
	 * leads the first trick). Writes the result to core/src/main/java/rules/BidTable.java.
	 */
	static void learn(int games) throws IOException {
		double[][][] won = new double[9][2][13], dealt = new double[9][2][13];
		double[][][][] fineWon = new double[9][2][13][BidTable.CONTEXTS], fineDealt = new double[9][2][13][BidTable.CONTEXTS];
		Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));
		for (int g = 0; g < games; g++) {
			Match m = Match.random(4, new Random(900000 + g));
			Map<Card, int[]> dealtAs = new HashMap<Card, int[]>(); // card -> {trump?, context}
			while (m.phase() != Match.Phase.GAME_OVER) {
				if (m.phase() == Match.Phase.HAND_OVER) {
					m.nextHand();
					dealtAs.clear();
					continue;
				}
				int s = m.currentSeat();
				if (m.phase() == Match.Phase.BIDDING) {
					if (dealtAs.isEmpty()) {
						for (int q = 0; q < 4; q++) {
							TableView v = m.view(q);
							for (Card c : v.hand) {
								dealtAs.put(c, new int[] { c.suit() == m.trump() ? 1 : 0, BidTable.context(v, c) });
							}
						}
					}
					m.placeBid(s, bot.chooseBid(m.view(s)));
				} else {
					Card c = bot.choosePlay(m.view(s));
					if (m.playCard(s, c) >= 0) {
						List<Card> t = m.lastTrick();
						Card winner = t.get(Rules.trickWinner(t, m.trump()));
						int n = m.handSize();
						for (Card x : t) {
							int[] k = dealtAs.get(x);
							int r = x.rank().strength(), w = x == winner ? 1 : 0;
							dealt[n][k[0]][r]++;
							won[n][k[0]][r] += w;
							fineDealt[n][k[0]][r][k[1]]++;
							fineWon[n][k[0]][r][k[1]] += w;
						}
					}
				}
			}
		}
		StringBuilder rows = new StringBuilder();
		for (int n = 1; n <= 8; n++) {
			for (int t = 0; t < 2; t++) {
				rows.append("\t\t\t// hand ").append(n).append(t == 1 ? ", trumps" : ", side suits").append("\n");
				for (int r = 0; r < 13; r++) {
					double coarse = dealt[n][t][r] == 0 ? 0 : won[n][t][r] / dealt[n][t][r];
					rows.append("\t\t\t{ ");
					for (int k = 0; k < BidTable.CONTEXTS; k++) {
						double p = (fineWon[n][t][r][k] + PRIOR * coarse) / (fineDealt[n][t][r][k] + PRIOR);
						rows.append(String.format(java.util.Locale.US, "%.3f", p)).append(k < BidTable.CONTEXTS - 1 ? ", " : " },\n");
					}
				}
			}
		}
		String src = TEMPLATE.replace("$GAMES", String.valueOf(games)).replace("$ROWS", rows.toString());
		File out = new File("core/src/main/java/rules/BidTable.java");
		FileWriter w = new FileWriter(out);
		w.write(src);
		w.close();
		System.out.println("Wrote " + out.getPath());
	}

	private static final String TEMPLATE = "package rules;\n\n"
			+ "/**\n"
			+ " * How often a card takes a trick, learned from $GAMES simulated games between Normal\n"
			+ " * players: by hand size, whether it's a trump, rank, how many cards of its suit its\n"
			+ " * holder has (1, 2, 3 or more), and the holder's place in the bidding.\n"
			+ " * <p>\n"
			+ " * Generated by {@code ./gradlew core:arena -Parena=\"learn $GAMES\"}; don't edit the numbers by\n"
			+ " * hand. Regenerate (twice) after changing how the computer players play.\n"
			+ " */\n"
			+ "final class BidTable {\n\n"
			+ "\tprivate BidTable() {\n\t}\n\n"
			+ "\t/** Suit length (1, 2, 3+) times place in the bidding (0 = bids first and leads). */\n"
			+ "\tstatic final int CONTEXTS = 12;\n\n"
			+ "\t/** The chance this card in the viewer's hand takes a trick this hand. */\n"
			+ "\tstatic double chance(TableView v, Card c) {\n"
			+ "\t\tint row = ((v.handSize - 1) * 2 + (c.suit() == v.trump() ? 1 : 0)) * 13 + c.rank().strength();\n"
			+ "\t\treturn CHANCE[row][context(v, c)];\n"
			+ "\t}\n\n"
			+ "\t/** Which column: the card's suit length in the hand, and the viewer's place in the bidding. */\n"
			+ "\tstatic int context(TableView v, Card c) {\n"
			+ "\t\tint length = 0;\n"
			+ "\t\tfor (Card h : v.hand) {\n"
			+ "\t\t\tif (h.suit() == c.suit()) {\n"
			+ "\t\t\t\tlength++;\n"
			+ "\t\t\t}\n"
			+ "\t\t}\n"
			+ "\t\tint place = ((v.seat - v.dealer - 1) % v.players + v.players) % v.players;\n"
			+ "\t\treturn (Math.min(length, 3) - 1) * 4 + Math.min(place, 3);\n"
			+ "\t}\n\n"
			+ "\t/** Rows: hand 1 side-suit 2s, 3s... aces, then hand 1 trumps, hand 2... Columns: see context(). */\n"
			+ "\tprivate static final double[][] CHANCE = {\n"
			+ "$ROWS"
			+ "\t};\n"
			+ "}\n";

	public static void main(String[] args) throws IOException {
		String what = args.length > 0 ? args[0] : "levels";
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 200;
		long start = System.currentTimeMillis();
		if (what.equals("levels")) {
			for (Difficulty d : Difficulty.values()) {
				Stats s = run(n, level(d), level(d));
				report(d.label + " (all four)", s);
				byRound(s);
			}
		} else if (what.equals("vs")) {
			report("Normal vs 3 Easy", run(n, level(Difficulty.NORMAL), level(Difficulty.EASY)));
			report("Hard vs 3 Normal", run(n / 4 + 1, level(Difficulty.HARD), level(Difficulty.NORMAL)));
			report("Hard vs 3 Easy", run(n / 4 + 1, level(Difficulty.HARD), level(Difficulty.EASY)));
		} else if (what.equals("learn")) {
			learn(n);
		} else if (what.equals("samples")) {
			int keep = HardBot.samples;
			for (int k : new int[] { 10, 20, 40, 80 }) {
				HardBot.samples = k;
				long t = System.currentTimeMillis();
				report("Hard (" + k + " deals) vs 3 Normal", run(n, level(Difficulty.HARD), level(Difficulty.NORMAL)));
				System.out.printf("   %.1f s%n", (System.currentTimeMillis() - t) / 1000.0);
			}
			HardBot.samples = keep;
		} else {
			System.out.println("Experiments: levels N | vs N | learn N | samples N");
		}
		System.out.printf("(%.1f s)%n", (System.currentTimeMillis() - start) / 1000.0);
	}
}
