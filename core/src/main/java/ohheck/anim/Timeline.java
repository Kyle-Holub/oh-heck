package ohheck.anim;

/*
 * A tutorial scene's clock: seconds into a scene that loops after a pause or plays once and
 * holds.
 */
public final class Timeline {

	private float stepT, t, lastT;

	public void restart() {
		stepT = 0;
		t = lastT = 0;
	}

	public void advance(float dt, float length, boolean loops, float hold) {
		stepT += dt;
		lastT = t;
		t = loops ? stepT % (length + hold) : Math.min(stepT, length);
		if (t < lastT) {
			lastT = -1;
		}
	}

	public float t() {
		return t;
	}

	public float shown() {
		return stepT;
	}

	public float at(float a, float b) {
		return Ease.SMOOTH.apply(Ease.clamp((t - a) / (b - a), 0, 1));
	}

	public boolean passed(float a) {
		return lastT < a && t >= a;
	}

	public int count(float... times) {
		int n = 0;
		while (n < times.length && t >= times[n]) {
			n++;
		}
		return n;
	}
}
