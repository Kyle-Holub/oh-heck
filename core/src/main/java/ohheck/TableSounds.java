package ohheck;

import rules.Card;
import rules.Match;

/** The table's sounds: the riffle, each card played, and how your hand went. */
final class TableSounds implements TableFlow.Listener {

	private final Audio audio;

	TableSounds(Audio audio) {
		this.audio = audio;
	}

	@Override
	public void shuffling() {
		audio.play(Audio.Effect.SHUFFLE);
	}

	@Override
	public void played(Match m, int seat, Card c) {
		audio.play(Audio.Effect.PLAY_CARD);
		if (m.phase() == Match.Phase.HAND_OVER) {
			audio.play(m.madeBid(TableLayout.HUMAN) ? Audio.Effect.WIN : Audio.Effect.LOSE);
		}
	}
}
