package ohheck.anim;

/*
 * A jolt to the whole screen: a shake that dies away and a quick zoom in and back. The screen
 * reads the offset and zoom each frame from the clock.
 */
public final class Kick {

	private static final float SHAKE_LEN = 0.35f, SHAKE_SPEED = 55f, ZOOM_LEN = 0.45f;

	private float shakeAt = -99, shakeSize, zoomAt = -99, zoomSize;

	public void shake(float now, float size) {
		shakeAt = now;
		shakeSize = size;
	}

	public void zoom(float now, float size) {
		zoomAt = now;
		zoomSize = size;
	}

	public float dx(float now) {
		float p = (now - shakeAt) / SHAKE_LEN;
		return p < 0 || p >= 1 ? 0 : shakeSize * (1 - p) * (float) Math.sin(p * SHAKE_SPEED);
	}

	public float dy(float now) {
		float p = (now - shakeAt) / SHAKE_LEN;
		return p < 0 || p >= 1 ? 0 : shakeSize * 0.6f * (1 - p) * (float) Math.cos(p * SHAKE_SPEED * 0.8f);
	}

	public float zoom(float now) {
		float p = (now - zoomAt) / ZOOM_LEN;
		return p < 0 || p >= 1 ? 1 : 1 - zoomSize * (float) Math.sin(p * Math.PI);
	}

	public boolean active(float now) {
		return now - shakeAt < SHAKE_LEN || now - zoomAt < ZOOM_LEN;
	}
}
