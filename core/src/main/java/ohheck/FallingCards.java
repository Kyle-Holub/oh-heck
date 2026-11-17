package ohheck;

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
 * whether the face or the back is drawn, and the angle to a warm light shades
 * it, with a glint as it turns towards the light. Cards away from the focus
 * depth fade into pre-blurred copies (depth of field), and gold sparkles
 * drift through the same space.
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
		/** A card tossed off the table: thrown with this velocity, falling under gravity after its delay. */
		boolean tossed;
		final Vector3 vel = new Vector3();
		float delay;
	}

	private final Assets assets;
	private final Random rng;
	private final List<Flake> flakes = new ArrayList<Flake>();
	private float time, spawnDebt;
	private final boolean fromEmpty;
	/** Only tossed cards, all in focus and evenly lit, with no sparkles: the table at the game's end. */
	private boolean tableMode;
	private final float period, phase;

	// scratch space, reused every frame
	private final Vector3 v = new Vector3(), n = new Vector3(), toCam = new Vector3();
	private final Quaternion dq = new Quaternion();
	private final float[] gx = new float[(GRID_X + 1) * (GRID_Y + 1)];
	private final float[] gy = new float[(GRID_X + 1) * (GRID_Y + 1)];
	private final float[] quad = new float[20];

	public FallingCards(Assets assets, Random rng) {
		this(assets, rng, false);
	}

	/**
	 * @param fromEmpty start with no cards on screen and a heavy shower, so the
	 *                  first cards visibly fall in from the top (the opening sequence)
	 */
	public FallingCards(Assets assets, Random rng, boolean fromEmpty) {
		this.fromEmpty = fromEmpty;
		this.assets = assets;
		this.rng = rng;
		this.period = 9f + rng.nextFloat() * 8f;
		this.phase = rng.nextFloat() * MathUtils.PI2;
	}

	/** For the table at the game's end: nothing falls until cards are {@link #toss tossed}. */
	public static FallingCards forTable(Assets assets, Random rng) {
		FallingCards f = new FallingCards(assets, rng, true);
		f.tableMode = true;
		f.falling = false;
		return f;
	}

	/** Upward pop, sideways scatter, depth drift and gravity for tossed cards, in screen units. */
	private static final float TOSS_UP_MIN = 260f, TOSS_UP_MAX = 460f, TOSS_SIDE = 160f, GRAVITY = 1500f,
			TOSS_SPIN_MIN = 160f, TOSS_SPIN_MAX = 480f, SPIN_UP_TIME = 0.35f;

	/**
	 * Throws a card off the table: it starts exactly where it lies (centre (x, y) relative to
	 * the screen centre, w wide, turned rot degrees), and after delay seconds pops up, starts
	 * tumbling and falls off the bottom of the screen.
	 */
	public void toss(Card c, float x, float y, float w, float rot, float delay) {
		Flake f = new Flake();
		float z = CARD_W * FOCAL / w, k = z / FOCAL;
		f.pos.set(x * k, y * k, z);
		f.rot.set(Vector3.Z, rot);
		f.vel.set((x / 400f * TOSS_SIDE + (rng.nextFloat() * 2 - 1) * TOSS_SIDE) * k,
				MathUtils.lerp(TOSS_UP_MIN, TOSS_UP_MAX, rng.nextFloat()) * k, (rng.nextFloat() * 2 - 0.6f) * 260f);
		f.axis.set(rng.nextFloat() * 2 - 1, rng.nextFloat() * 2 - 1, (rng.nextFloat() * 2 - 1) * 0.4f).nor();
		f.spin = MathUtils.lerp(TOSS_SPIN_MIN, TOSS_SPIN_MAX, rng.nextFloat()) * (rng.nextBoolean() ? 1 : -1);
		f.card = c;
		f.tossed = true;
		f.delay = delay;
		flakes.add(f);
	}

	/** Whether any card is still on its way off the screen (or waiting to be thrown). */
	public boolean active() {
		return !flakes.isEmpty();
	}

	/** Whether a card has been thrown yet (false while every tossed card is still waiting). */
	public boolean anyThrown() {
		for (Flake f : flakes) {
			if (f.tossed && f.age >= f.delay) {
				return true;
			}
		}
		return false;
	}

	/** Cards falling right now; showers ebb and swell over time. */
	private float spawnRate() {
		float e = 0.5f + 0.5f * MathUtils.sin(MathUtils.PI2 * time / period + phase);
		float opening = fromEmpty ? 14f * (float) Math.exp(-fallingFor / 2.5f) : 0f;
		return 1.5f + 13f * e * e * e + opening;
	}

	/** Whether new cards appear; until then only the sparkles drift (the opening starts with them alone). */
	private boolean falling = true;
	/** Seconds since cards started falling, for the opening shower. */
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
		if (!tableMode) {
			updateSparkles(dt, viewW, viewH);
		}
		if (falling) {
			fallingFor += dt;
			spawnDebt += spawnRate() * dt;
			while (spawnDebt >= 1f) {
				spawnDebt -= 1f;
				if (flakes.size() < MAX_CARDS) {
					spawn(viewW, viewH, true);
				}
			}
		}
		for (Iterator<Flake> it = flakes.iterator(); it.hasNext();) {
			Flake f = it.next();
			f.age += dt;
			if (f.tossed) {
				float t = f.age - f.delay;
				if (t <= 0) {
					continue; // still lying on the table
				}
				float k = f.pos.z / FOCAL;
				f.vel.y -= GRAVITY * k * dt;
				f.pos.mulAdd(f.vel, dt);
				dq.set(f.axis, f.spin * Math.min(1f, t / SPIN_UP_TIME) * dt);
			} else {
				f.pos.y -= f.fall * dt;
				f.pos.x += f.swayAmp * MathUtils.cos(f.swayFreq * f.age + f.swayPhase) * dt;
				dq.set(f.axis, f.spin * dt);
			}
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

	// ---- drawing

	/** Depth that is in sharp focus; cards nearer or further are blurred. */
	private static final float FOCUS = 1050f;
	private static final float WARM_R = 1f, WARM_G = 0.9f, WARM_B = 0.76f;
	private static final Vector3 VIEW_LIGHT = new Vector3();

	private final List<Flake> glinting = new ArrayList<Flake>();
	private final float[] glintStrength = new float[MAX_CARDS + 1];

	/** Draws sparkles and cards with the screen centre at (cx, cy). */
	public void draw(SpriteBatch batch, float cx, float cy) {
		Collections.sort(flakes, FAR_FIRST);
		if (!tableMode) {
			drawSparkles(batch, cx, cy, true);
		}
		glinting.clear();
		for (Flake f : flakes) {
			if (!f.tossed || f.age >= f.delay) {
				drawFlake(batch, f, cx, cy);
			}
		}
		if (!tableMode) {
			drawSparkles(batch, cx, cy, false);
		}
		drawGlints(batch, cx, cy);
	}

	/** How out of focus a depth is: 0 = sharp, 1 = fully blurred. Near cards blur faster than far ones. */
	private static float defocus(float z) {
		return MathUtils.clamp(z < FOCUS ? (FOCUS - z) / 420f : (z - FOCUS) / 950f, 0f, 1f);
	}

	private void drawFlake(SpriteBatch batch, Flake f, float cx, float cy) {
		// which side faces the camera, and how it is lit
		n.set(0, 0, -1);
		f.rot.transform(n);
		toCam.set(f.pos).scl(-1).nor();
		boolean front = n.dot(toCam) > 0;
		float lit = 0.38f + 0.62f * Math.abs(n.dot(LIGHT));
		// further away is darker, so the shower has depth
		float depth = (f.pos.z - NEAR) / (FAR - NEAR);
		// (capped at 1: packed vertex colours overflow into the next channel above that)
		// (on the table: no depth falloff, and flat cards keep their full brightness)
		float b = Math.min(0.94f, tableMode ? 0.5f + 0.6f * lit : lit * MathUtils.lerp(1.05f, 0.42f, depth));
		float d = tableMode ? 0f : defocus(f.pos.z);
		// the title's warm light; on the table, the cards' own colours (so they don't change as they leave)
		float warmR = tableMode ? 1f : WARM_R, warmG = tableMode ? 1f : WARM_G, warmB = tableMode ? 1f : WARM_B;

		// a glint when the card turns its face towards the light
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

	/**
	 * Adds the glints on top, with additive blending so they brighten what is there (softer on
	 * the table, where the cards are already bright enough to trip the bloom).
	 */
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
			drawQuad(batch, f, cx, cy, r, CARD_W, CARD_H, Color.toFloatBits(1f, 0.88f, 0.62f, Math.min(tableMode ? 0.25f : 0.7f, glintStrength[i])));
		}
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	/** Projects a card-shaped grid of size w x h and draws region across it. */
	private void drawQuad(SpriteBatch batch, Flake f, float cx, float cy, TextureRegion r, float w, float h, float color) {
		for (int j = 0; j <= GRID_Y; j++) {
			for (int i = 0; i <= GRID_X; i++) {
				v.set(((float) i / GRID_X - 0.5f) * w, ((float) j / GRID_Y - 0.5f) * h, 0);
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

	// ---- gold sparkles: glowing specks drifting in the same 3D space as the cards

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

	/** Puts sparkle i at a random depth, at height fraction yFrac (-1 bottom .. 1 top). */
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
			sy[i] -= sDrift[i] * dt; // a slow fall, like dust in a light beam
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
			// out-of-focus sparkles spread into large, faint discs of light (bokeh)
			float size = sSize[i] * scale * (1f + d * 5f);
			float twinkle = 0.55f + 0.45f * MathUtils.sin(time * sRate[i] * 2f + sPhase[i]);
			float a = Math.min(1f, twinkle * MathUtils.lerp(1.2f, 0.3f, d) * MathUtils.clamp(scale * 1.6f, 0.35f, 1f));
			float g = MathUtils.lerp(0.62f, 0.86f, sHue[i]);
			batch.setColor(1f, g, MathUtils.lerp(0.25f, 0.55f, sHue[i]), a);
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
