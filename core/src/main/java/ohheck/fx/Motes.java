package ohheck.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.ui.Palette;

/* Gold motes rising off the trump card. */
public final class Motes extends ParticlePool {

	private static final float RATE = 6f;
	private float toSpawn;

	public Motes() {
		super(70);
	}

	public void update(float dt, boolean spawning, float cx, float cy, float cw, float ch) {
		if (spawning) {
			toSpawn = Math.min(toSpawn + dt * RATE, 1 + dt * RATE);
		}
		while (toSpawn >= 1) {
			int i = free();
			if (i < 0) {
				break;
			}
			toSpawn -= 1;
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
	public void step(float dt) {
		super.step(dt);
		for (int i = 0; i < max; i++) {
			if (alive(i)) {
				x[i] += MathUtils.sin(clock * 1.7f + i) * 4f * dt;
			}
		}
	}

	public void draw(SpriteBatch batch, Texture glow) {
		Color c = Palette.GOLD_LIGHT;
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < max; i++) {
			if (!alive(i)) {
				continue;
			}
			float age = age(i);
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
