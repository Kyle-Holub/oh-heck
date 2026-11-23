package ohheck.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ohheck.players.Bot;
import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Match;
import ohheck.rules.Move;

/*
 * One attempt at a trick shot: your moves, the others' replies as they come, and the undos left. An
 * undo takes back your last move by dealing again and replaying the ones before it; running out
 * of undos on a failed attempt starts the trick shot over with them all back.
 */
public final class TrickShotPlay {

	public static final int UNDOS = 3;

	private final TrickShot shot;
	private final Bot others = Bot.of(Difficulty.NORMAL, new Random(0));
	private final List<Move> yours = new ArrayList<Move>();
	private Match match;
	private int undosLeft;

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

	public int undosLeft() {
		return undosLeft;
	}

	public void restart() {
		yours.clear();
		undosLeft = UNDOS;
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
		yours.add(m);
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

	public boolean canUndo() {
		return undosLeft > 0 && !yours.isEmpty() && !solved();
	}

	public void undo() {
		if (!canUndo()) {
			return;
		}
		undosLeft--;
		List<Move> keep = new ArrayList<Move>(yours.subList(0, yours.size() - 1));
		yours.clear();
		match = shot.deal();
		for (Move m : keep) {
			finishOthers();
			move(m);
		}
		finishOthers();
	}
}
