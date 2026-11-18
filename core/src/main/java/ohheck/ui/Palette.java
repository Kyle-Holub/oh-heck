package ohheck.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * The game's colours: espresso darks, cream, oxblood red and gold, matching
 * the art drawn by tools/make_art.py.
 */
public final class Palette {

	private Palette() {
	}

	/** Behind everything: near-black espresso. */
	public static final Color BG = new Color(0.055f, 0.035f, 0.024f, 1f);
	/** Headings, scores, highlights. */
	public static final Color GOLD = new Color(0.89f, 0.72f, 0.37f, 1f);
	public static final Color GOLD_LIGHT = new Color(0.97f, 0.85f, 0.56f, 1f);
	/** Normal text. */
	public static final Color CREAM = new Color(0.95f, 0.91f, 0.83f, 1f);
	/** Secondary text. */
	public static final Color MUTED = new Color(0.68f, 0.6f, 0.49f, 1f);
	/** "Set!", missed bids. */
	public static final Color RED = new Color(0.9f, 0.42f, 0.36f, 1f);
	/** Tint for cards you can't play. */
	public static final Color CARD_DIM = new Color(0.5f, 0.46f, 0.42f, 1f);
	/** Dims the screen behind the pause menu. */
	public static final Color BACKDROP = new Color(0.03f, 0.02f, 0.01f, 0.8f);
	/** Darkens the falling cards behind the title menu. */
	public static final Color SHADE = new Color(0.03f, 0.02f, 0.01f, 0.35f);
}
