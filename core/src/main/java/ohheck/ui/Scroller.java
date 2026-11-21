package ohheck.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public final class Scroller {

	private static final float BAR_W = 4, BAR_MIN = 30, SIDE_INSET = 5, END_INSET = 11;
	private static final Color BAR = new Color(0.89f, 0.72f, 0.37f, 0.5f);

	private float offset;
	private float velocity;

	public void reset() {
		offset = 0;
		velocity = 0;
	}

	public void drag(float dy) {
		offset += dy;
		velocity = dy * 60f;
	}

	public void update(float dt, boolean touched, float max) {
		if (!touched) {
			offset += velocity * dt;
			velocity *= Math.pow(0.04, dt);
		}
		offset = MathUtils.clamp(offset, 0, Math.max(0, max));
	}

	public float offset() {
		return offset;
	}

	public void scrollTo(float to) {
		offset = to;
	}

	public void drawBar(Painter paint, Rectangle area, float content) {
		if (content <= area.height) {
			return;
		}
		float track = area.height - 2 * END_INSET, bar = Math.max(BAR_MIN, track * area.height / content);
		float at = offset / (content - area.height);
		paint.rect(area.x + area.width - SIDE_INSET - BAR_W, area.y + END_INSET + (track - bar) * (1 - at), BAR_W, bar, BAR);
	}
}
