package ohheck.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.ui.Assets;

/*
 * A torchlit stone corridor walked in 3D: walls, floor and ceiling as short projected slabs, each corner lit by
 * the flickering torches near it and lost to darkness with distance, so the light falls smoothly across the
 * stone. The corridor ends at an arched doorway glowing with the hall beyond. walk(p) goes from the entrance (0)
 * to the doorway (1).
 */
public final class Corridor {

	private static final float FOCAL = 700f, HALF_W = 250f, HALF_H = 230f, LENGTH = 3400f, SLAB = 50f, NEAR = 30f,
			TILE = 230f, FOG = 1300f, AMBIENT = 0.04f, TORCH_EVERY = 560f, TORCH_FIRST = 380f, TORCH_Y = 50f,
			TORCH_REACH = 300f, STOP_SHORT = 520f, STRIDE = 1.9f, BOB = 7f, FLAME = 120f, ARCH_W = 260f, ARCH_H = 390f;
	private static final int STRIPS = 8;
	private static final Color WARM = new Color(1f, 0.68f, 0.38f, 1f), FLAME_TINT = new Color(1f, 0.62f, 0.25f, 1f),
			HALL = new Color(1f, 0.85f, 0.6f, 1f);

	private final Assets assets;
	private final float[] quad = new float[20];
	private float time, camera, bob;

	public Corridor(Assets assets) {
		this.assets = assets;
	}

	public void update(float dt) {
		time += dt;
	}

	public void walk(float progress) {
		camera = progress * (LENGTH - STOP_SHORT);
		bob = MathUtils.sin(progress * STRIDE * MathUtils.PI2 * 2) * BOB * (1 - progress);
	}

	public void draw(SpriteBatch batch, float w, float h) {
		float cx = w / 2, cy = h / 2 + bob;
		for (float z = LENGTH - SLAB; z >= camera - SLAB; z -= SLAB) {
			float near = Math.max(z, camera + NEAR), far = z + SLAB;
			if (far <= near) {
				continue;
			}
			slab(batch, assets.stoneWall, cx, cy, -HALF_W, -HALF_H, -HALF_W, HALF_H, near, far, true);
			slab(batch, assets.stoneWall, cx, cy, HALF_W, HALF_H, HALF_W, -HALF_H, near, far, true);
			slab(batch, assets.stoneFloor, cx, cy, -HALF_W, -HALF_H, HALF_W, -HALF_H, near, far, false);
			slab(batch, assets.stoneWall, cx, cy, HALF_W, HALF_H, -HALF_W, HALF_H, near, far, false);
		}
		endWall(batch, cx, cy);
		torches(batch, cx, cy);
	}

	private void slab(SpriteBatch batch, Texture t, float cx, float cy, float x0, float y0, float x1, float y1, float near,
			float far, boolean wall) {
		for (int k = 0; k < STRIPS; k++) {
			float ax = MathUtils.lerp(x0, x1, (float) k / STRIPS), ay = MathUtils.lerp(y0, y1, (float) k / STRIPS);
			float bx = MathUtils.lerp(x0, x1, (float) (k + 1) / STRIPS), by = MathUtils.lerp(y0, y1, (float) (k + 1) / STRIPS);
			corner(0, cx, cy, ax, ay, near, near, wall ? ay : ax);
			corner(5, cx, cy, bx, by, near, near, wall ? by : bx);
			corner(10, cx, cy, bx, by, far, far, wall ? by : bx);
			corner(15, cx, cy, ax, ay, far, far, wall ? ay : ax);
			batch.draw(t, quad, 0, 20);
		}
	}

	private void corner(int o, float cx, float cy, float x, float y, float z, float u, float v) {
		float depth = z - camera;
		quad[o] = cx + FOCAL * x / depth;
		quad[o + 1] = cy + FOCAL * y / depth;
		float light = Math.min(1.2f, AMBIENT + torchLight(x, y, z)) * (float) Math.exp(-depth / FOG);
		quad[o + 2] = Color.toFloatBits(Math.min(1, light * WARM.r * 1.4f), Math.min(1, light * WARM.g * 1.4f),
				Math.min(1, light * WARM.b * 1.4f), 1f);
		quad[o + 3] = u / TILE;
		quad[o + 4] = v / TILE;
	}

	private float torchLight(float x, float y, float z) {
		float sum = 0;
		for (int i = 0; TORCH_FIRST + i * TORCH_EVERY < LENGTH; i++) {
			float tz = TORCH_FIRST + i * TORCH_EVERY, tx = torchX(i), dx = x - tx, dy = y - TORCH_Y, dz = z - tz;
			float d2 = (dx * dx + dy * dy + dz * dz) / (TORCH_REACH * TORCH_REACH);
			sum += flicker(i) / (1 + d2 * d2);
		}
		return sum;
	}

	private static float torchX(int i) {
		return (i % 2 == 0 ? -1 : 1) * (HALF_W - 14);
	}

	private float flicker(int i) {
		return 0.85f + 0.1f * MathUtils.sin(time * 11f + i * 1.7f) + 0.05f * MathUtils.sin(time * 23f + i * 4.1f);
	}

	private void torches(SpriteBatch batch, float cx, float cy) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; TORCH_FIRST + i * TORCH_EVERY < LENGTH; i++) {
			float depth = TORCH_FIRST + i * TORCH_EVERY - camera;
			if (depth < NEAR * 3) {
				continue;
			}
			float scale = FOCAL / depth, fog = (float) Math.exp(-depth / (FOG * 2)), f = flicker(i);
			float x = cx + torchX(i) * scale, y = cy + TORCH_Y * scale, size = FLAME * scale * f;
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			float iron = 0.12f * fog;
			batch.setColor(iron, iron * 0.8f, iron * 0.6f, 1f);
			batch.draw(assets.white, x - size * 0.1f, y - size * 0.55f, size * 0.2f, size * 0.55f);
			batch.draw(assets.white, x - size * 0.2f, y - size * 0.05f, size * 0.4f, size * 0.1f);
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
			batch.setColor(FLAME_TINT.r, FLAME_TINT.g, FLAME_TINT.b, 0.35f * fog);
			batch.draw(assets.glow, x - size * 2, y - size * 1.6f, size * 4, size * 4);
			batch.setColor(1f, 0.82f, 0.5f, 0.9f * fog);
			batch.draw(assets.glow, x - size * 0.35f, y - size * 0.2f, size * 0.7f, size * (1.1f + 0.2f * (f - 0.85f) * 5));
			batch.setColor(1f, 0.95f, 0.8f, fog);
			batch.draw(assets.glow, x - size * 0.15f, y - size * 0.05f, size * 0.3f, size * 0.5f);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void endWall(SpriteBatch batch, float cx, float cy) {
		corner(0, cx, cy, -HALF_W, -HALF_H, LENGTH, -HALF_W, -HALF_H);
		corner(5, cx, cy, HALF_W, -HALF_H, LENGTH, HALF_W, -HALF_H);
		corner(10, cx, cy, HALF_W, HALF_H, LENGTH, HALF_W, HALF_H);
		corner(15, cx, cy, -HALF_W, HALF_H, LENGTH, -HALF_W, HALF_H);
		batch.draw(assets.stoneWall, quad, 0, 20);
		float depth = LENGTH - camera, scale = FOCAL / depth, fog = (float) Math.exp(-depth / FOG);
		float aw = ARCH_W * scale, ah = ARCH_H * scale, x = cx - aw / 2, y = cy - HALF_H * scale;
		batch.setColor(0, 0, 0, 1);
		batch.draw(assets.plaque, x, y, aw, ah);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		float glow = 0.35f + 0.65f * (1 - depth / LENGTH);
		batch.setColor(HALL.r, HALL.g, HALL.b, glow);
		batch.draw(assets.plaque, x, y, aw, ah);
		batch.draw(assets.glow, cx - aw * 1.5f, y - ah * 0.2f, aw * 3, ah * 1.5f);
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}
}
