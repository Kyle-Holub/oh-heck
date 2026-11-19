package ohheck.table;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.Glints;
import ohheck.fx.Motes;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Suit;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.Painter;
import ohheck.ui.PostProcessor;
import ohheck.ui.TapAreas;

/**
 * Draws the table itself, as it stands in a {@link PlayScreen}: the counters (top left),
 * the trump card with its glow and motes, each player's panel and face-down cards, and your
 * hand (laying out its tap areas as it goes). The screen keeps the state; this only draws it.
 */
final class TableDrawing {

	private static final int HUMAN = TableLayout.HUMAN;
	/** The trump card's shadow lift; its warm glow, how long it takes to light, and its size against the card. */
	private static final float TRUMP_LIFT = 4, TRUMP_GLOW_IN = 0.6f, TRUMP_GLOW_W = 1.9f, TRUMP_GLOW_H = 1.6f;
	private static final Color TRUMP_GLOW = new Color(1f, 0.78f, 0.4f, 0.22f);
	/** The trump card turns over once the deal is done, then its glow comes up. */
	private static final Tween TRUMP_TURNING = Tween.over(TableFlow.TRUMP_FLIP_TIME, Ease.LINEAR),
			TRUMP_LIGHTING = Tween.over(TRUMP_GLOW_IN, Ease.LINEAR).after(TableFlow.TRUMP_FLIP_TIME);
	/** The tally's first mark: in from the left, down from the top. */
	private static final float TALLY_X = 22, TALLY_DROP = 12;
	/** Your cards' shadow lift, on top of how far a card is raised. */
	private static final float HAND_LIFT = 5;

	private final TableScene s;
	private final PostProcessor post;
	private final SpriteBatch batch;
	private final Assets assets;
	private final TableLayout table;
	private final CardArt art;
	private final PlayerPanel panel;
	/** Reused for each placard, each frame. */
	private final SeatState state = new SeatState();
	private final Counters counters;
	private final Motes motes = new Motes();

	/** Draws scene's table with screen's batch and art; post takes the light for the glow. */
	TableDrawing(TableScene scene, Painter paint, CardArt art, PostProcessor post) {
		this.s = scene;
		this.post = post;
		this.batch = paint.batch;
		this.assets = paint.assets;
		this.table = scene.layout();
		this.art = art;
		this.panel = new PlayerPanel(paint);
		this.counters = new Counters(paint);
	}

	/** The gold motes rising off the trump card while it's face up on the table. */
	void updateMotes(float dt, boolean trumpShowing) {
		motes.update(dt, trumpShowing, table.trumpX(), table.trumpY(), table.trumpW(), table.trumpH());
	}

	// ---- the counters, top left: the hand tally, and the bid tracker under it

	void tally() {
		Match m = s.match();
		int done = m.phase() == Match.Phase.GAME_OVER ? m.rules().numberOfHands() : m.handIndex();
		counters.tally(TALLY_X, table.h - TALLY_DROP, m.rules().numberOfHands(), done, Counters.pulse(s.time()), 1f);
	}

	void bidTracker() {
		Match m = s.match();
		float x0 = TALLY_X - Counters.TALLY_W / 2; // lined up with the tally's first mark
		float avail = table.panelX(2) - 14 - x0; // and clear of the top player's panel
		counters.tracker(x0, table.h - 12 - Counters.TALLY_H - Counters.TRACKER_BELOW_TALLY, avail, m.handSize(),
				m.bidsSoFar(), 1f);
	}

	// ---- the trump card

	void trump(boolean gone) {
		TableFlow flow = s.flow();
		if (flow.state() == TableState.SHUFFLE || gone) {
			if (gone) {
				motes.draw(batch, assets.glow); // the last few still rising
			}
			return; // the trump card is still in the deck, or thrown off the table
		}
		float x = table.trumpX(), y = table.trumpY(), cw = table.trumpW(), ch = table.trumpH();
		art.shadow(x, y, cw, ch, TRUMP_LIFT);
		float p = flow.isDealing() ? 0 : TRUMP_TURNING.progress(flow.trumpTime());
		// face up, the trump is lit: a warm glow behind it, and the card itself brightened
		float lit = TRUMP_LIGHTING.progress(flow.trumpTime());
		if (p >= 1 && lit > 0) {
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
			float gw = cw * TRUMP_GLOW_W, gh = ch * TRUMP_GLOW_H;
			batch.setColor(TRUMP_GLOW.r, TRUMP_GLOW.g, TRUMP_GLOW.b, TRUMP_GLOW.a * lit);
			batch.draw(assets.glow, x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh);
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			batch.setColor(Color.WHITE);
		}
		Card trump = s.match().trumpCard();
		art.flip(trump, x, y, cw, ch, p);
		if (p >= 1) {
			Glints.draw(batch, assets.glow, trump, x, y, cw, ch, cw, 0, s.time(), lit);
		}
		if (p >= 1 && lit > 0) {
			// and a light on it in the screen effects, lifting the corner's vignette (after the bloom,
			// so the card brightens without glaring)
			post.light((x + cw / 2) / table.w, (y + ch / 2) / table.h, 0.3f, lit);
		}
		motes.draw(batch, assets.glow);
	}

	// ---- the players

	/** A seat's panel and, for opponents, their face-down cards (while dealing, only those that have landed). */
	void seat(int seat) {
		TableFlow flow = s.flow();
		Match m = s.match();
		boolean turn = flow.state() == TableState.TURN && m.currentSeat() == seat;
		state.reset().named(s.name(seat)).score(m.score(seat)).turn(turn).dealer(m.dealer() == seat)
				.bid(m.bid(seat)).won(m.tricksWon(seat)).cards(m.handSize()).plain(flow.isDealing());
		panel.draw(state, table.panelX(seat), table.panelTop(seat), s.time());
	}

	/** Your hand, sorted, laid out, with a tap area for each card (CardSprites draws them). */
	void layoutHand() {
		TableFlow flow = s.flow();
		Match m = s.match();
		List<Card> order = s.handOrder();
		order.clear();
		order.addAll(m.hand(HUMAN));
		Collections.sort(order, DISPLAY_ORDER);
		int n = order.size();
		TapAreas.sizeTo(s.handRects(), n);
		if (n == 0) {
			return;
		}
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		table.layoutHand(n);
		List<Card> legal = s.humanCanPlay() ? m.legalPlays(HUMAN) : Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			float x = table.handStart + i * table.handGap;
			float y = TableLayout.HAND_Y + (legal.contains(order.get(i)) ? TableLayout.LIFT : 0);
			s.handRects().get(i).set(x, y, i == n - 1 ? cw : table.handGap, ch);
		}
	}

	/** Your hand sorted by suit (alternating colours), then rank. */
	private static final Comparator<Card> DISPLAY_ORDER = new Comparator<Card>() {
		private final Suit[] order = { Suit.CLUBS, Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS };

		private int suitIndex(Suit s) {
			for (int i = 0; i < order.length; i++) {
				if (order[i] == s) {
					return i;
				}
			}
			return 0;
		}

		@Override
		public int compare(Card a, Card b) {
			int d = suitIndex(a.suit()) - suitIndex(b.suit());
			return d != 0 ? d : a.rank().strength() - b.rank().strength();
		}
	};
}
