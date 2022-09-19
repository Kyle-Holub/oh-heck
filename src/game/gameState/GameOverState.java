package game.gameState;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import game.Game;
import game.main.Main;
import game.main.MainCanvas;
import util.input.Keyboard;
import util.input.Mouse;

/**
 * The end state of the game
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class GameOverState extends GameState {

	private static Keyboard k;
	private static Mouse m;

	private GameStateManager gsm;

	private BufferedImage gameOver;
	private static final String[] OPTIONS = { "New Game", "Main Menu", "Exit" };
	
	// click boxes for menu buttons
	private static Rectangle[] clickBoxes = new Rectangle[OPTIONS.length];
	private static final int BOX_W = 300;
	private static final int BOX_H = 50;
	private static final int CLICK_X = MainCanvas.SCREEN_W / 2 - BOX_W / 2;
	private static final int CLICK_Y = MainCanvas.SCREEN_H / 2 - BOX_H + 10;

	private int curChoice = 0;

	public GameOverState(GameStateManager gameStateManager) {
		this.gsm = gameStateManager;

		try {
			gameOver = ImageIO.read(getClass().getResourceAsStream("/images/gameOver.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		// init click boxes
		for (int i = 0; i < OPTIONS.length; i++) {
			clickBoxes[i] = new Rectangle(CLICK_X, CLICK_Y + (BOX_H * i), BOX_W, BOX_H);
		}
	}

	@Override
	public void update() {
		processInput();
	}

	@Override
	public void draw(Graphics g) {
		// TODO draw highscore

		// draw game over image
		g.drawImage(gameOver, 70, 100, null);

		// draw menu options
		g.setFont(MenuState.OPTION_FONT);
		FontMetrics fm = g.getFontMetrics();
		int x, y;
		for (int i = 0; i < OPTIONS.length; i++) {
			x = MainCanvas.SCREEN_W / 2 - fm.stringWidth(OPTIONS[i]) / 2;
			y = MainCanvas.SCREEN_H / 2 + i * 50;
			if (curChoice == i) {
				g.setColor(Color.WHITE);
			} else {
				g.setColor(MenuState.TEXT_COLOR);
			}
			g.drawString(OPTIONS[i], x, y);
		}
		
		if (Main.DEBUG) {
			for (int i = 0; i < clickBoxes.length; i++) {
				g.drawRect(clickBoxes[i].x, clickBoxes[i].y, clickBoxes[i].width,
						clickBoxes[i].height); 
			}
		}
	}

	@Override
	public void processInput() {
		// mouse input
		m = Game.pollMouse(); 
		for (int i = 0; i < clickBoxes.length; i++) {
			if (clickBoxes[0].contains(m.getPosition())) { curChoice = 0; }
			if (clickBoxes[1].contains(m.getPosition())) { curChoice = 1; }
			if (clickBoxes[2].contains(m.getPosition())) { curChoice = 2; } 
		}
		if (m.buttonDownOnce(1)) { 
			for (int i = 0; i < clickBoxes.length; i++) { 
				if (clickBoxes[i].contains(m.getPosition())) { select(); } 
			}
		}
		
		// keyboard input
		k = Game.pollKeyboard();

		// enter key
		if (k.keyDownOnce(KeyEvent.VK_ENTER)) {
			select();

		// left key
		} else if (k.keyDownOnce(KeyEvent.VK_UP)) {
			curChoice--;
			if (curChoice < 0) {
				curChoice = OPTIONS.length - 1;
			}

		// right key
		} else if (k.keyDownOnce(KeyEvent.VK_DOWN)) {
			curChoice++;
			if (curChoice > OPTIONS.length - 1) {
				curChoice = 0;
			}
		}
	}

	private void select() {
		if (curChoice == 0) {
			gsm.setState(GameStateManager.PLAYSTATE);
		} else if (curChoice == 1) {
			gsm.setState(GameStateManager.MENUSTATE);
		} else if (curChoice == 2) {
			System.exit(0);
		}
	}
}