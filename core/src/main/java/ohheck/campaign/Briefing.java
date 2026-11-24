package ohheck.campaign;

import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.rules.RuleSet;

/* What a chapter, or a step of it, is about to ask of you, in words drawn from its rules and its players. */
public record Briefing(String tag, String title, String body, String go) {

	public static Briefing chapter(Chapter c) {
		return new Briefing("brief:chapter", "Chapter " + c.number() + ": " + c.mode.label,
				rulesLine(c.rules()) + " Win the table against " + names(c.rivals) + ", sharpen up with trick shots, then beat "
						+ c.champion().name + " in a duel to clear the chapter.",
				"Continue");
	}

	public static Briefing table(Chapter c) {
		return new Briefing("brief:table", "The table",
				"A full game of " + c.rules().numberOfHands() + " hands against " + names(c.rivals) + ", under " + c.mode.label
						+ " rules. Finish first, outright, to win the table and open the trick shots and the duel.",
				"Play");
	}

	public static Briefing trickShots(Chapter c, Campaign progress) {
		int n = TrickShots.of(c).size();
		return new Briefing("brief:shots", "Trick shots",
				n + " hands laid face up, each with a goal: make your bid exactly, stop a rival making theirs, or find the"
						+ " one bid you can make. The others play as a careful player would, so the same moves always meet the"
						+ " same replies. Good practice for the duel. " + progress.madeIn(c) + " of " + n + " made so far.",
				"Play");
	}

	public static Briefing duel(Chapter c) {
		Player champion = c.champion();
		RuleSet r = c.duelRules();
		return new Briefing("brief:duel", "Duel: " + champion.name,
				"One on one against " + champion.name + ", who " + lowerFirst(champion.trait) + " A short deck, nine to ace,"
						+ " over " + r.numberOfHands() + " hands of 1 to " + r.mostCards + " cards. Make your bid twice running and"
						+ " your next made bid scores double. Win to clear the chapter.",
				"Play");
	}

	private static String rulesLine(RuleSet r) {
		String scoring = r.bigBids ? "Make your bid exactly for " + r.made() + " points and a bonus that doubles with each trick bid"
				: "Make your bid exactly for " + r.made() + " points, plus " + r.perTrickBid() + " for each trick bid";
		return "Hands run " + r.order.label(r.mostCards) + " cards. " + scoring + "; miss, high or low, and lose "
				+ r.perTrickOff() + " for each trick off." + (r.blindBids ? " Bids are made unseen and shown together." : "");
	}

	private static String names(Seating s) {
		return s.at(1).name + ", " + s.at(2).name + " and " + s.at(3).name;
	}

	private static String lowerFirst(String s) {
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}
}
