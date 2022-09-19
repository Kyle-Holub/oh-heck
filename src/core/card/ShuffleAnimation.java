package core.card;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import core.RoundKeeper;
import core.player.PlayerQueue;
import util.JukeBox;

/**
 * A hard coded class for animating the shuffling
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class ShuffleAnimation {
	
	private static final int BASE_Y = 150;
	private static final int NUM_SHUFFLE_CARDS = 24;
	private static final int MAX_PAUSE = 20;
	private static final int SPEED = 6;
	private RoundKeeper round;
	private int startX;
	private int halfX;
	private int[] xPos;
	private int yPos0;
	private int yPos1;
	private int leftStop;
	private int index;
	private int pauseFrames;
	private Point[] homes;
	private Card[] cardsToDeal;
	private PlayerQueue q;
	
	private boolean phase1;
	private boolean phase2;
	private boolean phase3;
	private boolean phase4;
	private boolean phase5;
	private boolean phase6;
	private boolean shuffledOnce;
	private boolean isDealing;
	
	public ShuffleAnimation(int x, int y, int numCards, PlayerQueue q, RoundKeeper round) {
		this.q = q;
		this.round = round;
		homes = q.getHomePositions();
		startX = x - Deck.getHalfDeckImage().getWidth() / 2;
		cardsToDeal = new Card[numCards * homes.length];
		xPos = new int[NUM_SHUFFLE_CARDS];
		initSplitPositions();
		yPos0 = y;
		leftStop = x - 110;
		halfX = (startX - leftStop) / 2;
		index = 2;
		phase1 = true;
		pauseFrames = 0;
		isDealing = true;
		
		JukeBox.load("/sfx/shuffle.mp3", "shuffle");
		JukeBox.load("/sfx/playCard.mp3", "playCard");
	}
	
	public void initSplitPositions() {
		xPos[0] = startX;
		yPos0 = BASE_Y;
		xPos[1] = xPos[0];
		yPos1 = BASE_Y;
	}
	
	public boolean isDealing() {
		return isDealing;
	}
	
	public void update() {
		if (phase1) {
			// move to center of table
			for (int i = 0; i < SPEED; i++) {
				if (yPos0 < BASE_Y) {
					yPos0++;
				} else if (yPos0 > BASE_Y) {
					yPos0--;
				} else {
					phase1 = false;
					phase2 = true;
					break;
				}
			}
		} else if (phase2) {
			// split in two half decks
			for (int i = 0; i < SPEED; i++) {
				if (xPos[0] > leftStop) {
					xPos[0]--;
					xPos[1]++;
				} else {
					phase2 = false;
					phase3 = true;
					// init flippy card positions
					for (int j = 2; j < NUM_SHUFFLE_CARDS - 1; j += 2) {
						xPos[j] = xPos[0];
						xPos[j + 1] = xPos[1];
					}
					JukeBox.play("shuffle");
					break;
				}
			}
		} else if (phase3) {
			// start sliding card backs between the cards
			if (index < NUM_SHUFFLE_CARDS - 1) {
				xPos[index] += 6;
				xPos[index + 1] -= 6;
				if (xPos[index] >= startX) {
					index +=2;
				} else if (xPos[index] >= halfX && index + 2 < NUM_SHUFFLE_CARDS - 2){
					xPos[index + 2] += 3;
					xPos[index + 3] -= 3;
				}
			} else {
				phase3 = false;
				phase4 = true;
			} 
		} else if (phase4) {
			// pause after shuffling
			if (pauseFrames > MAX_PAUSE) {
				phase4 = false;
				if (shuffledOnce) {
					// if shuffled once, move to step 5
					phase5 = true;
					// init array of card back positions
					int j = 0;
					for (int i = 0, playerIndex = 0; i < cardsToDeal.length; i++, playerIndex++) {
						if (playerIndex >= homes.length) {
							playerIndex = 0;
							j++;
						}
						cardsToDeal[i] = new Card();
						cardsToDeal[i].setHome(homes[playerIndex].x + (j * 9), homes[playerIndex].y);
						cardsToDeal[i].setPos(startX, BASE_Y);
					}
					index = 0;
					JukeBox.play("playCard");
				} else {
					// reshuffle
					phase2 = true;
					initSplitPositions();
					shuffledOnce = true;
					pauseFrames = 0;
					index = 2;
				}
			} else {
				pauseFrames++;
			}
		} else if (phase5) {
			if (index < cardsToDeal.length) {
				// update card positions towards home
				cardsToDeal[index].updateTowardsHome(.15f);
				if (cardsToDeal[index].isHome()) {
					index++;
					JukeBox.play("playCard");
				}
			} else {
				phase5 = false;
				phase6 = true;
				q.centerCards();
				xPos[0] = startX;
				yPos0 = BASE_Y;
			}
		} else if (phase6) { // move deck back towards its home
			for (int i = 0; i < SPEED; i++) {
				if (xPos[0] < Deck.DECK_X) {
					xPos[0]++;
				}
				if (yPos0 < Deck.DECK_Y) {
					yPos0++;
				} else if (yPos0 > Deck.DECK_Y) {
					yPos0--;
				}
				if (xPos[0] == Deck.DECK_X && yPos0 == Deck.DECK_Y) {
					round.setDealingCards(false);
					phase6 = false;
				}
			}
		}
	} 

	public void drawDealCards(Graphics g) {
		
		if (phase1) {
			g.drawImage(Deck.getFullDeckImage(), xPos[0], yPos0, null);
		} else if (phase2) {
			g.drawImage(Deck.getHalfDeckImage(), xPos[1], yPos1, null);
			g.drawImage(Deck.getHalfDeckImage(), xPos[0], yPos0, null);
		} else if (phase3) {
			if (index < NUM_SHUFFLE_CARDS) {
				g.drawImage(Card.getCardBackImage(), xPos[index], yPos0, null);
				g.drawImage(Card.getCardBackImage(), xPos[index + 1], yPos0, null);
				if (index + 2 < NUM_SHUFFLE_CARDS - 3) {
					g.drawImage(Card.getCardBackImage(), xPos[index + 2], yPos0, null);
					g.drawImage(Card.getCardBackImage(), xPos[index + 3], yPos0, null);
				}
				if (index < NUM_SHUFFLE_CARDS / 2) {
					g.drawImage(Deck.getHalfDeckImage(), xPos[1], yPos1, null);
					g.drawImage(Deck.getHalfDeckImage(), xPos[0], yPos0, null);
				} else {
					g.drawImage(Deck.getHalfDeckImage(), startX, BASE_Y, null);
				}
			}else {
				g.drawImage(Deck.getHalfDeckImage(), startX, BASE_Y, null);
			}
		} else if (phase4) {
			g.drawImage(Deck.getFullDeckImage(), startX, BASE_Y, null);
		} else if (phase5) {
			g.drawImage(Deck.getFullDeckImage(), startX, BASE_Y, null);
			for (int i = 0; i <  cardsToDeal.length; i++) {
				g.drawImage(Card.getCardBackImage(), 
						(int)cardsToDeal[i].getX(), 
						(int)cardsToDeal[i].getY(), null);
			}
			if (index < cardsToDeal.length) {
				g.drawImage(Card.getCardBackImage(), 
						(int)cardsToDeal[index].getX(), 
						(int)cardsToDeal[index].getY(), null);
			}
		} else if (phase6) {
			g.drawImage(Deck.getFullDeckImage(), xPos[0], yPos0, null);
		}
		
		g.setColor(Color.BLACK);
		g.drawString("Press ENTER to skip", 10, 470);
		
	}

	public void setDealing(boolean b) {
		isDealing = false;
	}
	
	public void skipPositions() {
		q.centerCards();
	}
}
