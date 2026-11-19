package ohheck.table;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;

/**
 * The trick in the middle of the table: how long things take, each card's resting tilt,
 * and "Trick Won!" over a trick you took. The cards themselves are {@link CardSprites}: each
 * flies in from where it was played and settles at its tilt; a completed trick stays a
 * moment with the winning card framed, then the winner gathers it in.
 */
final class TrickView {

	/** A card played takes this long to land. */
	static final float PLAY_FLIGHT = 0.25f;
	/** A completed trick: on show, then gathered in over the last COLLECT_TIME. */
	static final float TRICK_PAUSE = 1.7f, COLLECT_TIME = 0.45f;
	/** Extra time for "Trick Won!" when you took it. */
	private static final float TRICK_WON_SHOW = 0.9f;
	private static final float MAX_TILT = 9f;

	/** "Trick Won!" fades in as the gathering begins, if it's yours. */
	private static final Tween TRICK_WON = Tween.over(0.3f, Ease.LINEAR).after(TRICK_PAUSE - COLLECT_TIME / 2);

	private TrickView() {
	}

	/** How long a completed trick stays on screen. */
	static float showLength(boolean youWon) {
		return TRICK_PAUSE + (youWon ? TRICK_WON_SHOW : 0);
	}

	/** "Trick Won!" in the middle, elapsed seconds after you completed and won a trick. */
	static void drawTrickWon(CardArt art, Assets assets, TableLayout table, Match match, float elapsed) {
		if (match.lastTrickWinner() == Seating.YOU && elapsed > TRICK_WON.start()) {
			art.art(assets.trickWonText, table.centreX(), table.centreY(), 0.8f, TRICK_WON.progress(elapsed));
		}
	}

	/** A card's resting tilt: random-looking, but steady for that card in that hand. */
	static float tilt(Card c, int seat, int hand) {
		long k = ((c.suit().ordinal() * 13L + c.rank().ordinal()) * 31 + hand) * 8 + seat;
		k = (k + 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
		k ^= k >>> 31;
		float r = (k & 0xFFFF) / 65535f;
		return (r * 2 - 1) * MAX_TILT;
	}
}
