package core.card;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

import core.card.Card.FaceValue;
import core.card.Card.Suit;
import core.player.Player;

/**
 * This class stores an ArrayList of 52 card objects.
 * 
 * @author Kyle Holub 
 * @version 1.0
 * @since 1.0 8/17/2015 initial version
 */
public class Deck {
	
	// the cards in the deck
	private ArrayList<Card> cards;
	
	// the x position of the deck
	public static final int DECK_X = 550;
			
	// the y position of the deck
	public static final int DECK_Y = 100;
	
	// card width and height
	public final static int WIDTH = 73;
	public final static int HEIGHT = 98;
	
	private static BufferedImage cardSet;
	private static BufferedImage[][] cardImages;
	
	// auxillary card images
	private static BufferedImage halfDeckImage;
	private static BufferedImage fullDeckImage;
	
	private static final short CARD_VALUES = 13;
	private static final short CARD_SUITS = 4;
	
	/**
	 * Constructor
	 */
	public Deck() {
		
		// load the images
		initCards();
		
		int suit = 0;
		
		this.cards = new ArrayList<Card>(52);
		for (Suit s : Suit.values()) {
			int value = 0;
			for (FaceValue v: FaceValue.values()) {
				cards.add(new Card(s, v, getImage(suit, value)));
				value++;
			}
			suit++;
		}
	}
	
	public void initCards() {
		try {
			halfDeckImage = ImageIO.read(getClass().getResourceAsStream("/images/halfDeck.png"));
			fullDeckImage = ImageIO.read(getClass().getResourceAsStream("/images/fullDeck.png"));
			cardSet = ImageIO.read(getClass().getResourceAsStream("/images/cards.png"));
			cardImages = new BufferedImage[CARD_SUITS][CARD_VALUES];
	
			BufferedImage subimage;
			for (int row = 0; row < CARD_SUITS; row++) {
				for(int col = 0; col < CARD_VALUES; col++) {
					subimage = cardSet.getSubimage(col * WIDTH,
							row * HEIGHT,
							WIDTH,
							HEIGHT);
					cardImages[row][col] = subimage;
				}
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shuffles the cards randomly
	 */
	public void shuffle() {
		Random r = new Random();
		// swap each element with a random element twice
		for (int i = 0; i < cards.size(); i++) {
			int j = r.nextInt(cards.size());
			Card temp = cards.get(i);   // temp = i
			cards.set(i, cards.get(j)); // i = j
			cards.set(j, temp);         // j = temp
		}
		for (int i = 0; i < cards.size(); i++) {
			int j = r.nextInt(cards.size());
			Card temp = cards.get(i);   // temp = i
			cards.set(i, cards.get(j)); // i = j
			cards.set(j, temp);         // j = temp
		}
	}
	
	/**
	 * Deals a card from the top of the deck.
	 * @return a Card object
	 */
	public Card dealCard() {
		Card card = cards.get(0);
		cards.remove(0);
		return card;
	}
	
	public PlayerCard[] dealCards(int amount, Player player) {
		PlayerCard[] cards = new PlayerCard[amount];
		for (int i = 0; i < amount; i++) {
			cards[i] = new PlayerCard(dealCard(), player, i);
		}
		return cards;
	}
	
	
	
	public BufferedImage getImage(int x, int y) {
		return cardImages[x][y];
	}
	
	public static BufferedImage getHalfDeckImage() {
		return halfDeckImage;
	}
	
	public static BufferedImage getFullDeckImage() {
		return fullDeckImage;
	}
}
