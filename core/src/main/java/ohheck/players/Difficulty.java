package ohheck.players;

import java.util.Random;
import java.util.function.Function;

/**
 * A level of play: a player's skill, and as the Options setting, the odds of meeting each
 * level. A new level is one more constant.
 */
public enum Difficulty {
	EASY("Easy", EasyBot::new, meets(0.65, 0.35, 0), leans(0.6, 0.8)),
	NORMAL("Normal", rng -> new NormalBot(), meets(0.25, 0.5, 0.25), leans(0.2, 0.6)),
	HARD("Hard", HardBot::new, meets(0, 0.35, 0.65), leans(0.12, 0.4));

	public final String label;
	private final Function<Random, Bot> bot;
	private final double[] chanceOfMeeting;
	final double bidLean, playLean;

	Difficulty(String label, Function<Random, Bot> bot, double[] chanceOfMeeting, double[] leans) {
		this.label = label;
		this.bot = bot;
		this.chanceOfMeeting = chanceOfMeeting;
		this.bidLean = leans[0];
		this.playLean = leans[1];
	}

	private static double[] meets(double easy, double normal, double hard) {
		return new double[] { easy, normal, hard };
	}

	private static double[] leans(double bid, double play) {
		return new double[] { bid, play };
	}

	public Bot bot(Random rng) {
		return bot.apply(rng);
	}

	public double chanceOf(Difficulty playerLevel) {
		return chanceOfMeeting[playerLevel.ordinal()];
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
