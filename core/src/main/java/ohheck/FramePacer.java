package ohheck;

/* Drops the frame rate to 30 fps while nothing on screen is moving. */
final class FramePacer {

	private static final long IDLE_FRAME_NS = 1000000000L / 30;

	private long frameStart;
	private boolean idle;

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

	void endFrame(boolean nothingMoving) {
		idle = nothingMoving;
	}
}
