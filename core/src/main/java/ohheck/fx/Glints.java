package ohheck.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.rules.Card;

/**
 * The sparkle on trump cards: now and then a small four-pointed glint flares and fades at a
 * random spot on the card. Nothing is stored; each glint's place and timing come from the
 * card, its slot and the clock, so every trump on the table twinkles out of step with the rest.
 */
public final class Glints {

	/** Glints that can be lit on one card at once. */
	private static final int SLOTS = 3;
	/** Each slot's cycle in seconds (a little different per slot), and the share of it spent lit. */
	private static final float PERIOD = 2.4f, PERIOD_STEP = 0.7f, LIT = 0.28f;
	/** A glint's arm length, as a share of the card's width, at its brightest. */
	private static final float SIZE = 0.26f;

	private Glints() {
	}

	/**
	 * Draws the glints on card c, whose face is at (x, y) sized cw x ch, turned rot degrees
	 * about its centre. Glints land only in the first visibleW of its width (the part not
	 * hidden under the next card in a hand).
	 */
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

	/**
	 * A four-pointed glint. Light added to a cream card barely shows, so it is drawn in two
	 * layers: a deep gold cross painted over the card (it reads on a light face), then a soft
	 * halo, white-gold streaks and a bright core added as light on top.
	 */
	private static void star(SpriteBatch batch, Texture glow, float x, float y, float arm, float a, float rot) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(0.78f, 0.47f, 0.06f, 0.8f * a);
		streaks(batch, glow, x, y, arm * 1.05f, arm * 0.3f, rot);
		batch.setColor(0.72f, 0.4f, 0.04f, 0.55f * a);
		batch.draw(glow, x - arm * 0.3f, y - arm * 0.3f, arm * 0.6f, arm * 0.6f);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(1f, 0.8f, 0.45f, 0.35f * a);
		batch.draw(glow, x - arm * 0.6f, y - arm * 0.6f, arm * 1.2f, arm * 1.2f);
		batch.setColor(1f, 0.93f, 0.75f, 0.9f * a);
		streaks(batch, glow, x, y, arm, arm * 0.14f, rot);
		batch.setColor(1f, 1f, 0.95f, a);
		batch.draw(glow, x - arm * 0.16f, y - arm * 0.16f, arm * 0.32f, arm * 0.32f);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	/** Two crossed streaks, arm long either side of (x, y) and thin across. */
	private static void streaks(SpriteBatch batch, Texture glow, float x, float y, float arm, float thin, float rot) {
		for (int i = 0; i < 2; i++) {
			batch.draw(glow, x - arm, y - thin / 2, arm, thin / 2, arm * 2, thin, 1f, 1f, rot + 90 * i, 0, 0,
					glow.getWidth(), glow.getHeight(), false, false);
		}
	}

	/** A steady pseudo-random number in [0, 1) for the pair (a, b). */
	private static float hash01(int a, int b) {
		int h = a * 0x27d4eb2d ^ b * 0x165667b1;
		h ^= h >>> 15;
		h *= 0x2c1b3c6d;
		h ^= h >>> 12;
		return (h & 0xffffff) / (float) 0x1000000;
	}
}
