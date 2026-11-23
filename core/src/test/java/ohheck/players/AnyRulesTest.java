package ohheck.players;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import ohheck.rules.Card;
import ohheck.rules.GameMode;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.Rank;
import ohheck.rules.RuleSet;
import ohheck.rules.TableView;

public class AnyRulesTest {

	@Test
	public void blindBidsDontReadTheHiddenBidsAsZero() {
		Match m = new Match(4, new Random(2), 0, GameMode.POKER_FACE.rules(null));
		m.placeBid(1, m.handSize());
		m.placeBid(2, m.handSize());
		TableView v = m.view(3);
		double[] p = { 0.2, 0.5, 0.9 }, before = p.clone();
		NormalBot.adjustForTheTable(v, p);
		assertArrayEquals(before, p, 0);
	}

	@Test
	public void aThirteenCardHandCountsItsAcesAsTricks() {
		RuleSet rules = RuleSet.STANDARD.withOrder(HandOrder.DOWN).withMostCards(RuleSet.MOST_CARDS);
		for (long seed = 0; seed < 20; seed++) {
			Match m = new Match(4, new Random(seed), 0, rules);
			for (int seat = 0; seat < 4; seat++) {
				TableView v = m.view(seat);
				for (Card c : v.hand()) {
					if (c.rank() == Rank.ACE) {
						assertTrue(c + " in " + v.hand(), BidTable.chance(v, c) > 0.95);
					}
				}
			}
		}
	}

	@Test
	public void everyLevelFinishesABlindMarathon() {
		RuleSet rules = new RuleSet(HandOrder.UP_AND_DOWN, RuleSet.MOST_CARDS, true, true);
		Match m = Match.random(4, new Random(5), rules);
		Bot[] bots = { Difficulty.EASY.bot(new Random(1)), Difficulty.NORMAL.bot(new Random(2)),
				Difficulty.HARD.bot(new Random(3)), Player.byId(8).bot(new Random(4)) };
		while (m.phase() != Match.Phase.GAME_OVER) {
			int s = m.currentSeat();
			if (m.phase() == Match.Phase.BIDDING) {
				m.placeBid(s, bots[s].chooseBid(m.view(s)));
			} else if (m.phase() == Match.Phase.PLAYING) {
				m.playCard(s, bots[s].choosePlay(m.view(s)));
			} else {
				m.nextHand();
			}
		}
	}
}
