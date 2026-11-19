package ohheck.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.rules.Replay;
import ohheck.rules.RuleSet;

/** Moves as values: a game's log, played back on the same deal, gets to the same place. */
public class ReplayTest {

	/** A whole game between Normal players, on a deal from this seed. */
	private static Match played(long seed) {
		Match m = new Match(4, new Random(seed), 0, HandOrder.UP, RuleSet.STANDARD);
		Bot bot = Bot.of(Difficulty.NORMAL, new Random(seed + 1));
		while (m.phase() != Match.Phase.GAME_OVER) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				m.nextHand();
			} else {
				m.apply(bot.chooseMove(m.view(m.currentSeat())));
			}
		}
		return m;
	}

	@Test
	public void aReplayReachesTheSamePlace() {
		Match game = played(7);
		Match again = Replay.onto(new Match(4, new Random(7), 0, HandOrder.UP, RuleSet.STANDARD), game.moves());
		again.nextHand(); // (moving on from the last hand to the final scores isn't a move)
		assertEquals(game.save(), again.save());
		assertEquals(game.moves(), again.moves());
	}

	@Test
	public void theLogHoldsEveryBidAndCard() {
		Match game = played(8);
		int bids = 0, plays = 0;
		for (Move m : game.moves()) {
			if (m instanceof Move.Bid) {
				bids++;
			} else {
				plays++;
			}
		}
		assertEquals("4 bids a hand", 8 * 4, bids);
		assertEquals("1 + 2 + ... + 8 cards each", 4 * 36, plays);
	}

	@Test
	public void anIllegalMoveIsRefusedAndNotLogged() {
		Match m = new Match(4, new Random(9), 0, HandOrder.UP, RuleSet.STANDARD);
		try {
			m.apply(new Move.Bid(m.currentSeat(), 5)); // one card each: no bid of 5
			fail();
		} catch (IllegalArgumentException expected) {
			assertTrue(m.moves().isEmpty());
		}
	}
}
