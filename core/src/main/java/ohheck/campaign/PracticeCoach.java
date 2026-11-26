package ohheck.campaign;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;

/*
 * What the practice round teaches, and when: at each moment of the game the table asks for the next tip, shows
 * it until it is tapped, then asks again. Each tip points at one thing on the table. After the free hands the
 * tip is a prompt on how you did. Every fact in a tip comes from the round's rules.
 */
public final class PracticeCoach {

	public enum Moment {
		SAT_DOWN, DEALER_PICKED, HAND_DEALT, YOUR_BID, YOUR_PLAY, HAND_SCORED, FINISHED
	}

	public enum Anchor {
		TABLE, DEALER, TALLY, TRUMP, CARD, BID, TRACKER, SCORE_ROW
	}

	public record Tip(String id, String text, Anchor anchor, int number, Card card) {
	}

	private record Lesson(String id, Moment when, int hand, int trick, String text, Anchor anchor, int number, String card) {
	}

	private static final RuleSet R = PracticeRound.RULES;
	private static final List<Lesson> LESSONS = List.of(
			new Lesson("welcome", Moment.SAT_DOWN, -1, -1,
					"Oh Heck is a trick-taking game for a standard deck of 52 cards. Predict how many tricks you'll win, then win"
							+ " exactly that many.",
					Anchor.TABLE, 0, null),
			new Lesson("dealer", Moment.DEALER_PICKED, 0, -1,
					"The first dealer is picked at random. Each hand, the deal passes clockwise to the next player.", Anchor.DEALER, 0,
					null),
			new Lesson("deal", Moment.HAND_DEALT, 0, -1, "Each hand deals one more card than the last: 1, then 2, then 3, and so on up"
					+ " to " + RuleSet.USUAL_CARDS + " in a full game.", Anchor.TALLY, 0, null),
			new Lesson("trump", Moment.HAND_DEALT, 0, -1,
					"The card turned up sets the trump suit. A trump beats any card of another suit.", Anchor.TRUMP, 0, null),
			new Lesson("ranks", Moment.HAND_DEALT, 0, -1,
					"Aces are high and 2s are low. You hold the ace of trumps, so it can't lose.", Anchor.CARD, 0, "AS"),
			new Lesson("bid", Moment.YOUR_BID, 0, -1,
					"Before playing, everyone bids how many tricks they'll win. Your ace will take the trick: bid 1.", Anchor.BID, 1, null),
			new Lesson("tracker", Moment.YOUR_PLAY, 0, -1,
					"Top left, a card marks each trick bid so far. Red means the table has bid more tricks than there are.",
					Anchor.TRACKER, 0, null),
			new Lesson("trick", Moment.YOUR_PLAY, 0, -1,
					"Everyone plays one card in turn: that's a trick. The highest card of the suit led wins, unless a trump is played.",
					Anchor.CARD, 0, "AS"),
			new Lesson("scoring", Moment.HAND_SCORED, 0, -1,
					"Made it! Win exactly your bid for " + R.made() + " points, plus " + R.perTrickBid() + " for each trick you bid. Miss"
							+ " it, high or low, and lose " + R.perTrickOff() + " for each trick off.",
					Anchor.SCORE_ROW, Seating.YOU, null),
			new Lesson("bid2", Moment.YOUR_BID, 1, -1,
					"Your heart is low, but a trump can win when you can't follow suit. Bid 1.", Anchor.BID, 1, null),
			new Lesson("follow", Moment.YOUR_PLAY, 1, 0,
					"You must follow the suit that was led if you can. Hearts were led, so play your heart.", Anchor.CARD, 0, "5H"),
			new Lesson("cantFollow", Moment.YOUR_PLAY, 1, 1,
					"No clubs? Then you may play any card, and only a trump can beat the suit led. Here a 2 beats an ace!",
					Anchor.CARD, 0, "2D"),
			new Lesson("zero", Moment.YOUR_BID, 2, -1,
					"Zero is a real bid. With a hand this weak, winning nothing can score " + R.score(0, 0) + ".", Anchor.BID, 0, null),
			new Lesson("hook", Moment.YOUR_BID, 3, -1,
					"As dealer you bid last, and you can't make the bids add up to the tricks, so someone is always set. That hook"
							+ " is how the game got its name: Oh Heck!",
					Anchor.BID, -1, null),
			new Lesson("finished", Moment.FINISHED, -1, -1,
					"Highest total at the end wins. That's everything: good luck!", Anchor.TABLE, 0, null));

	private final Set<String> shown = new HashSet<String>();

	public Tip next(Moment moment, Match m) {
		for (Lesson l : LESSONS) {
			if (l.when == moment && (l.hand < 0 || l.hand == m.handIndex()) && (l.trick < 0 || l.trick == tricksPlayed(m))
					&& !shown.contains(l.id)) {
				int number = l.anchor == Anchor.BID && l.number < 0 ? m.forbiddenBid() : l.number;
				return new Tip(l.id, l.text, l.anchor, number, l.card == null ? null : Card.parse(l.card));
			}
		}
		if (moment == Moment.HAND_SCORED && m.handIndex() >= PracticeRound.SCRIPTED_HANDS) {
			String id = "prompt" + m.handIndex();
			return shown.contains(id) ? null : new Tip(id, prompt(m), Anchor.SCORE_ROW, Seating.YOU, null);
		}
		return null;
	}

	public void done(Tip t) {
		shown.add(t.id());
	}

	private static int tricksPlayed(Match m) {
		int n = 0;
		for (int s = 0; s < m.players(); s++) {
			n += m.tricksWon(s);
		}
		return n;
	}

	static String prompt(Match m) {
		int bid = m.bid(Seating.YOU), won = m.tricksWon(Seating.YOU);
		if (won == bid) {
			return bid == 0 ? "Zero made: winning nothing scored " + R.score(0, 0) + "." : "Made it! Exactly your bid, for " + R.score(bid, won) + ".";
		}
		int off = Math.abs(bid - won);
		return (won > bid ? "Set: you won " + off + " more than you bid" : "Set: you won " + off + " fewer than you bid") + ", and lose "
				+ R.perTrickOff() + " for each one off.";
	}
}
