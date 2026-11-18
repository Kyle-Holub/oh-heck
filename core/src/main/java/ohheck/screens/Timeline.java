package ohheck.screens;

/**
 * The clock of one tutorial scene: seconds into the scene, which loops (with a pause
 * after it) or plays once and holds its last frame. Scenes ask it where they are:
 * {@link #at} to ease something in, {@link #passed} for a moment that happens once per
 * loop, {@link #reached} and {@link #count} for things that switch on at set times.
 * No libGDX, so it's tested in plain JUnit.
 */
final class Timeline {

	private float stepT, t, lastT;

	/** Back to the start of a scene. */
	void restart() {
		stepT = 0;
		t = lastT = 0;
	}

	/**
	 * Moves on by dt. A looping scene of this length plays again after a pause of hold;
	 * one that doesn't loop stops at its end.
	 */
	void advance(float dt, float length, boolean loops, float hold) {
		stepT += dt;
		lastT = t;
		t = loops ? stepT % (length + hold) : Math.min(stepT, length);
		if (t < lastT) {
			lastT = -1; // the scene looped: its moments happen again
		}
	}

	/** Seconds into the scene (this loop). */
	float t() {
		return t;
	}

	/** Seconds since the scene was first shown, counting every loop. */
	float shown() {
		return stepT;
	}

	/** 0 before a, 1 after b, eased between. */
	float at(float a, float b) {
		float k = Math.max(0, Math.min(1, (t - a) / (b - a)));
		return k * k * (3 - 2 * k);
	}

	/** Whether the scene just passed time a (once per loop). */
	boolean passed(float a) {
		return lastT < a && t >= a;
	}

	/** Whether the scene has reached time a (this loop). */
	boolean reached(float a) {
		return t >= a;
	}

	/** How many of these times (in order) the scene has reached: for things that arrive one by one. */
	int count(float... times) {
		int n = 0;
		while (n < times.length && t >= times[n]) {
			n++;
		}
		return n;
	}
}
