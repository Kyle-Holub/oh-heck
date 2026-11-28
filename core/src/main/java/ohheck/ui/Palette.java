package ohheck.ui;

import com.badlogic.gdx.graphics.Color;

/*
 * The game's colours. SPARKLE is the one that changes: the dressed felt's sparkle colour, which every spark,
 * firework, mote and flare is shaded from (sparkle: deeper as power rises above 1, paler as toWhite rises).
 */
public final class Palette {

	private Palette() {
	}

	public static final Color EMBER = new Color(1f, 0.8f, 0.4f, 1f), LAMP = new Color(1f, 0.92f, 0.7f, 1f);
	public static final Color MAP_INK = new Color(0.27f, 0.16f, 0.09f, 1f), MAP_INK_FAINT = new Color(0.27f, 0.16f, 0.09f, 0.45f);
	public static final Color FLASH = new Color(1f, 0.9f, 0.7f, 0.4f);

	public static final Color SPARKLE = new Color(1f, 0.72f, 0.31f, 1f);

	public static void sparkle(float power, float toWhite, float[] rgb) {
		float r = (float) Math.pow(SPARKLE.r, power), g = (float) Math.pow(SPARKLE.g, power), b = (float) Math.pow(SPARKLE.b, power);
		float top = Math.max(r, Math.max(g, Math.max(b, 0.001f)));
		rgb[0] = r / top + (1 - r / top) * toWhite;
		rgb[1] = g / top + (1 - g / top) * toWhite;
		rgb[2] = b / top + (1 - b / top) * toWhite;
	}

	public static final Color BG = new Color(0.055f, 0.035f, 0.024f, 1f);
	public static final Color GOLD = new Color(0.89f, 0.72f, 0.37f, 1f);
	public static final Color GOLD_LIGHT = new Color(0.97f, 0.85f, 0.56f, 1f);
	public static final Color CREAM = new Color(0.95f, 0.91f, 0.83f, 1f);
	public static final Color MUTED = new Color(0.68f, 0.6f, 0.49f, 1f);
	public static final Color RED = new Color(0.9f, 0.42f, 0.36f, 1f);
	public static final Color CARD_DIM = new Color(0.5f, 0.46f, 0.42f, 1f);
	public static final Color BACKDROP = new Color(0.03f, 0.02f, 0.01f, 0.8f);
	public static final Color SHADE = new Color(0.03f, 0.02f, 0.01f, 0.35f);
}
