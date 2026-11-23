package ohheck.table;

import ohheck.campaign.Badge;
import ohheck.campaign.Feats;
import ohheck.records.Records;
import ohheck.rules.Match;

/* Awards the badges a game earns and saves them with the campaign. */
final class BadgeKeeper implements TableFlow.Listener {

	private final Records records;
	private final Feats feats = new Feats();

	BadgeKeeper(Records records) {
		this.records = records;
	}

	void fresh() {
		feats.startGame();
	}

	@Override
	public void handEnded(Match m) {
		feats.handEnded(m);
	}

	@Override
	public void gameEnded(Match m) {
		boolean any = false;
		for (Badge b : feats.gameEnded(m)) {
			any |= records.campaign().earn(b);
		}
		if (any) {
			records.saveCampaign();
		}
	}
}
