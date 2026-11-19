package ohheck.table;

import java.util.Random;

import com.badlogic.gdx.math.Rectangle;

import ohheck.platform.GameOptions;
import ohheck.players.Bot;
import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;

/** Plays your seat in the screenshot run, through the same taps a person would make. */
final class Autoplay implements YourSeat {

	private static final float MOVE_DELAY = 1.2f, RESULTS_WAIT = 2.2f, MENU_WAIT = 1.5f;
	private static final int MENU_DEMO_HAND = 6;

	private final TableScene screen;
	private final TableInput input;
	private final OverlayTargets targets;
	private final GameOptions options;
	private final Bot bot = Bot.of(Difficulty.NORMAL, new Random(7));
	private float timer;
	private boolean menuShown;

	Autoplay(TableScene screen, TableInput input, OverlayTargets targets, GameOptions options) {
		this.screen = screen;
		this.input = input;
		this.targets = targets;
		this.options = options;
	}

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

	@Override
	public boolean menuOnPause() {
		return false;
	}

	@Override
	public boolean thinkAtOnce() {
		return true;
	}

	@Override
	public void update(float dt) {
		FlowView flow = screen.flow();
		if (flow.yourTurnToBid() || flow.yourTurnToPlay()) {
			yourTurn(dt);
		} else if (flow.state() == TableState.RESULTS) {
			onResults(flow.stateTime());
		}
	}

	@Override
	public void whilePaused(float dt) {
		timer += dt;
		if (timer > MENU_WAIT) {
			timer = 0;
			tapCentre(targets.menuItem(MenuItem.RESUME));
		}
	}

	private void yourTurn(float dt) {
		Match m = screen.match();
		timer += dt;
		if (!menuShown && m.handIndex() == MENU_DEMO_HAND && m.phase() == Match.Phase.BIDDING) {
			menuShown = true;
			timer = 0;
			tapCentre(targets.button(OverlayTargets.Button.MENU));
			return;
		}
		if (timer < MOVE_DELAY) {
			return;
		}
		timer = 0;
		if (m.phase() == Match.Phase.BIDDING) {
			int b = bot.chooseBid(m.view(Seating.YOU));
			tapCentre(targets.bid(b));
		} else {
			Card c = bot.choosePlay(m.view(Seating.YOU));
			int i = screen.handOrder().indexOf(c);
			tapCentre(i >= 0 ? screen.handRects().get(i) : null);
		}
	}

	private void onResults(float shownFor) {
		if (shownFor >= RESULTS_WAIT) {
			input.tap(screen.layout().w / 2, screen.layout().h / 2);
		}
	}

	private void tapCentre(Rectangle r) {
		if (r != null && r.width > 0) {
			input.tap(r.x + Math.min(r.width, 30) / 2, r.y + r.height / 2);
		}
	}
}
