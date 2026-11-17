package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/** Faint gold motes drifting up off the edges of a card (the trump). */
final class Motes extends ParticlePool {

	/** New motes per second. */
	private static final float RATE = 7f;
	private float toSpawn;

	Motes() {
		super(28);
	}

	/** Moves the motes on; while spawning, new ones rise from the card at (cx, cy) sized cw x ch. */
	void update(float dt, boolean spawning, float cx, float cy, float cw, float ch) {
		if (spawning) {
			toSpawn = Math.min(toSpawn + dt * RATE, 1 + dt * RATE);
		}
		while (toSpawn >= 1) {
			int i = free();
			if (i < 0) {
				break;
			}
			toSpawn -= 1;
			// somewhere along the card's outline, mostly the top and sides
			float px, py;
			if (MathUtils.random() < 0.4f) {
				px = cx + MathUtils.random(cw);
				py = cy + ch - MathUtils.random(4f);
			} else {
				px = MathUtils.randomBoolean() ? cx + MathUtils.random(3f) : cx + cw - MathUtils.random(3f);
				py = cy + MathUtils.random(ch * 0.2f, ch);
			}
			born(i, px, py, MathUtils.random(-5f, 5f), MathUtils.random(7f, 18f), MathUtils.random(1.6f, 3.2f),
					MathUtils.random(3f, 7f));
		}
		toSpawn = Math.min(toSpawn, 1);
		step(dt);
	}

	@Override
	void step(float dt) {
		super.step(dt);
		// a gentle sideways sway on top of each mote's drift
		for (int i = 0; i < max; i++) {
			if (alive(i)) {
				x[i] += MathUtils.sin(clock * 1.7f + i) * 4f * dt;
			}
		}
	}

	void draw(SpriteBatch batch, Texture glow) {
		Color c = Palette.GOLD_LIGHT;
		for (int i = 0; i < max; i++) {
			if (!alive(i)) {
				continue;
			}
			float age = age(i);
			// fade in quickly, out slowly, and twinkle a little
			float a = Math.min(age * 6f, 1f) * (1 - age) * (0.75f + 0.25f * MathUtils.sin(clock * 6f + i * 2.3f));
			float sz = size[i] * (1 - age * 0.4f);
			batch.setColor(c.r, c.g, c.b, 0.45f * a);
			batch.draw(glow, x[i] - sz / 2, y[i] - sz / 2, sz, sz);
		}
		batch.setColor(Color.WHITE);
	}
}
