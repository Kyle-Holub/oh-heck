package ohheck.campaign;

import java.util.Random;

import ohheck.players.Bot;
import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Match;
import ohheck.rules.Move;

/*
 * One attempt at a trick shot: the deal, your moves, and the others' replies as they come.
 */
public final class TrickShotPlay {

	private final TrickShot shot;
	private final Bot others = Bot.of(Difficulty.NORMAL, new Random(0));
	private Match match;

	public TrickShotPlay(TrickShot shot) {
		this.shot = shot;
		restart();
	}

	public TrickShot shot() {
		return shot;
	}

	public Match match() {
		return match;
	}

	public void restart() {
		match = shot.deal();
	}

	public boolean yourTurn() {
		return !over() && match.currentSeat() == Seating.YOU;
	}

	public boolean othersToMove() {
		return !over() && match.currentSeat() != Seating.YOU;
	}

	public boolean over() {
		return match.phase() == Match.Phase.HAND_OVER;
	}

	public boolean solved() {
		return over() && shot.reached(match);
	}

	public void move(Move m) {
		if (!yourTurn()) {
			throw new IllegalStateException("Not your turn");
		}
		match.apply(m);
	}

	public void stepOthers() {
		if (othersToMove()) {
			match.apply(others.chooseMove(match.view(match.currentSeat())));
		}
	}

	public void finishOthers() {
		while (othersToMove()) {
			stepOthers();
		}
	}
}
