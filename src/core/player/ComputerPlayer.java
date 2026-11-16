package core.player;

import java.awt.Point;
import java.util.ArrayList;

import core.RoundKeeper;
import core.card.Card;
import core.card.CardComparator;
import core.card.Deck;
import core.card.PlayerCard;
import game.main.Main;
import rules.Bot;
import util.JukeBox;

/**
 * A computer player on screen. Its decisions come from {@link rules.Bot}.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class ComputerPlayer extends Player {

	private static final float DECISION_TIME = .5f;
	private static final int HOME_Y = 100;
	private static final Bot BOT = new Bot();

	private int homeAxis;
	// private int playerNumber;
	private long timerStart;
	private boolean timerSet;

	public ComputerPlayer(String name, int i) {
		super(name, i);
		timerSet = false;

		homeAxis = getNameTag().getPosX() + (PlayerUI.TAG_W / 2) - (Deck.WIDTH / 2);
	}

	public void updateBet(int totalBids) {
		if (!timerSet) {
			timerStart = System.currentTimeMillis();
			timerSet = true;
		}

		if ((System.currentTimeMillis() - timerStart) / 1000 > DECISION_TIME) {
			timerSet = false;
			int bid = BOT.chooseBid(handCards(), RoundKeeper.getTrumpSuit(), totalBids);
			if (Main.DEBUG) { System.out.println(getName() + " bet " + bid + " tricks"); }
			setBet(bid);
		}
	}

	@Override
	public PlayerCard playCard(ArrayList<PlayerCard> playedCards) {
		if (!timerSet) {
			timerStart = System.currentTimeMillis();
			timerSet = true;
		}

		if ((System.currentTimeMillis() - timerStart) / 1000 > DECISION_TIME) {
			timerSet = false;
			rules.Card choice = BOT.choosePlay(handCards(), CardComparator.toRules(playedCards),
					RoundKeeper.getTrumpSuit(), getBet(), getTricks());
			PlayerCard cardToPlay = null;
			for (PlayerCard card : getHand()) {
				if (card.rulesCard() == choice) {
					cardToPlay = card;
					break;
				}
			}
			if (Main.DEBUG) { System.out.println(getName() + " plays " + choice); }

			cardToPlay.setPos(homeAxis, 0);
			cardToPlay.setHome(homeAxis, HOME_Y);
			setCardPlayed(cardToPlay);
			JukeBox.play("playCard");
			return cardToPlay;
		}
		return null;
	}

	public void centerCards() {
		Card[] cards = this.getHand().toArray(new Card[0]);
		for (int i = 0; i < cards.length; i++) {
			cards[i].setHome((int) cards[i].getX(), -150);
		}
	}

	@Override
	public Point getHome() {
		Point p = new Point();
		p.x = getNameTag().getPosX();
		p.y = getNameTag().getPosY() + 25;
		return p;
	}
}