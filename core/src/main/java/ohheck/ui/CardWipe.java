package ohheck.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Ease;

/*
 * A curtain of card backs that covers the screen and sweeps off to the right, column by column,
 * uncovering what is behind: the way into a campaign table and back out of it.
 */
public final class CardWipe {

	private static final float CARD_W = 118, CARD_H = CARD_W * 98 / 73, STEP_X = 84, STEP_Y = 112, COLUMN_DELAY = 0.028f,
			ROW_DELAY = 0.012f, FLIGHT = 0.38f, TILT = 8;
	public static final float LENGTH = 0.85f;

	private CardWipe() {
	}

	public static void draw(SpriteBatch batch, TextureRegion back, float w, float h, float t) {
		if (t >= LENGTH) {
			return;
		}
		int cols = (int) (w / STEP_X) + 3, rows = (int) (h / STEP_Y) + 3;
		float spread = Math.max(0.01f, LENGTH - FLIGHT - (rows - 1) * ROW_DELAY) / cols;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				float delay = (cols - 1 - c) * Math.min(COLUMN_DELAY, spread) + r * ROW_DELAY;
				float p = Ease.IN.apply(MathUtils.clamp((t - delay) / FLIGHT, 0, 1));
				if (p >= 1) {
					continue;
				}
				float x = c * STEP_X - CARD_W / 2 + p * (w + CARD_W * 2), y = r * STEP_Y - CARD_H / 2 + (r % 2) * 20 + p * 60;
				float rot = ((c * 7 + r * 13) % 5 - 2) * TILT / 2 - p * 25;
				batch.draw(back, x, y, CARD_W / 2, CARD_H / 2, CARD_W, CARD_H, 1, 1, rot);
			}
		}
	}
}
