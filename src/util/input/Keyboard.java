package util.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

/**
 * Keeps track of all key presses.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0 February 13th 2015
 */
public class Keyboard implements KeyListener {
	
	// array of booleans for whether a key is pressed
	private boolean[] keys;
	
	// array of ints for key codes
	private short[] polled;
	
	/**
	 * Constructor
	 */
    public Keyboard() {
       keys = new boolean[256];
       polled = new short[256];
    }
    
    /**
     * Tests if a key is down.
     * 
     * @param keyCode the keyCode to check
     * @return true if the key is down
     */
    public boolean keyDown(int keyCode) {
       return polled[keyCode] > 0;
    }
   
    /**
     * Tests if a key is down only once.
     * 
     * @param keyCode the keyCode to check
     * @return true if the key has been pressed once
     */
    public boolean keyDownOnce(int keyCode) {
       return polled[keyCode] == 1;
    }
   
    /**
     * Updates the polled array with new key values.
     */
    public synchronized void poll() {
       for(int i = 0; i < keys.length; ++i) {
          if(keys[i]) {
             polled[i]++;
          } else {
            polled[i] = 0;
          }
       }
    }
    
    /**
     * Sets the keyCode's boolean value to true.
     */
    @Override
    public synchronized void keyPressed(KeyEvent e) {
       int keyCode = e.getKeyCode();
       if(keyCode >= 0 && keyCode < keys.length) {
          keys[keyCode] = true;
       }
    }
    
    /**
     * Sets the keyCode's boolean value to false.
     */
    @Override
    public synchronized void keyReleased(KeyEvent e) {
       int keyCode = e.getKeyCode();
       if(keyCode >= 0 && keyCode < keys.length) {
          keys[keyCode] = false;
       }
    }
    
    public ArrayList<String> charsPressedOnce() {
    	ArrayList<String> keysList = new ArrayList<String>();
    	boolean shiftPressed = false;
    	for (int i = 0; i < keys.length; i++) {
    		if (!shiftPressed && keys[i] && i == 16) {
    			shiftPressed = true;
    		} else if (keys[i] && polled[i] < 2) {
    			if (shiftPressed) {
    				keysList.add(KeyEvent.getKeyText(i));
    				System.out.println(i);
    			} else {
    				keysList.add(KeyEvent.getKeyText(i).toLowerCase());
    				System.out.println(i + 32);
    			}
    		}
    	}
    	return keysList;
    }
    
    
    public void keyTyped(KeyEvent e) {} // not needed 
}
