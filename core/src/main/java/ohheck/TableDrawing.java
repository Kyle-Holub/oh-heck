package ohheck;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import rules.Card;
import rules.Match;
import rules.Suit;

/**
 * Draws the table itself, as it stands in a {@link PlayScreen}: the counters (top left),
 * the trump card with its glow and motes, each player's panel and face-down cards, and your
 * hand (laying out its tap areas as it goes). The screen keeps the state; this only draws it.
 */
final class TableDrawing {

	private static final int HUMAN = TableLayout.HUMAN;

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
	TableDrawing(TableScene scene, BaseScreen screen, CardArt art, PostProcessor post) {
		this.s = scene;
		this.post = post;
		this.batch = screen.batch;
		this.assets = screen.assets;
		this.table = scene.layout();
		this.art = art;
		this.panel = new PlayerPanel(screen);
		this.counters = new Counters(screen);
	}

	/** The gold motes rising off the trump card while it's face up on the table. */
	void updateMotes(float dt, boolean trumpShowing) {
		motes.update(dt, trumpShowing, table.trumpX(), table.trumpY(), table.trumpW(), table.trumpH());
	}

	// ---- the counters, top left: the hand tally, and the bid tracker under it

	void tally() {
		Match m = s.match();
		int done = m.phase() == Match.Phase.GAME_OVER ? 8 : m.handIndex();
		counters.tally(22, table.h - 12, done, Counters.pulse(s.time()), 1f);
	}

	void bidTracker() {
		Match m = s.match();
		float x0 = 22 - Counters.TALLY_W / 2; // lined up with the tally's first mark
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
		art.shadow(x, y, cw, ch, 4);
		float p = flow.isDealing() ? 0 : MathUtils.clamp(flow.trumpTime() / TableFlow.TRUMP_FLIP_TIME, 0, 1);
		// face up, the trump is lit: a warm glow behind it, and the card itself brightened
		float lit = MathUtils.clamp((flow.trumpTime() - TableFlow.TRUMP_FLIP_TIME) / 0.6f, 0, 1);
		if (p >= 1 && lit > 0) {
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
			float gw = cw * 1.9f, gh = ch * 1.6f;
			batch.setColor(1f, 0.78f, 0.4f, 0.22f * lit);
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
		if (seat == HUMAN) {
			return;
		}
		int total = m.hand(seat).size();
		int n = flow.isDealing() ? flow.deal.dealtTo(seat) : total;
		float y = table.fanOrigin(seat).y;
		for (int i = 0; i < n; i++) {
			batch.draw(assets.back(), table.fanCardX(seat, i, total), y, TableLayout.backW(), TableLayout.backH());
		}
	}

	/** Your hand, sorted; lays out the screen's tap areas for it as it goes. */
	void hand() {
		TableFlow flow = s.flow();
		Match m = s.match();
		List<Card> order = s.handOrder();
		order.clear();
		order.addAll(m.hand(HUMAN));
		Collections.sort(order, DISPLAY_ORDER);
		int n = order.size();
		BaseScreen.sizeTo(s.handRects(), n);
		if (n == 0) {
			return;
		}
		int shown = flow.isDealing() ? flow.deal.dealtTo(HUMAN) : n;
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		table.layoutHand(n);
		boolean myTurn = s.humanCanPlay();
		List<Card> legal = myTurn ? m.legalPlays(HUMAN) : Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			Card c = order.get(i);
			boolean canPlay = legal.contains(c);
			float x = table.handStart + i * table.handGap;
			float y = TableLayout.HAND_Y + (canPlay ? TableLayout.LIFT : 0);
			s.handRects().get(i).set(x, y, i == n - 1 ? cw : table.handGap, ch);
			if (i >= shown || (s.isDragging() && c == s.pressedCard())) {
				continue;
			}
			art.shadow(x, y, cw, ch, y - TableLayout.HAND_Y + 5);
			batch.setColor(myTurn && !canPlay ? Palette.CARD_DIM : Color.WHITE);
			float flip = flow.isDealing() ? flow.deal.flipOfYourCard(i) : 1f;
			art.flip(c, x, y, cw, ch, flip);
			if (flip >= 1 && flow.trumpFaceUp() && c.suit() == m.trump()) {
				Glints.draw(batch, assets.glow, c, x, y, cw, ch, i == n - 1 ? cw : table.handGap, 0, s.time(),
						myTurn && !canPlay ? 0.5f : 1f);
			}
		}
		batch.setColor(Color.WHITE);
	}

	/** The card you're dragging, under your finger at (x, y). */
	void dragged(Card c, float x, float y) {
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		batch.draw(assets.face(c), x - cw / 2, y - ch / 2, cw, ch);
		if (c.suit() == s.match().trump()) {
			Glints.draw(batch, assets.glow, c, x - cw / 2, y - ch / 2, cw, ch, cw, 0, s.time(), 1f);
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
