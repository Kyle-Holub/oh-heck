package ohheck.table;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Ease;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/*
 * The end-of-hand score card: a row per player with their bid, tricks, points and total, best first. As it
 * appears the rows stand in the order they had before the hand, the totals count up by the hand's points,
 * then any player who overtook another hops past them into their new place.
 */
public final class ScoreCard {

	public static final float W = 540, H = 262;
	private static final float ROW = 26, TITLE_DROP = 14, HEAD_DROP = 52, NUMERAL_H = 26, NUMERAL_X = 8, NUMERAL_Y = 8;
	private static final float[] COLS = { 22, 62, 255, 315, 380, 465 };
	private static final String[] HEAD = { "", "Player", "Bid", "Won", "Points", "Total" };
	private static final String SET = "  (set)";
	static final float COUNT_FROM = 0.35f, COUNT_LEN = 0.9f;
	private static final float SWAP_FROM = 1.35f, SWAP_LEN = 0.4f, HOP = 10;
	public static final float LENGTH = SWAP_FROM + SWAP_LEN;
	private final int[] before = new int[8];

	public record Row(int place, String name, boolean made, int bid, int won, int points, int total) {
	}

	private final Painter paint;
	private final Color numeral = new Color();

	public ScoreCard(Painter paint) {
		this.paint = paint;
	}

	public float draw(float cx, float cy, String title, List<Row> rows, float alpha, float t) {
		float px = cx - W / 2, py = cy - H / 2;
		paint.fade(alpha);
		paint.frame(paint.assets.panel, px, py, W, H);
		paint.fade(1f);
		paint.text(paint.ui(1.3f, Palette.CREAM, alpha), title, cx, py + H - TITLE_DROP, true);
		BitmapFont f = paint.ui(1.1f);
		float y = py + H - HEAD_DROP;
		colour(f, Palette.MUTED, alpha);
		for (int c = 0; c < COLS.length; c++) {
			f.draw(paint.batch, HEAD[c], px + COLS[c], y);
		}
		float count = Ease.SMOOTH.apply(MathUtils.clamp((t - COUNT_FROM) / COUNT_LEN, 0, 1));
		float swap = Ease.SMOOTH.apply(MathUtils.clamp((t - SWAP_FROM) / SWAP_LEN, 0, 1));
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			int was = 0;
			for (int j = 0; j < rows.size(); j++) {
				int bj = rows.get(j).total - rows.get(j).points, bi = r.total - r.points;
				was += bj > bi || bj == bi && j < i ? 1 : 0;
			}
			before[i] = was;
		}
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			float at = MathUtils.lerp(before[i], i, swap), hop = before[i] != i ? MathUtils.sin(swap * MathUtils.PI) * HOP : 0;
			float ry = y - ROW * (at + 1) + hop;
			colour(f, Palette.MUTED, alpha);
			f.draw(paint.batch, swap >= 1 ? Wording.ordinal(r.place) : Wording.ordinal(before[i] + 1), px + COLS[0], ry);
			drawRow(f, r, px, ry, alpha, Math.round(r.total - r.points + r.points * count));
		}
		return py;
	}

	private void drawRow(BitmapFont t, Row r, float px, float y, float alpha, int total) {
		colour(t, r.made ? Palette.CREAM : Palette.RED, alpha);
		t.draw(paint.batch, r.made ? r.name : r.name + SET, px + COLS[1], y);
		paint.numeral(r.bid, px + COLS[2] + NUMERAL_X, y - NUMERAL_Y, NUMERAL_H, numeral.set(1, 1, 1, alpha));
		t.draw(paint.batch, Numbers.text(r.won), px + COLS[3], y);
		t.draw(paint.batch, Numbers.signed(r.points), px + COLS[4], y);
		colour(t, r.place == 1 ? Palette.GOLD : Palette.CREAM, alpha);
		t.draw(paint.batch, Numbers.text(total), px + COLS[5], y);
	}

	private static void colour(BitmapFont f, Color c, float alpha) {
		f.setColor(c.r, c.g, c.b, c.a * alpha);
	}
}
