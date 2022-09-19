package game.gameState;

import java.awt.Graphics;
import java.util.ArrayList;

import util.JukeBox;

/**
 * Manager class for changing game states.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class GameStateManager {
	
	// array to hold all game states
	private ArrayList<GameState> gameStates;

	// index of the game state list
	private int currentState;

	public static final int MENUSTATE = 0;
	public static final int PLAYSTATE = 1;
	public static final int GAMEOVERSTATE = 2;
	public static final int RULESTATE = 3;
	public static final int OPTIONSTATE = 4;
	public static final int WELCOMESTATE = 5;

	public GameStateManager() {

		gameStates = new ArrayList<GameState>();
		JukeBox.init();
		currentState = WELCOMESTATE;
		gameStates.add(new MenuState(this));
		gameStates.add(new PlayState(this));
		gameStates.add(new GameOverState(this));
		gameStates.add(new RuleState(this));
		gameStates.add(new OptionState(this));
		gameStates.add(new WelcomeState(this));
	}

	public void setState(int state) {
		currentState = state;
		gameStates.get(currentState).init();
	}

	public void update() {
		gameStates.get(currentState).update();
	}

	public void draw(Graphics g) {
		gameStates.get(currentState).draw(g);
	}

	public void processInput() {
		gameStates.get(currentState).processInput();
	}
}
