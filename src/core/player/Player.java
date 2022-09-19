package core.player;

import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.ListIterator;

import core.card.Card;
import core.card.Card.Suit;
import core.card.PlayerCard;

/**
 * An abstract class for players of the game.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public abstract class Player {
	
	// the player's name tag
	private PlayerUI nameTag;
	
	private boolean betMade;
	
	private int score;
	
	// the player's hand
	private volatile ArrayList<PlayerCard> hand;
	
	private boolean isCardPlayed;
	
	private boolean isDealer;
	
	private boolean turn;
	
	private PlayerCard cardPlayed;
	
	// the player's bet for a round
	private int bet;

	private int playerIndex;

	// the player's tricks obtained for a round
	private int tricks;

	/**
	 * Constructor
	 */
	public Player(String name, int i) {
		this.playerIndex = i;
		nameTag = new PlayerUI(name, i, this);
		hand = new ArrayList<PlayerCard>();
		score = 0;
		tricks = 0;
		
		betMade = false;
		isCardPlayed = false;
		cardPlayed = null;
		isDealer = false;
		turn = false;
	}
	
	public abstract PlayerCard playCard(ArrayList<PlayerCard> playedCards);

	public abstract void updateBet(int totalBets);

	public void newRound() {
		nameTag.reset();
		betMade = false;
		bet = 0;
	}
	
	public boolean isDealer() {
		return isDealer;
	}
	
	public boolean isTurn() {
		return turn;
	}
	
	public void setTurn(boolean t) {
		turn = t;
	}
	
	
	public void nextTrick() {
		isCardPlayed = false;
		hand.remove(cardPlayed);
		cardPlayed = null;
	}

	public String getName() {
		return nameTag.getName();
	}
	
	public void toggleDealer() {
		isDealer= !isDealer;
	}
	
	public PlayerUI getNameTag() {
		return nameTag;
	}
	
	public void drawNameTag(Graphics g) {
		nameTag.drawTag(g);
	}

	public int getNumberOfCards() {
		return hand.size();
	}
	
	public ListIterator<PlayerCard> getHandIterator() {
		return hand.listIterator();
	}
	
	public int getScore() {
		return score;
	}
	
	public ArrayList<PlayerCard> getHand() {
		return hand;
	}
	
	public int getBet() {
		return bet;
	}
	
	public int getTricks() {
		return tricks;
	}
	
	public void addTrick() {
		tricks++;
		this.nameTag.setTricks(tricks);
	}
	
	public void setBet(int bet) {
		this.bet = bet;
		this.nameTag.setBet(bet);
		betMade = true;
	}
	
	public boolean hasPlayedCard() {
		return isCardPlayed;
	}
	
	public void setCardPlayed(PlayerCard card) {
		isCardPlayed = true;
		cardPlayed = card;
	}
	
	public int getIndex() {
		return playerIndex;
	}
	
	public int getCardIndex(Card cardToPlay) {
		return hand.indexOf(cardToPlay);
	}
	
	public abstract void centerCards();
	
	public PlayerCard getHighestCard(ArrayList<PlayerCard> playableCards) {
		PlayerCard highest = playableCards.get(0);
		for (PlayerCard card: playableCards) {
			if (card.greaterFaceValue(highest.getFaceValue())) {
				highest = card;
			}
		}
		return highest;
	}

	public Card getHighestCard() {
		Card highest = hand.get(0);
		for (Card card: hand) {
			if (card.greaterFaceValue(highest.getFaceValue())) {
				highest = card;
			}
		}
		return highest;
	}

	public PlayerCard getLowestCard() {
		PlayerCard lowest = hand.get(0);
		for (PlayerCard card: hand) {
			if (!card.greaterFaceValue(lowest.getFaceValue())) {
				lowest = card;
			}
		}
		return lowest;
	}
	
	public PlayerCard getLowestCard(ArrayList<PlayerCard> cards) {
		PlayerCard lowest = cards.get(0);
		for (PlayerCard card: cards) {
			if (!card.greaterFaceValue(lowest.getFaceValue())) {
				lowest = card;
			}
		}
		return lowest;
	}
 
	public void setName(String name) {
		this.nameTag.setName(name);
	}

	/**
	 * Gives a card to the player.
	 * @param card a Card object
	 */
	public void giveCard(PlayerCard card) {
		hand.add(card);
	}
	
	/**
	 * Gives a group of cards to the player.
	 * @param cards an array of Card objects
	 */
	public void giveCards(PlayerCard[] cards) {
		for (PlayerCard card : cards) {
			this.giveCard(card);
		}
	}
	
	public boolean haveSuit(Suit suit) {
		for (Card card: hand) {
			if (card.sameSuit(suit)) {
				return true;
			}
		}
		return false;
	}

	public void updateCards() {
		for (PlayerCard card: hand) {
			card.update();
		}	
	}
	
	public void drawCards(Graphics g) {
		for (PlayerCard card: hand) {
			card.draw(g);
		}
	}
	
	public void tallyScore() {
		if (bet == tricks) {
			score += 10 + (2 * bet);
		} else {
			int i = Math.abs(bet - tricks);
			score += (i * -5);
		}
		bet = 0;
		tricks = 0;
	}
	
	public boolean hasMadeBet() {
		return betMade;
	}

	public PlayerCard getCardPlayed() {
		return cardPlayed;
	}

	public abstract Point getHome();
}
