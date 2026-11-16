package ohheck;

/** Start-up options. The defaults are for normal play. */
public final class GameOptions {

	/** Seed for repeatable deals, or null for random games. */
	public Long seed = null;

	/** Let the bot play the human's seat too (used for automated screenshots). */
	public boolean autoplayHuman = false;

	/** Multiplies the speed of all pauses; 2 = twice as fast. */
	public float speed = 1f;

	/** Called once per frame after drawing, with a short description of what is on screen. */
	public FrameListener frameListener = null;

	public interface FrameListener {
		void onFrame(String state);
	}
}
