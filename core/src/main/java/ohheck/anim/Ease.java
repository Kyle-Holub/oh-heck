package ohheck.anim;

/**
 * How a movement speeds up and slows down over its progress (0 to 1). The curves give the
 * same numbers as libGDX's Interpolation, so moving to them changes nothing on screen.
 */
public enum Ease {
	/** Steady. */
	LINEAR {
		@Override
		public float apply(float p) {
			return p;
		}
	},
	/** Fast, then settling (libGDX pow2Out): cards flying in, panels sliding to rest. */
	OUT {
		@Override
		public float apply(float p) {
			return (float) Math.pow(p - 1, 2) * -1 + 1;
		}
	},
	/** Slow, then away (libGDX pow2In): cards gathered up and taken off. */
	IN {
		@Override
		public float apply(float p) {
			return (float) Math.pow(p, 2);
		}
	},
	/** Gently in and out (smoothstep): fades. */
	SMOOTH {
		@Override
		public float apply(float p) {
			return p * p * (3 - 2 * p);
		}
	};

	/** The eased progress, for progress p from 0 to 1. */
	public abstract float apply(float p);

	/** v kept between lo and hi. */
	public static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : v > hi ? hi : v;
	}

	public static float lerp(float from, float to, float p) {
		return from + (to - from) * p;
	}
}
