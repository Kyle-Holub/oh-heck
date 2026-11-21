package ohheck.anim;

/* Easing curves over progress 0 to 1; the same numbers as libGDX's Interpolation. */
public enum Ease {
	LINEAR {
		@Override
		public float apply(float p) {
			return p;
		}
	},
	OUT {
		@Override
		public float apply(float p) {
			return (float) Math.pow(p - 1, 2) * -1 + 1;
		}
	},
	IN {
		@Override
		public float apply(float p) {
			return (float) Math.pow(p, 2);
		}
	},
	SMOOTH {
		@Override
		public float apply(float p) {
			return p * p * (3 - 2 * p);
		}
	};

	public abstract float apply(float p);

	public static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : v > hi ? hi : v;
	}

	public static float lerp(float from, float to, float p) {
		return from + (to - from) * p;
	}
}
