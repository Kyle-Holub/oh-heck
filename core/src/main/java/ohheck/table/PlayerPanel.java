package ohheck.table;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import ohheck.rules.RuleSet;
import ohheck.ui.Assets;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/*
 * A player's placard: name, score, dealer mark, and the bid and tricks. Your placard breathes once when your
 * turn comes; when a hand is scored your bid's icons flash gold if you made it, or crack and dim if you were set.
 */
public final class PlayerPanel {

	public static final float W = 214, H = 80;

	public static final float ICON_GAP = 3, ICON_ASPECT = 18f / 14f;
	private static final float ICON_MAX_W = 28, ICON_ROOM_H = 42;

	private static int rows(int handSize) {
		return handSize > RuleSet.USUAL_CARDS ? 2 : 1;
	}

	private static int perRow(int handSize) {
		return (Math.max(1, handSize) + rows(handSize) - 1) / rows(handSize);
	}

	public static float iconWidth(int handSize) {
		int rows = rows(handSize);
		float fitsRow = (W - 20) / perRow(handSize) - ICON_GAP;
		float fitsRows = ((ICON_ROOM_H - (rows - 1) * ICON_GAP) / rows) / ICON_ASPECT;
		return Math.min(ICON_MAX_W, Math.min(fitsRow, fitsRows));
	}
	private static final String DOUBLED = "x2";
	private static final Color BID_FADE = new Color(1f, 1f, 1f, 0.32f);
	private static final Color WAIT_TEXT = new Color(0.72f, 0.66f, 0.58f, 1f), WAIT_SCORE = new Color(0.62f, 0.5f, 0.3f, 1f);

	private final Painter paint;
	private final TextureRegion line;

	public PlayerPanel(Painter paint) {
		this.paint = paint;
		this.line = new TextureRegion(paint.assets.white);
	}

	public void draw(SeatState seat, float x, float top, float time) {
		Assets assets = paint.assets;
		float y = top - H;
		if (seat.turn) {
			paint.frame(assets.panelTurn, x - 4, y - 4, W + 8, H + 8);
			if (seat.pulse > 0) {
				float grow = 4 + PULSE_GROW * seat.pulse;
				paint.fade(seat.pulse);
				paint.frame(assets.panelTurn, x - grow, y - grow, W + 2 * grow, H + 2 * grow);
				paint.fade(1f);
			}
		} else if (seat.waiting()) {
			paint.frame(assets.panelWait, x, y, W, H);
		} else {
			paint.frame(assets.panel, x, y, W, H);
		}
		drawNameAndScore(seat, x, top);
		float iy = y + 6;
		if (seat.hasBid()) {
			if (seat.cracked) {
				paint.batch.setColor(Palette.CARD_DIM);
			}
			drawTricks(seat, x + 10, iy);
			paint.batch.setColor(Color.WHITE);
			if (seat.gilt > 0) {
				gild(seat, x + 10, iy);
			}
			if (seat.cracked) {
				crack(seat, x + 10, iy);
			}
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
		if (!seat.scored) {
			return;
		}
		f.setColor(waiting && !seat.turn ? WAIT_SCORE : Palette.GOLD);
		String score = Numbers.text(seat.score);
		paint.text(f, score, x + W - 10, top - 9, false);
		if (seat.doubled) {
			paint.glyphs.setText(f, score);
			paint.text(paint.ui(1.0f, Palette.GOLD_LIGHT), DOUBLED, x + W - 18 - paint.glyphs.width, top - 11, false);
		}
	}

	private static final float PULSE_GROW = 10, CRACK_W = 2.5f;
	private static final Color CRACK = new Color(0.08f, 0.05f, 0.03f, 0.9f);

	private float iconsWidth(SeatState seat) {
		int shown = Math.max(1, Math.max(seat.bid, seat.won));
		float iw = iconWidth(seat.handSize);
		return Math.min(shown, perRow(seat.handSize)) * (iw + ICON_GAP) - ICON_GAP;
	}

	private void gild(SeatState seat, float x, float y) {
		float iw = iconWidth(seat.handSize), ih = iw * ICON_ASPECT * rows(seat.handSize), w = iconsWidth(seat);
		paint.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		paint.batch.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, 0.9f * seat.gilt);
		paint.batch.draw(paint.assets.glow, x - iw, y - ih / 2, w + 2 * iw, ih * 2);
		paint.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		paint.batch.setColor(Color.WHITE);
	}

	private void crack(SeatState seat, float x, float y) {
		float ih = iconWidth(seat.handSize) * ICON_ASPECT, w = iconsWidth(seat);
		float[] xs = { 0, 0.22f, 0.4f, 0.63f, 0.8f, 1 }, ys = { 0.85f, 0.45f, 0.7f, 0.25f, 0.55f, 0.1f };
		for (int i = 0; i + 1 < xs.length; i++) {
			float ax = x + xs[i] * w, ay = y + ys[i] * ih, bx = x + xs[i + 1] * w, by = y + ys[i + 1] * ih;
			float len = (float) Math.hypot(bx - ax, by - ay), angle = MathUtils.atan2(by - ay, bx - ax) * MathUtils.radiansToDegrees;
			paint.batch.setColor(CRACK);
			paint.batch.draw(line, ax, ay - CRACK_W / 2, 0, CRACK_W / 2, len, CRACK_W, 1, 1, angle);
		}
		paint.batch.setColor(Color.WHITE);
	}

	private void drawHourglass(SeatState seat, float x, float y, float time) {
		float a = seat.turn ? 0.75f + 0.25f * MathUtils.sin(time * 4f) : 0.55f;
		float iw = iconWidth(seat.handSize), ih = iw * ICON_ASPECT;
		paint.batch.setColor(1f, 1f, 1f, a);
		paint.batch.draw(paint.assets.waitingIcon, x, y, iw, ih);
		paint.batch.setColor(Color.WHITE);
	}

	public void drawTricks(SeatState seat, float x, float y) {
		Assets assets = paint.assets;
		int bid = seat.bid, won = seat.won;
		int tricks = Math.max(bid, won), first = bid == 0 && won == 0 ? 1 : 0;
		float iw = iconWidth(seat.handSize), ih = iw * ICON_ASPECT;
		int rows = rows(seat.handSize), perRow = perRow(seat.handSize);
		float topRow = y + (rows - 1) * (ih + ICON_GAP);
		if (first == 1) {
			paint.batch.draw(assets.zeroIcon, x, topRow, iw, ih);
		}
		for (int i = 0; i < tricks; i++) {
			int k = first + i;
			float ix = x + (k % perRow) * (iw + ICON_GAP), iy = topRow - (k / perRow) * (ih + ICON_GAP);
			if (i < Math.min(bid, won)) {
				paint.batch.draw(assets.trickIcon, ix, iy, iw, ih);
			} else if (i < bid) {
				paint.batch.setColor(BID_FADE);
				paint.batch.draw(assets.trickIcon, ix, iy, iw, ih);
				paint.batch.setColor(Color.WHITE);
			} else {
				paint.batch.draw(assets.overIcon, ix, iy, iw, ih);
			}
		}
	}
}
