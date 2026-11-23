package ohheck.campaign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;

public class StudiesTest {

	private static int[] lines(Study s) {
		int[] solvedAndAll = new int[2];
		walk(s, new ArrayList<Move>(), solvedAndAll);
		return solvedAndAll;
	}

	private static void walk(Study s, List<Move> mine, int[] solvedAndAll) {
		StudyPlay p = new StudyPlay(s);
		for (Move m : mine) {
			p.finishOthers();
			p.move(m);
		}
		p.finishOthers();
		if (p.over()) {
			solvedAndAll[1]++;
			solvedAndAll[0] += p.solved() ? 1 : 0;
			return;
		}
		Match m = p.match();
		if (m.phase() == Match.Phase.BIDDING) {
			for (int b = 0; b <= m.handSize(); b++) {
				if (m.isLegalBid(b)) {
					mine.add(new Move.Bid(0, b));
					walk(s, mine, solvedAndAll);
					mine.remove(mine.size() - 1);
				}
			}
			return;
		}
		for (Card c : m.legalPlays(0)) {
			mine.add(new Move.Play(0, c));
			walk(s, mine, solvedAndAll);
			mine.remove(mine.size() - 1);
		}
	}

	@Test
	public void everyStudyHasALineThatWorksAndMostLinesDont() {
		for (Study s : Studies.ALL) {
			int[] r = lines(s);
			assertTrue(s.id + " has no solution", r[0] > 0);
			assertTrue(s.id + " is solved by " + r[0] + " of " + r[1] + " lines", r[0] * 4 <= r[1]);
		}
	}

	@Test
	public void studiesHaveUniqueIdsAndChapterOneHasSome() {
		Set<String> ids = new HashSet<String>();
		for (Study s : Studies.ALL) {
			assertTrue(s.id, ids.add(s.id));
		}
		assertTrue(Studies.of(Chapter.ONE).size() >= 4);
	}

	@Test
	public void anUndoTakesBackYourLastCard() {
		StudyPlay p = new StudyPlay(Studies.ALL.get(0));
		p.finishOthers();
		Card first = p.match().legalPlays(0).get(0);
		p.move(new Move.Play(0, first));
		p.finishOthers();
		p.undo();
		assertEquals(StudyPlay.UNDOS - 1, p.undosLeft());
		assertTrue(p.match().hand(0).contains(first));
		assertTrue(p.yourTurn());
	}

	@Test
	public void restartingGivesTheUndosBack() {
		StudyPlay p = new StudyPlay(Studies.ALL.get(0));
		p.finishOthers();
		p.move(new Move.Play(0, p.match().legalPlays(0).get(0)));
		p.undo();
		p.restart();
		assertEquals(StudyPlay.UNDOS, p.undosLeft());
		assertFalse(p.canUndo());
	}
}
