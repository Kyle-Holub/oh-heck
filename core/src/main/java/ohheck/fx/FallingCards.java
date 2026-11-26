package ohheck.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.Suit;
import ohheck.ui.Assets;
import ohheck.ui.Palette;

/* Playing cards tumbling down the screen in 3D, projected through a pinhole camera each frame. */
public final class FallingCards {

	private static final float FOCAL = 700f;
	private static final float NEAR = 450f, FAR = 2300f;
	private static final float CARD_W = Assets.CARD_W * 1.6f, CARD_H = Assets.CARD_H * 1.6f;
	private static final int GRID_X = 3, GRID_Y = 4;
	private static final int MAX_CARDS = 110;
	private static final Vector3 LIGHT = new Vector3(-0.3f, 0.6f, -0.75f).nor();

	private static final class Flake {
		final Vector3 pos = new Vector3();
		final Quaternion rot = new Quaternion();
		final Vector3 axis = new Vector3();
		float fall, spin, swayAmp, swayFreq, swayPhase, age;
		Card card;
	}

	private final Assets assets;
	private final Random rng;
	private final float[] rgb = new float[3];
	private final List<Flake> flakes = new ArrayList<Flake>();
	private float time, spawnDebt;
	private final boolean fromEmpty;
	private final float period, phase;

	private final Vector3 v = new Vector3(), n = new Vector3(), toCam = new Vector3();
	private final Quaternion dq = new Quaternion();
	private final float[] gx = new float[(GRID_X + 1) * (GRID_Y + 1)];
	private final float[] gy = new float[(GRID_X + 1) * (GRID_Y + 1)];
	private final float[] quad = new float[20];

	public FallingCards(Assets assets, Random rng) {
		this(assets, rng, false);
	}

	public FallingCards(Assets assets, Random rng, boolean fromEmpty) {
		this.fromEmpty = fromEmpty;
		this.assets = assets;
		this.rng = rng;
		this.period = 9f + rng.nextFloat() * 8f;
		this.phase = rng.nextFloat() * MathUtils.PI2;
	}

	public boolean active() {
		return !flakes.isEmpty();
	}

	private float spawnRate() {
		float e = 0.5f + 0.5f * MathUtils.sin(MathUtils.PI2 * time / period + phase);
		float opening = fromEmpty ? 14f * (float) Math.exp(-fallingFor / 2.5f) : 0f;
		return 1.5f + 13f * e * e * e + opening;
	}

	private boolean falling = true;
	private float fallingFor;

	public void setFalling(boolean falling) {
		this.falling = falling;
	}

	public void update(float dt, float viewW, float viewH) {
		if (flakes.isEmpty() && time == 0 && !fromEmpty) {
			for (int i = 0; i < 22; i++) {
				spawn(viewW, viewH, false);
			}
		}
		time += dt;
		updateSparkles(dt, viewW, viewH);
		if (falling) {
			spawnFalling(dt, viewW, viewH);
		}
		for (Iterator<Flake> it = flakes.iterator(); it.hasNext();) {
			Flake f = it.next();
			if (move(f, dt) && f.pos.y < -(halfH(f.pos.z, viewH) + CARD_H * 1.5f)) {
				it.remove();
			}
		}
	}

	private void spawnFalling(float dt, float viewW, float viewH) {
		fallingFor += dt;
		spawnDebt += spawnRate() * dt;
		while (spawnDebt >= 1f) {
			spawnDebt -= 1f;
			if (flakes.size() < MAX_CARDS) {
				spawn(viewW, viewH, true);
			}
		}
	}

	private boolean move(Flake f, float dt) {
		f.age += dt;
		f.pos.y -= f.fall * dt;
		f.pos.x += f.swayAmp * MathUtils.cos(f.swayFreq * f.age + f.swayPhase) * dt;
		dq.set(f.axis, f.spin * dt);
		f.rot.set(dq.mul(f.rot)).nor();
		return true;
	}

	private float halfH(float z, float viewH) {
		return viewH / 2 * z / FOCAL;
	}

	private float halfW(float z, float viewW) {
		return viewW / 2 * z / FOCAL;
	}

	private void spawn(float viewW, float viewH, boolean aboveScreen) {
		Flake f = new Flake();
		float z = NEAR + rng.nextFloat() * (FAR - NEAR);
		float hw = halfW(z, viewW) * 1.15f, hh = halfH(z, viewH);
		f.pos.set((rng.nextFloat() * 2 - 1) * hw, aboveScreen ? hh + CARD_H : (rng.nextFloat() * 2 - 1) * hh, z);
		f.fall = 180f + rng.nextFloat() * 220f;
		f.swayAmp = 40f + rng.nextFloat() * 120f;
		f.swayFreq = 1f + rng.nextFloat() * 2.5f;
		f.swayPhase = rng.nextFloat() * MathUtils.PI2;
		f.axis.setToRandomDirection();
		f.spin = (90f + rng.nextFloat() * 330f) * (rng.nextBoolean() ? 1 : -1);
		v.setToRandomDirection();
		f.rot.set(v, rng.nextFloat() * 360f);
		f.card = Card.of(Rank.values()[rng.nextInt(13)], Suit.values()[rng.nextInt(4)]);
		flakes.add(f);
	}

	private static final float FOCUS = 1050f;
	private static final float WARM_R = 1f, WARM_G = 0.9f, WARM_B = 0.76f;
	private static final Vector3 VIEW_LIGHT = new Vector3();

	private final List<Flake> glinting = new ArrayList<Flake>();
	private final float[] glintStrength = new float[MAX_CARDS + 1];

	public void draw(SpriteBatch batch, float cx, float cy) {
		Collections.sort(flakes, FAR_FIRST);
		drawSparkles(batch, cx, cy, true);
		glinting.clear();
		for (Flake f : flakes) {
			drawFlake(batch, f, cx, cy);
		}
		drawSparkles(batch, cx, cy, false);
		drawGlints(batch, cx, cy);
	}

	private static float defocus(float z) {
		return MathUtils.clamp(z < FOCUS ? (FOCUS - z) / 420f : (z - FOCUS) / 950f, 0f, 1f);
	}

	private void drawFlake(SpriteBatch batch, Flake f, float cx, float cy) {
		n.set(0, 0, -1);
		f.rot.transform(n);
		toCam.set(f.pos).scl(-1).nor();
		boolean front = n.dot(toCam) > 0;
		float lit = 0.38f + 0.62f * Math.abs(n.dot(LIGHT));
		float depth = (f.pos.z - NEAR) / (FAR - NEAR);
		float b = Math.min(0.94f, lit * MathUtils.lerp(1.05f, 0.42f, depth));
		float d = defocus(f.pos.z);
		float warmR = WARM_R, warmG = WARM_G, warmB = WARM_B;

		VIEW_LIGHT.set(LIGHT).add(toCam).nor();
		float spec = (float) Math.pow(Math.abs(n.dot(VIEW_LIGHT)), 40) * (1 - d);
		if (spec > 0.04f && glinting.size() < glintStrength.length) {
			glintStrength[glinting.size()] = spec;
			glinting.add(f);
		}

		if (d > 0.02f) {
			TextureRegion blur = front ? assets.blurredFace(f.card) : assets.blurredBack();
			float grow = 1f / (1f - 2f * Assets.blurMarginX()), growY = 1f / (1f - 2f * Assets.blurMarginY());
			float a = Math.min(1f, d * 3f);
			drawQuad(batch, f, cx, cy, blur, CARD_W * grow, CARD_H * growY, Color.toFloatBits(b * warmR, b * warmG, b * warmB, a));
		}
		if (d < 0.98f) {
			TextureRegion r = front ? assets.face(f.card) : assets.back();
			drawQuad(batch, f, cx, cy, r, CARD_W, CARD_H, Color.toFloatBits(b * warmR, b * warmG, b * warmB, 1f - d));
		}
	}

	private void drawGlints(SpriteBatch batch, float cx, float cy) {
		if (glinting.isEmpty()) {
			return;
		}
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < glinting.size(); i++) {
			Flake f = glinting.get(i);
			n.set(0, 0, -1);
			f.rot.transform(n);
			toCam.set(f.pos).scl(-1);
			TextureRegion r = n.dot(toCam) > 0 ? assets.face(f.card) : assets.back();
			drawQuad(batch, f, cx, cy, r, CARD_W, CARD_H, Color.toFloatBits(1f, 0.88f, 0.62f, Math.min(0.7f, glintStrength[i])));
		}
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void drawQuad(SpriteBatch batch, Flake f, float cx, float cy, TextureRegion r, float w, float h, float color) {
		for (int j = 0; j <= GRID_Y; j++) {
			for (int i = 0; i <= GRID_X; i++) {
				v.set(((float) i / GRID_X - 0.5f) * w, ((float) j / GRID_Y - 0.5f) * h, 0);
				f.rot.transform(v);
				v.add(f.pos);
				if (v.z < 1f) {
					return;
				}
				int k = j * (GRID_X + 1) + i;
				gx[k] = cx + FOCAL * v.x / v.z;
				gy[k] = cy + FOCAL * v.y / v.z;
			}
		}

		float u0 = r.getU(), u1 = r.getU2(), vTop = r.getV(), vBottom = r.getV2();
		for (int j = 0; j < GRID_Y; j++) {
			for (int i = 0; i < GRID_X; i++) {
				float ua = MathUtils.lerp(u0, u1, (float) i / GRID_X);
				float ub = MathUtils.lerp(u0, u1, (float) (i + 1) / GRID_X);
				float va = MathUtils.lerp(vBottom, vTop, (float) j / GRID_Y);
				float vb = MathUtils.lerp(vBottom, vTop, (float) (j + 1) / GRID_Y);
				put(0, j * (GRID_X + 1) + i, color, ua, va);
				put(5, (j + 1) * (GRID_X + 1) + i, color, ua, vb);
				put(10, (j + 1) * (GRID_X + 1) + i + 1, color, ub, vb);
				put(15, j * (GRID_X + 1) + i + 1, color, ub, va);
				batch.draw(r.getTexture(), quad, 0, 20);
			}
		}
	}

	private void put(int o, int k, float color, float u, float v) {
		quad[o] = gx[k];
		quad[o + 1] = gy[k];
		quad[o + 2] = color;
		quad[o + 3] = u;
		quad[o + 4] = v;
	}

	private static final int SPARKLES = 230;
	private final float[] sx = new float[SPARKLES], sy = new float[SPARKLES], sz = new float[SPARKLES];
	private final float[] sDrift = new float[SPARKLES], sPhase = new float[SPARKLES], sRate = new float[SPARKLES],
			sSize = new float[SPARKLES], sHue = new float[SPARKLES];
	private boolean sparklesReady;

	private void initSparkles(float viewW, float viewH) {
		for (int i = 0; i < SPARKLES; i++) {
			placeSparkle(i, viewW, viewH, (rng.nextFloat() * 2 - 1));
		}
		sparklesReady = true;
	}

	private void placeSparkle(int i, float viewW, float viewH, float yFrac) {
		float z = 300f + rng.nextFloat() * (FAR + 400f - 300f);
		sz[i] = z;
		sx[i] = (rng.nextFloat() * 2 - 1) * halfW(z, viewW) * 1.1f;
		sy[i] = yFrac * halfH(z, viewH) * 1.1f;
		sDrift[i] = 6f + rng.nextFloat() * 22f;
		sPhase[i] = rng.nextFloat() * MathUtils.PI2;
		sRate[i] = 0.6f + rng.nextFloat() * 2.2f;
		sSize[i] = 4f + rng.nextFloat() * 8f;
		sHue[i] = rng.nextFloat();
	}

	private void updateSparkles(float dt, float viewW, float viewH) {
		if (!sparklesReady) {
			initSparkles(viewW, viewH);
		}
		for (int i = 0; i < SPARKLES; i++) {
			sy[i] -= sDrift[i] * dt;
			sx[i] += MathUtils.sin(time * 0.4f + sPhase[i]) * 6f * dt;
			if (sy[i] < -halfH(sz[i], viewH) * 1.15f) {
				placeSparkle(i, viewW, viewH, 1.1f);
			}
		}
	}

	private void drawSparkles(SpriteBatch batch, float cx, float cy, boolean behind) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < SPARKLES; i++) {
			if ((sz[i] >= FOCUS) != behind) {
				continue;
			}
			float scale = FOCAL / sz[i];
			float d = defocus(sz[i]);
			float size = sSize[i] * scale * (1f + d * 5f);
			float twinkle = 0.55f + 0.45f * MathUtils.sin(time * sRate[i] * 2f + sPhase[i]);
			float a = Math.min(1f, twinkle * MathUtils.lerp(1.2f, 0.3f, d) * MathUtils.clamp(scale * 1.6f, 0.35f, 1f));
			Palette.sparkle(MathUtils.lerp(1.5f, 0.6f, sHue[i]), 0, rgb);
			batch.setColor(rgb[0], rgb[1], rgb[2], a);
			batch.draw(assets.glow, cx + sx[i] * scale - size / 2, cy + sy[i] * scale - size / 2, size, size);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	private static final Comparator<Flake> FAR_FIRST = new Comparator<Flake>() {
		@Override
		public int compare(Flake a, Flake b) {
			return Float.compare(b.pos.z, a.pos.z);
		}
	};
}
