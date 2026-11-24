package ohheck.campaign;

import java.util.ArrayList;
import java.util.List;

import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.rules.RuleSet;

/*
 * What a chapter, or a step of it, is about to ask of you, as a few short pages in the tutorial's style: a picture on the
 * felt and a one-line caption, in words drawn from its rules and its players.
 */
public record Briefing(String tag, Chapter chapter, String go, List<Briefing.Page> pages) {

	public enum Picture {
		CHAPTER, SCORING, HANDS, RIVALS, TABLE, WIN, SHOTS, CHAMPION, SHORT_DECK, MOMENTUM
	}

	public record Page(Picture picture, String heading, String line, String figure) {
	}

	public static Briefing chapter(Chapter c) {
		RuleSet r = c.rules();
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.CHAPTER, "Chapter " + c.number() + ": " + c.mode.label,
				"Each chapter is a new table: its own players, their house rules, and a champion to beat.", c.mode.label));
		p.add(new Page(Picture.RIVALS, "The players",
				names(c.rivals) + " sit here, with " + c.champion().name + ", the champion. Tap a player to learn their habit.",
				""));
		p.add(new Page(Picture.HANDS, "House rules",
				"Hands run " + r.order.label(r.mostCards) + " cards." + (r.blindBids ? " Bids are made unseen and shown together." : ""),
				r.order.label(r.mostCards)));
		p.add(new Page(Picture.SCORING, "Scoring", scoringLine(r), scoringFigure(r)));
		p.add(new Page(Picture.CHAMPION, "The champion",
				"Win the table, then beat " + c.champion().name + " at a final table and in a duel. Trick shots help you prepare.",
				c.champion().name));
		return new Briefing("brief:chapter", c, "Continue", p);
	}

	public static Briefing table(Chapter c) {
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.TABLE, "The table",
				"A full game of " + c.rules().numberOfHands() + " hands against " + names(c.rivals) + ".",
				c.rules().numberOfHands() + " hands"));
		p.add(new Page(Picture.WIN, "To win", "Finish first, outright, to open the trick shots and the final table.", "1st"));
		return new Briefing("brief:table", c, "Play", p);
	}

	public static Briefing finalTable(Chapter c) {
		Player champion = c.champion();
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.CHAMPION, "The final table",
				champion.name + " takes " + c.leaving().name + "'s seat for a full game against " + names(c.seating(Stage.FINAL)) + ".",
				champion.name));
		p.add(new Page(Picture.MOMENTUM, champion.name + "'s rule: momentum",
				"Make your bid twice running and your next made bid scores double.", "x2"));
		p.add(new Page(Picture.WIN, "To win", "Finish first, outright, to earn your duel with " + champion.name + ".", "1st"));
		return new Briefing("brief:final", c, "Play", p);
	}

	public static Briefing trickShots(Chapter c, Campaign progress) {
		int n = TrickShots.of(c).size();
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.SHOTS, "Trick shots",
				"Every hand face up, and a goal: make your bid, set a rival too, or find the one sure bid.",
				progress.madeIn(c) + " of " + n + " made"));
		p.add(new Page(Picture.CHAMPION, "Practice for the duel",
				"The others play as a careful player would, so the same moves meet the same replies.", c.champion().name));
		return new Briefing("brief:shots", c, "Play", p);
	}

	public static Briefing duel(Chapter c) {
		Player champion = c.champion();
		RuleSet r = c.rules(Stage.DUEL);
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.CHAMPION, "Duel: " + champion.name, champion.trait, champion.name));
		p.add(new Page(Picture.SHORT_DECK, "A short deck",
				"Nine to ace, over " + r.numberOfHands() + " hands of 1 to " + r.mostCards + " cards.", ""));
		p.add(new Page(Picture.MOMENTUM, "Momentum", "Make your bid twice running and your next made bid scores double.", "x2"));
		p.add(new Page(Picture.WIN, "To win", "Beat " + champion.name + " to clear the chapter and claim its prizes.", "1st"));
		return new Briefing("brief:duel", c, "Play", p);
	}

	private static String scoringLine(RuleSet r) {
		String made = r.bigBids ? "Make your bid exactly for " + r.made() + ", and a bonus that grows with the bid."
				: "Make your bid exactly for " + r.made() + ", plus " + r.perTrickBid() + " a trick.";
		return made + " Miss and lose " + r.perTrickOff() + " a trick off.";
	}

	private static String scoringFigure(RuleSet r) {
		return r.bigBids ? r.made() + " + bonus" : r.made() + " + " + r.perTrickBid() + " a trick";
	}

	private static String names(Seating s) {
		return s.at(1).name + ", " + s.at(2).name + " and " + s.at(3).name;
	}
}
