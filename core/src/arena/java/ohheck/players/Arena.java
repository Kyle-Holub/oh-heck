package ohheck.players;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ohheck.records.Stats;
import ohheck.rules.Card;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;
import ohheck.rules.TableView;

/*
 * Bots against bots, for measuring AI changes, and the learning that generates BidTable. Deals
 * are played in duplicate, rotating seats, so luck cancels out.
 */
public final class Arena {

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

	static RuleSet rules = RuleSet.STANDARD;
	static int players = 4;

	static final class Stats {
		static final int SIZES = RuleSet.MOST_CARDS + 1;
		long games, hands, exact;
		double points, miss, wins;
		final long[] handsBy = new long[SIZES], exactBy = new long[SIZES], overBy = new long[SIZES], underBy = new long[SIZES];
		final double[] pointsBy = new double[SIZES], expectedBy = new double[SIZES], wonBy = new double[SIZES];
	}

	static void game(long deal, int seat, Maker test, Maker field, Stats st) {
		Bot[] bots = new Bot[players];
		for (int i = 0; i < players; i++) {
			bots[i] = (i == seat ? test : field).make(deal * 4 + i);
		}
		Match m = Match.random(players, new Random(deal), rules);
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
				st.points += m.rules().score(b, w);
				st.pointsBy[n] += m.rules().score(b, w);
				st.expectedBy[n] += expected;
				st.wonBy[n] += w;
				m.nextHand();
				continue;
			}
			int s = m.currentSeat();
			TableView v = m.view(s);
			if (m.pickingTrump() || m.swapping()) {
				m.apply(bots[s].chooseMove(v));
			} else if (m.phase() == Match.Phase.BIDDING) {
				if (s == seat) {
					expected = 0;
					for (Card c : v.hand()) {
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

	static Stats run(int deals, Maker test, Maker field) {
		Stats st = new Stats();
		for (int d = 0; d < deals; d++) {
			for (int seat = 0; seat < players; seat++) {
				game(5000 + d, seat, test, field, st);
			}
		}
		return st;
	}

	static void variants(int n) {
		for (HandOrder order : HandOrder.values()) {
			for (int most : new int[] { RuleSet.USUAL_CARDS, RuleSet.MOST_CARDS }) {
				for (int flags = 0; flags < 4; flags++) {
					variant(n, new RuleSet(order, most, (flags & 1) != 0, (flags & 2) != 0));
				}
			}
		}
		for (RuleSet twisted : new RuleSet[] { RuleSet.STANDARD.withMomentum(true), RuleSet.STANDARD.withPickTrump(true),
				RuleSet.STANDARD.withHandsWon(true), RuleSet.STANDARD.withWidow(true) }) {
			variant(n, twisted);
		}
		rules = RuleSet.STANDARD;
	}

	private static void variant(int n, RuleSet r) {
		rules = r;
		System.out.println("== " + rules.code());
		report("  Easy (all four)", run(n, level(Difficulty.EASY), level(Difficulty.EASY)));
		report("  Normal (all four)", run(n, level(Difficulty.NORMAL), level(Difficulty.NORMAL)));
		report("  Normal vs 3 Easy", run(n, level(Difficulty.NORMAL), level(Difficulty.EASY)));
		report("  Hard vs 3 Normal", run(Math.max(1, n / 4), level(Difficulty.HARD), level(Difficulty.NORMAL)));
	}

	static void report(String name, Stats s) {
		System.out.printf("%-30s %5.2f pts/hand   %4.1f%% exact   %4.2f off   %4.1f%% wins   (%d games)%n", name,
				s.points / s.hands, 100.0 * s.exact / s.hands, s.miss / s.hands, 100 * s.wins / s.games, s.games);
	}

	static void byRound(Stats s) {
		System.out.println("   hand  exact  over  under  pts/hand  tricks expected  taken");
		for (int n = 1; n < Stats.SIZES; n++) {
			if (s.handsBy[n] > 0) {
				double h = s.handsBy[n];
				System.out.printf("   %d     %3.0f%%   %3.0f%%  %3.0f%%    %5.2f       %4.2f        %4.2f%n", n,
						100 * s.exactBy[n] / h, 100 * s.overBy[n] / h, 100 * s.underBy[n] / h, s.pointsBy[n] / h,
						s.expectedBy[n] / h, s.wonBy[n] / h);
			}
		}
	}

	private static final double PRIOR = 40;

	static void watch(RuleSet r, int players, int games, long seeds, int smallest, double[][][] won, double[][][] dealt,
			double[][][][] fineWon, double[][][][] fineDealt) {
		Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));
		for (int g = 0; g < games; g++) {
			Match m = Match.random(players, new Random(seeds + g), r);
			Map<Card, int[]> dealtAs = new HashMap<Card, int[]>();
			while (m.phase() != Match.Phase.GAME_OVER) {
				if (m.phase() == Match.Phase.HAND_OVER) {
					m.nextHand();
					dealtAs.clear();
					continue;
				}
				int s = m.currentSeat();
				if (m.pickingTrump() || m.swapping()) {
					m.apply(bot.chooseMove(m.view(s)));
				} else if (m.phase() == Match.Phase.BIDDING) {
					if (dealtAs.isEmpty()) {
						for (int q = 0; q < players; q++) {
							TableView v = m.view(q);
							for (Card c : v.hand()) {
								dealtAs.put(c, new int[] { c.suit() == m.trump() ? 1 : 0, BidTable.context(v, c) });
							}
						}
					}
					m.placeBid(s, bot.chooseBid(m.view(s)));
				} else {
					Card c = bot.choosePlay(m.view(s));
					if (m.playCard(s, c) >= 0 && m.handSize() >= smallest) {
						List<Card> t = m.lastTrick();
						Card winner = t.get(m.rules().trickWinner(t, m.trump()));
						int n = m.handSize();
						for (Card x : t) {
							int[] k = dealtAs.get(x);
							int rank = x.rank().strength(), w = x == winner ? 1 : 0;
							dealt[n][k[0]][rank]++;
							won[n][k[0]][rank] += w;
							fineDealt[n][k[0]][rank][k[1]]++;
							fineWon[n][k[0]][rank][k[1]] += w;
						}
					}
				}
			}
		}
	}

	static void learn(int games) throws IOException {
		int sizes = RuleSet.MOST_CARDS + 1;
		double[][][] won = new double[sizes][2][13], dealt = new double[sizes][2][13];
		double[][][][] fineWon = new double[sizes][2][13][BidTable.CONTEXTS], fineDealt = new double[sizes][2][13][BidTable.CONTEXTS];
		RuleSet bigHands = RuleSet.STANDARD.withOrder(HandOrder.DOWN).withMostCards(RuleSet.MOST_CARDS);
		watch(RuleSet.STANDARD, 4, games, 900000, 1, won, dealt, fineWon, fineDealt);
		watch(bigHands, 4, games, 1900000, RuleSet.USUAL_CARDS + 1, won, dealt, fineWon, fineDealt);
		String src = TEMPLATE.replace("$GAMES", String.valueOf(games)).replace("$ROWS", rows(sizes, won, dealt, fineWon, fineDealt));
		File out = new File("core/src/main/java/ohheck/players/BidTable.java");
		FileWriter w = new FileWriter(out);
		w.write(src);
		w.close();
		System.out.println("Wrote " + out.getPath());
	}

	static void learnDuel(int games) throws IOException {
		int sizes = RuleSet.DUEL_HANDS + 1;
		double[][][] won = new double[sizes][2][13], dealt = new double[sizes][2][13];
		double[][][][] fineWon = new double[sizes][2][13][BidTable.CONTEXTS], fineDealt = new double[sizes][2][13][BidTable.CONTEXTS];
		watch(RuleSet.DUEL, 2, games, 2900000, 1, won, dealt, fineWon, fineDealt);
		String src = DUEL_TEMPLATE.replace("$GAMES", String.valueOf(games)).replace("$ROWS", rows(sizes, won, dealt, fineWon, fineDealt));
		File out = new File("core/src/main/java/ohheck/players/DuelBidTable.java");
		FileWriter w = new FileWriter(out);
		w.write(src);
		w.close();
		System.out.println("Wrote " + out.getPath());
	}

	static String rows(int sizes, double[][][] won, double[][][] dealt, double[][][][] fineWon, double[][][][] fineDealt) {
		StringBuilder rows = new StringBuilder();
		for (int n = 1; n < sizes; n++) {
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
		return rows.toString();
	}

	private static final String DUEL_TEMPLATE = "package ohheck.players;\n\n"
			+ "import ohheck.rules.Card;\n"
			+ "import ohheck.rules.TableView;\n\n"
			+ "/**\n"
			+ " * BidTable for duels: how often a card takes a trick in a two-player short-deck game, learned\n"
			+ " * from $GAMES simulated duels between Normal players, by the same rows and columns as BidTable.\n"
			+ " * <p>\n"
			+ " * Generated by {@code ./gradlew core:arena -Parena=\"learn-duel $GAMES\"}; don't edit the numbers by\n"
			+ " * hand. Regenerate (twice) after changing how the computer players play.\n"
			+ " */\n"
			+ "final class DuelBidTable {\n\n"
			+ "\tprivate DuelBidTable() {\n\t}\n\n"
			+ "\tstatic double chance(TableView v, Card c) {\n"
			+ "\t\tint row = ((v.handSize() - 1) * 2 + (c.suit() == v.trump() ? 1 : 0)) * 13 + c.rank().strength();\n"
			+ "\t\treturn CHANCE[row][BidTable.context(v, c)];\n"
			+ "\t}\n\n"
			+ "\tprivate static final double[][] CHANCE = {\n"
			+ "$ROWS"
			+ "\t};\n"
			+ "}\n";

	private static final String TEMPLATE = "package ohheck.players;\n\n"
			+ "import ohheck.rules.Card;\n"
			+ "import ohheck.rules.TableView;\n\n"
			+ "/**\n"
			+ " * How often a card takes a trick, learned from $GAMES simulated games between Normal\n"
			+ " * players (and as many games counting down from 13 cards, for hands of 9 and up): by hand\n"
			+ " * size, whether it's a trump, rank, how many cards of its suit its holder has (1, 2, 3 or\n"
			+ " * more), and the holder's place in the bidding.\n"
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
			+ "\t\tif (v.players() == 2) {\n"
			+ "\t\t\treturn DuelBidTable.chance(v, c);\n"
			+ "\t\t}\n"
			+ "\t\tint row = ((v.handSize() - 1) * 2 + (c.suit() == v.trump() ? 1 : 0)) * 13 + c.rank().strength();\n"
			+ "\t\treturn CHANCE[row][context(v, c)];\n"
			+ "\t}\n\n"
			+ "\t/** Which column: the card's suit length in the hand, and the viewer's place in the bidding. */\n"
			+ "\tstatic int context(TableView v, Card c) {\n"
			+ "\t\tint length = 0;\n"
			+ "\t\tfor (Card h : v.hand()) {\n"
			+ "\t\t\tif (h.suit() == c.suit()) {\n"
			+ "\t\t\t\tlength++;\n"
			+ "\t\t\t}\n"
			+ "\t\t}\n"
			+ "\t\tint place = ((v.seat() - v.dealer() - 1) % v.players() + v.players()) % v.players();\n"
			+ "\t\treturn (Math.max(1, Math.min(length, 3)) - 1) * 4 + Math.min(place, 3);\n"
			+ "\t}\n\n"
			+ "\t/** Rows: hand 1 side-suit 2s, 3s... aces, then hand 1 trumps, hand 2... Columns: see context(). */\n"
			+ "\tprivate static final double[][] CHANCE = {\n"
			+ "$ROWS"
			+ "\t};\n"
			+ "}\n";

	public static void main(String[] args) throws IOException {
		String what = args.length > 0 ? args[0] : "levels";
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 200;
		if (args.length > 2) {
			rules = RuleSet.parse(args[2]);
		}
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
		} else if (what.equals("duel")) {
			rules = args.length > 2 ? rules : RuleSet.DUEL;
			players = 2;
			report("Normal vs Easy", run(n, level(Difficulty.NORMAL), level(Difficulty.EASY)));
			report("Hard vs Normal", run(n / 4 + 1, level(Difficulty.HARD), level(Difficulty.NORMAL)));
			report("Hard vs Easy", run(n / 4 + 1, level(Difficulty.HARD), level(Difficulty.EASY)));
			for (final Player p : Player.ALL) {
				report(p.name + " (" + p.level.label + ", " + p.style.label + ") vs Normal", run(p.level == Difficulty.HARD ? n / 4 + 1 : n, new Maker() {
					@Override
					public Bot make(long seed) {
						return p.bot(new Random(seed));
					}
				}, level(Difficulty.NORMAL)));
			}
		} else if (what.equals("variants")) {
			variants(n);
		} else if (what.equals("learn-duel")) {
			learnDuel(n);
		} else if (what.equals("learn")) {
			learn(n);
		} else if (what.equals("players")) {
			for (final Player p : Player.ALL) {
				report(p.name + " (" + p.level.label + ", " + p.style.label + ") vs 3 Normal", run(n, new Maker() {
					@Override
					public Bot make(long seed) {
						return p.bot(new Random(seed));
					}
				}, level(Difficulty.NORMAL)));
			}
		} else if (what.equals("effort")) {
			for (double k : new double[] { 0.5, 1, 2 }) {
				final double effort = k;
				long t = System.currentTimeMillis();
				report("Hard (effort " + k + ") vs 3 Normal", run(n, new Maker() {
					@Override
					public Bot make(long seed) {
						return new HardBot(new Random(seed), HardBot.SPOIL, effort, true);
					}
				}, level(Difficulty.NORMAL)));
				System.out.printf("   %.1f s%n", (System.currentTimeMillis() - t) / 1000.0);
			}
		} else {
			System.out.println("Experiments: levels N | vs N | variants N | learn N | effort N | players N, then optionally a rules code");
		}
		System.out.printf("(%.1f s)%n", (System.currentTimeMillis() - start) / 1000.0);
	}
}
