package ohheck.anim;

/*
 * Turns variable frame times into whole fixed-length steps, carrying the remainder, so the
 * game plays out the same at any frame rate.
 */
public final class FixedSteps {

	private final float step, maxFrame;
	private float pending;

	public FixedSteps(float step, float maxFrame) {
		this.step = step;
		this.maxFrame = maxFrame;
	}

	public int frame(float delta) {
		pending += Math.min(delta, maxFrame);
		int n = 0;
		while (pending >= step * 0.999f) {
			pending -= step;
			n++;
		}
		return n;
	}

	public void reset() {
		pending = 0;
	}

	public float step() {
		return step;
	}
}
