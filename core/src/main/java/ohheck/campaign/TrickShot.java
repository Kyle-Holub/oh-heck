package ohheck.campaign;

import java.util.ArrayList;
import java.util.List;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;

/*
 * A hand-built puzzle: every hand face up, a fixed deal, and a goal you can reach with the right
 * line of play. The other players bid as written and play as a Normal player would, so the same
 * moves always meet the same replies. Hands are written seat by seat from you, clockwise.
 */
public final class TrickShot {

	public enum Goal {
		MAKE_IT("Make your bid exactly"), SET_THEM("Make your bid and block the player %s"), CHOOSE_THE_BID("Choose a bid you can make");

		private final String text;

		Goal(String text) {
			this.text = text;
		}
	}

	private static final String[] SIDES = { "", "on the left", "across", "on the right" };

	public final String id, title;
	public final Chapter chapter;
	public final Goal goal;
	public final int rival;
	private final String trump;
	private final String[] hands;
	private final int dealer;
	private final int[] bids;

	TrickShot(String id, String title, Chapter chapter, Goal goal, int rival, String trump, int dealer, int[] bids, String... hands) {
		this.id = id;
		this.title = title;
		this.chapter = chapter;
		this.goal = goal;
		this.rival = rival;
		this.trump = trump;
		this.dealer = dealer;
		this.bids = bids;
		this.hands = hands;
	}

	public String goalText() {
		return String.format(goal.text, SIDES[Math.max(0, rival)]);
	}

	public Match deal() {
		List<List<Card>> dealt = new ArrayList<List<Card>>();
		for (String hand : hands) {
			List<Card> cards = new ArrayList<Card>();
			for (String code : hand.trim().split("\\s+")) {
				cards.add(Card.parse(code));
			}
			dealt.add(cards);
		}
		Match m = Match.dealt(chapter.rules(), dealer, dealt, Card.parse(trump));
		while (m.phase() == Match.Phase.BIDDING && !(m.currentSeat() == Seating.YOU && goal == Goal.CHOOSE_THE_BID)) {
			m.placeBid(m.currentSeat(), bids[m.currentSeat()]);
		}
		return m;
	}

	public boolean reached(Match m) {
		return m.madeBid(Seating.YOU) && (goal != Goal.SET_THEM || !m.madeBid(rival));
	}
}
