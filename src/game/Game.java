package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import game.gameState.GameStateManager;
import game.main.Main;
import game.main.MainCanvas;
import util.FrameRateCounter;
import util.input.Keyboard;
import util.input.Mouse;

/**
 * The main update and render loop for the game
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class Game {
	
	public static boolean playMusic;
	
	// canvas
	private MainCanvas canvas;
	
	// game state manager
	private static GameStateManager gsm;

	// input
	private static Keyboard keyboard;
	private static Mouse mouse;

	// rendering objects
	private Thread renderThread;
	private BufferStrategy bs;
	private static final int NUM_BUFFERS = 2;
	
	// fps counter
	private static FrameRateCounter fpsCounter;
	
	/**
	 * Constructor
	 */
	public Game(MainCanvas canvas) {
		this.canvas = canvas;
	}
	
	/**
	 * Initialize the render thread, and set up ActionListeners.
	 */
	public void init() {
		if (renderThread == null) {
			canvas.setBackground(Color.BLACK);
			canvas.setIgnoreRepaint(true);
			canvas.setSize(MainCanvas.SCREEN_W, MainCanvas.SCREEN_H);
			
			// initialize game state manager
			gsm = new GameStateManager();
			
			// initialize keyboard listener
			keyboard = new Keyboard();
			canvas.addKeyListener(keyboard);
			
			// initialize mouse listeners
			mouse = new Mouse(canvas);
			canvas.addMouseListener(mouse);
			canvas.addMouseMotionListener(mouse);
			canvas.addMouseWheelListener(mouse);
			
			// create rendering buffers
			canvas.createBufferStrategy(NUM_BUFFERS);
			bs = canvas.getBufferStrategy();
			
			// bring the canvas into focus
			canvas.requestFocus();
			
			// initialize the FPS counter
			fpsCounter = new FrameRateCounter();
			fpsCounter.initialize();
			
			// init play music flag
			playMusic = false;
			
			// start the rendering thread
			renderThread = new Thread(canvas);
			renderThread.start();
		}
	}
	
	/*
	 * Update game
	 */
	public void update() {
		gsm.update();
	}
	
	/*
	 * Render a single frame
	 */
	public void render() {
		do {
			do {
				Graphics g = null;
				try {
					g = bs.getDrawGraphics();
					g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
					draw(g);
				} finally {
					if (g != null) {
						g.dispose();
					}
				}
			} while (bs.contentsRestored());
			bs.show();
		} while (bs.contentsLost());
	}
	
	public void draw(Graphics g) {
		gsm.draw(g);
		
		// draw fps
		if (Main.DEBUG) {
			g.setColor(Color.WHITE);
			g.setFont(Game.getFPSFont());
			g.drawString(Game.getFPS(), 10, 15);
		}
	}
	
	public static Keyboard pollKeyboard() {
		keyboard.poll();
		return keyboard;
	}
	
	public static Mouse pollMouse() {
		mouse.poll();
		return mouse;
	}
	
	public static String getFPS() {
		fpsCounter.calculateFPS();
		return fpsCounter.getFPS();
	}
	
	public static Font getFPSFont() {
		return fpsCounter.getFont();
	}
}
