package game.gameState;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import game.Game;
import game.main.Main;
import game.main.MainCanvas;
import util.input.Keyboard;
import util.input.Mouse;

/**
 * The main menu state of the game.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class MenuState extends GameState {

	// state manager
	private GameStateManager gsm;
	
	// keyboard & mouse
	private static Keyboard k;
	private static Mouse m;

	// menu choices
	private static int currentChoice = 0;
	private static final String[] OPTIONS = { "Start", "How to Play", "Options", "Exit" };
	
	// click boxes for menu buttons
	private static Rectangle[] clickBoxes = new Rectangle[OPTIONS.length];
	private static final int BOX_W = 300;
	private static final int BOX_H = 50;
	private static final int CLICK_X = MainCanvas.SCREEN_W / 2 - BOX_W / 2;
	private static final int CLICK_Y = MainCanvas.SCREEN_H / 2 - BOX_H + 10;

	// title text images
	private static BufferedImage titleText;
	private static BufferedImage loadingText;
	
	// title alignment
	private static int titlePosY;
	private static int titlePosX;

	// fonts
	public static final Font OPTION_FONT = new Font("Serif", Font.PLAIN, 48);
	private static final Font SELECT_FONT = new Font("Serif", Font.ITALIC, 48);
	private static final Font VERSION_FONT = new Font("Sans Serif", Font.PLAIN, 14);
	private static FontMetrics fm;

	// default text color
	public static final Color TEXT_COLOR = new Color(15, 25, 105);

	// flags for loading substate
	private boolean loading;
	private boolean loadingDisplayed;

	public MenuState(GameStateManager gsm) {
		this.gsm = gsm;
		init();
	}

	public void init() {

		// load images
		try {
			titleText = ImageIO.read(getClass().getResourceAsStream("/images/titleText.png"));
			loadingText = ImageIO.read(getClass().getResourceAsStream("/images/loadingText.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// center image positions
		titlePosX = MainCanvas.SCREEN_W / 2 - titleText.getWidth() / 2;
		titlePosY = MainCanvas.SCREEN_H / 2 - titleText.getHeight() - 50;

		// init click boxes
		for (int i = 0; i < OPTIONS.length; i++) {
			clickBoxes[i] = new Rectangle(CLICK_X, CLICK_Y + (BOX_H * i), BOX_W, BOX_H);
		}
	}

	public void update() {
		processInput();
		if (loading) {
			if (loadingDisplayed) {
				// switch to next game state only after loading screen is rendered
				loadingDisplayed = false;
				loading = false;
				gsm.setState(GameStateManager.PLAYSTATE);
			} else {
				loadingDisplayed = true;
			}
		}
	}

	public void draw(Graphics g) {
		g.setFont(OPTION_FONT);

		// draw title
		if (loading) {
			g.drawImage(loadingText, titlePosX, titlePosY, null);
		} else {

			// init FontMetrics
			if (fm == null) {
				fm = g.getFontMetrics();
			}

			// draw title
			g.drawImage(titleText, titlePosX, titlePosY, null);

			// draw menu OPTIONS
			for (int i = 0; i < OPTIONS.length; i++) {

				// select color
				if (i == currentChoice) {
					g.setColor(Color.BLUE);
					g.setFont(SELECT_FONT);
				} else {
					g.setColor(TEXT_COLOR);
					g.setFont(OPTION_FONT);
				}

				// center the x position based on string length, adjust y
				// position, and draw
				int totalWidth = (fm.stringWidth(OPTIONS[i]));
				int x = (MainCanvas.SCREEN_W - totalWidth) / 2;
				int y = (MainCanvas.SCREEN_H / 2) + i * fm.getAscent();
				g.drawString(OPTIONS[i], x, y);
			}

			// draw version info
			g.setColor(Color.WHITE);
			g.setFont(VERSION_FONT);
			g.drawString("\u00a92017 Kyle Holub", MainCanvas.SCREEN_W - 120, MainCanvas.SCREEN_H - 10);
			g.drawString(Main.VERSION, 5, MainCanvas.SCREEN_H - 10);

			if (Main.DEBUG) {
				for (int i = 0; i < clickBoxes.length; i++) {
					g.drawRect(clickBoxes[i].x, clickBoxes[i].y, clickBoxes[i].width,
							clickBoxes[i].height); 
				}
			}
		}
	}

	public void processInput() {

		// keyboard input
		k = Game.pollKeyboard();
		if (k.keyDownOnce(KeyEvent.VK_ENTER)) {
			select();
		}
		if (k.keyDownOnce(KeyEvent.VK_UP)) {
			currentChoice--;
			if (currentChoice == -1) {
				currentChoice = OPTIONS.length - 1;
			}
		}
		if (k.keyDownOnce(KeyEvent.VK_DOWN)) {
			currentChoice++;
			if (currentChoice == OPTIONS.length) {
				currentChoice = 0;
			}
		}

		// mouse input
		m = Game.pollMouse(); 
		for (int i = 0; i < clickBoxes.length; i++) {
			if (clickBoxes[0].contains(m.getPosition())) { currentChoice = 0; }
			if (clickBoxes[1].contains(m.getPosition())) { currentChoice = 1; }
			if (clickBoxes[2].contains(m.getPosition())) { currentChoice = 2; }
			if (clickBoxes[3].contains(m.getPosition())) { currentChoice = 3; } 
		}
		if (m.buttonDownOnce(1)) { 
			for (int i = 0; i < clickBoxes.length; i++) { 
				if (clickBoxes[i].contains(m.getPosition())) { select(); } 
			}
		}
	}

	private void select() {
		if (currentChoice == 0) {
			// start
			loading = true;
		}
		if (currentChoice == 1) {
			// help
			gsm.setState(GameStateManager.RULESTATE);
		}
		if (currentChoice == 2) {
			// options
			gsm.setState(GameStateManager.OPTIONSTATE);
		}
		if (currentChoice == 3) {
			// exit
			System.exit(0);
		}
	}
}
