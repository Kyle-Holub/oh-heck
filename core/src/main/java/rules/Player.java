package rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One of the twelve computer players you can meet at the table: a name, how well they play,
 * and a habit. Who sits down is drawn at the start of each game, the difficulty setting
 * deciding how likely each seat is to go to an easy, normal or expert player.
 */
public final class Player {

	public final int id;
	public final String name;
	public final Difficulty level;
	public final Style style;
	/** A line about them, for the Players screen. */
	public final String trait;

	private Player(int id, String name, Difficulty level, Style style, String trait) {
		this.id = id;
		this.name = name;
		this.level = level;
		this.style = style;
		this.trait = trait;
	}

	/** Everyone (look a player up with {@link #byId(int)}, not by place in this list). */
	public static final List<Player> ALL = Collections.unmodifiableList(Arrays.asList(
			new Player(0, "Edmund", Difficulty.EASY, Style.TIMID,
					"Never trusts his cards: often bids a trick fewer than they're worth."),
			new Player(1, "Barnaby", Difficulty.EASY, Style.BOLD,
					"Sure every hand's a winner, and often bids a trick more than it holds."),
			new Player(2, "Nathaniel", Difficulty.EASY, Style.ACE_LEADER,
					"Can't sit on a good card: leads his aces and kings at once, wanted or not."),
			new Player(3, "Wilfred", Difficulty.EASY, Style.GAMBLER,
					"Bids on a whim: a trick up or down from his cards, as the mood takes him."),
			new Player(4, "Rupert", Difficulty.NORMAL, Style.STEADY,
					"Counts the cards and bids what they say. Hard to rattle."),
			new Player(5, "Phillip", Difficulty.NORMAL, Style.TRUMP_HAPPY,
					"Leads trumps early and often, to draw everyone else's."),
			new Player(6, "Cornelius", Difficulty.NORMAL, Style.FLUFFER,
					"Plays a canny zero game: a hand worth about one trick, he bids nothing."),
			new Player(7, "Oswald", Difficulty.NORMAL, Style.STREAKY,
					"Plays his luck: bolder after making a bid, warier after a miss."),
			new Player(8, "Horatio", Difficulty.HARD, Style.STEADY,
					"Reads every bid, thinks the hand through and rarely misses."),
			new Player(9, "Virgil", Difficulty.HARD, Style.SPOILER,
					"An expert who'd rather sink the leader than pad his own score."),
			new Player(10, "Mortimer", Difficulty.HARD, Style.TRUMP_HOARDER,
					"Expert play, and a habit of keeping his trumps for the last tricks."),
			new Player(11, "Percival", Difficulty.HARD, Style.FRONT_RUNNER,
					"An expert who plays it safe in front and takes his chances from behind.")));

	/**
	 * The player with this id, or null if there's none (a player since removed). Ids are
	 * permanent: saves and statistics refer to players by id, so never renumber or reuse one;
	 * a new player takes the next unused id, and the list's order doesn't matter.
	 */
	public static Player byId(int id) {
		for (Player p : ALL) {
			if (p.id == id) {
				return p;
			}
		}
		return null;
	}

	/** One more than the highest id in use (for per-player counts). */
	public static int idLimit() {
		int max = -1;
		for (Player p : ALL) {
			max = Math.max(max, p.id);
		}
		return max + 1;
	}

	/** How much more a spoiler weighs the leader's points than other experts do. */
	static final double SPOILER_SPOIL = 1.2;

	/** This player's bot; rng drives its randomness. */
	public Bot bot(Random rng) {
		Bot base = style == Style.SPOILER && level == Difficulty.HARD ? new HardBot(rng, SPOILER_SPOIL)
				: Bot.of(level, rng);
		return style == Style.STEADY || style == Style.SPOILER ? base : new StyledBot(base, style, level, rng);
	}

	/**
	 * The chance each seat goes to an easy, normal or expert player, by the difficulty
	 * setting: Easy never seats an expert, Hard never a beginner, Normal a mix of all three.
	 */
	static final double[][] ODDS = { { 0.65, 0.35, 0 }, { 0.25, 0.5, 0.25 }, { 0, 0.35, 0.65 } };

	/** Three different players for a new game at this setting (their ids, for seats 1-3). */
	public static int[] draw(Difficulty setting, Random rng) {
		List<Integer> chosen = new ArrayList<Integer>();
		while (chosen.size() < 3) {
			double r = rng.nextDouble();
			double[] odds = ODDS[setting.ordinal()];
			int level = r < odds[0] ? 0 : r < odds[0] + odds[1] ? 1 : 2;
			List<Integer> free = new ArrayList<Integer>();
			for (Player p : ALL) {
				if (p.level.ordinal() == level && !chosen.contains(p.id)) {
					free.add(p.id);
				}
			}
			if (!free.isEmpty()) {
				chosen.add(free.get(rng.nextInt(free.size())));
			}
		}
		return new int[] { chosen.get(0), chosen.get(1), chosen.get(2) };
	}
}
