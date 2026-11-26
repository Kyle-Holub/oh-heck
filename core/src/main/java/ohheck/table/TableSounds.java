package ohheck.table;

import ohheck.platform.Audio;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Suit;

/*
 * The table's sounds and buzzes. A card's sound waits for it to land (heavier for yours, deeper for a trump
 * cutting another suit's lead); the rest come as the table moves on: a knock when your turn comes, cards
 * raked to the winner, a coin for a made bid, the hearth crackling as momentum builds, and the music easing
 * down while your bid hangs on the last trick.
 */
final class TableSounds implements TableFlow.Listener {

	private final Audio audio;
	private float now, landsAt = -1;
	private boolean landingIsYours, landingCuts, wasYourTurn, wasSweeping, wasTense;

	TableSounds(Audio audio) {
		this.audio = audio;
	}

	@Override
	public void shuffling() {
		audio.play(Audio.Effect.SHUFFLE);
	}

	@Override
	public void played(Match m, int seat, Card c) {
		Suit lead = (m.currentTrick().isEmpty() ? m.lastTrick() : m.currentTrick()).get(0).suit();
		landsAt = now + TrickView.PLAY_FLIGHT;
		landingIsYours = seat == Seating.YOU;
		landingCuts = c.suit() == m.trump() && lead != m.trump();
	}

	@Override
	public void handEnded(Match m) {
		boolean made = m.madeBid(Seating.YOU);
		audio.play(made ? Audio.Effect.COIN : Audio.Effect.LOSE);
		if (m.rules().momentum && made) {
			audio.play(Audio.Effect.CRACKLE);
			if (m.onMomentum(Seating.YOU)) {
				audio.buzz(Audio.Buzz.THUD);
			}
		}
	}

	void update(TableFlow flow, float time) {
		now = time;
		if (landsAt >= 0 && now >= landsAt) {
			landsAt = -1;
			land();
		}
		boolean yourTurn = flow.yourTurnToBid() || flow.yourTurnToPlay() || flow.yourTurnToPick() || flow.yourTurnToSwap();
		if (yourTurn && !wasYourTurn) {
			audio.play(Audio.Effect.KNOCK);
		}
		wasYourTurn = yourTurn;
		boolean sweeping = flow.state() == TableState.TRICK && flow.stateTime() >= TrickView.TRICK_PAUSE - TrickView.COLLECT_TIME;
		if (sweeping && !wasSweeping) {
			audio.play(Audio.Effect.SWEEP);
		}
		wasSweeping = sweeping;
		boolean tense = flow.bidHangsOnLastTrick();
		if (tense != wasTense) {
			audio.duck(tense);
		}
		wasTense = tense;
	}

	void stop() {
		if (wasTense) {
			audio.duck(false);
			wasTense = false;
		}
	}

	private void land() {
		audio.play(Audio.Effect.PLAY_CARD, landingIsYours ? 1f : 0.7f);
		if (landingCuts) {
			audio.play(Audio.Effect.WHUMP);
		} else if (landingIsYours) {
			audio.play(Audio.Effect.THUMP);
		}
		if (landingIsYours) {
			audio.buzz(Audio.Buzz.TAP);
		}
	}
}
