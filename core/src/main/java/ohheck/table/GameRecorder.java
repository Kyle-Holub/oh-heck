package ohheck.table;

import ohheck.players.Difficulty;
import ohheck.records.Records;
import ohheck.rules.Match;

/**
 * Keeps your records as a game goes: each hand and each finished game in the statistics,
 * and a finished game in the high scores. No libGDX.
 */
final class GameRecorder implements TableFlow.Listener {

	private static final int HUMAN = TableLayout.HUMAN;

	private final Records records;
	private Difficulty level;
	private int[] players;
	private int newHighScore = -1;

	GameRecorder(Records records) {
		this.records = records;
	}

	/** The table being played: its difficulty setting and players (ids, seats 1 on). fresh: a new game, not a resumed one. */
	void table(Difficulty level, int[] players, boolean fresh) {
		this.level = level;
		this.players = players;
		newHighScore = -1;
		if (fresh) {
			records.stats().startGame();
			records.saveStats(true);
		}
	}

	/** Where the finished game went in the high scores (0 = best), or -1. */
	int newHighScore() {
		return newHighScore;
	}

	@Override
	public void handEnded(Match m) {
		records.saveStats(records.stats().countHand(m.handIndex(), level, m.bid(HUMAN), m.tricksWon(HUMAN), m.lastHandPoints(HUMAN)));
	}

	@Override
	public void gameEnded(Match m) {
		newHighScore = records.recordHighScore(m.score(HUMAN), m.placeOf(HUMAN), level, Records.today());
		int[] finals = new int[m.players()];
		for (int s = 0; s < finals.length; s++) {
			finals[s] = m.score(s);
		}
		records.saveStats(records.stats().countGame(level, finals, m.rules().numberOfHands(), players));
	}
}
