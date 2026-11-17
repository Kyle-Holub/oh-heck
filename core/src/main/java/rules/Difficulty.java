package rules;

/** How well the computer players play. */
public enum Difficulty {
	/** Sensible, but makes mistakes. */
	EASY("Easy"),
	/** Plays like a decent human: counts cards, trumps in, sheds danger. */
	NORMAL("Normal"),
	/** Looks ahead through many possible deals, plays for points and spoils the leader. */
	HARD("Hard");

	public final String label;

	Difficulty(String label) {
		this.label = label;
	}
}
