package ohheck.rules;

/* A game's rules by name: three fixed sets, and Custom, which plays whatever rules the player chose. */
public enum GameMode {
	CLASSIC("Classic", RuleSet.STANDARD),
	HIGH_STAKES("High Stakes", RuleSet.STANDARD.withBigBids(true)),
	POKER_FACE("Poker Face", RuleSet.STANDARD.withBlindBids(true)),
	MARATHON("Marathon", RuleSet.STANDARD.withOrder(HandOrder.UP_AND_DOWN).withMostCards(RuleSet.MOST_CARDS)),
	CUSTOM("Custom", null);

	public final String label;
	private final RuleSet fixed;

	GameMode(String label, RuleSet fixed) {
		this.label = label;
		this.fixed = fixed;
	}

	public RuleSet rules(RuleSet custom) {
		return fixed != null ? fixed : custom;
	}

	public GameMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public static GameMode of(RuleSet rules) {
		for (GameMode m : values()) {
			if (rules.equals(m.fixed)) {
				return m;
			}
		}
		return CUSTOM;
	}

	public static GameMode named(String name) {
		for (GameMode m : values()) {
			if (m.name().equals(name)) {
				return m;
			}
		}
		return CLASSIC;
	}
}
