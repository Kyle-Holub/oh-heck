package ohheck.table;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/* The end-of-hand score card: a row per player with their bid, tricks, points and total, best first. */
public final class ScoreCard {

	public static final float W = 540, H = 262;
	private static final float ROW = 26, TITLE_DROP = 14, HEAD_DROP = 52, NUMERAL_H = 26, NUMERAL_X = 8, NUMERAL_Y = 8;
	private static final float[] COLS = { 22, 62, 255, 315, 380, 465 };
	private static final String[] HEAD = { "", "Player", "Bid", "Won", "Points", "Total" };
	private static final String SET = "  (set)";

	public record Row(int place, String name, boolean made, int bid, int won, int points, int total) {
	}

	private final Painter paint;
	private final Color numeral = new Color();

	public ScoreCard(Painter paint) {
		this.paint = paint;
	}

	public float draw(float cx, float cy, String title, List<Row> rows, float alpha) {
		float px = cx - W / 2, py = cy - H / 2;
		paint.fade(alpha);
		paint.frame(paint.assets.panel, px, py, W, H);
		paint.fade(1f);
		paint.text(paint.ui(1.3f, Palette.CREAM, alpha), title, cx, py + H - TITLE_DROP, true);
		BitmapFont t = paint.ui(1.1f);
		float y = py + H - HEAD_DROP;
		colour(t, Palette.MUTED, alpha);
		for (int c = 0; c < COLS.length; c++) {
			t.draw(paint.batch, HEAD[c], px + COLS[c], y);
		}
		for (Row r : rows) {
			y -= ROW;
			colour(t, Palette.MUTED, alpha);
			t.draw(paint.batch, Wording.ordinal(r.place), px + COLS[0], y);
			colour(t, r.made ? Palette.CREAM : Palette.RED, alpha);
			t.draw(paint.batch, r.made ? r.name : r.name + SET, px + COLS[1], y);
			paint.numeral(r.bid, px + COLS[2] + NUMERAL_X, y - NUMERAL_Y, NUMERAL_H, numeral.set(1, 1, 1, alpha));
			t.draw(paint.batch, Numbers.text(r.won), px + COLS[3], y);
			t.draw(paint.batch, Numbers.signed(r.points), px + COLS[4], y);
			colour(t, r.place == 1 ? Palette.GOLD : Palette.CREAM, alpha);
			t.draw(paint.batch, Numbers.text(r.total), px + COLS[5], y);
		}
		return py;
	}

	private static void colour(BitmapFont f, Color c, float alpha) {
		f.setColor(c.r, c.g, c.b, c.a * alpha);
	}
}
