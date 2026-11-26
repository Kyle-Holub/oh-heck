package ohheck.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.ui.Palette;

/* Rockets that rise and burst into sparks when you win. */
public final class Fireworks {

	private static final float[] POWERS = { 5f, 4f, 3.2f, 2.2f, 1f, 1f, 1f }, TO_WHITE = { 0, 0, 0, 0, 0, 0.45f, 0.8f };
	private final float[][] palette = new float[POWERS.length][3];
	private static final int[] WEIGHTS = { 3, 3, 2, 2, 3, 2, 1 };

	private static final int ROCKETS = 30;
	private static final float SHOW_LENGTH = 5f, GRAVITY = 90f, ROCKET_GRAVITY = 300f;

	private final float[] rx = new float[ROCKETS], ry = new float[ROCKETS], rvx = new float[ROCKETS],
			rvy = new float[ROCKETS], launchAt = new float[ROCKETS];
	private final int[] colour = new int[ROCKETS], colour2 = new int[ROCKETS];
	private final boolean[] rising = new boolean[ROCKETS], waiting = new boolean[ROCKETS];

	private final Sparkles sparks = new Sparkles(2600);
	private float clock;

	public void show(float w, float h) {
		clock = 0;
		for (int k = 0; k < palette.length; k++) {
			Palette.sparkle(POWERS[k], TO_WHITE[k], palette[k]);
		}
		for (int i = 0; i < ROCKETS; i++) {
			waiting[i] = true;
			rising[i] = false;
			float t = i < 4 ? i * 0.12f : i < ROCKETS - 7 ? MathUtils.random(0.4f, SHOW_LENGTH - 1.2f)
					: SHOW_LENGTH - 1.1f + MathUtils.random(0.4f);
			launchAt[i] = t;
			float side = MathUtils.random(0.06f, 0.32f);
			rx[i] = (MathUtils.randomBoolean(0.8f) ? (MathUtils.randomBoolean() ? side : 1 - side)
					: MathUtils.random(0.35f, 0.65f)) * w;
			ry[i] = -10;
			float peak = MathUtils.random(0.45f, 0.92f) * h;
			rvy[i] = (float) Math.sqrt(2 * ROCKET_GRAVITY * (peak - ry[i]));
			rvx[i] = MathUtils.random(-30f, 30f);
			colour[i] = pick();
			colour2[i] = MathUtils.randomBoolean(0.35f) ? pick() : colour[i];
		}
	}

	public boolean active() {
		for (int i = 0; i < ROCKETS; i++) {
			if (waiting[i] || rising[i]) {
				return true;
			}
		}
		return sparks.active();
	}

	public void update(float dt) {
		clock += dt;
		for (int i = 0; i < ROCKETS; i++) {
			if (waiting[i] && clock >= launchAt[i]) {
				waiting[i] = false;
				rising[i] = true;
			}
			if (!rising[i]) {
				continue;
			}
			rvy[i] -= ROCKET_GRAVITY * dt;
			rx[i] += rvx[i] * dt;
			ry[i] += rvy[i] * dt;
			sparks.spawn(rx[i] + MathUtils.random(-1f, 1f), ry[i], MathUtils.random(-8f, 8f), MathUtils.random(-30f, -5f),
					MathUtils.random(0.25f, 0.45f), MathUtils.random(3f, 5f), palette[4], 0.2f);
			if (rvy[i] <= 20f) {
				rising[i] = false;
				burst(i);
			}
		}
		sparks.step(dt);
	}

	private void burst(int i) {
		int n = MathUtils.random(80, 110);
		float speed = MathUtils.random(150f, 260f);
		for (int k = 0; k < n; k++) {
			float a = MathUtils.PI2 * k / n + MathUtils.random(-0.05f, 0.05f);
			float s = speed * MathUtils.random(0.75f, 1.05f);
			float[] c = palette[k % 2 == 0 ? colour[i] : colour2[i]];
			sparks.spawn(rx[i], ry[i], MathUtils.cos(a) * s, MathUtils.sin(a) * s, MathUtils.random(1.3f, 2.2f),
					MathUtils.random(8f, 13f), c, 1f);
		}
		for (int k = 0; k < 6; k++) {
			sparks.spawn(rx[i], ry[i], MathUtils.random(-20f, 20f), MathUtils.random(-20f, 20f), 0.35f, 26f, palette[6], 1f);
		}
	}

	private static int pick() {
		int total = 0;
		for (int w : WEIGHTS) {
			total += w;
		}
		int r = MathUtils.random(total - 1);
		for (int i = 0; i < WEIGHTS.length; i++) {
			r -= WEIGHTS[i];
			if (r < 0) {
				return i;
			}
		}
		return 0;
	}

	public void draw(SpriteBatch batch, Texture glow) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < ROCKETS; i++) {
			if (rising[i]) {
				batch.setColor(1f, 0.85f, 0.5f, 0.9f);
				batch.draw(glow, rx[i] - 5, ry[i] - 5, 10, 10);
			}
		}
		sparks.draw(batch, glow);
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	private static final class Sparkles extends ParticlePool {
		private final float[] r, g, b, bright;

		Sparkles(int max) {
			super(max);
			r = new float[max];
			g = new float[max];
			b = new float[max];
			bright = new float[max];
		}

		void spawn(float px, float py, float pvx, float pvy, float life, float sz, float[] c, float brightness) {
			int i = free();
			if (i < 0) {
				return;
			}
			born(i, px, py, pvx, pvy, life, sz);
			r[i] = c[0];
			g[i] = c[1];
			b[i] = c[2];
			bright[i] = brightness;
		}

		@Override
		void accelerate(int i, float dt) {
			vy[i] -= GRAVITY * dt;
			vx[i] *= 1f - 1.2f * dt;
			vy[i] *= 1f - 1.2f * dt;
		}

		void draw(SpriteBatch batch, Texture glow) {
			for (int i = 0; i < max; i++) {
				if (!alive(i)) {
					continue;
				}
				float age = age(i);
				float a = (1 - age) * bright[i];
				if (age > 0.6f) {
					a *= 0.6f + 0.4f * MathUtils.sin(clock * 40f + i * 1.7f);
				}
				float big = size[i] * 2.2f;
				batch.setColor(r[i], g[i], b[i], Math.min(1, a * 0.75f));
				batch.draw(glow, x[i] - big / 2, y[i] - big / 2, big, big);
				float core = size[i] * 0.6f;
				batch.setColor(Math.min(1, r[i] + 0.3f), Math.min(1, g[i] + 0.3f), Math.min(1, b[i] + 0.3f), a);
				batch.draw(glow, x[i] - core / 2, y[i] - core / 2, core, core);
			}
		}
	}
}
