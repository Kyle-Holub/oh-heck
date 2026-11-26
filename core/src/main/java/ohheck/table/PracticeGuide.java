package ohheck.table;

import ohheck.campaign.PracticeCoach.Moment;
import ohheck.campaign.PracticeCoach.Tip;
import ohheck.campaign.PracticeCoach;
import ohheck.campaign.PracticeRound;
import ohheck.rules.Match;
import ohheck.rules.Move;

/*
 * The practice round at the table: reads which moment the table is at, asks the coach for a tip, and holds
 * the table still while a tip is up, until it is tapped away.
 */
final class PracticeGuide {

	private final PracticeCoach coach = new PracticeCoach();
	private Tip tip;

	boolean update(TableScene s) {
		if (tip == null) {
			for (Moment moment : Moment.values()) {
				if (at(moment, s)) {
					tip = coach.next(moment, s.match());
					if (tip != null) {
						break;
					}
				}
			}
		}
		return tip != null;
	}

	Tip tip() {
		return tip;
	}

	void dismiss() {
		if (tip != null) {
			coach.done(tip);
			tip = null;
		}
	}

	boolean allows(Match m, Move move) {
		return PracticeRound.allowed(m, move);
	}

	private static boolean at(Moment moment, TableScene s) {
		TableFlow f = s.flow();
		Match m = s.match();
		TableState state = f.state();
		switch (moment) {
		case SAT_DOWN:
			return state == TableState.PICK_DEALER;
		case DEALER_PICKED:
			return state == TableState.PICK_DEALER && DealerPick.settled(f.stateTime())
					|| m.handIndex() == 0 && (state == TableState.SHUFFLE || state == TableState.DEAL);
		case HAND_DEALT:
			return state == TableState.TURN && f.trumpFaceUp();
		case YOUR_BID:
			return f.yourTurnToBid() && f.trumpFaceUp();
		case YOUR_PLAY:
			return f.yourTurnToPlay();
		case HAND_SCORED:
			return state == TableState.RESULTS && f.stateTime() >= ScoreCard.LENGTH;
		case FINISHED:
			return state == TableState.GAME_OVER && s.resultsShown();
		default:
			return false;
		}
	}
}
