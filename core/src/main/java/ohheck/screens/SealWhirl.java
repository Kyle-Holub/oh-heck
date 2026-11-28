package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.FallingCards;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/*
 * The way from the map into a chapter: the tavern's seal draws the eye in as everything else goes dark, sparkles
 * gather and spread round it, then it twists away into itself like a whirlpool (the middle winding faster than the
 * rim), the sparkles turning with it, and becomes a fog that thins as the chapter fades in at OPENS.
 */
final class SealWhirl {

	static final float LENGTH = 2.6f, OPENS = 1.75f, ZOOM_SCALE = 2.6f;
	static final Tween ZOOM_IN = Tween.over(0.7f, Ease.SMOOTH), TWIST = Tween.over(0.95f, Ease.IN).after(0.85f);
	private static final float SEAL = 104, TURN = 900f, SPIN = 0.35f, SHRINK = 0.75f, EMBLEM = 0.45f, SWIRL_SPEED = 1.5f, FOG_FROM = 0.3f,
			FOG_TO = 1.4f;
	private static final int SWIRLERS = 56, GRID = 12;
	private static final Tween DARKEN = Tween.over(0.6f, Ease.SMOOTH).after(0.1f), SPREAD = Tween.over(0.9f, Ease.OUT).after(0.35f),
			FOG_IN = Tween.over(0.7f, Ease.SMOOTH).after(1.2f), FOG_OUT = Tween.over(0.85f, Ease.SMOOTH).after(OPENS),
			FADING = Tween.over(0.85f, Ease.SMOOTH).after(OPENS);

	private final Painter paint;
	private final SpriteBatch batch;
	private final float[] quad = new float[20];
	private final Matrix4 grown = new Matrix4(), still = new Matrix4();
	private final Color shade = new Color();

	SealWhirl(Painter paint) {
		this.paint = paint;
		this.batch = paint.batch;
	}

	static float scale(float t) {
		return 1 + (ZOOM_SCALE - 1) * ZOOM_IN.progress(t);
	}

	void draw(float t, float fromX, float fromY, float w, float h, Texture emblem, FallingCards fog) {
		float zoomIn = ZOOM_IN.progress(t), twist = TWIST.progress(t);
		float dark = t < OPENS ? DARKEN.progress(t) : 1 - FADING.progress(t);
		paint.rect(0, 0, w, h, shade.set(0, 0, 0, dark));
		float cx = MathUtils.lerp(fromX, w / 2, zoomIn), cy = MathUtils.lerp(fromY, h / 2, zoomIn);
		float size = SEAL * scale(t), shrunk = size * (1 - SHRINK * twist);
		if (twist < 1) {
			twirl(paint.assets.seal, cx, cy, shrunk, twist, 1 - twist * twist);
			twirl(emblem, cx, cy, shrunk * EMBLEM, twist, 1 - twist * twist);
		}
		float fogIn = FOG_IN.progress(t), fogA = fogIn * (1 - FOG_OUT.progress(t));
		if (fogA > 0) {
			float grow = MathUtils.lerp(FOG_FROM, FOG_TO, fogIn);
			batch.setTransformMatrix(grown.idt().translate(cx, cy, 0).scale(grow, grow, 1).translate(-cx, -cy, 0));
			fog.drawMist(batch, cx, cy, fogA);
			batch.setTransformMatrix(still);
		}
		float glow = twist * (1 - FADING.progress(t)), glowSize = size * (1 + twist);
		paint.glow(cx - glowSize, cy - glowSize, glowSize * 2, glowSize * 2, Palette.LAMP, 0.35f * glow);
		swirl(cx, cy, size, t, twist);
	}

	private void twirl(Texture texture, float cx, float cy, float size, float twist, float alpha) {
		float colour = shade.set(1, 1, 1, alpha).toFloatBits();
		for (int j = 0; j < GRID; j++) {
			for (int i = 0; i < GRID; i++) {
				corner(0, i, j, cx, cy, size, twist, colour);
				corner(5, i + 1, j, cx, cy, size, twist, colour);
				corner(10, i + 1, j + 1, cx, cy, size, twist, colour);
				corner(15, i, j + 1, cx, cy, size, twist, colour);
				batch.draw(texture, quad, 0, 20);
			}
		}
	}

	private void corner(int o, int i, int j, float cx, float cy, float size, float twist, float colour) {
		float u = (float) i / GRID, v = (float) j / GRID, px = (u - 0.5f) * size, py = (v - 0.5f) * size;
		float r = Math.min(1, (float) Math.hypot(px, py) / (size / 2)), inner = 1 - r;
		float turn = (twist * TURN * inner * inner + twist * TURN * SPIN) * MathUtils.degRad;
		float cos = MathUtils.cos(turn), sin = MathUtils.sin(turn);
		quad[o] = cx + px * cos - py * sin;
		quad[o + 1] = cy + px * sin + py * cos;
		quad[o + 2] = colour;
		quad[o + 3] = u;
		quad[o + 4] = 1 - v;
	}

	private void swirl(float cx, float cy, float size, float t, float twist) {
		float spread = SPREAD.progress(t), out = FADING.progress(t), a = spread * (1 - out);
		if (a <= 0) {
			return;
		}
		float whirl = twist * TURN * MathUtils.degRad * 0.8f;
		paint.lighting(true);
		for (int i = 0; i < SWIRLERS; i++) {
			float lane = 0.5f + 0.9f * spread * (0.6f + 0.4f * ((i * 7) % 10) / 10f);
			float angle = i * MathUtils.PI2 / SWIRLERS + t * SWIRL_SPEED * (1 + (i % 3) * 0.15f) + whirl * (1.3f - lane * 0.4f);
			float r = size * lane * (1 - 0.45f * twist) + out * size * 1.6f;
			paint.sparkle(cx + MathUtils.cos(angle) * r, cy + MathUtils.sin(angle) * r, 8 + (i % 4) * 4, 0.6f + (i % 5) * 0.2f,
					a * (0.55f + 0.45f * MathUtils.sin(t * 7 + i)));
		}
		paint.lighting(false);
	}
}
