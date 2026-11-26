package ohheck.campaign;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BriefingTest {

	private static String text(Briefing b) {
		StringBuilder s = new StringBuilder();
		for (Briefing.Page p : b.pages()) {
			s.append(p.heading()).append(' ').append(p.line()).append(' ').append(p.figure()).append(' ');
		}
		return s.toString();
	}

	@Test
	public void briefingsNameTheRulesAndThePlayers() {
		String chapter = text(Briefing.chapter(Chapter.ONE));
		assertTrue(chapter, chapter.contains("1-8 cards") && chapter.contains("Nathaniel")
				&& chapter.contains("Edmund, Barnaby and Horatio"));
		String table = text(Briefing.table(Chapter.ONE));
		assertTrue(table, table.contains("8 hands") && table.contains("Edmund, Barnaby and Horatio"));
		assertTrue(chapter, chapter.contains("with Nathaniel, the champion"));
		assertTrue(text(Briefing.trickShots(Chapter.ONE, new Campaign())).contains("0 of 3 made"));
		String finalTable = text(Briefing.finalTable(Chapter.ONE));
		assertTrue(finalTable, finalTable.contains("Nathaniel takes Barnaby's seat") && finalTable.contains("with Edmund and Horatio")
				&& finalTable.contains("Momentum"));
		String secondFinal = text(Briefing.finalTable(Chapter.TWO));
		assertTrue(secondFinal, secondFinal.contains("Winner picks trump") && secondFinal.contains("Phillip's rule"));
		String duel = text(Briefing.duel(Chapter.ONE));
		assertTrue(duel, duel.contains("a trick for every trump") && duel.contains("5 hands"));
		String secondDuel = text(Briefing.duel(Chapter.TWO));
		assertTrue(secondDuel, secondDuel.contains("big bids") && !secondDuel.contains("momentum") && secondDuel.contains("next trump suit"));
		String thirdDuel = text(Briefing.duel(Chapter.THREE));
		assertTrue(thirdDuel, thirdDuel.contains("blind bids") && thirdDuel.contains("Hands won") && thirdDuel.contains("one card a hand"));
		String fourthDuel = text(Briefing.duel(Chapter.FOUR));
		assertTrue(fourthDuel, fourthDuel.contains("9 hands") && fourthDuel.contains("The widow"));
	}

	@Test
	public void captionsStayShort() {
		for (Chapter c : Chapter.values()) {
			for (Briefing b : new Briefing[] { Briefing.chapter(c), Briefing.table(c), Briefing.trickShots(c, new Campaign()), Briefing.finalTable(c),
					Briefing.duel(c) }) {
				for (Briefing.Page p : b.pages()) {
					assertTrue(p.line(), p.line().length() <= 110);
				}
			}
		}
	}
}
