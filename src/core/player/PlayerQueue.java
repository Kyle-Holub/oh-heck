package core.player;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import javax.imageio.ImageIO;

import core.RoundKeeper;
import core.card.Card;
import core.card.Deck;
import core.card.PlayerCard;
import game.main.Main;

/**
 * Queue of players, to keep track of turns easier
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class PlayerQueue {

	private Queue<Player> q;

	private static ArrayList<PlayerCard> playedCards;
	private static HumanPlayer human;

	private static BufferedImage bidTricks;
	private static BufferedImage setText;

	/*
	 * Constructor
	 */
	public PlayerQueue(HumanPlayer player, ComputerPlayer[] AI) {

		try {
			bidTricks = ImageIO.read(getClass().getResourceAsStream("/images/bidTricks.png"));
			setText = ImageIO.read(getClass().getResourceAsStream("/images/setText.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		human = player;
		// init queue
		q = new LinkedList<Player>();

		// add AI players
		for (Player ai : AI) {
			q.add(ai);
		}
		// add human player
		q.add(player);

		playedCards = new ArrayList<PlayerCard>();
		q.peek().setTurn(true);
		// allPlayersBid = false;
	}

	public void resetBids() {
		// allPlayersBid = false;
		nextDealer();
		for (Player p : q) {
			p.newRound();
		}
	}

	public void nextDealer() {
		Player[] p = q.toArray(new Player[0]);
		for (int i = 0; i < p.length; i++) {
			if (p[i].isDealer()) {
				p[i].toggleDealer();
				if ((i + 1) >= p.length) {
					p[0].toggleDealer();
				} else {
					p[i + 1].toggleDealer();
				}
				nextTurn();
				nextTurn();
				break;
			}
			nextTurn();
		}
	}

	public void updateScores() {
		if (Main.DEBUG) { System.out.println("bet=" + human.getBet() + " tricks=" + human.getTricks()); }

		for (Player p : q) {
			p.tallyScore();
		}
	}

	public void giveCards(Deck deck, int cardsPerPlayer) {
		// give each player cards
		for (Player player : q) {
			player.giveCards(deck.dealCards(cardsPerPlayer, player));
		}
	}

	public void playCards() {
		q.peek().playCard(playedCards);
		// if player at font of queue has played card, send to back of queue
		if (q.peek().hasPlayedCard()) {
			playedCards.add(q.peek().getCardPlayed());
			nextTurn();
		}
	}

	public void centerCards() {
		for (Player p : q) {
			p.centerCards();
		}
	}

	public Point[] getHomePositions() {
		Player[] p = q.toArray(new Player[0]);
		Point[] pos = new Point[q.size()];
		for (int i = 0; i < p.length; i++) {
			pos[i] = p[i].getHome();
		}
		return pos;
	}

	public void initNextTrick() {
		for (Player p : q) {
			p.nextTrick();
		}
		playedCards = new ArrayList<PlayerCard>();
	}

	// TODO crappy method
	public int getNumberOfCards() {
		return q.peek().getNumberOfCards();
	}

	public void update() {
		for (Player player : q) {
			player.updateCards();
		}
	}

	public int getTotalBets() {
		int bets = 0;
		for (Player player : q) {
			bets += player.getBet();
		}
		return bets;
	}

	/*
	 * Updates the bids of all players
	 */
	public void updateBids() {
		q.peek().updateBet(getTotalBets());

		// if current player has bet, set next turn
		if (q.peek().hasMadeBet()) {
			// if player is dealer, all bids have been made
			if (q.peek().isDealer()) {
				RoundKeeper.setIsAllPlayersBid(true); // TODO make non-static
			}
			nextTurn();
		}
	}

	private void nextTurn() {
		q.peek().setTurn(false);
		q.add(q.peek());
		q.remove();
		q.peek().setTurn(true);
	}

	public void removeCards(ArrayList<PlayerCard> playedCards) {
		for (PlayerCard card : playedCards) {
			card.getPlayer().getHand().remove((Card) card);
		}
		playedCards = new ArrayList<PlayerCard>();
	}

	public void drawCards(Graphics g) {
		for (Player player : q) {
			player.drawCards(g);
		}
	}

	public void drawNameTags(Graphics g) {
		for (Player player : q) {
			player.drawNameTag(g);
		}
	}

	public void shuffle() {
		Random rand = new Random();
		for (int i = 0; i < rand.nextInt(q.size()); i++) {
			nextTurn();
		}
		q.peek().toggleDealer();
		// PlayState.display(q.peek().getName() + " is the dealer.");
		nextTurn();
	}

	public void setTurn(Player p) {
		while (!q.peek().equals(p)) {
			nextTurn();
		}
	}

	public HumanPlayer getUser() {
		return human;
	}

	/**
	 * @return playedCards
	 */
	public static ArrayList<PlayerCard> getPlayedCards() {
		return playedCards;
	}

	public boolean isHandEmpty() {
		return q.peek().getHand().isEmpty();

	}

	/**
	 * Checks if every player has played a card
	 * 
	 * @return true if all players have played their cards false if one player
	 *         has not played a card
	 */
	public boolean isAllCardsPlayed() {
		for (Player player : q) {
			if (!player.hasPlayedCard()) {
				return false;
			}
		}
		return true;
	}

	public Player getDealer() {
		for (Player player : q) {
			if (player.isDealer()) {
				return player;
			}
		}
		return null;
	}

	public static void updatePlayedCards() {
		for (PlayerCard p : playedCards) {
			p.updateTowardsHome(.03f);
		}

	}

	public Player getWinner() {
		Player[] players = new Player[q.size()];
		players = q.toArray(players);
		Player winner = players[0];
		for (int i = 1; i < players.length; i++) {
			if (players[i].getScore() > winner.getScore()) {
				winner = players[i];
			}
		}
		return winner;
	}

	public void drawHandResults(Graphics g) {
		// for each player, display if they made their bid or were set
		g.setFont(new Font("serif", Font.BOLD, 30));
		for (Player p : q) {
			boolean set = false;
			if (p.getBet() != p.getTricks()) {
				set = true;
			}
			if (p.equals(human)) {
				if (set) {
					g.drawImage(setText, p.getHome().x - 105, p.getHome().y, null);
				} else {
					g.drawImage(bidTricks, p.getHome().x - 115, p.getHome().y, null);
				}
			} else {
				String s = "Bid Tricks";
				if (set) {
					s = "Set!";
				}
				g.drawString(s, p.getHome().x, p.getHome().y + 25);
			}
		}
	}
}