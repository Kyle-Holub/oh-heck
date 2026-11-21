package ohheck.table;

import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.records.Records;
import ohheck.rules.Match;
import ohheck.util.Dates;

/* Records each hand and finished game in the statistics and high scores. */
final class GameRecorder implements TableFlow.Listener {

	private final Records records;
	private Difficulty level;
	private Seating players;
	private int newHighScore = -1;

	GameRecorder(Records records) {
		this.records = records;
	}

	void table(Difficulty level, Seating players, boolean fresh) {
		this.level = level;
		this.players = players;
		newHighScore = -1;
		if (fresh) {
			records.stats().startGame();
			records.saveStats();
		}
	}

	int newHighScore() {
		return newHighScore;
	}

	@Override
	public void handEnded(Match m) {
		if (records.stats().countHand(m.handIndex(), level, m.bid(Seating.YOU), m.tricksWon(Seating.YOU), m.lastHandPoints(Seating.YOU))) {
			records.saveStats();
		}
	}

	@Override
	public void gameEnded(Match m) {
		newHighScore = records.recordHighScore(m.score(Seating.YOU), m.placeOf(Seating.YOU), level, Dates.today());
		int[] finals = new int[m.players()];
		for (int s = 0; s < finals.length; s++) {
			finals[s] = m.score(s);
		}
		if (records.stats().countGame(level, finals, m.rules().numberOfHands(), players)) {
			records.saveStats();
		}
	}
}
