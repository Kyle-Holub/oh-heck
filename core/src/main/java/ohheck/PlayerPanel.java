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

	/** Draws the placard showing seat, its top-left corner at (x, top); time pulses the hourglass. */
	void draw(SeatState seat, float x, float top, float time) {
		Assets assets = screen.assets;
		float y = top - H;
		if (seat.turn) {
			screen.frame(assets.panelTurn, x - 4, y - 4, W + 8, H + 8);
		} else if (seat.waiting()) {
			screen.frame(assets.panelWait, x, y, W, H);
		} else {
			screen.frame(assets.panel, x, y, W, H);
		}
		drawNameAndScore(seat, x, top);
		float iy = y + 6;
		if (seat.hasBid()) {
			drawTricks(seat, x + 10, iy);
		} else if (seat.waiting()) {
			drawHourglass(seat, x + 10, iy, time);
		}
	}

	/** The name (with the dealer's chip after it) on the left, the score on the right. */
	private void drawNameAndScore(SeatState seat, float x, float top) {
		boolean waiting = seat.waiting();
		BitmapFont f = screen.ui(1.3f);
		f.setColor(seat.turn ? Palette.GOLD_LIGHT : waiting ? WAIT_TEXT : Palette.CREAM);
		screen.glyphs.setText(f, seat.name);
		float nameW = screen.glyphs.width, nameH = screen.glyphs.height;
		f.draw(screen.batch, seat.name, x + 10, top - 9);
		if (seat.dealer) {
			// centred on the name's letters
			float chip = 26, mid = top - 9 - nameH / 2;
			screen.batch.draw(screen.assets.dealerIcon, x + 16 + nameW, mid - chip / 2, chip, chip);
		}
		f.setColor(waiting && !seat.turn ? WAIT_SCORE : BaseScreen.GOLD);
		screen.text(f, String.valueOf(seat.score), x + W - 10, top - 9, false);
	}

	/** Yet to bid: dim while waiting their turn, pulsing while they decide. */
	private void drawHourglass(SeatState seat, float x, float y, float time) {
		float a = seat.turn ? 0.75f + 0.25f * MathUtils.sin(time * 4f) : 0.55f;
		float iw = iconWidth(seat.handSize), ih = iw * 18f / 14f;
		screen.batch.setColor(1f, 1f, 1f, a);
		screen.batch.draw(screen.assets.waitingIcon, x, y, iw, ih);
		screen.batch.setColor(Color.WHITE);
	}

	/**
	 * One icon per trick: faded = bid, not won yet; solid = won; red = over the bid. A bid of
	 * zero shows its icon until a trick is taken, when the red over-the-bid icons replace it.
	 */
	private void drawTricks(SeatState seat, float x, float y) {
		Assets assets = screen.assets;
		int bid = seat.bid, won = seat.won;
		int tricks = Math.max(bid, won), first = bid == 0 && won == 0 ? 1 : 0;
		float iw = iconWidth(seat.handSize), ih = iw * 18f / 14f;
		if (first == 1) {
			screen.batch.draw(assets.zeroIcon, x, y, iw, ih);
		}
		for (int i = 0; i < tricks; i++) {
			float ix = x + (first + i) * (iw + ICON_GAP);
			if (i < Math.min(bid, won)) {
				screen.batch.draw(assets.trickIcon, ix, y, iw, ih);
			} else if (i < bid) {
				screen.batch.setColor(BID_FADE);
				screen.batch.draw(assets.trickIcon, ix, y, iw, ih);
				screen.batch.setColor(Color.WHITE);
			} else {
				screen.batch.draw(assets.overIcon, ix, y, iw, ih);
			}
		}
	}

}
