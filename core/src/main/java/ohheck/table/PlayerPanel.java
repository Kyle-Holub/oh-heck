package ohheck.table;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;

import ohheck.ui.Assets;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/* A player's placard: name, score, dealer mark, and the bid and tricks. */
public final class PlayerPanel {

	public static final float W = 214, H = 80;

	private static final float ICON_MAX_W = 28, ICON_GAP = 3;

	private static float iconWidth(int handSize) {
		return Math.min(ICON_MAX_W, (W - 20) / Math.max(1, handSize) - ICON_GAP);
	}
	private static final Color BID_FADE = new Color(1f, 1f, 1f, 0.32f);
	private static final Color WAIT_TEXT = new Color(0.72f, 0.66f, 0.58f, 1f), WAIT_SCORE = new Color(0.62f, 0.5f, 0.3f, 1f);

	private final Painter paint;

	public PlayerPanel(Painter paint) {
		this.paint = paint;
	}

	public void draw(SeatState seat, float x, float top, float time) {
		Assets assets = paint.assets;
		float y = top - H;
		if (seat.turn) {
			paint.frame(assets.panelTurn, x - 4, y - 4, W + 8, H + 8);
		} else if (seat.waiting()) {
			paint.frame(assets.panelWait, x, y, W, H);
		} else {
			paint.frame(assets.panel, x, y, W, H);
		}
		drawNameAndScore(seat, x, top);
		float iy = y + 6;
		if (seat.hasBid()) {
			drawTricks(seat, x + 10, iy);
		} else if (seat.waiting()) {
			drawHourglass(seat, x + 10, iy, time);
		}
	}

	private void drawNameAndScore(SeatState seat, float x, float top) {
		boolean waiting = seat.waiting();
		BitmapFont f = paint.ui(1.3f, seat.turn ? Palette.GOLD_LIGHT : waiting ? WAIT_TEXT : Palette.CREAM);
		paint.glyphs.setText(f, seat.name);
		float nameW = paint.glyphs.width, nameH = paint.glyphs.height;
		f.draw(paint.batch, seat.name, x + 10, top - 9);
		if (seat.dealer) {
			float chip = 26, mid = top - 9 - nameH / 2;
			paint.batch.draw(paint.assets.dealerIcon, x + 16 + nameW, mid - chip / 2, chip, chip);
		}
		f.setColor(waiting && !seat.turn ? WAIT_SCORE : Palette.GOLD);
		paint.text(f, Numbers.text(seat.score), x + W - 10, top - 9, false);
	}

	private void drawHourglass(SeatState seat, float x, float y, float time) {
		float a = seat.turn ? 0.75f + 0.25f * MathUtils.sin(time * 4f) : 0.55f;
		float iw = iconWidth(seat.handSize), ih = iw * 18f / 14f;
		paint.batch.setColor(1f, 1f, 1f, a);
		paint.batch.draw(paint.assets.waitingIcon, x, y, iw, ih);
		paint.batch.setColor(Color.WHITE);
	}

	private void drawTricks(SeatState seat, float x, float y) {
		Assets assets = paint.assets;
		int bid = seat.bid, won = seat.won;
		int tricks = Math.max(bid, won), first = bid == 0 && won == 0 ? 1 : 0;
		float iw = iconWidth(seat.handSize), ih = iw * 18f / 14f;
		if (first == 1) {
			paint.batch.draw(assets.zeroIcon, x, y, iw, ih);
		}
		for (int i = 0; i < tricks; i++) {
			float ix = x + (first + i) * (iw + ICON_GAP);
			if (i < Math.min(bid, won)) {
				paint.batch.draw(assets.trickIcon, ix, y, iw, ih);
			} else if (i < bid) {
				paint.batch.setColor(BID_FADE);
				paint.batch.draw(assets.trickIcon, ix, y, iw, ih);
				paint.batch.setColor(Color.WHITE);
			} else {
				paint.batch.draw(assets.overIcon, ix, y, iw, ih);
			}
		}
	}
}
