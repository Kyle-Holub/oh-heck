package ohheck;

import com.badlogic.gdx.math.Rectangle;

import java.util.Random;

import rules.Bot;
import rules.Card;
import rules.Difficulty;
import rules.Match;

/**
 * Plays your seat for the automated screenshot run (GameOptions.autoplayHuman),
 * through the same taps a person would make, and names what's on screen so
 * the run knows when to take each picture. Not used in normal play.
 */
final class Autoplay implements TableFlow.Listener {

	/** Pause before each of your moves; time the scores stay up; time the pause menu stays open. */
	private static final float MOVE_DELAY = 1.2f, RESULTS_WAIT = 2.2f, MENU_WAIT = 1.5f;
	/** The hand (0-based) whose bidding opens the pause menu once, to photograph it. */
	private static final int MENU_DEMO_HAND = 6;

	private final TableScene screen;
	/** Where its taps go, as yours would. */
	private final TableInput input;
	private final GameOptions options;
	private final Bot bot = Bot.of(Difficulty.NORMAL, new Random(7));
	private float timer;
	private boolean menuShown;

	Autoplay(TableScene screen, TableInput input, GameOptions options) {
		this.screen = screen;
		this.input = input;
		this.options = options;
	}

	/** Before each deal: plays any hands listed in GameOptions.skipHands instantly, so the run reaches the interesting ones sooner. */
	@Override
	public void beforeDeal(Match m) {
		while (m.phase() == Match.Phase.BIDDING && options.skipHands.contains(m.handIndex() + 1)) {
			while (m.phase() == Match.Phase.BIDDING || m.phase() == Match.Phase.PLAYING) {
				int s = m.currentSeat();
				if (m.phase() == Match.Phase.BIDDING) {
					m.placeBid(s, bot.chooseBid(m.view(s)));
				} else {
					m.playCard(s, bot.choosePlay(m.view(s)));
				}
			}
			m.nextHand();
		}
	}

	/** While the pause menu is open: close it again after a moment. */
	void whilePaused(float dt) {
		timer += dt;
		if (timer > MENU_WAIT) {
			timer = 0;
			tapCentre(screen.overlays().menuItems.isEmpty() ? null : screen.overlays().menuItems.get(0));
		}
	}

	/** Your turn to bid or play: wait a moment, then tap what the bot would choose. */
	void yourTurn(float dt) {
		Match m = screen.match();
		timer += dt;
		if (!menuShown && m.handIndex() == MENU_DEMO_HAND && m.phase() == Match.Phase.BIDDING) {
			menuShown = true;
			timer = 0;
			tapCentre(screen.overlays().menuButton);
			return;
		}
		if (timer < MOVE_DELAY) {
			return;
		}
		timer = 0;
		if (m.phase() == Match.Phase.BIDDING) {
			int b = bot.chooseBid(m.view(TableLayout.HUMAN));
			tapCentre(b < screen.overlays().bidButtons.size() ? screen.overlays().bidButtons.get(b) : null);
		} else {
			Card c = bot.choosePlay(m.view(TableLayout.HUMAN));
			int i = screen.handOrder().indexOf(c);
			tapCentre(i >= 0 ? screen.handRects().get(i) : null);
		}
	}

	/** The score table is up: move on after a while. */
	void onResults(float shownFor) {
		if (shownFor >= RESULTS_WAIT) {
			input.tap(screen.layout().w / 2, screen.layout().h / 2);
		}
	}

	private void tapCentre(Rectangle r) {
		if (r != null) {
			input.tap(r.x + Math.min(r.width, 30) / 2, r.y + r.height / 2);
		}
	}

	/** A short description of what's on screen, such as "deal:3" or "trick:5". */
	static String describe(TableScene s) {
		Match m = s.match();
		int hand = m.handIndex() + 1;
		if (s.isMenuOpen()) {
			return "menu:" + hand;
		}
		switch (s.state()) {
		case SHUFFLE:
			return "shuffle:" + hand;
		case DEAL:
			return "deal:" + hand;
		case TRICK: {
			float t = s.stateTime();
			return (t >= TrickView.TRICK_PAUSE ? "trickwon:"
					: t > TrickView.TRICK_PAUSE - TrickView.COLLECT_TIME ? "collect:" : "trick:") + hand;
		}
		case TURN:
			boolean yours = m.currentSeat() == TableLayout.HUMAN;
			if (m.phase() == Match.Phase.BIDDING) {
				return (yours ? "bid:" : "bidding:") + hand;
			}
			return yours ? (s.isDragging() ? "drag:" : "play:") + hand + ":" + m.currentTrick().size() : "playing:" + hand;
		case HAND_END:
			return "handend:" + hand;
		case RESULTS:
			return "handover:" + hand;
		default:
			// the final hand's cards thrown off the table, then the scores
			return s.resultsShown() ? "gameover" : "tossing";
		}
	}
}
