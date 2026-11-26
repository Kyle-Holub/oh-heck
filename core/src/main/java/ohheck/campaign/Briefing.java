package ohheck.campaign;

import java.util.ArrayList;
import java.util.List;

import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.rules.RuleSet;

/*
 * What a chapter, or a step of it, is about to ask of you, in words drawn from its rules and its players. A chapter's
 * briefing is a few pages in the tutorial's style, a picture and a one-line caption each; a step's briefing shows its
 * pages' pictures side by side above their lines run together.
 */
public record Briefing(String tag, Chapter chapter, String go, List<Briefing.Page> pages) {

	public enum Picture {
		CHAPTER, HOUSE_RULES, RIVALS, TABLE, WIN, SHOTS, CHAMPION, SHORT_DECK, TWIST
	}

	public record Page(Picture picture, String heading, String line, String figure) {
	}

	public boolean isStage() {
		return !tag.equals(CHAPTER_TAG);
	}

	private static final String CHAPTER_TAG = "brief:chapter";

	public static Briefing chapter(Chapter c) {
		RuleSet r = c.rules();
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.CHAPTER, "Chapter " + c.number() + ": " + c.mode.label,
				"Each chapter is a new table: its own players, their house rules, and a champion to beat.", c.mode.label));
		p.add(new Page(Picture.RIVALS, "The players",
				names(c.rivals) + " sit here, with " + c.champion().name + ", the champion. Tap a player to learn their habit.",
				""));
		p.add(new Page(Picture.HOUSE_RULES, "House rules", scoringLine(r) + (r.blindBids ? " Bids are blind." : ""),
				r.order.label(r.mostCards).replace(" to ", "-") + " cards\n" + scoringFigure(r)));
		p.add(new Page(Picture.CHAMPION, "The champion",
				"Win the table, then beat " + c.champion().name + " at a final table and in a duel. Trick shots help you prepare.",
				c.champion().name));
		return new Briefing(CHAPTER_TAG, c, "Continue", p);
	}

	public static Briefing table(Chapter c) {
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.TABLE, "The table",
				"A full game of " + c.rules().numberOfHands() + " hands against " + names(c.rivals) + ".",
				c.rules().numberOfHands() + " hands"));
		p.add(new Page(Picture.WIN, "To win", "Finish first to open the trick shots and the final table.", "1st"));
		return new Briefing("brief:table", c, "Play", p);
	}

	public static Briefing finalTable(Chapter c) {
		Player champion = c.champion();
		List<Page> p = new ArrayList<Page>();
		p.add(new Page(Picture.CHAMPION, "The final table",
				champion.name + " takes " + c.leaving().name + "'s seat for a full game with " + c.rivals.at(1).name + " and "
						+ c.rivals.at(3).name + ".",
				champion.name));
		p.add(new Page(Picture.TWIST, c.twist.label, champion.name + "'s rule: " + c.twist.line, c.twist.figure));
		p.add(new Page(Picture.WIN, "To win", "Finish first to earn your duel with " + champion.name + ".", "1st"));
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
		p.add(new Page(Picture.SHORT_DECK, "A short deck", "Nine to ace, " + r.numberOfHands() + " hands of 1 to " + r.mostCards
				+ " cards" + (r.bigBids ? ", big bids" : "") + (r.blindBids ? ", blind bids" : "")
				+ ".", "9-A"));
		p.add(new Page(Picture.TWIST, c.twist.label, capital(c.twist.line), c.twist.figure));
		p.add(new Page(Picture.WIN, "To win", "Win to clear the chapter.", "1st"));
		return new Briefing("brief:duel", c, "Play", p);
	}

	private static String scoringLine(RuleSet r) {
		String made = r.bigBids ? "Make your bid exactly for " + r.made() + ", and a bonus that grows with the bid."
				: "Make your bid exactly for " + r.made() + ", plus " + r.perTrickBid() + " a trick.";
		return made + " Miss and lose " + r.perTrickOff() + " a trick off.";
	}

	private static String scoringFigure(RuleSet r) {
		return r.bigBids ? r.made() + " + bonus" : r.made() + " + " + r.perTrickBid() + " per trick";
	}

	private static String capital(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String names(Seating s) {
		return s.at(1).name + ", " + s.at(2).name + " and " + s.at(3).name;
	}
}
