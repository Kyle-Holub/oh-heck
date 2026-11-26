package ohheck.campaign;

import java.util.ArrayList;
import java.util.List;

import ohheck.players.Seating;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;

/* Watches your hands through a game and names the badges it earned when the game ends. A game it didn't see from the start earns no Clean Sweep. */
public final class Feats {

	private static final int BIG_BID = 5;

	private int handsSeen;
	private boolean missed, bigSpent;

	public void startGame() {
		handsSeen = 0;
		missed = bigSpent = false;
	}

	public void handEnded(Match m) {
		handsSeen++;
		if (!m.madeBid(Seating.YOU)) {
			missed = true;
		} else if (m.rules().bigBids && m.bid(Seating.YOU) >= BIG_BID) {
			bigSpent = true;
		}
	}

	public List<Badge> gameEnded(Match m) {
		List<Badge> earned = new ArrayList<Badge>();
		boolean won = m.leaders().size() == 1 && m.leaders().get(0) == Seating.YOU;
		boolean clean = !missed && handsSeen >= m.rules().numberOfHands();
		if (clean) {
			earned.add(Badge.CLEAN_SWEEP);
		}
		if (bigSpent) {
			earned.add(Badge.BIG_SPENDER);
		}
		if (m.rules().blindBids && won && clean) {
			earned.add(Badge.POKER_FACE);
		}
		if (m.rules().mostCards == RuleSet.MOST_CARDS) {
			earned.add(Badge.MARATHONER);
		}
		if (m.players() == Seating.DUEL_SEATS && won) {
			earned.add(Badge.FIRST_DUEL);
		}
		return earned;
	}
}
