package ohheck.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.rules.Card;

/*
 * Four-pointed sparkles on trump cards, derived from the card and the clock rather than
 * stored.
 */
public final class Glints {

	private static final int SLOTS = 3;
	private static final float PERIOD = 2.4f, PERIOD_STEP = 0.7f, LIT = 0.28f;
	private static final float SIZE = 0.26f;

	private Glints() {
	}

	public static void draw(SpriteBatch batch, Texture glow, Card c, float x, float y, float cw, float ch, float visibleW,
			float rot, float time, float alpha) {
		int id = c.suit().ordinal() * 13 + c.rank().ordinal();
		float cos = MathUtils.cosDeg(rot), sin = MathUtils.sinDeg(rot);
		float cx = x + cw / 2, cy = y + ch / 2;
		for (int slot = 0; slot < SLOTS; slot++) {
			float period = PERIOD + slot * PERIOD_STEP;
			float t = time / period + hash01(id * 31 + slot * 7, 0);
			int cycle = MathUtils.floor(t);
			float k = (t - cycle) / LIT;
			if (k >= 1) {
				continue;
			}
			float glow01 = MathUtils.sin(MathUtils.PI * k) * alpha;
			float u = 0.12f + 0.76f * hash01(id * 131 + slot, cycle);
			float v = 0.1f + 0.8f * hash01(id * 977 + slot * 13, cycle * 3 + 1);
			float ox = Math.min(u * cw, visibleW - cw * 0.08f) - cw / 2, oy = v * ch - ch / 2;
			float gx = cx + ox * cos - oy * sin, gy = cy + ox * sin + oy * cos;
			star(batch, glow, gx, gy, cw * SIZE * (0.6f + 0.4f * glow01), glow01, rot + 15 * slot);
		}
		batch.setColor(Color.WHITE);
	}

	private static void star(SpriteBatch batch, Texture glow, float x, float y, float arm, float a, float rot) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(0.66f, 0.36f, 0.02f, 0.95f * a);
		streaks(batch, glow, x, y, arm * 1.1f, arm * 0.36f, rot);
		batch.setColor(0.6f, 0.3f, 0.02f, 0.7f * a);
		batch.draw(glow, x - arm * 0.35f, y - arm * 0.35f, arm * 0.7f, arm * 0.7f);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(1f, 0.78f, 0.35f, 0.3f * a);
		batch.draw(glow, x - arm * 0.6f, y - arm * 0.6f, arm * 1.2f, arm * 1.2f);
		batch.setColor(1f, 0.9f, 0.6f, 0.6f * a);
		streaks(batch, glow, x, y, arm * 0.9f, arm * 0.08f, rot);
		batch.setColor(1f, 1f, 0.92f, a);
		batch.draw(glow, x - arm * 0.12f, y - arm * 0.12f, arm * 0.24f, arm * 0.24f);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	private static void streaks(SpriteBatch batch, Texture glow, float x, float y, float arm, float thin, float rot) {
		for (int i = 0; i < 2; i++) {
			batch.draw(glow, x - arm, y - thin / 2, arm, thin / 2, arm * 2, thin, 1f, 1f, rot + 90 * i, 0, 0,
					glow.getWidth(), glow.getHeight(), false, false);
		}
	}

	private static float hash01(int a, int b) {
		int h = a * 0x27d4eb2d ^ b * 0x165667b1;
		h ^= h >>> 15;
		h *= 0x2c1b3c6d;
		h ^= h >>> 12;
		return (h & 0xffffff) / (float) 0x1000000;
	}
}
