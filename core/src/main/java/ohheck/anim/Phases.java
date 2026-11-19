package ohheck.anim;

/**
 * A sequence of stages, one after another, each lasting so long: the deal's gather, riffle
 * passes and return; a trick's pause and gathering in. Ask which stage a time falls in and
 * how far into it.
 *
 * <pre>
 * Phases shuffle = new Phases(0.4f, 3 * 0.9f, 0.35f); // gather, riffles, home
 * int stage = shuffle.at(t);                           // 0, 1, 2, or 3 once over
 * float into = shuffle.into(t);                        // seconds into that stage
 * </pre>
 */
public final class Phases {

	private final float[] lengths;

	public Phases(float... lengths) {
		this.lengths = lengths.clone();
	}

	/** Which stage t falls in (0 for the first); the number of stages once they're all over. */
	public int at(float t) {
		for (int i = 0; i < lengths.length; i++) {
			if (t < lengths[i]) {
				return i;
			}
			t -= lengths[i];
		}
		return lengths.length;
	}

	/** Seconds into the stage t falls in (past the end: seconds since it all finished). */
	public float into(float t) {
		for (float len : lengths) {
			if (t < len) {
				return t;
			}
			t -= len;
		}
		return t;
	}

	/** All the stages together. */
	public float length() {
		float sum = 0;
		for (float len : lengths) {
			sum += len;
		}
		return sum;
	}
}
