package ohheck.campaign;

/*
 * The games of a chapter, in order: its table, the final table where the champion sits in under
 * the duel's twist, and the duel. Each opens once the one before is won.
 */
public enum Stage {
	TABLE("tables", "The table"), FINAL("finals", "The final table"), DUEL("duels", "The duel");

	public final String key, title;

	Stage(String key, String title) {
		this.key = key;
		this.title = title;
	}
}
