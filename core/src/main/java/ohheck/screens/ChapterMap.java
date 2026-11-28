package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.campaign.Stage;
import ohheck.ui.Assets;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/*
 * The campaign's map: a parchment chart with a tavern for each chapter, marked by a wax seal, and the trail between
 * them inked in dots (solid past a cleared chapter, faint ahead). The map can lie tilted away and below the view, as
 * if seen lying on a table before the eye drops to it, and its trail can be part inked. Marker positions are fractions of the map.
 */
final class ChapterMap {

	private static final float[][] AT = { { 0.15f, 0.36f }, { 0.38f, 0.66f }, { 0.62f, 0.34f }, { 0.85f, 0.64f } };
	private static final float[] TRAIL_FROM = { 0.02f, 0.12f }, TRAIL_TO = { 0.99f, 0.86f };
	private static final String[] ROMAN = { "I", "II", "III", "IV" };
	private static final float SEAL = 104, EMBLEM = 46, PADLOCK = 40, FOCAL = 700f, DOT = 7, DOT_GAP = 15, PIP = 14, PIP_GAP = 20;
	private static final int STRIPS = 16, MAX_DOTS = 600, PER_SEGMENT = 40, PIPS = 4;
	private static final float JIGGLE_TURNS = 7, JIGGLE_DEGREES = 14, JIGGLE_SHIFT = 5;

	private final Painter paint;
	private final SpriteBatch batch;
	private final Assets assets;
	private final Rectangle[] markers = new Rectangle[AT.length];
	private final String[] versus = new String[AT.length];
	private final float[] dotX = new float[MAX_DOTS], dotY = new float[MAX_DOTS];
	private final int[] dotLeg = new int[MAX_DOTS];
	private final float[] quad = new float[20];
	private final Color tint = new Color();
	private int dots;
	private float w, top;

	ChapterMap(Painter paint) {
		this.paint = paint;
		this.batch = paint.batch;
		this.assets = paint.assets;
		for (int i = 0; i < markers.length; i++) {
			markers[i] = new Rectangle();
			versus[i] = "vs " + Chapter.values()[i].champion().name;
		}
	}

	private static boolean done(Campaign progress, Chapter c, int pip) {
		switch (pip) {
		case 0:
			return progress.won(c, Stage.TABLE);
		case 1:
			return progress.allMade(c);
		case 2:
			return progress.won(c, Stage.FINAL);
		default:
			return progress.cleared(c);
		}
	}

	void layout(float width, float height) {
		if (width == w && height == top) {
			return;
		}
		w = width;
		top = height;
		for (int i = 0; i < AT.length; i++) {
			markers[i].set(AT[i][0] * w - SEAL / 2, AT[i][1] * top - SEAL / 2, SEAL, SEAL);
		}
		dots = 0;
		float lastX = Float.NaN, lastY = 0;
		for (int leg = -1; leg < AT.length; leg++) {
			for (int k = 0; k < PER_SEGMENT; k++) {
				float t = (float) k / PER_SEGMENT;
				float x = spline(leg, t, 0) * w, y = spline(leg, t, 1) * top;
				if (Float.isNaN(lastX) || Math.hypot(x - lastX, y - lastY) >= DOT_GAP) {
					if (dots < MAX_DOTS && !nearMarker(x, y)) {
						dotX[dots] = x;
						dotY[dots] = y;
						dotLeg[dots++] = leg;
					}
					lastX = x;
					lastY = y;
				}
			}
		}
	}

	private boolean nearMarker(float x, float y) {
		for (Rectangle m : markers) {
			float dx = x - (m.x + m.width / 2), dy = y - (m.y + m.height / 2);
			if (dx * dx + dy * dy < SEAL * SEAL * 0.36f) {
				return true;
			}
		}
		return false;
	}

	private static float spline(int leg, float t, int axis) {
		float p0 = point(leg - 1, axis), p1 = point(leg, axis), p2 = point(leg + 1, axis), p3 = point(leg + 2, axis);
		float t2 = t * t, t3 = t2 * t;
		return 0.5f * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (3 * p1 - p0 - 3 * p2 + p3) * t3);
	}

	private static float point(int i, int axis) {
		return i < 0 ? TRAIL_FROM[axis] : i >= AT.length ? TRAIL_TO[axis] : AT[i][axis];
	}

	Rectangle marker(Chapter c) {
		return markers[c.ordinal()];
	}

	void drawMap(float tilt, float distance, float light, float drop) {
		float cx = w / 2, cy = top / 2 + drop, cos = MathUtils.cosDeg(tilt), sin = MathUtils.sinDeg(tilt);
		float color = tint.set(light, light, light, 1).toFloatBits();
		for (int k = 0; k < STRIPS; k++) {
			float y0 = -top / 2 + top * k / STRIPS, y1 = -top / 2 + top * (k + 1) / STRIPS;
			corner(0, cx, cy, -w / 2, y0, cos, sin, distance, color, 0, 1 - (float) k / STRIPS);
			corner(5, cx, cy, w / 2, y0, cos, sin, distance, color, 1, 1 - (float) k / STRIPS);
			corner(10, cx, cy, w / 2, y1, cos, sin, distance, color, 1, 1 - (float) (k + 1) / STRIPS);
			corner(15, cx, cy, -w / 2, y1, cos, sin, distance, color, 0, 1 - (float) (k + 1) / STRIPS);
			batch.draw(assets.campaignMap, quad, 0, 20);
		}
	}

	private void corner(int o, float cx, float cy, float x, float y, float cos, float sin, float distance, float color, float u,
			float v) {
		float z = distance + y * sin;
		quad[o] = cx + FOCAL * x / z;
		quad[o + 1] = cy + FOCAL * y * cos / z;
		quad[o + 2] = color;
		quad[o + 3] = u;
		quad[o + 4] = v;
	}

	void drawTrail(Campaign progress, float inked) {
		int shown = (int) (dots * inked);
		for (int i = 0; i < shown; i++) {
			int leg = dotLeg[i];
			boolean walked = leg < 0 || progress.cleared(Chapter.values()[leg]);
			Color ink = walked ? Palette.MAP_INK : Palette.MAP_INK_FAINT;
			batch.setColor(ink);
			batch.draw(assets.glow, dotX[i] - DOT / 2, dotY[i] - DOT / 2, DOT, DOT);
		}
		batch.setColor(Color.WHITE);
	}

	void drawMarker(Campaign progress, Chapter c, float alpha, float opened, float jiggle, boolean lit, float pulse) {
		Rectangle m = markers[c.ordinal()];
		float cx = m.x + m.width / 2, cy = m.y + m.height / 2, pop = 1 + 0.15f * MathUtils.sin(opened * MathUtils.PI);
		if (lit) {
			paint.glow(cx - SEAL * 1.1f, cy - SEAL * 1.1f, SEAL * 2.2f, SEAL * 2.2f, Palette.EMBER, 0.35f * alpha * pulse);
		}
		float size = SEAL * pop;
		paint.centred(paint.serif(1.5f, Palette.MAP_INK, alpha), ROMAN[c.ordinal()], cx, cy + SEAL / 2 + 34);
		if (opened < 1) {
			batch.setColor(1, 1, 1, alpha * (1 - opened));
			batch.draw(assets.sealLocked, cx - size / 2, cy - size / 2, size, size);
			float swing = MathUtils.sin(jiggle * JIGGLE_TURNS * MathUtils.PI2) * (1 - jiggle);
			int tw = assets.padlock.getWidth(), th = assets.padlock.getHeight();
			batch.draw(assets.padlock, cx - PADLOCK / 2 + swing * JIGGLE_SHIFT, cy - PADLOCK / 2, PADLOCK / 2, PADLOCK, PADLOCK, PADLOCK, 1,
					1, swing * JIGGLE_DEGREES, 0, 0, tw, th, false, false);
			batch.setColor(Color.WHITE);
			paint.centred(paint.ui(0.95f, Palette.MAP_INK_FAINT, alpha * (1 - opened)), "Locked", cx, cy - SEAL / 2 - 12);
		}
		if (opened > 0) {
			batch.setColor(1, 1, 1, alpha * opened);
			batch.draw(assets.seal, cx - size / 2, cy - size / 2, size, size);
			batch.draw(assets.emblem(c.emblem), cx - EMBLEM / 2, cy - EMBLEM / 2, EMBLEM, EMBLEM);
			batch.setColor(Color.WHITE);
			paint.centred(paint.serif(1.2f, Palette.MAP_INK, alpha * opened), c.mode.label, cx, cy - SEAL / 2 - 10);
			paint.centred(paint.ui(0.9f, Palette.MAP_INK, alpha * opened), versus[c.ordinal()], cx, cy - SEAL / 2 - 38);
			batch.setColor(1, 1, 1, alpha * opened);
			for (int i = 0; i < PIPS; i++) {
				float px = cx + (i - (PIPS - 1) / 2f) * PIP_GAP - PIP / 2;
				batch.draw(done(progress, c, i) ? assets.pipOn : assets.pipOff, px, cy - SEAL / 2 - 70, PIP, PIP);
			}
			batch.setColor(Color.WHITE);
		}
	}
}
