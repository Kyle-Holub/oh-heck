package core;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import core.card.Card;
import core.card.Card.Suit;
import core.card.CardComparator;
import core.card.Deck;
import core.card.PlayerCard;
import core.card.ShuffleAnimation;
import core.player.Player;
import core.player.PlayerQueue;
import game.gameState.PlayState;
import game.main.MainCanvas;

/**
 * A class for managing the game rounds.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0 8/21/2015 initial version
 */
public class RoundKeeper {
	
	// number of cards at the beginning
	private static final int NUM_CARDS_AT_START = 1;
	
	// time to wait when selecting winner (for suspense)
	private static final int WINNER_SELECT_TIME = 1;
	
	// time to display winner
	private static final int WINNER_DISPLAY_TIME = 2;
	
	// time to display results of a hand
	private static final int HAND_OVER_DISPLAY_TIME = 3;
	
	private static final int GAME_OVER_DISPLAY_TIME = 10;
	
	// player queue
	private PlayerQueue players;
	
	// cards per player
	private int cardsPerPlayer;
	
	// the deck of cards
	private Deck deck;
		
	// shuffle animation
	private ShuffleAnimation animation;
	
	// flag indicating if all players have submitted a bid TODO make non-static
	private static boolean allPlayersBid;
	
	// flag indicating whether all players have played a card
	private boolean allCardsPlayed;
	
	// flag for pause while selecting winner
	private boolean selectingWinner;
	
	// to display winner
	private boolean displayingWinner;
	
	// to display hand over
	private boolean displayingHandOver;
	
	// to display winner of the game
	private boolean displayingGameOver;
	
	// flag for game over
	private boolean gameOver;
	
	// indicates if the number of cards dealt is decreasing
	private boolean dealerDecreasing;
	
	// true if human player won a trick
	private boolean playerWon;
	
	// the trump suit
	private static Suit trumpSuit; // TODO make non-static
	
	// timer start
	private long timerStart;
	
	// winner font and string
	private Font winnerFont;
	private String winnerString;
	
	private BufferedImage trickWon;
	
	private Player gameWinner;
	
	/**
	 * Constructor
	 * @param players the queue of player objects
	 */
	public RoundKeeper(PlayerQueue players) {
		this.players = players;
		this.cardsPerPlayer = NUM_CARDS_AT_START;
		
		try {
			trickWon = ImageIO.read(getClass().getResourceAsStream("/images/trickWonText.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// init all flags
		dealerDecreasing = true;
		winnerFont = new Font("Serif", Font.BOLD, 26);
		
		// init deck and deal
		dealCards();
	}
	
	public void dealCards() {
		deck = new Deck();
		deck.shuffle();

		// init the shuffle animation object
		int x = players.getDealer().getNameTag().getPosX() + 
				players.getDealer().getNameTag().getWidth() / 2;
		int y;
		if (players.getDealer().equals(players.getUser())) {
			y = +600;
		} else {
			y = -100;
		}
		animation = new ShuffleAnimation(x, y, cardsPerPlayer, players, this);
		
		// deal user cards
		players.giveCards(deck, cardsPerPlayer);

		// init top card
		Card topCard = deck.dealCard();
		PlayState.topCardImage = topCard.getImage();
		trumpSuit = topCard.getSuit();
	}

	public void update() {
		
		// update the player objects
		players.update();
		
		// if dealing cards, update the animation
		if (animation.isDealing()) {
			animation.update();
		} 
		
		// if bids are not made, make bids
		else if (!allPlayersBid) { 
			players.updateBids(); 
		} 
		
		// if each player has not played a card, update cards
		else if (!allCardsPlayed) {
			players.playCards(); 
			allCardsPlayed = players.isAllCardsPlayed();
			if (allCardsPlayed) {
				selectingWinner = true;
				timerStart = System.currentTimeMillis();
			}
		}
		
		else if (selectingWinner) {
			// wait to select winner
			if ((System.currentTimeMillis() - timerStart) / 1000 > WINNER_SELECT_TIME) {
				selectingWinner = false;
				displayingWinner = true;
				// determine winner
				ArrayList<PlayerCard> playedCards = PlayerQueue.getPlayedCards();
				PlayerCard winningCard = CardComparator.determineWinner(PlayerQueue.getPlayedCards(), trumpSuit);
				Iterator<PlayerCard> it = playedCards.iterator();
				while (it.hasNext()) {
					it.next().setHome(winningCard.getPlayer().getHome().x, 
									  winningCard.getPlayer().getHome().y);
				}
				if (winningCard.getPlayer().equals(players.getUser())) {
					playerWon = true;
				}
				
				// add to their score
				winningCard.getPlayer().addTrick();
				
				// format the display string
				String won;
				boolean trump = false;
				
				// if the winning card is the same as the trump suit but not the same suit as the first card
				if (CardComparator.isSameSuit(winningCard, trumpSuit) 
						&& !CardComparator.isSameSuit(winningCard, PlayerQueue.getPlayedCards().get(0).getSuit())) {
					// the card is a trump card
					trump = true;
				}
				if (trump) {
					won = " trumped";
				} else {
					won = " won";
				}
				String s = winningCard.getPlayer().getName() + won + " with " +
						winningCard.getCardName();
				winnerString = s;
				
				// winner has next turn
				players.setTurn(winningCard.getPlayer());
				
				// start timer for displaying winner
				timerStart = System.currentTimeMillis();
			}
		} else  if (displayingWinner){	
			PlayerQueue.updatePlayedCards();
			if ((System.currentTimeMillis() - timerStart) / 1000 > WINNER_DISPLAY_TIME) {
				displayingWinner = false;
				players.initNextTrick();
				
				// check if hand is empty
				if (players.isHandEmpty()) {
					
					displayingHandOver = true;
					
					// start time for displaying results of hand
					timerStart = System.currentTimeMillis();
					
					// toggle dealer decreasing
					if (cardsPerPlayer <= 1) {
						dealerDecreasing = false;
					}
					players.getUser().playEndHandSound();
					
					if (dealerDecreasing) {
						cardsPerPlayer -= 1;
					} else {
						cardsPerPlayer += 1;
					}
					
				// hand is not empty, get ready for next trick
				} else {
					allCardsPlayed = false;
					playerWon = false;
					nextTrick();
				}
			}
		} else if (displayingHandOver) {
			if ((System.currentTimeMillis() - timerStart) / 1000 > HAND_OVER_DISPLAY_TIME) {
				displayingHandOver = false;
				// if game complete
				if (cardsPerPlayer >= 9 && !dealerDecreasing) {
					timerStart = System.currentTimeMillis();
					displayingGameOver = true;
					players.updateScores();
					players.resetBids();
					gameWinner = players.getWinner();
					System.out.println("Game complete");
				} else {
					nextTrick();
					handOver();
				}
			}
		} else if (displayingGameOver) {			
			if ((System.currentTimeMillis() - timerStart) / 1000 > GAME_OVER_DISPLAY_TIME) {
				System.out.println("GAME OVER!!");
				gameOver = true;
				nextTrick();
				handOver();
			}
			
		} else {
			System.out.println("error");
		}
	}
	
	private void nextTrick() {
		// get ready for next trick
		allCardsPlayed = false;
		playerWon = false;
		players.initNextTrick();
	}
	
	private void handOver() {
		allPlayersBid = false;
		players.updateScores();
		players.resetBids();
		dealCards();
	}
	
	public static Suit getTrumpSuit() {
		return trumpSuit;
	}
	
	/**
	 * @return number of cards per player
	 */
	public int getCardsPerPlayer() {
		return cardsPerPlayer;
	}

	public static void setIsAllPlayersBid(boolean b) {
		allPlayersBid = b;
	}
	
	public void setDealingCards(boolean t) {
		animation.setDealing(t);
	}

	public void drawShuffling(Graphics g) {
		if (isDealingCards()) {
			animation.drawDealCards(g);
		}
	}

	public boolean isDealingCards() {
		return animation.isDealing();
	}
	
	public boolean isDisplayingWinner() {
		return displayingWinner;
	}
	
	public void drawHandResults(Graphics g) {
		players.drawHandResults(g);
	}

	public void drawWinner(Graphics g) {
		
		g.setFont(winnerFont);
		g.setColor(Color.GRAY.brighter());
		FontMetrics fm = g.getFontMetrics();
		int x = MainCanvas.SCREEN_W / 2 - fm.stringWidth(winnerString) / 2;
		int y = MainCanvas.SCREEN_H / 2 - fm.getHeight() / 2;
		g.fill3DRect(x - 10,
				y - 10,
				fm.stringWidth(winnerString) + 20,
				fm.getHeight() + 20,
				true);
		
		g.setColor(Color.BLACK);
		g.drawString(winnerString, x, y + 26);
		
		if (playerWon) {
			g.drawImage(trickWon, 150, 150, null);
		}
		
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public boolean isDisplayingHandResults() {
		return displayingHandOver;
	}
	
	public Player getGameWinner() {
		return gameWinner;
	}

	public boolean isDisplayingGameOver() {
		return displayingGameOver;
	}

	public void drawGameResults(Graphics g) {
		g.setFont(new Font(Font.SERIF, Font.BOLD, 40));
		g.drawString(gameWinner.getName() + " won the game!", 50, 100);
	}

	public void skipDealing() {
		animation.setDealing(false);
		animation.skipPositions();
	}
}
