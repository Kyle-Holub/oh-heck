package game.gameState;

import game.Game;
import util.input.Keyboard;
import util.input.Mouse;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

// TODO not implemented
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
		/* nothing to init */
    }

    @Override
    public void update() {
        processInput();
    }

    @Override
    public void draw(Graphics g) {
        g.drawString("Welcome", 200, 200);
        String nameString = "";
        for (int i = 0; i < name.size(); i++) {
            nameString += name.get(i);
        }
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
