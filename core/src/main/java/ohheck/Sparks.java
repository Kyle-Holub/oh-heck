package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/** Gold sparks thrown up in a burst when you make your bid or win the game. */
final class Sparks {

	private static final int MAX = 140;
	private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX],
			life = new float[MAX], maxLife = new float[MAX], size = new float[MAX], hue = new float[MAX];
	private float clock;

	void burst(float cx, float cy, int count) {
		for (int n = 0, i = 0; i < MAX && n < count; i++) {
			if (life[i] > 0) {
				continue;
			}
			float angle = MathUtils.random(0.15f, MathUtils.PI - 0.15f);
			float speed = MathUtils.random(140f, 420f);
			x[i] = cx + MathUtils.random(-60f, 60f);
			y[i] = cy + MathUtils.random(-20f, 20f);
			vx[i] = MathUtils.cos(angle) * speed;
			vy[i] = MathUtils.sin(angle) * speed;
			maxLife[i] = life[i] = MathUtils.random(0.9f, 1.8f);
			size[i] = MathUtils.random(5f, 14f);
			hue[i] = MathUtils.random();
			n++;
		}
	}

	boolean active() {
		for (int i = 0; i < MAX; i++) {
			if (life[i] > 0) {
				return true;
			}
		}
		return false;
	}

	void update(float dt) {
		clock += dt;
		for (int i = 0; i < MAX; i++) {
			if (life[i] <= 0) {
				continue;
			}
			life[i] -= dt;
			vy[i] -= 380f * dt; // gravity
			vx[i] *= 1f - 0.8f * dt;
			x[i] += vx[i] * dt;
			y[i] += vy[i] * dt;
		}
	}

	/** Drawn additively: a soft glow with a bright twinkling core. */
	void draw(SpriteBatch batch, Texture glow) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < MAX; i++) {
			if (life[i] <= 0) {
				continue;
			}
			float a = life[i] / maxLife[i];
			float twinkle = 0.7f + 0.3f * MathUtils.sin(clock * 30f + i);
			float g = MathUtils.lerp(0.65f, 0.9f, hue[i]);
			batch.setColor(1f, g, 0.35f, a * 0.55f);
			float big = size[i] * 2.4f;
			batch.draw(glow, x[i] - big / 2, y[i] - big / 2, big, big);
			batch.setColor(1f, 0.95f, 0.8f, a * twinkle);
			float core = size[i] * 0.7f;
			batch.draw(glow, x[i] - core / 2, y[i] - core / 2, core, core);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}
}
