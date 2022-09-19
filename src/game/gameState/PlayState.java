package game.gameState;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import core.RoundKeeper;
import core.card.Deck;
import core.player.ComputerPlayer;
import core.player.HumanPlayer;
import core.player.PlayerQueue;
import game.Game;
import game.main.Main;
import util.JukeBox;
import util.input.Keyboard;
import util.input.Mouse;

/**
 * The card playing state of the game.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class PlayState extends GameState {

	// keyboard
	private static Keyboard k;
	private static Mouse m;

	// player objects
	private static HumanPlayer user;
	private static PlayerQueue players;

	// round keeper
	private static RoundKeeper round;

	// images
	private static BufferedImage tableImage;
	private static BufferedImage deckImage;
	public static BufferedImage topCardImage;
	
	// playbox
	private Rectangle playBox = new Rectangle(150, 100, 300, 150);

	public PlayState(GameStateManager gsm) {
		this.gsm = gsm;
	}

	public void init() {

		// initialize players
		HumanPlayer player = new HumanPlayer("Player", 0);
		PlayState.user = player;
		ComputerPlayer[] AI = new ComputerPlayer[3];
		AI[0] = new ComputerPlayer("Tom", 1);
		AI[1] = new ComputerPlayer("Max", 2);
		AI[2] = new ComputerPlayer("Dan", 3);
		players = new PlayerQueue(player, AI);
		players.shuffle();

		// load images and music
		try {
			tableImage = ImageIO.read(getClass().getResourceAsStream("/images/table8.jpg"));
			deckImage = ImageIO.read(getClass().getResourceAsStream("/images/deck.png"));
			if (Game.playMusic) {
				JukeBox.load("/music/backTrack.mp3", "backTrack");
				JukeBox.loop("backTrack", 1, JukeBox.getFrames("backTrack")-1);// - 2200);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//initialize the round
		round = new RoundKeeper(players);
	}

	public void update() {

		processInput();

		round.update();
		
		if (round.isGameOver()) {
			gsm.setState(GameStateManager.GAMEOVERSTATE);
		}
	}

	public static int getTableWidth() {
		return tableImage.getWidth();
	}

	public void setTopCardImage(BufferedImage topCardImage) {
		PlayState.topCardImage = topCardImage;
	}

	public void draw(Graphics g) {
		// draw playing table
		g.drawImage(tableImage, 0, 0, null);

		// draw deck with top card
		if (!round.isDealingCards()) {
			g.drawImage(deckImage, Deck.DECK_X, Deck.DECK_Y, null);
			g.drawImage(topCardImage, Deck.DECK_X, Deck.DECK_Y, null);
		}
		// draw cards in hand
		players.drawCards(g);

		if (user.isTurn) {
			user.drawSelectedCard(g);
		}

		// draw player bar
		players.drawNameTags(g);

		// round number
		g.setColor(Color.BLACK);

		// draw bid UI
		round.drawShuffling(g);
		if (user.getIsBidUIDisplayed()) {
			user.drawBidUI(g);
		}

		if (round.isDisplayingWinner()) {
			round.drawWinner(g);
		} else if (round.isDisplayingHandResults()) {
			round.drawHandResults(g);
		} else if (round.isDisplayingGameOver()) {
			round.drawGameResults(g);
		}
		
		if (Main.DEBUG) { g.drawRect(playBox.x, playBox.y, playBox.width, playBox.height); }
	}

	public void processInput() {
		k = Game.pollKeyboard();
		m = Game.pollMouse();

		// if dealing cards
		if (round.isDealingCards()) {
			if (k.keyDownOnce(KeyEvent.VK_ENTER) || m.buttonDownOnce(1)) {
				round.skipDealing();
			}
		// if user has not made a bet and the dealer is done dealing
		} else if (!user.hasMadeBet() && !round.isDealingCards()) {
			int currChoice = user.getCurrChoice();
			
			// mouse
			if (user.setCurrChoice(m.getPosition()) && m.buttonDownOnce(1)) {
				user.selectBid();
			}
			
			// keyboard
			if (k.keyDownOnce(KeyEvent.VK_ENTER)) {
				user.selectBid();
			}
			if (k.keyDownOnce(KeyEvent.VK_LEFT)) {
				currChoice -= 1;
				if(currChoice < 0) {
					currChoice = user.getNumberOfCards();
				}
				user.setCurrChoice(currChoice);
			} else if (k.keyDownOnce(KeyEvent.VK_RIGHT)) {
				currChoice += 1;
				if(currChoice > user.getNumberOfCards()) {
					currChoice = 0;
				}
				user.setCurrChoice(currChoice);
			}
		// else playing cards
		} else if (!user.hasPlayedCard() && !round.isDisplayingHandResults()){
			int cardChoice = user.getCardChoice();
			
			// mouse
			if (m.buttonDown(1)) {
				// if card is already clicked, drag
				if (user.getHand().get(user.getCardChoice()).isClicked()) {
					user.getHand().get(user.getCardChoice()).drag(m.getPosition());
				} 
				// if card is clicked for the first time, toggle clicked
				else if (user.setCardChoice(m.getPosition())) {
					user.getHand().get(user.getCardChoice()).setClicked(true);
				}
			} 
			// else button is released or not pressed
			else {
				try {
					if (playBox.contains((user.getHand().get(user.getCardChoice()).getPosition()))) {
						user.selectCard();
					} else if (user.getHand().get(user.getCardChoice()).isClicked()) {
						user.getHand().get(user.getCardChoice()).setClicked(false);
					} 
					else {
						user.setCardChoice(m.getPosition());
					} 
				} catch (Exception e) {

				}
			}
			
			
			//keyboard
			if (k.keyDownOnce(KeyEvent.VK_ENTER)) {
				user.selectCard();
			}
			if (k.keyDownOnce(KeyEvent.VK_LEFT)) {
				boolean[] canPlay = user.getPlayableCards();
				// move left to the next available playable card
				if (canPlay != null) {
					for (int choice = cardChoice - 1, i = 0; i < canPlay.length - 1; i++, choice--) {
						if (choice < 0) {
							choice = user.getNumberOfCards() - 1;
						}
						if (canPlay[choice]) {
							user.setCardChoice(choice);
							break;
						}
					}
				}
			} else if (k.keyDownOnce(KeyEvent.VK_RIGHT)) {
				boolean[] canPlay = user.getPlayableCards();
				// move right to the next available playable card
				if (canPlay != null) {
					for (int choice = cardChoice + 1, i = 0; i < canPlay.length - 1; i++, choice++) {
						if (choice > user.getNumberOfCards() - 1) {
							choice = 0;
						}
						if (canPlay[choice]) {
							user.setCardChoice(choice);
							break;
						}
					}
				}
			}
		}
	}
}