package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/** Gold motes drifting up off the edges of a card (the trump), glowing softly as they rise and fade. */
final class Motes extends ParticlePool {

	/** New motes per second. */
	private static final float RATE = 6f;
	private float toSpawn;

	Motes() {
		super(70);
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
			born(i, px, py, MathUtils.random(-6f, 6f), MathUtils.random(12f, 28f), MathUtils.random(1.8f, 3.4f),
					MathUtils.random(4f, 9f));
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

	/** Drawn additively: a soft gold halo round a bright core, twinkling. */
	void draw(SpriteBatch batch, Texture glow) {
		Color c = Palette.GOLD_LIGHT;
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < max; i++) {
			if (!alive(i)) {
				continue;
			}
			float age = age(i);
			// fade in quickly, out slowly, and twinkle
			float a = Math.min(age * 6f, 1f) * (1 - age) * (0.6f + 0.4f * MathUtils.sin(clock * 7f + i * 2.3f));
			float sz = size[i] * (1 - age * 0.4f);
			batch.setColor(c.r, c.g * 0.9f, c.b * 0.6f, 0.45f * a);
			batch.draw(glow, x[i] - sz, y[i] - sz, sz * 2, sz * 2);
			batch.setColor(1f, 0.95f, 0.8f, 0.75f * a);
			batch.draw(glow, x[i] - sz * 0.3f, y[i] - sz * 0.3f, sz * 0.6f, sz * 0.6f);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}
}
