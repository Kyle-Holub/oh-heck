package ohheck.players;

import java.util.Random;
import java.util.function.BiFunction;

/*
 * A level of play: a player's skill, and as the Options setting, who sits at your table (each
 * computer seat's chance of each level). A new level is one more constant.
 */
public enum Difficulty {
	EASY("Easy", EasyBot::new, table(meets(1, 0, 0), meets(1, 0, 0), meets(0.6, 0.4, 0)), leans(0.6, 0.8)),
	NORMAL("Normal", (rng, poised) -> new NormalBot(), table(meets(0, 1, 0), meets(0, 1, 0), meets(0.2, 0.6, 0.2)), leans(0.2, 0.6)),
	HARD("Hard", (rng, poised) -> new HardBot(rng), table(meets(0, 0, 1), meets(0, 0, 1), meets(0, 0, 1)), leans(0.12, 0.4));

	public final String label;
	private final BiFunction<Random, Boolean, Bot> bot;
	private final double[][] seats;
	final double bidLean, playLean;

	Difficulty(String label, BiFunction<Random, Boolean, Bot> bot, double[][] seats, double[] leans) {
		this.label = label;
		this.bot = bot;
		this.seats = seats;
		this.bidLean = leans[0];
		this.playLean = leans[1];
	}

	private static double[] meets(double easy, double normal, double hard) {
		return new double[] { easy, normal, hard };
	}

	private static double[][] table(double[]... seats) {
		return seats;
	}

	private static double[] leans(double bid, double play) {
		return new double[] { bid, play };
	}

	public Bot bot(Random rng) {
		return bot(rng, false);
	}

	Bot bot(Random rng, boolean poised) {
		return bot.apply(rng, poised);
	}

	public int tableSeats() {
		return seats.length;
	}

	public double chanceAt(int seat, Difficulty playerLevel) {
		return seats[seat][playerLevel.ordinal()];
	}

	public String key() {
		return name().toLowerCase();
	}

	public static Difficulty named(String name) {
		for (Difficulty d : values()) {
			if (d.name().equals(name)) {
				return d;
			}
		}
		return NORMAL;
	}
}
