package game.main;

import java.awt.EventQueue;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * Class which contains the main method of the game.
 * 
 * TODO include tie breaking case 
 * TODO display 1st through 4th place at end
 * TODO add fireworks or something at the end
 * TODO redo hand of 1 card animation
 * TODO display places after 1 card hand
 * TODO keep record of high scores
 * TODO when someone first opens the game, ask for their name
 * TODO add music to the title screen
 * 
 * DISPLAY and ANIMATION 
 * TODO gray out cards that are unplayable 
 * TODO gif visible on title screen
 * 
 * SETTINGS 
 * TODO change name from "player"
 * TODO enable/disable sound effects (not music)
 * 
 * 
 * @author Kyle Holub
 * @version 1.0.5 Fixed a few bugs
 * 			1.0.4 8/26/2017 Improved mouse support
 * 			1.0.3 8/24/2017 Added mouse support
 * 			1.0.2 08/23/2017 Made it possible to skip deal animation
 * 			1.0.1 04/07/2017 Adjust the AI
 * 			1.0 04/06/2017 Completing the game is now possible
 */
public class Main extends JFrame {

	public static final boolean DEBUG = false;
	public static final String VERSION = "v1.0.5";

	public static void main(String[] args) {
		final Main main = new Main();
		EventQueue.invokeLater(main::launch);
	}

	private void launch() {
		setTitle("Oh Heck!");
		ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/favicon2.png")));
		setIconImage(icon.getImage());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		getContentPane().add(new MainCanvas());

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
