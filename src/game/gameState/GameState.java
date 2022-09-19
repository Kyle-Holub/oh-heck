package game.gameState;

import java.awt.Graphics;

/** 
 * Abstract class for game states
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public abstract class GameState {
	
	protected GameStateManager gsm;
	
	public abstract void init();
	public abstract void update();
	public abstract void draw(Graphics g);
	public abstract void processInput();
}
