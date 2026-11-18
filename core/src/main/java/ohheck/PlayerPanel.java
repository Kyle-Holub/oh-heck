package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;

/**
 * A player's bar, after the original game's: name, dealer icon and score, and
 * one card icon per trick: faded = bid but not won yet, solid = won, red =
 * won over the bid; a bid of zero shows a struck-through circle until a trick is taken. Glows
 * gold while it's that player's turn; until they've bid, it sits sunk into the table (same size), dimmed,
 * with an hourglass.
 */
final class PlayerPanel {

	static final float W = 214, H = 80;

	private static final float ICON_MAX_W = 28, ICON_GAP = 3;

	/**
	 * The icons' width this hand: as big as fits the most any player can show (one per card
	 * dealt), up to 28, so every panel's icons are the same size.
	 */
	private static float iconWidth(int handSize) {
		return Math.min(ICON_MAX_W, (W - 20) / Math.max(1, handSize) - ICON_GAP);
	}
	private static final Color BID_FADE = new Color(1f, 1f, 1f, 0.32f);
	/** Not bid yet: the panel's text dimmed. */
	private static final Color WAIT_TEXT = new Color(0.72f, 0.66f, 0.58f, 1f), WAIT_SCORE = new Color(0.62f, 0.5f, 0.3f, 1f);

	private final BaseScreen screen;

	PlayerPanel(BaseScreen screen) {
		this.screen = screen;
	}

	/**
	 * @param bid  the player's bid this hand, or null if they haven't bid
	 * @param won  tricks taken this hand
	 */
	void draw(String name, float x, float top, boolean turn, boolean dealer, int score, Integer bid, int won, int handSize,
			boolean dealing, float time) {
		Assets assets = screen.assets;
		float y = top - H;
		boolean waiting = bid == null && !dealing;
		if (turn) {
			screen.frame(assets.panelTurn, x - 4, y - 4, W + 8, H + 8);
		} else if (waiting) {
			screen.frame(assets.panelWait, x, y, W, H);
		} else {
			screen.frame(assets.panel, x, y, W, H);
		}

		// name, dealer icon and score
		BitmapFont f = screen.ui(1.3f);
		f.setColor(turn ? Palette.GOLD_LIGHT : waiting ? WAIT_TEXT : Palette.CREAM);
		screen.glyphs.setText(f, name);
		float nameW = screen.glyphs.width, nameH = screen.glyphs.height;
		f.draw(screen.batch, name, x + 10, top - 9);
		if (dealer) {
			// centred on the name's letters
			float chip = 26, mid = top - 9 - nameH / 2;
			screen.batch.draw(assets.dealerIcon, x + 16 + nameW, mid - chip / 2, chip, chip);
		}
		f.setColor(waiting && !turn ? WAIT_SCORE : BaseScreen.GOLD);
		screen.text(f, String.valueOf(score), x + W - 10, top - 9, false);

		float iy = y + 6;
		if (bid == null) {
			if (waiting) {
				// an hourglass: dim while waiting their turn, pulsing while they decide
				float a = turn ? 0.75f + 0.25f * MathUtils.sin(time * 4f) : 0.55f;
				float iw = iconWidth(handSize), ih = iw * 18f / 14f;
				screen.batch.setColor(1f, 1f, 1f, a);
				screen.batch.draw(assets.waitingIcon, x + 10, iy, iw, ih);
				screen.batch.setColor(Color.WHITE);
			}
			return;
		}

		// a bid of zero shows its icon until a trick is taken; then the red over-the-bid icons replace it
		int tricks = Math.max(bid, won), first = bid == 0 && won == 0 ? 1 : 0;
		float iw = iconWidth(handSize), ih = iw * 18f / 14f;
		if (first == 1) {
			screen.batch.draw(assets.zeroIcon, x + 10, iy, iw, ih);
		}
		for (int i = 0; i < tricks; i++) {
			float ix = x + 10 + (first + i) * (iw + ICON_GAP);
			if (i < Math.min(bid, won)) {
				screen.batch.draw(assets.trickIcon, ix, iy, iw, ih);
			} else if (i < bid) {
				screen.batch.setColor(BID_FADE);
				screen.batch.draw(assets.trickIcon, ix, iy, iw, ih);
				screen.batch.setColor(Color.WHITE);
			} else {
				screen.batch.draw(assets.overIcon, ix, iy, iw, ih);
			}
		}
	}
}
