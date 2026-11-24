package ohheck.campaign;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BriefingTest {

	@Test
	public void briefingsNameTheRulesAndThePlayers() {
		Briefing chapter = Briefing.chapter(Chapter.ONE);
		assertTrue(chapter.body(), chapter.body().contains("1 to 8") && chapter.body().contains("Nathaniel")
				&& chapter.body().contains("Edmund, Barnaby and Horatio"));
		assertTrue(Briefing.table(Chapter.ONE).body().contains("8 hands"));
		assertTrue(Briefing.trickShots(Chapter.ONE, new Campaign()).body().contains("0 of 3 made"));
		Briefing duel = Briefing.duel(Chapter.ONE);
		assertTrue(duel.body(), duel.body().contains("counts a trick for every trump") && duel.body().contains("5 hands"));
	}
}
