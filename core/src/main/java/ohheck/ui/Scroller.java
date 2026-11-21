package ohheck.ui;

import com.badlogic.gdx.math.MathUtils;

public final class Scroller {

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
}
