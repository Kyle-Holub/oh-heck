package core.card;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A class which stores a card's suit and face value.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0 8/17/2015 initial version
 */
public class Card {

	private static BufferedImage cardBackImage;

	// suit
	private Suit suit;

	// face value
	private FaceValue value;

	// image
	private BufferedImage cardImage;

	// position
	private float posX;
	private float posY;

	// home position
	private int homeX;
	private int homeY;

	/**
	 * Constructor
	 * 
	 * @param suit
	 *            Suit
	 * @param value
	 *            FaceValue
	 */
	public Card(Suit suit, FaceValue value, BufferedImage cardImage) {
		this.suit = suit;
		this.value = value;
		this.cardImage = cardImage;
		try {
			cardBackImage = ImageIO.read(getClass().getResourceAsStream("/images/cardback.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static BufferedImage getCardBackImage() {
		return cardBackImage;
	}

	/**
	 * Constructor
	 * 
	 * @param card
	 */
	public Card(Card card) {
		this.suit = card.suit;
		this.value = card.value;
		this.cardImage = card.cardImage;
	}

	/**
	 * No Arg Constructor
	 */
	public Card() {

	}

	public void updateTowardsHome(float speed) {
		if (!isHome()) {
			float x = 0, y = 0;

			float dx = getHomeX() - getX();
			dx *= speed;
			x = dx;

			float dy = getHomeY() - getY();
			dy *= speed;
			y = dy;
			translate(x, y);
		}
	}

	public void setPos(int x, int y) {
		this.posX = x;
		this.posY = y;
	}

	public void setHome(int x, int y) {
		this.homeX = x;
		this.homeY = y;
	}

	public int getHomeX() {
		return homeX;
	}

	public int getHomeY() {
		return homeY;
	}

	public boolean isHome() {
		if (Math.abs((getHomeX() - getX())) < 2) {
			posX = homeX;
		}
		if (Math.abs((getHomeY() - getY())) < 2) {
			posY = homeY;
		}
		if (posX == homeX && posY == homeY) {
			setPos(homeX, homeY);
			return true;
		}
		return false;
	}

	public void translate(float x, float y) {
		posX += x;
		posY += y;
	}

	public void draw(Graphics g) {
		g.drawImage(this.getImage(), (int) posX, (int) posY, null);
	}

	public Point getPosition() {
		return new Point((int) posX, (int) posY);
	}

	public void setX(float x) {
		this.posX = x;
	}

	public void setY(float y) {
		this.posY = y;
	}

	public float getX() {
		return posX;
	}

	public float getY() {
		return posY;
	}

	/**
	 * Gets the suit.
	 * 
	 * @return Suit
	 */
	public Suit getSuit() {
		return suit;
	}

	/**
	 * Gets the face value.
	 * 
	 * @return FaceValue
	 */
	public FaceValue getFaceValue() {
		return value;
	}

	public BufferedImage getImage() {
		return cardImage;
	}

	/**
	 * Determines if this card is of the same suit.
	 * 
	 * @param suit Suit
	 * @return true if this card is of the same suit, else false.
	 */
	public boolean sameSuit(Suit suit) {
		if (this.suit.equals(suit)) {
			return true;
		}
		return false;
	}

	public boolean greaterFaceValue(FaceValue value) {
		if (this.value.getValue() > value.getValue()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns a string representation of the card.
	 */
	public String toString() {
		return value.toString() + " of " + suit.toString();
	}

	/**
	 * Enumeration for four possible suits.
	 */
	public enum Suit {
		CLUBS(0), SPADES(1), HEARTS(2), DIAMONDS(3);

		private int value;

		Suit(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	/**
	 * Enumeration for the face values.
	 */
	public enum FaceValue {
		ACE(12), TWO(0), THREE(1), FOUR(2), FIVE(3), SIX(4), SEVEN(5), EIGHT(6), NINE(7), TEN(8), JACK(9), QUEEN(
				10), KING(11);

		private int value;

		FaceValue(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}
}
