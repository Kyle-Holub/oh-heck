package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/** Faint gold motes drifting up off the edges of a card (the trump). */
final class Motes {

	private static final int MAX = 28;
	/** New motes per second. */
	private static final float RATE = 7f;
	private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX],
			life = new float[MAX], maxLife = new float[MAX], size = new float[MAX];
	private float clock, toSpawn;

	/** Moves the motes on; while spawning, new ones rise from the card at (cx, cy) sized cw x ch. */
	void update(float dt, boolean spawning, float cx, float cy, float cw, float ch) {
		clock += dt;
		if (spawning) {
			toSpawn = Math.min(toSpawn + dt * RATE, 1 + dt * RATE);
		}
		for (int i = 0; i < MAX && toSpawn >= 1; i++) {
			if (life[i] > 0) {
				continue;
			}
			toSpawn -= 1;
			// somewhere along the card's outline, mostly the top and sides
			if (MathUtils.random() < 0.4f) {
				x[i] = cx + MathUtils.random(cw);
				y[i] = cy + ch - MathUtils.random(4f);
			} else {
				x[i] = MathUtils.randomBoolean() ? cx + MathUtils.random(3f) : cx + cw - MathUtils.random(3f);
				y[i] = cy + MathUtils.random(ch * 0.2f, ch);
			}
			vx[i] = MathUtils.random(-5f, 5f);
			vy[i] = MathUtils.random(7f, 18f);
			maxLife[i] = life[i] = MathUtils.random(1.6f, 3.2f);
			size[i] = MathUtils.random(3f, 7f);
		}
		toSpawn = Math.min(toSpawn, 1);
		for (int i = 0; i < MAX; i++) {
			if (life[i] <= 0) {
				continue;
			}
			life[i] -= dt;
			x[i] += (vx[i] + MathUtils.sin(clock * 1.7f + i) * 4f) * dt;
			y[i] += vy[i] * dt;
		}
	}

	void draw(SpriteBatch batch, Texture glow) {
		Color c = Palette.GOLD_LIGHT;
		for (int i = 0; i < MAX; i++) {
			if (life[i] <= 0) {
				continue;
			}
			float age = 1 - life[i] / maxLife[i];
			// fade in quickly, out slowly, and twinkle a little
			float a = Math.min(age * 6f, 1f) * (1 - age) * (0.75f + 0.25f * MathUtils.sin(clock * 6f + i * 2.3f));
			float sz = size[i] * (1 - age * 0.4f);
			batch.setColor(c.r, c.g, c.b, 0.45f * a);
			batch.draw(glow, x[i] - sz / 2, y[i] - sz / 2, sz, sz);
		}
		batch.setColor(Color.WHITE);
	}
}
