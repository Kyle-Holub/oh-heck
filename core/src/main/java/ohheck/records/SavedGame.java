package ohheck.records;

import java.util.Random;

import ohheck.campaign.Chapter;
import ohheck.campaign.Stage;
import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Match;

/*
 * The game in progress, the latest one played, with its difficulty and seating, saved as one text. A campaign game
 * also names its chapter and stage; a custom game saves NONE for both.
 */
public record SavedGame(Match match, Difficulty level, Seating players, Chapter chapter, Stage stage) {

	private static final String NONE = "NONE";

	public String encode() {
		return match.save() + "table.level=" + level.name() + "\n" + "table.players=" + players.encode() + "\n" + "table.chapter="
				+ (chapter == null ? NONE : chapter.name()) + "\n" + "table.stage=" + (stage == null ? NONE : stage.name()) + "\n";
	}

	public boolean campaign() {
		return chapter != null;
	}

	public static SavedGame decode(String saved, Random rng) {
		Match m = Match.load(saved, rng);
		Difficulty level = Difficulty.valueOf(value(saved, "table.level"));
		Seating table = Seating.parse(value(saved, "table.players"));
		if (table == null || !table.onRoster()) {
			throw new IllegalArgumentException("No such table: " + value(saved, "table.players"));
		}
		String chapter = value(saved, "table.chapter"), stage = value(saved, "table.stage");
		if (chapter.equals(NONE) != stage.equals(NONE)) {
			throw new IllegalArgumentException("A chapter without its stage, or a stage without its chapter");
		}
		return new SavedGame(m, level, table, chapter.equals(NONE) ? null : Chapter.valueOf(chapter), stage.equals(NONE) ? null : Stage.valueOf(stage));
	}

	private static String value(String saved, String key) {
		for (String line : saved.split("\n")) {
			if (line.startsWith(key + "=")) {
				return line.substring(key.length() + 1).trim();
			}
		}
		throw new IllegalArgumentException("Missing " + key);
	}
}
