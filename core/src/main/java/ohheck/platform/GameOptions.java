package ohheck.platform;

/** Start-up options. The defaults are for normal play. */
public final class GameOptions {

	/** Seed for repeatable deals, or null for random games. */
	public Long seed = null;

	/** Save the game in progress on the device (off for automated screenshot runs). */
	public boolean persist = true;

	/** Statistics to start with in place of the saved ones (the screenshot run fills some in), or null. */
	public ohheck.records.Stats stats;

	/** Let the bot play the human's seat too (used for automated screenshots). */
	public boolean autoplayHuman = false;

	/** Multiplies the speed of all pauses; 2 = twice as fast. */
	public float speed = 1f;

	/**
	 * If above 0, every frame advances the game by exactly this many seconds
	 * instead of the real time since the last frame, so a scripted run goes as
	 * fast as frames can be drawn. (Screens cap one frame's step at 0.1 s.)
	 */
	public float fixedStep = 0f;

	/** Hands (1-based) that autoplay resolves instantly without drawing them. */
	public java.util.Set<Integer> skipHands = new java.util.HashSet<Integer>();

	/** Called once per frame after drawing, with a short description of what is on screen (used for automated screenshots). */
	public FrameListener frameListener = null;

	public interface FrameListener {
		void onFrame(String state);
	}
}
