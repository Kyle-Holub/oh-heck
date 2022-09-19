package core.card;

import java.awt.Point;
import java.awt.Rectangle;

import core.player.Player;

/**
 * Extended Card class for cards held by players.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class PlayerCard extends Card {
	
	// how fast cards move
	private boolean clicked;
	private boolean clickable;
	private Rectangle clickBox;
	private Player player;
	private int index;
	
	public PlayerCard(Card card, Player player, int index) {
		super(card);
		clicked = false;
		clickable = true;
		this.index = index;
		this.player = player;
		this.setPos(-Deck.WIDTH, -Deck.HEIGHT);
		this.setHome(-Deck.WIDTH, -Deck.HEIGHT);
	}
	
	public int getIndex() {
		return index;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public void setClickable(boolean b) {
		clickable = b;
	}
	
	public String toString() {
		return player.getName() + " played " + super.toString();
	}
	
	public String toCardString() {
		return super.toString();
	}
	
	public void setPos(int x, int y) {
		this.setX(x);
		this.setY(y);
		this.clickBox = new Rectangle(x, y, Deck.WIDTH, Deck.HEIGHT);
	}
	
	public void drag(Point p) {
		setPos(p.x - (Deck.WIDTH / 2), p.y - (Deck.HEIGHT / 2));
	}
	
	public void update() {
		if (!isClicked() && !isHome()) {
			updateTowardsHome(.05f);
		}
	}
	
	public boolean contains(Point p) {
		return clickBox.contains(p);
	}
	
	public boolean isClicked() { return clicked; }
	
	public void setClicked(boolean b) {
		if (clickable) {
			clicked = b;
		} 
	}
	
	public Rectangle getClickBox() {
		return clickBox;
	}

	public String getCardName() {
		return super.toString();
	}
	
} 
