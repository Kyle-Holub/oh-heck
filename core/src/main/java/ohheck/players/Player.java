package ohheck.players;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/*
 * One of the twelve computer players. Ids are permanent, since saves and statistics refer to
 * them: never renumber or reuse one.
 */
public final class Player {

	public final int id;
	public final String name;
	public final Difficulty level;
	public final Style style;
	public final String trait;

	private Player(int id, String name, Difficulty level, Style style, String trait) {
		this.id = id;
		this.name = name;
		this.level = level;
		this.style = style;
		this.trait = trait;
	}

	public static final List<Player> ALL = Collections.unmodifiableList(Arrays.asList(
			new Player(0, "Edmund", Difficulty.EASY, Style.TIMID,
					"Never trusts his cards: often bids a trick fewer than they're worth."),
			new Player(1, "Barnaby", Difficulty.EASY, Style.BOLD,
					"Sure every hand's a winner, and often bids a trick more than it holds."),
			new Player(2, "Nathaniel", Difficulty.EASY, Style.TRUMP_COUNTER,
					"Counts a trick for every trump and every ace, and bids that."),
			new Player(3, "Horatio", Difficulty.EASY, Style.SPENDTHRIFT,
					"Can't wait to win: takes his tricks with his biggest cards, then runs dry."),
			new Player(4, "Rupert", Difficulty.NORMAL, Style.STEADY,
					"Counts the cards and bids what they say. Hard to rattle."),
			new Player(5, "Phillip", Difficulty.NORMAL, Style.GREEDY,
					"Never leaves points on the table: between two close bids, he takes the higher."),
			new Player(6, "Cornelius", Difficulty.NORMAL, Style.FLUFFER,
					"Plays a canny zero game: a hand worth about one trick, he bids nothing."),
			new Player(7, "Oswald", Difficulty.NORMAL, Style.STREAKY,
					"Plays his luck: bolder after making a bid, warier after a miss."),
			new Player(8, "Wilfred", Difficulty.HARD, Style.THINKER,
					"Reads every bid, thinks the hand through and rarely misses."),
			new Player(9, "Virgil", Difficulty.HARD, Style.SPOILER,
					"An expert who'd rather sink the leader than pad his own score."),
			new Player(10, "Mortimer", Difficulty.HARD, Style.VOID_MAKER,
					"An expert who throws away his short suits early, to trump them later."),
			new Player(11, "Percival", Difficulty.HARD, Style.FRONT_RUNNER,
					"An expert who plays it safe in front and takes his chances from behind.")));

	public static Player byId(int id) {
		for (Player p : ALL) {
			if (p.id == id) {
				return p;
			}
		}
		return null;
	}

	public static int idLimit() {
		int max = -1;
		for (Player p : ALL) {
			max = Math.max(max, p.id);
		}
		return max + 1;
	}

	public Bot bot(Random rng) {
		return style.bot(level, rng);
	}

	private static Difficulty pick(Difficulty setting, int seat, double r) {
		Difficulty[] levels = Difficulty.values();
		double sum = 0;
		for (Difficulty l : levels) {
			sum += setting.chanceAt(seat, l);
			if (r < sum) {
				return l;
			}
		}
		return levels[levels.length - 1];
	}

	public static Seating draw(Difficulty setting, Random rng) {
		List<Integer> chosen = new ArrayList<Integer>();
		while (chosen.size() < setting.tableSeats()) {
			Difficulty level = pick(setting, chosen.size(), rng.nextDouble());
			List<Integer> free = new ArrayList<Integer>();
			for (Player p : ALL) {
				if (p.level == level && !chosen.contains(p.id)) {
					free.add(p.id);
				}
			}
			if (!free.isEmpty()) {
				chosen.add(free.get(rng.nextInt(free.size())));
			}
		}
		Collections.shuffle(chosen, rng);
		int[] ids = new int[chosen.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = chosen.get(i);
		}
		return Seating.of(ids);
	}
}
