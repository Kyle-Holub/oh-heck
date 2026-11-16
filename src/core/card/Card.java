package core.card;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import rules.Rank;
import rules.Suit;

/**
 * A card on screen: a rules.Card plus its image and position.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0 8/17/2015 initial version
 */
public class Card {

	private static BufferedImage cardBackImage;

	// the card's suit and rank
	private rules.Card card;

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
	 * @param card
	 *            the card's suit and rank
	 */
	public Card(rules.Card card, BufferedImage cardImage) {
		this.card = card;
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
		this.card = card.card;
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
		return card.suit();
	}

	/**
	 * Gets the rank.
	 * 
	 * @return Rank
	 */
	public Rank getFaceValue() {
		return card.rank();
	}

	/**
	 * The card's suit and rank, for use with the rules package.
	 */
	public rules.Card rulesCard() {
		return card;
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
		return card.suit() == suit;
	}

	/**
	 * Returns a string representation of the card.
	 */
	public String toString() {
		return card.toString();
	}
}
