package ohheck.anim;

/*
 * A movement over a time window: a delay, a length and an ease, asked for its progress at any
 * time.
 */
public final class Tween {

	private final float start, length;
	private final Ease ease;

	private Tween(float start, float length, Ease ease) {
		this.start = start;
		this.length = length;
		this.ease = ease;
	}

	public static Tween over(float length, Ease ease) {
		return new Tween(0, length, ease);
	}

	public Tween after(float start) {
		return new Tween(start, length, ease);
	}

	public float start() {
		return start;
	}

	public float end() {
		return start + length;
	}

	public float progress(float t) {
		return ease.apply(raw(t));
	}

	public float raw(float t) {
		return length <= 0 ? (t >= start ? 1 : 0) : Ease.clamp((t - start) / length, 0, 1);
	}

	public float value(float t, float from, float to) {
		return Ease.lerp(from, to, progress(t));
	}

	public boolean active(float t) {
		return t >= start && t < end();
	}

	public boolean done(float t) {
		return t >= end();
	}
}
