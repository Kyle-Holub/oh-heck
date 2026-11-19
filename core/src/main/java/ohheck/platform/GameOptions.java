package ohheck.platform;

/** How a launcher runs the game; the defaults are normal play. */
public final class GameOptions {

	public Long seed = null;

	public boolean persist = true;

	public ohheck.records.Stats stats;

	public boolean autoplayHuman = false;

	public float speed = 1f;

	public float fixedStep = 0f;

	public java.util.Set<Integer> skipHands = new java.util.HashSet<Integer>();

	public FrameListener frameListener = null;

	public interface FrameListener {
		void onFrame(String state);
	}
}
