package ohheck.players;

import java.util.Random;
import java.util.function.Function;

/**
 * A level of play: a player's skill, and as the Options setting, the odds of meeting each
 * level. A new level is one more constant.
 */
public enum Difficulty {
	EASY("Easy", EasyBot::new, new double[] { 0.65, 0.35, 0 }, 0.6, 0.8),
	NORMAL("Normal", rng -> new NormalBot(), new double[] { 0.25, 0.5, 0.25 }, 0.2, 0.6),
	HARD("Hard", HardBot::new, new double[] { 0, 0.35, 0.65 }, 0.12, 0.4);

	public final String label;
	private final Function<Random, Bot> bot;
	private final double[] seatOdds;
	final double styleLean;
	final double playHabit;

	Difficulty(String label, Function<Random, Bot> bot, double[] seatOdds, double styleLean, double playHabit) {
		this.label = label;
		this.bot = bot;
		this.seatOdds = seatOdds;
		this.styleLean = styleLean;
		this.playHabit = playHabit;
	}

	public Bot bot(Random rng) {
		return bot.apply(rng);
	}

	public double chanceOf(Difficulty playerLevel) {
		return seatOdds[playerLevel.ordinal()];
	}

	public String key() {
		return name().toLowerCase();
	}

	public static Difficulty fromSaved(String name, int legacyOrdinal) {
		if (name != null) {
			for (Difficulty d : values()) {
				if (d.name().equals(name)) {
					return d;
				}
			}
		}
		return legacyOrdinal >= 0 && legacyOrdinal < values().length ? values()[legacyOrdinal] : NORMAL;
	}
}
