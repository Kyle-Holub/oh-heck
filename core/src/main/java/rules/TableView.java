package rules;

import java.util.ArrayList;
import java.util.List;

/**
 * What one player can see at the table: their own hand, the trump card, the
 * bids, tricks and scores, the cards played so far this hand, and which
 * players have shown they're out of a suit. Never anyone else's cards. This is
 * all a {@link Bot} gets.
 */
public final class TableView {

	public int seat, players, handIndex, handSize, dealer, numberOfHands;
	/** The rules this game is played by. */
	public RuleSet rules = RuleSet.STANDARD;
	public Card trumpCard;
	public final List<Card> hand = new ArrayList<Card>();
	/** null where a player hasn't bid yet. */
	public final Integer[] bids;
	public final int[] tricksWon, scores;
	/** Cards each player still holds. */
	public final int[] cardsLeft;
	/** The trick in progress, lead first, and who played each card. */
	public final List<Card> trick = new ArrayList<Card>();
	public final List<Integer> trickSeats = new ArrayList<Integer>();
	/** Every card played so far this hand, in order, and who played it. */
	public final List<Card> history = new ArrayList<Card>();
	public final List<Integer> historySeats = new ArrayList<Integer>();
	/** Cards played so far this hand (including the trick in progress), by {@link #index}. */
	public final boolean[] played = new boolean[52];
	/** voids[seat][suit]: that player failed to follow that suit this hand, so holds none of it. */
	public final boolean[][] voids;

	public TableView(int players) {
		this.players = players;
		bids = new Integer[players];
		tricksWon = new int[players];
		scores = new int[players];
		cardsLeft = new int[players];
		voids = new boolean[players][4];
	}

	/** A number from 0 to 51 for each card. */
	public static int index(Card c) {
		return c.suit().ordinal() * 13 + c.rank().strength();
	}

	public Suit trump() {
		return trumpCard.suit();
	}

	public Suit leadSuit() {
		return trick.isEmpty() ? null : trick.get(0).suit();
	}

	public int bidsSoFar() {
		int t = 0;
		for (Integer b : bids) {
			if (b != null) {
				t += b;
			}
		}
		return t;
	}

	public boolean isLegalBid(int bid) {
		return rules.isLegalBid(bid, handSize, bidsSoFar(), seat == dealer);
	}

	public List<Card> legalPlays() {
		return rules.legalPlays(hand, leadSuit());
	}

	/** Tricks this player still wants: their bid minus tricks taken (0 or less: wants no more). */
	public int need() {
		return (bids[seat] == null ? 0 : bids[seat]) - tricksWon[seat];
	}
}
