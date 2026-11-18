package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * The table's counters, top left: tally marks for the hands (a gate of five, then three)
 * and, under them, the bid tracker (one card per trick in the hand, filled as the bids
 * add up, red for each trick bid beyond the hand). Drawn at the table, and larger in the
 * tutorial (scale).
 */
final class Counters {

	static final float TALLY_H = 36f, TALLY_W = 6f, TALLY_STEP = 12f;
	/** Small fixed leans so the strokes look hand drawn. */
	private static final float[] TALLY_LEAN = { 3f, -2f, 4f, -1f, 0f, 2f, -3f, 1f };
	static final float TRACKER_GAP = 4, TRACKER_MAX_W = 21, TRACKER_BELOW_TALLY = 10;

	private final BaseScreen screen;
	private final TextureRegion stroke;

	Counters(BaseScreen screen) {
		this.screen = screen;
		this.stroke = new TextureRegion(screen.assets.stroke);
	}

	/**
	 * The tally, its top-left first mark at (left, top): done hands in gold, the one being
	 * played glowing (pulse 0-1), the rest faint.
	 */
	void tally(float left, float top, int done, float pulse, float scale) {
		float th = TALLY_H * scale, tw = TALLY_W * scale, step = TALLY_STEP * scale;
		for (int i = 0; i < 8; i++) {
			float cx, cy = top - th / 2, len = th, rot;
			if (i == 4) { // the bar across the first four
				cx = left + 1.5f * step;
				len = 4 * step + 8 * scale;
				rot = -52; // leaning up to the right
			} else {
				int col = i < 4 ? i : i - 1;
				cx = left + col * step + (i > 4 ? 16 * scale : 0);
				rot = TALLY_LEAN[i];
			}
			if (i == done) { // the hand being played glows
				mark(cx, cy, tw + 6 * scale, len + 6 * scale, rot, Palette.GOLD_LIGHT, 0.18f + 0.22f * pulse);
				mark(cx, cy, tw, len, rot, Palette.GOLD_LIGHT, 1f);
			} else if (i < done) {
				mark(cx, cy, tw, len, rot, Palette.GOLD, 1f);
			} else {
				mark(cx, cy, tw, len, rot, Palette.MUTED, 0.28f);
			}
		}
	}

	/**
	 * The bid tracker for a hand of n tricks with total bid so far: its top-left at (x0,
	 * top), at most avail wide. Returns its height.
	 */
	float tracker(float x0, float top, float avail, int n, int total, float scale) {
		int filled = Math.min(total, n), count = Math.max(total, n);
		float iw = Math.min(TRACKER_MAX_W * scale, avail / count - TRACKER_GAP * scale), ih = iw * 18f / 14f;
		for (int i = 0; i < count; i++) {
			Texture t = i < filled ? screen.assets.bidTaken : i < n ? screen.assets.bidOpen : screen.assets.bidOver;
			screen.batch.draw(t, x0 + i * (iw + TRACKER_GAP * scale), top - ih, iw, ih);
		}
		return ih;
	}

	/** A soft-edged bar centred on (cx, cy), rotated by rot degrees. */
	private void mark(float cx, float cy, float sw, float sh, float rot, Color c, float alpha) {
		screen.batch.setColor(c.r, c.g, c.b, alpha);
		screen.batch.draw(stroke, cx - sw / 2, cy - sh / 2, sw / 2, sh / 2, sw, sh, 1f, 1f, rot);
		screen.batch.setColor(Color.WHITE);
	}

	static float pulse(float time) {
		return 0.5f + 0.5f * MathUtils.sin(time * 3f);
	}
}
