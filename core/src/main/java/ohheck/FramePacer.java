package ohheck;

/**
 * Holds the frame rate down while nothing on screen is moving, to save battery: an idle
 * frame waits out the rest of its time before the next one starts.
 */
final class FramePacer {

	/** Frame length when nothing is moving: 30 fps. */
	private static final long IDLE_FRAME_NS = 1000000000L / 30;

	private long frameStart;
	private boolean idle;

	/** At the start of a frame: after an idle one, waits until its time is up. */
	void startFrame() {
		long now = System.nanoTime();
		if (idle) {
			long wait = frameStart + IDLE_FRAME_NS - now;
			if (wait > 0) {
				try {
					Thread.sleep(wait / 1000000, (int) (wait % 1000000));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				now = System.nanoTime();
			}
		}
		frameStart = now;
	}

	/** At the end of a frame: whether the next may run slow (nothing is moving). */
	void endFrame(boolean nothingMoving) {
		idle = nothingMoving;
	}
}
