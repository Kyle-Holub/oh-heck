package game.gameState;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Scanner;

import game.Game;
import util.input.Keyboard;
import util.input.Mouse;

public class WelcomeState extends GameState {
	private GameStateManager gsm;
	private Keyboard k;
	private Mouse m;
	
	private ArrayList<String> name = new ArrayList<String>();
	
	public WelcomeState(GameStateManager gsm) {
		this.gsm = gsm;
	}
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		processInput();
	}

	@Override
	public void draw(Graphics g) {
		// TODO Auto-generated method stub
		g.drawString("Welcome", 200, 200);
		String nameString = "";
		for (int i = 0; i < name.size(); i++) {
			nameString += name.get(i);
		}
		int width = g.getFontMetrics().stringWidth(nameString);
		System.out.println("width=" + width);
		g.drawString(nameString, 10, 150);
	}

	@Override
	public void processInput() {
		k = Game.pollKeyboard();
		ArrayList<String> keysPressed = k.charsPressedOnce();



		// back space
		if (name.size() > 0 && k.keyDownOnce(KeyEvent.VK_BACK_SPACE)) {
			name.remove(name.size() - 1);
		} else if (name.size() < 20) {
			for (int i = 0; i < keysPressed.size(); i++) {
				name.add(keysPressed.get(i));
			}
			//System.out.println(name);
		}
	}
}
