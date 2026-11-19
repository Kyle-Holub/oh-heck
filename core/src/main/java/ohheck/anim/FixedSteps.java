package ohheck.anim;

/**
 * The game loop's clock: frames come at whatever rate the device manages, but the game moves
 * on in steps of one fixed length, so it plays out the same everywhere (and in CI). Each frame
 * hands over its length; this says how many whole steps that makes, carrying the remainder
 * to the next frame.
 */
public final class FixedSteps {

	private final float step, maxFrame;
	private float pending;

	/** Steps of step seconds; after a stall, a frame counts for no more than maxFrame. */
	public FixedSteps(float step, float maxFrame) {
		this.step = step;
		this.maxFrame = maxFrame;
	}

	/** Adds a frame of delta seconds; returns how many steps to take now. */
	public int frame(float delta) {
		pending += Math.min(delta, maxFrame);
		int n = 0;
		while (pending >= step * 0.999f) { // (a hair under, so 0.1 s makes six 1/60 s steps despite rounding)
			pending -= step;
			n++;
		}
		return n;
	}

	/** Starts afresh (a screen shown again). */
	public void reset() {
		pending = 0;
	}

	public float step() {
		return step;
	}
}
