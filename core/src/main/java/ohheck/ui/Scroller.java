package ohheck.ui;

import com.badlogic.gdx.math.MathUtils;

/** A list scrolled by dragging, with a little fling when the finger lets go. */
public final class Scroller {

	/** How far the content has moved up, from 0 to the most it can. */
	private float offset;
	private float velocity;

	public void reset() {
		offset = 0;
		velocity = 0;
	}

	/** The finger moved up dy. */
	public void drag(float dy) {
		offset += dy;
		velocity = dy * 60f;
	}

	/** Coasts after a fling (while not touched), and keeps within 0 to max. */
	public void update(float dt, boolean touched, float max) {
		if (!touched) {
			offset += velocity * dt;
			velocity *= Math.pow(0.04, dt);
		}
		offset = MathUtils.clamp(offset, 0, Math.max(0, max));
	}

	/** How far the list is scrolled down (0 = the top). */
	public float offset() {
		return offset;
	}

	/** Jumps to a position (clamped to the list on the next update). */
	public void scrollTo(float to) {
		offset = to;
	}
}
