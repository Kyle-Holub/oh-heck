package ohheck.campaign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import ohheck.players.Bot;
import ohheck.players.Difficulty;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;

public class CampaignTest {

	@Test
	public void eachStageWaitsForTheOneBefore() {
		Campaign c = new Campaign();
		assertTrue(c.open(Chapter.ONE, Stage.TABLE));
		assertFalse(c.open(Chapter.ONE, Stage.FINAL));
		assertFalse(c.shotsOpen(Chapter.ONE));
		c.win(Chapter.ONE, Stage.TABLE);
		assertTrue(c.open(Chapter.ONE, Stage.FINAL) && c.shotsOpen(Chapter.ONE));
		assertFalse(c.open(Chapter.ONE, Stage.DUEL));
		c.win(Chapter.ONE, Stage.FINAL);
		assertTrue(c.open(Chapter.ONE, Stage.DUEL));
		assertFalse(c.cleared(Chapter.ONE));
		c.win(Chapter.ONE, Stage.DUEL);
		assertTrue(c.cleared(Chapter.ONE));
	}

	@Test
	public void theFinalTableSeatsTheChampionUnderMomentum() {
		assertTrue(Chapter.ONE.seating(Stage.FINAL).at(2) == Chapter.ONE.champion());
		assertTrue(Chapter.ONE.rules(Stage.FINAL).momentum);
		assertFalse(Chapter.ONE.rules(Stage.TABLE).momentum);
	}

	@Test
	public void unreadyChaptersStayShut() {
		Campaign c = new Campaign();
		c.win(Chapter.ONE, Stage.DUEL);
		assertTrue(c.open(Chapter.TWO));
		c.win(Chapter.TWO, Stage.DUEL);
		assertFalse(c.open(Chapter.THREE));
	}

	@Test
	public void progressReadsBackWhatItWrites() {
		Campaign c = new Campaign();
		c.win(Chapter.ONE, Stage.FINAL);
		c.solve("make-the-ace");
		c.solve("duck-twice");
		Campaign back = Campaign.decode(c.encode());
		assertEquals(c.encode(), back.encode());
		assertTrue(back.won(Chapter.ONE, Stage.FINAL) && !back.cleared(Chapter.ONE));
		assertTrue(back.solved("duck-twice"));
	}

	@Test
	public void unreadableProgressStartsAfresh() {
		assertEquals(new Campaign().encode(), Campaign.decode("nonsense\ntables=ZERO").encode());
	}

	@Test
	public void badgesAreSavedWithTheProgress() {
		Campaign c = new Campaign();
		assertTrue(c.earn(Badge.CLEAN_SWEEP));
		assertFalse(c.earn(Badge.CLEAN_SWEEP));
		assertTrue(Campaign.decode(c.encode()).has(Badge.CLEAN_SWEEP));
		assertFalse(Campaign.decode(c.encode()).has(Badge.MARATHONER));
	}

	@Test
	public void aChaptersLookIsOwnedOnceItIsCleared() {
		Campaign c = new Campaign();
		assertFalse(Cosmetic.EMERALD.owned(c));
		assertEquals(Cosmetic.CLASSIC, Cosmetic.CLASSIC.nextOwned(c));
		c.win(Chapter.ONE, Stage.DUEL);
		assertTrue(Cosmetic.EMERALD.owned(c));
		assertEquals(Cosmetic.EMERALD, Cosmetic.CLASSIC.nextOwned(c));
	}

	@Test
	public void featsCountOnlyGamesSeenWhole() {
		Match m = Match.random(2, new Random(4), RuleSet.DUEL);
		Feats seen = new Feats(), late = new Feats();
		seen.startGame();
		late.startGame();
		Bot bot = Bot.of(Difficulty.HARD, new Random(1));
		boolean missed = false;
		while (m.phase() != Match.Phase.GAME_OVER) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				seen.handEnded(m);
				if (m.handIndex() > 0) {
					late.handEnded(m);
				}
				missed |= !m.madeBid(0);
				m.nextHand();
			} else {
				m.apply(bot.chooseMove(m.view(m.currentSeat())));
			}
		}
		assertEquals(!missed, seen.gameEnded(m).contains(Badge.CLEAN_SWEEP));
		assertFalse(late.gameEnded(m).contains(Badge.CLEAN_SWEEP));
		assertEquals(m.leaders().equals(List.of(0)), seen.gameEnded(m).contains(Badge.FIRST_DUEL));
	}
}
