package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * The rules a game is played by: which way the hand sizes run, the biggest hand, how a made bid
 * scores (with big bids: 8, plus 4 for the first trick bid, doubling with each trick after), and
 * whether bids are blind (made unseen, revealed together, with no dealer's limit). Its
 * code is written into saves and high scores, so a code never changes meaning.
 */
public final class RuleSet {

	public static final int MOST_CARDS = 13, USUAL_CARDS = 8;
	public static final RuleSet STANDARD = new RuleSet(HandOrder.UP, USUAL_CARDS, false, false);

	private static final String BIG = "big", BLIND = "blind";
	private static final int BIG_MADE = 8, BIG_FIRST_TRICK = 4;

	public final HandOrder order;
	public final int mostCards;
	public final boolean bigBids, blindBids;

	public RuleSet(HandOrder order, int mostCards, boolean bigBids, boolean blindBids) {
		if (mostCards < 1 || mostCards > MOST_CARDS) {
			throw new IllegalArgumentException("No hand of " + mostCards);
		}
		this.order = order;
		this.mostCards = mostCards;
		this.bigBids = bigBids;
		this.blindBids = blindBids;
	}

	public RuleSet withOrder(HandOrder o) {
		return new RuleSet(o, mostCards, bigBids, blindBids);
	}

	public RuleSet withMostCards(int n) {
		return new RuleSet(order, n, bigBids, blindBids);
	}

	public RuleSet withBigBids(boolean on) {
		return new RuleSet(order, mostCards, on, blindBids);
	}

	public RuleSet withBlindBids(boolean on) {
		return new RuleSet(order, mostCards, bigBids, on);
	}

	public String code() {
		return order.name() + "/" + mostCards + (bigBids ? "/" + BIG : "") + (blindBids ? "/" + BLIND : "");
	}

	public static RuleSet parse(String code) {
		String[] p = code.split("/");
		List<String> flags = Arrays.asList(p).subList(Math.min(2, p.length), p.length);
		RuleSet r = p.length < 2 ? null
				: new RuleSet(HandOrder.valueOf(p[0]), Integer.parseInt(p[1]), flags.contains(BIG), flags.contains(BLIND));
		if (r == null || !r.code().equals(code)) {
			throw new IllegalArgumentException("Unknown rules: " + code);
		}
		return r;
	}

	public String describe() {
		return order.label(mostCards) + (bigBids ? ", big bids" : "") + (blindBids ? ", blind bids" : "");
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof RuleSet r && r.code().equals(code());
	}

	@Override
	public int hashCode() {
		return code().hashCode();
	}

	public int numberOfHands() {
		return order.hands(mostCards);
	}

	public int handSize(int handIndex) {
		return order.handSize(handIndex, mostCards);
	}

	public int nextSeat(int seat, int players) {
		return (seat + 1) % players;
	}

	public boolean beats(Card challenger, Card currentlyWinning, Suit trump) {
		if (challenger.suit() == currentlyWinning.suit()) {
			return challenger.rank().outranks(currentlyWinning.rank());
		}
		return challenger.suit() == trump;
	}

	public int trickWinner(List<Card> plays, Suit trump) {
		if (plays.isEmpty()) {
			throw new IllegalArgumentException("No cards played");
		}
		int winner = 0;
		for (int i = 1; i < plays.size(); i++) {
			if (beats(plays.get(i), plays.get(winner), trump)) {
				winner = i;
			}
		}
		return winner;
	}

	public boolean isLegalPlay(List<Card> hand, Card card, Suit leadSuit) {
		if (!hand.contains(card)) {
			return false;
		}
		if (leadSuit == null || card.suit() == leadSuit) {
			return true;
		}
		return !hasSuit(hand, leadSuit);
	}

	public List<Card> legalPlays(List<Card> hand, Suit leadSuit) {
		List<Card> legal = new ArrayList<Card>();
		for (Card c : hand) {
			if (isLegalPlay(hand, c, leadSuit)) {
				legal.add(c);
			}
		}
		return legal;
	}

	public static boolean hasSuit(List<Card> hand, Suit suit) {
		for (Card c : hand) {
			if (c.suit() == suit) {
				return true;
			}
		}
		return false;
	}

	public int forbiddenDealerBid(int handSize, int bidsSoFar) {
		return handSize - bidsSoFar;
	}

	public boolean isLegalBid(int bid, int handSize, int bidsSoFar, boolean isDealer) {
		if (bid < 0 || bid > handSize) {
			return false;
		}
		return blindBids || !(isDealer && bid == forbiddenDealerBid(handSize, bidsSoFar));
	}

	public int made() {
		return bigBids ? BIG_MADE : 10;
	}

	public int madeBonus(int bid) {
		if (bigBids) {
			return bid == 0 ? 0 : BIG_FIRST_TRICK << (bid - 1);
		}
		return perTrickBid() * bid;
	}

	public int perTrickBid() {
		return 2;
	}

	public int perTrickOff() {
		return 5;
	}

	public int score(int bid, int tricksTaken) {
		if (bid == tricksTaken) {
			return made() + madeBonus(bid);
		}
		return -perTrickOff() * Math.abs(bid - tricksTaken);
	}
}
