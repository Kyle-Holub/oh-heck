package ohheck.table;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;

/* The trick's timings and the "Trick Won!" glow. */
final class TrickView {

	static final float PLAY_FLIGHT = 0.25f;
	static final float TRICK_PAUSE = 1.7f, COLLECT_TIME = 0.45f;
	private static final float TRICK_WON_SHOW = 0.9f;
	private static final float MAX_TILT = 9f;

	private static final Tween TRICK_WON = Tween.over(0.3f, Ease.LINEAR).after(TRICK_PAUSE - COLLECT_TIME / 2);

	private TrickView() {
	}

	static float showLength(boolean youWon) {
		return TRICK_PAUSE + (youWon ? TRICK_WON_SHOW : 0);
	}

	static void drawTrickWon(CardArt art, Assets assets, TableLayout layout, Match match, float elapsed) {
		if (match.lastTrickWinner() == Seating.YOU && elapsed > TRICK_WON.start()) {
			art.art(assets.trickWonText, layout.centreX(), layout.centreY(), 0.8f, TRICK_WON.progress(elapsed));
		}
	}

	static float tilt(Card c, int seat, int hand) {
		long k = ((c.suit().ordinal() * 13L + c.rank().ordinal()) * 31 + hand) * 8 + seat;
		k = (k + 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
		k ^= k >>> 31;
		float r = (k & 0xFFFF) / 65535f;
		return (r * 2 - 1) * MAX_TILT;
	}
}
