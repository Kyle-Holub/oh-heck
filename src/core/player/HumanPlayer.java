package core.player;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import core.card.Card;
import core.card.Card.Suit;
import core.card.Deck;
import core.card.PlayerCard;
import game.main.MainCanvas;
import util.JukeBox;

/**
 * The class for the human player
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class HumanPlayer extends Player {

	// the home position for played cards
	public static final Point HOME = new Point(MainCanvas.SCREEN_W / 2, MainCanvas.SCREEN_H / 2 - 33);

	// bidding ui dimensions
	private static final int LINE_HEIGHT = 16;
	private static final int UI_WIDTH = MainCanvas.SCREEN_W / 2;
	private static final int UI_HEIGHT = MainCanvas.SCREEN_H / 4;
	private static final int UI_X = MainCanvas.SCREEN_W / 2 - UI_WIDTH / 2;
	private static final int UI_Y = MainCanvas.SCREEN_H / 2 - UI_HEIGHT / 2;
	private static final int SLIDER_WIDTH = UI_WIDTH - 30;
	private static final int SLIDER_HEIGHT = LINE_HEIGHT * 2;
	private static final int SLIDER_X = UI_X + (UI_WIDTH - SLIDER_WIDTH) / 2;
	private static final int SLIDER_Y = UI_Y + LINE_HEIGHT * 4;
	private static final int LINE_INDENT = UI_X + 10;
	private static final String LAST_PLAYER_PROMPT = "You are last and cannot bid: ";
	private static final String NOT_LAST_PLAYER_PROMPT = "You are not the last player to bid ";
	private static final String BID_WHATEVER_PROMPT = "You are last and can bid anything";
	private static final String ERROR_MESSAGE = "Sorry, you cannot bid ";
	public static final int CARD_BASE = 345; // the baseline for player's cards
	public static final int TAG_Y = 460; // the y alignment for the players name tag
	
	public static BufferedImage makeBidText;
	private boolean errorDisplayed;
	private int currChoice;
	private int cardChoice;
	private int totalBets;
	private boolean[] playableCards;
	public boolean cardsHighlighted;
	public boolean isTurn;
	private boolean isBidUIDisplayed;
	private PlayerCard selectedCard;
	private Rectangle[] bidHitBoxes;

	// a temp card object used to draw the selection outline
	private static Card tempCard;

	public HumanPlayer(String name, int i) {
		super(name, i);
		currChoice = 0;
		isBidUIDisplayed = false;
		cardsHighlighted = false;
		isTurn = false;
		bidHitBoxes = new Rectangle[0];
		
		try {
			makeBidText = ImageIO.read(getClass().getResourceAsStream("/images/makeBidText.png"));
			JukeBox.load("/sfx/win.mp3", "win");
			JukeBox.load("/sfx/lose.mp3", "lose");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void giveCards(PlayerCard[] cards) { super.giveCards(cards); }
	
	public boolean getIsBidUIDisplayed() {
		return isBidUIDisplayed;
	}

	public ArrayList<PlayerCard> getCards() {
		return getHand();
	}

	public void updateBet(int totalBets) {
		if (!isBidUIDisplayed && !hasMadeBet()) {
			this.totalBets = totalBets;
			currChoice = 0;
			isTurn = true;
			isBidUIDisplayed = true;
		}

	}

	public boolean[] getPlayableCards() {
		return playableCards;
	}

	public void setPlayableCards(Suit firstCardSuit) {
		playableCards = new boolean[this.getNumberOfCards()];
		// if suit is null, there is no first card, play anything
		if (firstCardSuit == null) {
			for (int i = 0; i < playableCards.length; i++) {
				playableCards[i] = true;
			}
		} else {
			int i = 0;
			boolean playable = false;
			// check for cards of the same suit as first card
			for (Card card: this.getHand()) {
				if (card.sameSuit(firstCardSuit)) {
					playableCards[i] = true;
					playable = true;
				}
				i++;
			}
			// if still no cards match, all cards are playable
			if (!playable) {
				i = 0;
				for (i = 0; i < getHand().size(); i++) {
					playableCards[i] = true;
				}
			} 
		}

		// set currChoice to first available card
		for (int i = 0; i < getHand().size(); i++) {
			if (playableCards[i]) {
				cardChoice = i;
				break;
			}
		}
	}


	@Override
	public PlayerCard playCard(ArrayList<PlayerCard> playedCards) {
		if (!hasPlayedCard() && !cardsHighlighted) {
			if (playedCards.isEmpty()) {
				setPlayableCards(null);
			} else {
				setPlayableCards(playedCards.get(0).getSuit());
			}
			cardsHighlighted = true;
			isTurn = true;
		}
		return null;
	}

	public void drawSelectedCard(Graphics g) {
		if (hasMadeBet()) {
			Graphics2D g2 = (Graphics2D)g;
			Stroke oldStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(2.0f));

			tempCard = this.getCardAtIndex(cardChoice);
			g2.setColor(Color.RED);
			try {
				g2.drawRect((int)tempCard.getX(), (int)tempCard.getY(), tempCard.getImage().getWidth(), tempCard.getImage().getHeight());
			} catch (Exception e) {
				System.out.println("ERROR drawing selected card. cardChoice=" + cardChoice);
			}
			g2.setStroke(oldStroke);
		}
	}

	private Card getCardAtIndex(int choice) {
		Iterator<PlayerCard> it = getCards().iterator();
		int index = 0;
		while (it.hasNext()) {
			if (index == choice) {
				return it.next();
			} else {
				index++;
				it.next();
			}
		}
		return null;

	}
	
	public void playEndHandSound() {
		if (getBet() == getTricks()) {
			JukeBox.play("win");
		} else {
			JukeBox.play("lose");
		}
	}

	public void selectCard() {
		if (isTurn && !this.hasPlayedCard()) {
			PlayerCard card = getHand().get(getCardChoice());
			card.setHome(HOME.x, HOME.y);
			setCardPlayed(card);
			card.setClickable(false);
			cardsHighlighted = false;
			isTurn = false;
			JukeBox.play("playCard");
			setCardChoice(0);
		}
	}

	public void drawBidUI(Graphics g) {
		// draw UI box background
		g.setColor(Color.GRAY.brighter());
		g.fill3DRect(UI_X, UI_Y, UI_WIDTH, UI_HEIGHT, true);

		
		// draw previous bid total
		g.setColor(Color.BLACK);
		String s = "Total bids made: " + totalBets;
		g.drawString(s, LINE_INDENT, UI_Y + LINE_HEIGHT * 2);

		// draw last player prompt
		int cantBid = this.getNumberOfCards() - totalBets;
		if (isDealer()) {
			if (cantBid < 0) {
				s = BID_WHATEVER_PROMPT;
			} else {
				s = LAST_PLAYER_PROMPT + " " + cantBid;
			}
		}
		else {
			s = NOT_LAST_PLAYER_PROMPT;
		}
		g.drawString(s, LINE_INDENT, UI_Y + LINE_HEIGHT * 3);

		// draw slider
		g.setColor(Color.gray);
		g.fillRect(SLIDER_X, SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT);

		// draw numbers
		g.setColor(Color.BLACK);
		int maxBid = getNumberOfCards() + 1;
		int padding = ((UI_WIDTH - (4 * maxBid)) / maxBid + 1);
		for (int i = 0; i < maxBid; i++) {
			int x = SLIDER_X + (i * 3) + (padding * i) + 1;
			int y = SLIDER_Y + 20;
			g.drawString(i + "", x, y);
		}

		// draw selection box
		int x = SLIDER_X + (currChoice * 3) + (padding * currChoice);
		int y = SLIDER_Y + 5;
		g.setColor(Color.RED);
		g.drawRect(x, y, 15, 20);
		
		// init hitboxes
		bidHitBoxes = new Rectangle[maxBid];
		for (int i = 0; i < maxBid; i++) {
			bidHitBoxes[i] = new Rectangle((SLIDER_X + (i * 3) + (padding * i)), SLIDER_Y + 5, 15, 20);
		}

		// draw error message box if needed
		if (errorDisplayed) {
			g.drawString(ERROR_MESSAGE + cantBid, LINE_INDENT, UI_Y + LINE_HEIGHT * 7);
		}

		g.drawImage(makeBidText, 90, 121, null);
	}

	public void selectBid() {
		if (isDealer() && currChoice == this.getNumberOfCards() - totalBets) {
			if (!errorDisplayed) {
				errorDisplayed = true;
			}
		} else {
			HumanPlayer.this.setBet(currChoice);
			errorDisplayed = false;
		}
		isBidUIDisplayed = false;
		isTurn = false;
	}

	public void centerCards() {
		ListIterator<PlayerCard> it = getHand().listIterator();
		PlayerCard p;
		int cardPad = (MainCanvas.SCREEN_W - (Deck.WIDTH * getHand().size())) / (getHand().size() + 1);
		while (it.hasNext()) {
			int x = (it.nextIndex() * Deck.WIDTH) + (cardPad * (it.nextIndex() + 1));
			int y = CARD_BASE;
			p = it.next();
			p.setHome(x, y);
			p.setPos(HOME.x, HOME.y);
		}
	}

	public int getCurrChoice() {
		return currChoice;
	}

	public void setCurrChoice(int currChoice) {
		this.currChoice = currChoice;
	}

	public int getCardChoice() {
		return cardChoice;
	}

	public void setCardChoice(int cardChoice) {
		this.cardChoice = cardChoice;
	}

	@Override
	public Point getHome() {
		Point p = new Point();
		p.x = getNameTag().getPosX();
		p.y = getNameTag().getPosY() - 120;
		return p;
	}

	public PlayerCard getSelectedCard() {
		return selectedCard;
	}
	
	public void setSelectedCard(PlayerCard card) {
		this.selectedCard = card;
	}

	public boolean setCurrChoice(Point position) {
		for (int i = 0; i < bidHitBoxes.length; i++) {
			if (bidHitBoxes[i].contains(position)) {
				currChoice = i;
				return true;
			}
		}
		return false;
	}

	public boolean setCardChoice(Point position) {
		if (!cardsHighlighted) {

		} else {
			for (int i = 0; i < getHand().size(); i++) {
				if (playableCards[i]) {
					if (getHand().get(i).contains(position)) {
						cardChoice = i;
						return true;
					}
				}
			}
		}
		return false;
	}
}
