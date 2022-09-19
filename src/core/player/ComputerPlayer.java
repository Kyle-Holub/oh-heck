package core.player;

import java.awt.Point;
import java.util.ArrayList;
import java.util.ListIterator;

import core.RoundKeeper;
import core.card.Card;
import core.card.CardComparator;
import core.card.Deck;
import core.card.PlayerCard;
import game.main.Main;
import util.JukeBox;

/**
 * A class for a AI players.
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class ComputerPlayer extends Player {

	private static final float DECISION_TIME = .5f;
	private static final int HOME_Y = 100;

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
			int trumpCards = 0;
			int highCards = 0;
			int betTotal = 0;
			ListIterator<PlayerCard> it = getHandIterator();

			// analyze how many trump cards and high value cards
			while (it.hasNext()) {
				Card card = it.next();
				if (card.getSuit() == RoundKeeper.getTrumpSuit()) {
					trumpCards++;
				} else if (card.getFaceValue().getValue() > 11) {
					highCards++;
				}
			}

			betTotal = trumpCards + highCards;

			// if cantBid == betTotal, then change the bid
			if (betTotal == getNumberOfCards() - totalBids) {
				if (betTotal > 0) {
					betTotal--;
				} else {
					betTotal++;
				}
			}

			if (Main.DEBUG) { System.out.println(getName() + " bet " + betTotal + " tricks"); }
			setBet(betTotal);
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
			ArrayList<PlayerCard> playableCards = new ArrayList<PlayerCard>();
			ArrayList<PlayerCard> trumpCards = new ArrayList<PlayerCard>();
			PlayerCard cardToPlay = null;
			boolean winTrick = true;

			// if there is more than 1 card in hand, then I have decisions to
			// make
			if (getNumberOfCards() > 1) {

				// determine if I want to win any more tricks
				if (this.getBet() <= this.getTricks()) {
					winTrick = false;
				}

				// add entire hand to playable cards list
				for (PlayerCard card : getHand()) {
					playableCards.add(card);
				}

				// if I am playing the leading card
				if (playedCards.size() < 1) {

					// determine if I have any trump cards
					for (PlayerCard card : getHand()) {
						if (card.sameSuit(RoundKeeper.getTrumpSuit())) {
							trumpCards.add(card);
						}
					}

					// delete trump cards from playable cards
					for (PlayerCard card : trumpCards) {
						playableCards.remove(card);
					}
					if (Main.DEBUG) {
						System.out.println("trumpCards=" + trumpCards.toString() + " playableCards="
								+ playableCards.toString() + " getHand()=" + getHand().toString());
					}

					// and if I want to win a trick
					if (winTrick) {
						// and if I have a card that isn't a trump
						if (!playableCards.isEmpty()) {
							// play the highest non-trump
							cardToPlay = getHighestCard(playableCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I am leading, I want a trick, and its the highest non trump. (highest card not a trump)");
							}
						} else {
							// else lead with the highest trump
							cardToPlay = getHighestCard(trumpCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I am leading, I want a trick, and I only have trumps. (highest trump)");
							}
						}
						// else if I want to sluff
					} else {
						// play the lowest trump if I have any
						if (!trumpCards.isEmpty()) {
							cardToPlay = getLowestCard(trumpCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I am leading, don't want a trick, and want to sluff my trumps. (lowest trump)");
							}
						} else {
							// else play the lowest card
							cardToPlay = getLowestCard(playableCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I am leading, don't want a trick, and I have no trumps. (lowest any)");
							}
						}
					}

					// else someone else lead and I must follow suit
				} else {

					// check if I have cards of the lead suit
					PlayerCard[] cards = new PlayerCard[playableCards.size()];
					cards = playableCards.toArray(cards);
					boolean hasLeadSuit = false;
					for (int i = 0; i < cards.length; i++) {
						if (cards[i].sameSuit(playedCards.get(0).getSuit())) {
							hasLeadSuit = true;
							break;
						}
					}

					// if I have cards of lead suit, remove cards not of the
					// lead suit
					if (hasLeadSuit) {
						for (int i = 0; i < cards.length; i++) {
							if (!cards[i].sameSuit(playedCards.get(0).getSuit())) {
								playableCards.remove(cards[i]);
							}
						}
						// else I don't have cards of the lead suit
					} else {
						// delete trump cards from playable cards
						for (PlayerCard card : trumpCards) {
							playableCards.remove(card);
						}
					}

					if (Main.DEBUG) {
						System.out.println("trumpCards=" + trumpCards.toString() + " playableCards="
								+ playableCards.toString() + " getHand()=" + getHand().toString());
					}

					// determine the highest card played before me
					PlayerCard highestCard = CardComparator.determineWinner(playedCards, RoundKeeper.getTrumpSuit());

					// if I have cards of the lead suit
					if (hasLeadSuit) {
						// and if I want to win a trick
						if (winTrick) {
							// if I want to win the trick, select the highest
							// card
							cardToPlay = getHighestCard(playableCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I match the leading suit and want a trick (highest)");
							}
							// if a higher card has been played...
							if (CardComparator.isGreaterCard(highestCard, cardToPlay, RoundKeeper.getTrumpSuit())
									&& playableCards.size() > 1) {
								// play the lowest card
								cardToPlay = getLowestCard(playableCards);
								if (Main.DEBUG) {
									System.out.println(cardToPlay.toString()
											+ " because I match the leading suit, want a trick, and someone played a higher card (lowest)");
								}
							}
						} else {
							// else if I want to sluff
							cardToPlay = getLowestCard(playableCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I match the leading suit and want to sluff (lowest)");
							}
						}
						// if I have no trumps
					} else if (trumpCards.isEmpty()) {
						if (winTrick) {
							// if I want to win more, get rid of the lowest card
							cardToPlay = getLowestCard(playableCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I have no matches, no trumps, and want a future trick (lowest card).");
							}
						} else {
							// if I want to sluff, get rid of the highest card I
							// can
							cardToPlay = getHighestCard(playableCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I have no matches, no trumps, and dont want a future trick (highest card).");
							}
						}
						// else if I have no cards that match the lead, but I do
						// have trumps
					} else {
						if (winTrick) {
							// if I want to win more, select the highest trump
							cardToPlay = getHighestCard(trumpCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString()
										+ " because I have no cards matching the lead, have trumps, and want a trick. (highest trump)");
							}
							// if I cannot beat the highest played card, sluff
							// the lowest card, or the lowest trump
							if (CardComparator.isGreaterCard(highestCard, cardToPlay, RoundKeeper.getTrumpSuit())
									&& playableCards.size() > 1) {
								if (!playableCards.isEmpty()) {
									cardToPlay = getLowestCard(playableCards);
									if (Main.DEBUG) {
										System.out.println(cardToPlay.toString()
												+ " **** but its not the highest trump so I will sluff my lowest non-trump.");
									}
								} else {
									cardToPlay = getLowestCard(trumpCards);
									if (Main.DEBUG) {
										System.out.println(cardToPlay.toString()
												+ " **** but its not the highest trump, and I have only trumps, so I will sluff my lowest trump.");
									}
								}
							}
						} else {
							// else if I want to sluff, play the card
							cardToPlay = getLowestCard(trumpCards);
							if (Main.DEBUG) {
								System.out.println(cardToPlay.toString() + " because I don't want any more tricks.");
							}
						}
					}
				}
				// else there's only one card, play it
			} else {
				cardToPlay = getHand().get(0);
			}

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