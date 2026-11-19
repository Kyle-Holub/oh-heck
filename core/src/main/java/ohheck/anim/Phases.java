package ohheck.anim;

/** Consecutive stages of fixed lengths, such as the shuffle's gather, riffles and return. */
public final class Phases {

	private final float[] lengths;

	public Phases(float... lengths) {
		this.lengths = lengths.clone();
	}

	public int at(float t) {
		for (int i = 0; i < lengths.length; i++) {
			if (t < lengths[i]) {
				return i;
			}
			t -= lengths[i];
		}
		return lengths.length;
	}

	public float into(float t) {
		for (float len : lengths) {
			if (t < len) {
				return t;
			}
			t -= len;
		}
		return t;
	}

	public float length() {
		float sum = 0;
		for (float len : lengths) {
			sum += len;
		}
		return sum;
	}
}
