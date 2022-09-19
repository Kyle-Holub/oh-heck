package game.gameState;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import game.Game;
import util.input.Keyboard;

/**
 * The option menu of the game.
 * Hard coded to change music settings only
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class OptionState extends GameState {
	private static Keyboard k;
	private static Font font;
	
	public OptionState(GameStateManager gameStateManager) {
		this.gsm = gameStateManager;
		
		font = new Font("serif", Font.BOLD, 25);
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
		g.setFont(font);
		
		g.drawString("SETTINGS", 260, 100);
		g.drawString("Music", 200, 150);
		g.drawString("ON", 300, 150);
		g.drawString("OFF", 400, 150);
		
		// draw selection box
		g.setColor(Color.RED);
		if (Game.playMusic) {
			g.drawRect(295, 125, 60, 32);
		} else {
			g.drawRect(395, 125, 60, 32);
		}
		
		g.drawString("Use arrow keys + ENTER to select", 150, 200);
	}

	@Override
	public void processInput() {
		// key input
		k = Game.pollKeyboard();
		if (k.keyDownOnce(KeyEvent.VK_ENTER) || k.keyDownOnce(KeyEvent.VK_SPACE)) {
			gsm.setState(GameStateManager.MENUSTATE);
		}
		if (k.keyDownOnce(KeyEvent.VK_RIGHT) || k.keyDownOnce(KeyEvent.VK_LEFT)) {
			Game.playMusic = !Game.playMusic;
		}
	}
}
