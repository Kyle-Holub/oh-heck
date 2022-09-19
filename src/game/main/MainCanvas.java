package game.main;

import java.awt.Canvas;

import game.Game;

/**
 * The main canvas upon which the game is rendered
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class MainCanvas extends Canvas implements Runnable {

	// target frames per second
	private static final float MAX_FPS = 65;

	// max number of nanoseconds each frame can use to render
	private static final long MAX_CYCLE_TIME = (long) (1.0E9 / MAX_FPS);

	// number of delays before thread has to yield
	private static final byte DELAYS_PER_YIELD = 5;

	// max number of frames that can be skipped
	private static final byte MAX_FRAME_SKIPS = 5;
	
	public static final short SCREEN_W = 640;// 740; // 740
	public static final short SCREEN_H = 480; //580; // 580

	// Game object
	private Game game;

	// running flag
	private volatile boolean running;

	/**
	 * Initialize the game.
	 * Once the component has been added to a parent component,
	 * the game is initialized.
	 */
	public void addNotify() {
		super.addNotify();
		
		// initialize the game
		game = new Game(this);
		game.init();
	}

	/**
	 * Run the main loop.
	 * Each iteration of the loop keeps track of the time elapsed 
	 * since the last iteration and sleeps the processor as needed.
	 */
	public void run() {

		long beforeTime, afterTime, timeDiff = 0, millSleep, nanoSleep;

		// time measurement in the rare event that Thread.sleep is interrupted
		long overSleepTime = 0L;

		// extra time if a frame uses more than MAX_CYCLE_TIME to render
		long excessTime = 0L;

		// number of frames that have used excess time
		byte delays = 0;

		beforeTime = System.nanoTime();

		running = true;
		while (running) {

			// update and render the game
			game.update();
			game.render();

			// compute the sleep time
			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;
			nanoSleep = (MAX_CYCLE_TIME - timeDiff) - overSleepTime;

			// if sleep > 0, some time left in this cycle
			if (nanoSleep > 0) {
				
				// attempt to sleep
				try {
					
					millSleep = (nanoSleep / 1000000);
					Thread.sleep(millSleep, (int)(nanoSleep - (millSleep * 1000000)));

					// render cycle is over, get beforeTime
					beforeTime = System.nanoTime();
					
				} catch(InterruptedException e) {
					overSleepTime = (System.nanoTime() - afterTime) - nanoSleep;
				}

				// else sleep <= 0; frame took longer than MAX_CYCLE_TIME
			} else {

				// store excess render time value
				excessTime -= nanoSleep;
				overSleepTime = 0L;

				// if there have been to many delays
				if (++delays >= DELAYS_PER_YIELD) {
					
					// give another thread a chance to run
					Thread.yield();
					delays = 0;
				}
				beforeTime = System.nanoTime();
				
				/*
				 * If frame animation is taking too long, update
				 * the game state without rendering it, to get the
				 * updates/sec nearer to the required FPS.
				 */
				byte skips = 0;
				while ((excessTime > MAX_CYCLE_TIME) && (skips < MAX_FRAME_SKIPS)) {
					excessTime -= MAX_CYCLE_TIME;
					
					// update but don't render
					game.update();
					skips++;
				}	
			}
		}
	}
}
