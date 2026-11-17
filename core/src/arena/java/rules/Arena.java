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

	/**
	 * Plays {@code games} games with Normal players and counts, for every card dealt, whether
	 * it took a trick: by hand size, trump or not, and rank. Writes the result to
	 * core/src/main/java/rules/BidTable.java.
	 */
	static void learn(int games) throws IOException {
		double[][][] won = new double[9][2][13], dealt = new double[9][2][13];
		Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));
		for (int g = 0; g < games; g++) {
			Match m = Match.random(4, new Random(900000 + g));
			Map<Card, Integer> trumpDealt = new HashMap<Card, Integer>();
			while (m.phase() != Match.Phase.GAME_OVER) {
				if (m.phase() == Match.Phase.HAND_OVER) {
					m.nextHand();
					trumpDealt.clear();
					continue;
				}
				int s = m.currentSeat();
				if (m.phase() == Match.Phase.BIDDING) {
					if (trumpDealt.isEmpty()) {
						for (int q = 0; q < 4; q++) {
							for (Card c : m.hand(q)) {
								trumpDealt.put(c, c.suit() == m.trump() ? 1 : 0);
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
							int k = trumpDealt.get(x);
							dealt[n][k][x.rank().strength()]++;
							if (x == winner) {
								won[n][k][x.rank().strength()]++;
							}
						}
					}
				}
			}
		}
		StringBuilder rows = new StringBuilder();
		for (int n = 1; n <= 8; n++) {
			for (int t = 0; t < 2; t++) {
				rows.append("\t\t\t{ ");
				for (int r = 0; r < 13; r++) {
					double p = dealt[n][t][r] == 0 ? 0 : won[n][t][r] / dealt[n][t][r];
					rows.append(String.format(java.util.Locale.US, "%.3f", p)).append(r < 12 ? ", " : " }");
				}
				rows.append(t == 0 ? ", // hand " + n + ", side suit\n" : (n < 8 ? ", // trump\n" : " // trump\n"));
			}
		}
		String src = "package rules;\n\n"
				+ "/**\n"
				+ " * How often a card takes a trick, learned from " + games + " simulated games between\n"
				+ " * Normal players: by hand size, whether it's a trump, and rank (2 up to ace).\n"
				+ " * <p>\n"
				+ " * Generated by {@code ./gradlew core:arena -Parena=\"learn " + games + "\"}; don't edit by hand.\n"
				+ " * Regenerate after changing how the computer players play.\n"
				+ " */\n"
				+ "final class BidTable {\n\n"
				+ "\tprivate BidTable() {\n\t}\n\n"
				+ "\t/** The chance this card takes a trick in this hand. */\n"
				+ "\tstatic double chance(TableView v, Card c) {\n"
				+ "\t\treturn CHANCE[(v.handSize - 1) * 2 + (c.suit() == v.trump() ? 1 : 0)][c.rank().strength()];\n"
				+ "\t}\n\n"
				+ "\t/** Rows: hand 1 side suit, hand 1 trump, hand 2 side suit... Columns: 2, 3, ... king, ace. */\n"
				+ "\tprivate static final double[][] CHANCE = {\n"
				+ rows
				+ "\t};\n"
				+ "}\n";
		File out = new File("core/src/main/java/rules/BidTable.java");
		FileWriter w = new FileWriter(out);
		w.write(src);
		w.close();
		System.out.println("Wrote " + out.getPath());
	}

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
