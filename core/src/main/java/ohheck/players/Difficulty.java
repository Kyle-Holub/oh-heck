package ohheck.players;

import java.util.Random;
import java.util.function.Function;

/**
 * A level of play. It's both how well a computer player plays (each player has one) and
 * the setting you choose in Options, which decides who you're likely to meet. Everything
 * that differs by level lives here, so a new level is one more constant.
 */
public enum Difficulty {
	/** Sensible, but makes mistakes. */
	EASY("Easy", EasyBot::new, new double[] { 0.65, 0.35, 0 }, 0.6, 0.8,
			"easy-going players", "easy-going player", "a beginner"),
	/** Plays like a decent human: counts cards, trumps in, sheds danger. */
	NORMAL("Normal", rng -> new NormalBot(), new double[] { 0.25, 0.5, 0.25 }, 0.2, 0.6,
			"solid players", "solid player", "a solid player"),
	/** Looks ahead through many possible deals, plays for points and spoils the leader. */
	HARD("Hard", HardBot::new, new double[] { 0, 0.35, 0.65 }, 0.12, 0.4,
			"experts", "expert", "an expert");

	public final String label;
	private final Function<Random, Bot> bot;
	/**
	 * As the Options setting: the chance a seat goes to an easy, normal or expert player
	 * (indexed by their level). Easy never seats an expert, Hard never a beginner.
	 */
	private final double[] seatOdds;
	/** As a player's level: how often a bidding habit wins out over the cards (measured in the arena's "players"). */
	final double styleLean;
	/** How often a playing habit wins out: a habit you can see, more so in weaker players. */
	final double playHabit;
	/** Players at this level, in the Options note: "some experts", "the odd expert", "never an expert". */
	final String many, oddOne, never;

	Difficulty(String label, Function<Random, Bot> bot, double[] seatOdds, double styleLean, double playHabit,
			String many, String oddOne, String never) {
		this.many = many;
		this.oddOne = oddOne;
		this.never = never;
		this.label = label;
		this.bot = bot;
		this.seatOdds = seatOdds;
		this.styleLean = styleLean;
		this.playHabit = playHabit;
	}

	/** A plain computer player at this level; rng drives any randomness (seed it for repeatable games). */
	public Bot bot(Random rng) {
		return bot.apply(rng);
	}

	/** As the Options setting: the chance a seat goes to a player of this level. */
	public double chanceOf(Difficulty playerLevel) {
		return seatOdds[playerLevel.ordinal()];
	}

	/** A short lowercase name ("easy"), for saved keys and image names. */
	public String key() {
		return name().toLowerCase();
	}

	/**
	 * A level read back from saved settings: by name, or (from older builds) by its
	 * ordinal; Normal if neither reads.
	 */
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
