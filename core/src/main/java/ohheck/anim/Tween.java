package ohheck.anim;

/**
 * A movement over a window of time: it starts at {@code start}, lasts {@code length}
 * seconds, and eases as it goes. Ask it where it is at any time t, so the same tween serves
 * any number of frames, rewinds and loops; nothing has to be updated or reset.
 *
 * <pre>
 * Tween fly = Tween.over(0.35f, Ease.OUT);          // from t = 0
 * Tween fade = Tween.over(0.3f, Ease.LINEAR).after(1.2f);
 * float x = fly.value(t, fromX, toX);
 * </pre>
 */
public final class Tween {

	private final float start, length;
	private final Ease ease;

	private Tween(float start, float length, Ease ease) {
		this.start = start;
		this.length = length;
		this.ease = ease;
	}

	/** A tween lasting this long from t = 0. */
	public static Tween over(float length, Ease ease) {
		return new Tween(0, length, ease);
	}

	/** The same tween, starting at t = start instead. */
	public Tween after(float start) {
		return new Tween(start, length, ease);
	}

	public float start() {
		return start;
	}

	public float end() {
		return start + length;
	}

	/** How far through it is at t, eased: 0 before it starts, 1 once it's done. */
	public float progress(float t) {
		return ease.apply(raw(t));
	}

	/** How far through it is at t, not eased. */
	public float raw(float t) {
		return length <= 0 ? (t >= start ? 1 : 0) : Ease.clamp((t - start) / length, 0, 1);
	}

	/** Between from and to, as far as it's got at t. */
	public float value(float t, float from, float to) {
		return Ease.lerp(from, to, progress(t));
	}

	/** Whether it's under way at t (started and not finished). */
	public boolean active(float t) {
		return t >= start && t < end();
	}

	public boolean done(float t) {
		return t >= end();
	}
}
