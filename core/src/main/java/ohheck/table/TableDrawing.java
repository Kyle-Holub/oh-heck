package ohheck.table;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.Glints;
import ohheck.fx.Motes;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.HandSort;
import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.Painter;
import ohheck.ui.PostProcessor;
import ohheck.ui.TapAreas;

/* Draws the counters, the trump card, the placards and your hand's layout. */
final class TableDrawing {

	private static final float TRUMP_LIFT = 4, TRUMP_GLOW_IN = 0.6f, TRUMP_GLOW_W = 1.9f, TRUMP_GLOW_H = 1.6f;
	private static final Color TRUMP_GLOW = new Color(1f, 0.78f, 0.4f, 0.22f);
	private static final Tween TRUMP_TURNING = Tween.over(TableFlow.TRUMP_FLIP_TIME, Ease.LINEAR),
			TRUMP_LIGHTING = Tween.over(TRUMP_GLOW_IN, Ease.LINEAR).after(TableFlow.TRUMP_FLIP_TIME);
	private static final float TALLY_X = 22, TALLY_DROP = 12;
	private static final float HAND_LIFT = 5;

	private final TableScene scene;
	private final PostProcessor post;
	private final SpriteBatch batch;
	private final Assets assets;
	private final TableLayout layout;
	private final CardArt art;
	private final PlayerPanel panel;
	private final SeatState state = new SeatState();
	private final Counters counters;
	private final Motes motes = new Motes();
	private final Supplier<HandSort> sort;

	TableDrawing(TableScene scene, Painter paint, CardArt art, PostProcessor post, Supplier<HandSort> sort) {
		this.sort = sort;
		this.scene = scene;
		this.post = post;
		this.batch = paint.batch;
		this.assets = paint.assets;
		this.layout = scene.layout();
		this.art = art;
		this.panel = new PlayerPanel(paint);
		this.counters = new Counters(paint);
	}

	void updateMotes(float dt, boolean trumpShowing) {
		motes.update(dt, trumpShowing && scene.match().trumpCard() != null, layout.trumpX(), layout.trumpY(), layout.trumpW(), layout.trumpH());
	}

	void tally() {
		Match m = scene.match();
		int done = m.phase() == Match.Phase.GAME_OVER ? m.rules().numberOfHands() : m.handIndex();
		counters.tally(TALLY_X, layout.h - TALLY_DROP, m.rules().numberOfHands(), done, Counters.pulse(scene.time()), 1f);
	}

	void bidTracker() {
		Match m = scene.match();
		float x0 = TALLY_X - Counters.TALLY_W / 2;
		float avail = layout.panelX(2) - 14 - x0;
		counters.tracker(x0, layout.h - 12 - Counters.TALLY_H - Counters.TRACKER_BELOW_TALLY, avail, m.handSize(),
				m.bidsSeenBy(Seating.YOU), 1f);
	}

	void trump(boolean gone) {
		TableFlow flow = scene.flow();
		if (flow.state() == TableState.SHUFFLE || gone) {
			if (gone) {
				motes.draw(batch, assets.glow);
			}
			return;
		}
		float x = layout.trumpX(), y = layout.trumpY(), cw = layout.trumpW(), ch = layout.trumpH();
		if (scene.match().trumpCard() == null) {
			if (!flow.isDealing()) {
				batch.draw(assets.cardSlot, x, y, cw, ch);
			}
			return;
		}
		art.shadow(x, y, cw, ch, TRUMP_LIFT);
		float p = flow.isDealing() ? 0 : TRUMP_TURNING.progress(flow.trumpTime());
		float lit = TRUMP_LIGHTING.progress(flow.trumpTime());
		if (p >= 1 && lit > 0) {
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
			float gw = cw * TRUMP_GLOW_W, gh = ch * TRUMP_GLOW_H;
			batch.setColor(TRUMP_GLOW.r, TRUMP_GLOW.g, TRUMP_GLOW.b, TRUMP_GLOW.a * lit);
			batch.draw(assets.glow, x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh);
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			batch.setColor(Color.WHITE);
		}
		Card trump = scene.match().trumpCard();
		art.flip(trump, x, y, cw, ch, p);
		if (p >= 1) {
			Glints.draw(batch, assets.glow, trump, x, y, cw, ch, cw, 0, scene.time(), lit);
		}
		if (p >= 1 && lit > 0) {
			post.light((x + cw / 2) / layout.w, (y + ch / 2) / layout.h, 0.3f, lit);
		}
		motes.draw(batch, assets.glow);
	}

	void seat(int seat) {
		TableFlow flow = scene.flow();
		Match m = scene.match();
		boolean picking = flow.state() == TableState.PICK_DEALER;
		boolean turn = picking ? DealerPick.litSeat(flow.stateTime(), m.dealer(), m.players()) == seat
				: flow.state() == TableState.TURN && m.currentSeat() == seat;
		boolean dealer = m.dealer() == seat && (!picking || DealerPick.landed(flow.stateTime()));
		state.reset().named(scene.name(seat)).score(scoreShown(flow, m, seat)).turn(turn).dealer(dealer)
				.doubled(m.onMomentum(seat)).bid(m.bidSeenBy(seat, Seating.YOU)).won(m.tricksWon(seat)).cards(m.handSize()).plain(flow.isDealing());
		panel.draw(state, layout.panelX(seat), layout.panelTop(seat), scene.time());
	}

	static int scoreShown(TableFlow flow, Match m, int seat) {
		boolean pending = m.phase() == Match.Phase.HAND_OVER
				&& (flow.state() == TableState.TRICK || flow.state() == TableState.HAND_END);
		return m.score(seat) - (pending ? m.lastHandPoints(seat) : 0);
	}

	void layoutHand() {
		TableFlow flow = scene.flow();
		Match m = scene.match();
		List<Card> order = scene.handOrder();
		order.clear();
		order.addAll(m.hand(Seating.YOU));
		Collections.sort(order, sort.get().order(m.trump()));
		int n = order.size();
		TapAreas.sizeTo(scene.handRects(), n);
		if (n == 0) {
			return;
		}
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		layout.layoutHand(n);
		List<Card> legal = scene.humanCanPlay() ? m.legalPlays(Seating.YOU) : Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			float x = layout.handStart + i * layout.handGap;
			float y = TableLayout.HAND_Y + (legal.contains(order.get(i)) ? TableLayout.LIFT : 0);
			scene.handRects().get(i).set(x, y, i == n - 1 ? cw : layout.handGap, ch);
		}
	}

}
