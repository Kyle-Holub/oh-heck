package ohheck.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.ui.Palette;

public final class Sparks extends ParticlePool {

	private static final float GRAVITY = 380f, DRAG = 0.8f;
	private final float[] hue, rgb = new float[3];

	public Sparks() {
		super(140);
		hue = new float[max];
	}

	public void burst(float cx, float cy, int count) {
		for (int n = 0; n < count; n++) {
			int i = free();
			if (i < 0) {
				return;
			}
			float angle = MathUtils.random(0.15f, MathUtils.PI - 0.15f);
			float speed = MathUtils.random(140f, 420f);
			born(i, cx + MathUtils.random(-60f, 60f), cy + MathUtils.random(-20f, 20f), MathUtils.cos(angle) * speed,
					MathUtils.sin(angle) * speed, MathUtils.random(0.9f, 1.8f), MathUtils.random(5f, 14f));
			hue[i] = MathUtils.random();
		}
	}

	@Override
	void accelerate(int i, float dt) {
		vy[i] -= GRAVITY * dt;
		vx[i] *= 1f - DRAG * dt;
	}

	public void draw(SpriteBatch batch, Texture glow) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < max; i++) {
			if (!alive(i)) {
				continue;
			}
			float a = 1 - age(i);
			float twinkle = 0.7f + 0.3f * MathUtils.sin(clock * 30f + i);
			Palette.sparkle(MathUtils.lerp(1.6f, 0.8f, hue[i]), 0, rgb);
			batch.setColor(rgb[0], rgb[1], rgb[2], a * 0.55f);
			float big = size[i] * 2.4f;
			batch.draw(glow, x[i] - big / 2, y[i] - big / 2, big, big);
			Palette.sparkle(1, 0.75f, rgb);
			batch.setColor(rgb[0], rgb[1], rgb[2], a * twinkle);
			float core = size[i] * 0.7f;
			batch.draw(glow, x[i] - core / 2, y[i] - core / 2, core, core);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}
}
