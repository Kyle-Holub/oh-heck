package ohheck.table;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import ohheck.rules.RuleSet;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/* The hand tally and the bid tracker. */
public final class Counters {

	public static final float TALLY_H = 36f, TALLY_W = 6f, TALLY_STEP = 12f;
	private static final float TALLY_GATE_GAP = 16;
	private static final float[] TALLY_LEAN = { 3f, -2f, 4f, -1f, 0f, 2f, -3f, 1f };
	public static final float TRACKER_GAP = 4, TRACKER_MAX_W = 21, TRACKER_BELOW_TALLY = 10;

	private final Painter paint;
	private final TextureRegion stroke;
	private final Color ink = new Color();

	public Counters(Painter paint) {
		this.paint = paint;
		this.stroke = new TextureRegion(paint.assets.stroke);
	}

	public void tally(float left, float top, int marks, int done, float pulse, float scale, float alarm) {
		Color ink = this.ink.set(Palette.GOLD).lerp(Palette.RED, alarm);
		float th = TALLY_H * scale, tw = TALLY_W * scale, step = TALLY_STEP * scale;
		for (int i = 0; i < marks; i++) {
			int gate = i / 5, k = i % 5;
			float gateX = left + gate * (4 * step + TALLY_GATE_GAP * scale);
			float cx, cy = top - th / 2, len = th, rot;
			if (k == 4) {
				cx = gateX + 1.5f * step;
				len = 4 * step + 8 * scale;
				rot = -52;
			} else {
				cx = gateX + k * step;
				rot = TALLY_LEAN[i % TALLY_LEAN.length];
			}
			if (i == done) {
				mark(cx, cy, tw + 6 * scale, len + 6 * scale, rot, Palette.GOLD_LIGHT, 0.18f + 0.22f * pulse);
				mark(cx, cy, tw, len, rot, Palette.GOLD_LIGHT, 1f);
			} else if (i < done) {
				mark(cx, cy, tw, len, rot, ink, 1f);
			} else {
				mark(cx, cy, tw, len, rot, Palette.MUTED, 0.28f);
			}
		}
	}

	public float tracker(float x0, float top, float avail, int n, int total, float scale) {
		int filled = Math.min(total, n), count = Math.max(total, n);
		int rows = n > RuleSet.USUAL_CARDS ? 2 : 1, perRow = (count + rows - 1) / rows;
		float gap = TRACKER_GAP * scale;
		float iw = Math.min(TRACKER_MAX_W * scale, avail / perRow - gap), ih = iw * 18f / 14f;
		for (int i = 0; i < count; i++) {
			Texture t = i < filled ? paint.assets.bidTaken : i < n ? paint.assets.bidOpen : paint.assets.bidOver;
			paint.batch.draw(t, x0 + (i % perRow) * (iw + gap), top - ih - (i / perRow) * (ih + gap), iw, ih);
		}
		return rows * ih + (rows - 1) * gap;
	}

	private void mark(float cx, float cy, float sw, float sh, float rot, Color c, float alpha) {
		paint.batch.setColor(c.r, c.g, c.b, alpha);
		paint.batch.draw(stroke, cx - sw / 2, cy - sh / 2, sw / 2, sh / 2, sw, sh, 1f, 1f, rot);
		paint.batch.setColor(Color.WHITE);
	}

	public static float pulse(float time) {
		return 0.5f + 0.5f * MathUtils.sin(time * 3f);
	}
}
