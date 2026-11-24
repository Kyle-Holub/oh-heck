package ohheck.campaign;

/*
 * The games of a chapter, in order: its table, the final table where the champion sits in under
 * the duel's twist, and the duel. Each opens once the one before is won.
 */
public enum Stage {
	TABLE("tables"), FINAL("finals"), DUEL("duels");

	public final String key;

	Stage(String key) {
		this.key = key;
	}
}
