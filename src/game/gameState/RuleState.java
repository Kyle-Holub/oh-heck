package game.gameState;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;

import game.Game;
import game.main.MainCanvas;
import util.input.Keyboard;
import util.input.Mouse;


/**
 * The "How to Play" state of the game.
 * Simply updates and displays a scrollable page of text.
 * Reads the text from file, generates TextLayout objects
 * for the paragraph's alignment, and then renders.
 * 
 * @author Kyle Holub
 * @version 1.0.1
 * @since 1.0
 */
public class RuleState extends GameState {
	// TODO add mouse controls for rule screen
	// minimum scrolling value
	private static final int MIN_Y_SCROLL = -500;
	private static final int SCROLL_SPEED = 3;
	private static final int CLICK_BOX_HEIGHT = 100;

	// x padding
	private static final int PAD_X = 10;

	// y padding
	private static final int PAD_Y = PAD_X;

	// width of text area
	private static final float BREAK_WIDTH = (float)MainCanvas.SCREEN_W - 2 * PAD_X;

	// text font
	private static final Font TEXT_FONT = new Font("serif", Font.PLAIN, 16);

	// heading font
	private static final Font HEADING_FONT = new Font("serif", Font.BOLD, 20);

	// title height
	private static final int TITLE_HEIGHT = 16;

	// the titles for paragraphs
	private static final String[] TITLES = {"How to Play Oh Heck", "Cards", "Hands", "Objective", "Dealing", "Trump Suit", "Bidding", "Play", "Scoring", "User Interface", "Controls"};

	// the paragraph objects
	private static Paragraph[] p;

	// the scroll position
	private int scrollY;

	// keyboard & mouse
	private Keyboard k;
	private Mouse m;

	public RuleState(GameStateManager gsm) {
		this.gsm = gsm;
	}

	@Override
	public void init() { }

	@Override
	public void update() {
		processInput();
	}

	@Override
	public void draw(Graphics g) {
		g.setColor(Color.WHITE);
		Graphics2D g2 = (Graphics2D)g;

		/*
		 * init paragraphs if needed
		 * initialization must occur here, because access to the
		 * graphics object is required
		 */
		if (p == null) {

			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/files/rules.txt")));

				p = new Paragraph[TITLES.length];
				for (int i = 0; i < TITLES.length; i++) {
					p[i] = new Paragraph(br.readLine(), g2);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)
						br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		float drawPosY = PAD_Y + scrollY + TITLE_HEIGHT;
		for (int i = 0; i < p.length; i++) {

			// draw heading
			g2.setFont(HEADING_FONT);
			g2.drawString(TITLES[i], PAD_X, drawPosY);
			drawPosY += 4; // add difference between font sizes

			// draw paragraph lines
			g2.setFont(TEXT_FONT);
			for (int j = 0; j < p[i].getNumOfLines(); j++) {
				TextLayout layout = p[i].getLayoutAt(j);

				// move y-coordinate by the ascent of the layout.
				drawPosY += layout.getAscent();

				// draw the text layout
				layout.draw(g2, PAD_X, drawPosY);

				// move y-coordinate in preparation for next layout
				drawPosY += layout.getDescent() + layout.getLeading();
			}
			drawPosY += 2 * TITLE_HEIGHT; // add padding to the end of the paragraph
		}		
	}

	@Override
	public void processInput() {
		/*
		 * Mouse input
		 */
		m = Game.pollMouse();
		if (m.getPosition().y < CLICK_BOX_HEIGHT) {
			scrollUp();
		} else if (m.getPosition().y > MainCanvas.SCREEN_H - CLICK_BOX_HEIGHT) {
			scrollDown();
		}
		
		/*
		 *  keyboard input
		 */
		k = Game.pollKeyboard();
		if (k.keyDownOnce(KeyEvent.VK_ENTER) ||
				k.keyDownOnce(KeyEvent.VK_SPACE) ||
				k.keyDownOnce(KeyEvent.VK_ESCAPE)) {
			gsm.setState(GameStateManager.MENUSTATE);
		// down
		} else if (k.keyDown(KeyEvent.VK_DOWN)) {
			scrollDown();
		// up
		} else if (k.keyDown(KeyEvent.VK_UP)) {
			scrollUp();
		}
	}
	
	private void scrollUp() {
		if (scrollY < 0)
			scrollY += SCROLL_SPEED;
	}
	
	private void scrollDown() {
		if (scrollY > MIN_Y_SCROLL)
			scrollY -= SCROLL_SPEED;
	}


	/*
	 * A class to hold TextLayout objects, so they don't have to
	 * be dynamically allocated upon every drawn frame.
	 */
	private class Paragraph {
		private ArrayList<TextLayout> layouts;

		public Paragraph(String text, Graphics2D g) {

			// get font render context
			FontRenderContext frc = g.getFontRenderContext();

			// set up line break measurer
			AttributedString line = new AttributedString(text);
			AttributedCharacterIterator it = line.getIterator();
			int start = it.getBeginIndex();
			int end = it.getEndIndex();
			LineBreakMeasurer lbm = new LineBreakMeasurer(it, frc);

			// get lines until entire paragraph has been generated
			layouts = new ArrayList<TextLayout>();
			lbm.setPosition(start);
			while (lbm.getPosition() < end) {
				layouts.add(lbm.nextLayout(BREAK_WIDTH));
			}
		}

		// returns number of lines in the paragraph
		public int getNumOfLines() {
			return layouts.size();
		}

		// returns the TextLayout object for a given line index
		public TextLayout getLayoutAt(int index) {
			return layouts.get(index);
		}
	}
}
