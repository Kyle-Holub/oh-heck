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
		assertTrue(chapter, chapter.contains("1 to 8") && chapter.contains("Nathaniel"));
		String table = text(Briefing.table(Chapter.ONE));
		assertTrue(table, table.contains("8 hands") && table.contains("Edmund, Barnaby and Horatio"));
		assertTrue(text(Briefing.trickShots(Chapter.ONE, new Campaign())).contains("0 of 3 made"));
		String duel = text(Briefing.duel(Chapter.ONE));
		assertTrue(duel, duel.contains("a trick for every trump") && duel.contains("5 hands"));
	}

	@Test
	public void captionsStayShort() {
		for (Chapter c : Chapter.values()) {
			for (Briefing b : new Briefing[] { Briefing.chapter(c), Briefing.table(c), Briefing.trickShots(c, new Campaign()),
					Briefing.duel(c) }) {
				for (Briefing.Page p : b.pages()) {
					assertTrue(p.line(), p.line().length() <= 110);
				}
			}
		}
	}
}
