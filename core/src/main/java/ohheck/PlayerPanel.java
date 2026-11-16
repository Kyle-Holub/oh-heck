package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * A player's bar, after the original game's: name, dealer icon and score, and
 * one card icon per trick: faded = bid but not won yet, solid = won, red =
 * won over the bid. Green while it's that player's turn.
 */
final class PlayerPanel {

	static final float W = 214, H = 80;

	private static final float ICON_MAX_W = 28, ICON_GAP = 3;
	private static final Color BG = new Color(0.04f, 0.06f, 0.2f, 0.78f);
	private static final Color TURN_BG = new Color(0f, 0.5f, 0.3f, 0.85f);
	private static final Color TURN_EDGE = new Color(0f, 0.96f, 0.5f, 1f);
	private static final Color BID_FADE = new Color(1f, 1f, 1f, 0.38f);

	private final BaseScreen screen;

	PlayerPanel(BaseScreen screen) {
		this.screen = screen;
	}

	/**
	 * @param bid  the player's bid this hand, or null if they haven't bid
	 * @param won  tricks taken this hand
	 */
	void draw(String name, float x, float top, boolean turn, boolean dealer, int score, Integer bid, int won,
			boolean dealing) {
		Assets assets = screen.assets;
		float y = top - H;
		if (turn) {
			screen.rect(x - 2, y - 2, W + 4, H + 4, TURN_EDGE);
		}
		screen.rect(x, y, W, H, turn ? TURN_BG : BG);

		// name, dealer icon and score
		BitmapFont f = screen.ui(1.3f);
		f.setColor(Color.WHITE);
		screen.glyphs.setText(f, name);
		float nameW = screen.glyphs.width;
		f.draw(screen.batch, name, x + 10, top - 9);
		if (dealer) {
			screen.batch.draw(assets.dealerIcon, x + 16 + nameW, top - 32, 24, 24);
		}
		f.setColor(BaseScreen.GOLD);
		screen.text(f, String.valueOf(score), x + W - 10, top - 9, false);

		float iy = y + 6;
		if (bid == null || Math.max(bid, won) == 0) {
			BitmapFont s = screen.ui(1f);
			s.setColor(Color.LIGHT_GRAY);
			String note = bid != null ? "bid 0" : dealing ? "" : turn ? "bidding..." : "no bid yet";
			s.draw(screen.batch, note, x + 10, iy + 20);
			return;
		}

		// as big as fits: 28 wide for a few tricks, shrinking to fit 8
		int icons = Math.max(bid, won);
		float iw = Math.min(ICON_MAX_W, (W - 20) / icons - ICON_GAP), ih = iw * 18f / 14f;
		for (int i = 0; i < icons; i++) {
			float ix = x + 10 + i * (iw + ICON_GAP);
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
