package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import rules.Card;
import rules.Rank;
import rules.Suit;

/**
 * A shower of playing cards tumbling down the screen in 3D, computed every
 * frame instead of played from a video.
 * <p>
 * Each card has a position, a fall speed, a sideways flutter and a spinning
 * orientation (a quaternion). Its corners are rotated, placed in front of a
 * pinhole camera at the origin looking down +z, and projected onto the
 * screen; the card is drawn as a grid of small textured quads so the
 * perspective barely warps the artwork. The side facing the camera decides
 * whether the face or the back is drawn, and the angle to a light shades it.
 */
public final class FallingCards {

	/** Focal length: at depth FOCAL, one world unit is one screen unit. */
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
	private final List<Flake> flakes = new ArrayList<Flake>();
	private float time, spawnDebt;
	private final float period, phase;

	// scratch space, reused every frame
	private final Vector3 v = new Vector3(), n = new Vector3(), toCam = new Vector3();
	private final Quaternion dq = new Quaternion();
	private final float[] gx = new float[(GRID_X + 1) * (GRID_Y + 1)];
	private final float[] gy = new float[(GRID_X + 1) * (GRID_Y + 1)];
	private final float[] quad = new float[20];

	public FallingCards(Assets assets, Random rng) {
		this.assets = assets;
		this.rng = rng;
		this.period = 9f + rng.nextFloat() * 8f;
		this.phase = rng.nextFloat() * MathUtils.PI2;
	}

	/** Cards falling right now; showers ebb and swell over time. */
	private float spawnRate() {
		float e = 0.5f + 0.5f * MathUtils.sin(MathUtils.PI2 * time / period + phase);
		return 1.5f + 13f * e * e * e;
	}

	public void update(float dt, float viewW, float viewH) {
		if (flakes.isEmpty() && time == 0) {
			for (int i = 0; i < 22; i++) {
				spawn(viewW, viewH, false);
			}
		}
		time += dt;
		spawnDebt += spawnRate() * dt;
		while (spawnDebt >= 1f) {
			spawnDebt -= 1f;
			if (flakes.size() < MAX_CARDS) {
				spawn(viewW, viewH, true);
			}
		}
		for (Iterator<Flake> it = flakes.iterator(); it.hasNext();) {
			Flake f = it.next();
			f.age += dt;
			f.pos.y -= f.fall * dt;
			f.pos.x += f.swayAmp * MathUtils.cos(f.swayFreq * f.age + f.swayPhase) * dt;
			dq.set(f.axis, f.spin * dt);
			f.rot.set(dq.mul(f.rot)).nor();
			if (f.pos.y < -(halfH(f.pos.z, viewH) + CARD_H * 1.5f)) {
				it.remove();
			}
		}
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

	/** Draws the cards with the screen centre at (cx, cy). */
	public void draw(SpriteBatch batch, float cx, float cy) {
		Collections.sort(flakes, FAR_FIRST);
		for (Flake f : flakes) {
			drawFlake(batch, f, cx, cy);
		}
	}

	private void drawFlake(SpriteBatch batch, Flake f, float cx, float cy) {
		// which side faces the camera, and how brightly it is lit
		n.set(0, 0, -1);
		f.rot.transform(n);
		toCam.set(f.pos).scl(-1);
		boolean front = n.dot(toCam) > 0;
		float b = 0.42f + 0.58f * Math.abs(n.dot(LIGHT));
		float color = Color.toFloatBits(b, b, b, 1f);
		TextureRegion r = front ? assets.face(f.card) : assets.back();

		// project the grid of points across the card
		for (int j = 0; j <= GRID_Y; j++) {
			for (int i = 0; i <= GRID_X; i++) {
				v.set(((float) i / GRID_X - 0.5f) * CARD_W, ((float) j / GRID_Y - 0.5f) * CARD_H, 0);
				f.rot.transform(v);
				v.add(f.pos);
				if (v.z < 1f) {
					return; // behind the camera: can't happen with these depths, but never divide by ~0
				}
				int k = j * (GRID_X + 1) + i;
				gx[k] = cx + FOCAL * v.x / v.z;
				gy[k] = cy + FOCAL * v.y / v.z;
			}
		}

		// draw each cell as a quad: bottom-left, top-left, top-right, bottom-right
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

	private static final Comparator<Flake> FAR_FIRST = new Comparator<Flake>() {
		@Override
		public int compare(Flake a, Flake b) {
			return Float.compare(b.pos.z, a.pos.z);
		}
	};
}
