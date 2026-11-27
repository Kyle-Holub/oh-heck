package ohheck.table;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.Glints;
import ohheck.fx.Motes;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.HandSort;
import ohheck.rules.Match;
import ohheck.rules.Suit;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.PostProcessor;
import ohheck.ui.TapAreas;

/* Draws the counters, the trump card, the placards and your hand's layout. */
final class TableDrawing {

	private static final float TRUMP_LIFT = 4, TRUMP_GLOW_IN = 0.6f, TRUMP_GLOW_W = 1.9f, TRUMP_GLOW_H = 1.6f;
	private static final Color TRUMP_GLOW = new Color(1f, 0.78f, 0.4f, 0.22f);
	private static final Tween TRUMP_TURNING = Tween.over(TableFlow.TRUMP_FLIP_TIME, Ease.LINEAR),
			TRUMP_LIGHTING = Tween.over(TRUMP_GLOW_IN, Ease.LINEAR).after(TableFlow.TRUMP_FLIP_TIME);
	private static final float TALLY_X = 22, TALLY_DROP = 12;

	private final TableScene scene;
	private final PostProcessor post;
	private final SpriteBatch batch;
	private final Painter paint;
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
		this.paint = paint;
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

	private static final String[] SUIT_SIGNS = { "\u2663", "\u2660", "\u2665", "\u2666" };
	private static final Color RED_TINT = new Color(0.95f, 0.25f, 0.2f, 1);
	static final float PUFF_LEN = 0.8f;
	private static final float PUFF_REACH = 80, PUFF_FROM = 18;
	private static final int PUFFS = 8;

	private static boolean red(Suit s) {
		return s == Suit.HEARTS || s == Suit.DIAMONDS;
	}

	private void suitPuff(Suit s, float cx, float cy, float p) {
		if (p < 0 || p >= 1) {
			return;
		}
		float e = Ease.OUT.apply(p);
		Color c = red(s) ? RED_TINT : Palette.CREAM;
		BitmapFont f = paint.serif(1.2f + 0.6f * e, c, 1 - p);
		for (int k = 0; k < PUFFS; k++) {
			float a = k * MathUtils.PI2 / PUFFS + 0.3f, r = PUFF_FROM + PUFF_REACH * e;
			paint.centred(f, SUIT_SIGNS[s.ordinal()], cx + MathUtils.cos(a) * r, cy + MathUtils.sin(a) * r + 10);
		}
	}

	private static final Tween PICK_DROP = Tween.over(0.35f, Ease.IN), WIDOW_SLIDE = Tween.over(0.35f, Ease.OUT).after(TableFlow.TRUMP_FLIP_TIME),
			BID_STAMP = Tween.over(0.22f, Ease.OUT), BID_FLIP = Tween.over(0.25f, Ease.OUT);
	private static final float PICK_SWELL = 1.4f, PICK_RISE = -90, ALARM_FLICKER = 9, STAMP_SIZE = 0.8f, REVEAL_STAGGER = 0.09f,
			WIDOW_SCALE = 0.6f, WIDOW_GAP = 8;
	private final int[] shownBid = { Match.NO_BID, Match.NO_BID, Match.NO_BID, Match.NO_BID };
	private final float[] bidAt = { -99, -99, -99, -99 };
	private final boolean[] revealed = new boolean[Seating.SEATS];

	void widow() {
		Match m = scene.match();
		TableFlow flow = scene.flow();
		if (m.widow().isEmpty() || flow.isDealing() || m.phase() == Match.Phase.HAND_OVER) {
			return;
		}
		float slide = WIDOW_SLIDE.progress(flow.trumpTime());
		float cw = layout.trumpW() * WIDOW_SCALE, ch = layout.trumpH() * WIDOW_SCALE, y = layout.trumpY() - ch - WIDOW_GAP;
		for (int k = 0; k < 2; k++) {
			float restX = layout.trumpX() + layout.trumpW() - (k + 1) * cw - k * WIDOW_GAP;
			float x = Ease.lerp(layout.trumpX() + layout.trumpW() / 2 - cw / 2, restX, slide);
			art.shadow(x, y, cw, ch, 3);
			batch.setColor(1, 1, 1, slide);
			batch.draw(assets.back(), x, y, cw, ch);
		}
		batch.setColor(Color.WHITE);
	}

	void tally() {
		Match m = scene.match();
		int done = m.phase() == Match.Phase.GAME_OVER ? m.rules().numberOfHands() : Math.min(m.handIndex(), m.rules().numberOfHands());
		float alarm = m.suddenDeath() ? 0.5f + 0.5f * MathUtils.sin(scene.time() * ALARM_FLICKER) : 0;
		counters.tally(TALLY_X, layout.h - TALLY_DROP, m.rules().numberOfHands(), done, Counters.pulse(scene.time()), 1f, alarm);
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
		Match m = scene.match();
		if (m.trumpCard() == null || m.pickingTrump() || m.pickedTrump() != null) {
			if (!flow.isDealing()) {
				batch.draw(assets.cardSlot, x, y, cw, ch);
				pickedTrump(x, y, cw, ch);
			}
			return;
		}
		art.shadow(x, y, cw, ch, TRUMP_LIFT);
		float p = flow.isDealing() ? 0 : TRUMP_TURNING.progress(flow.trumpTime());
		float lit = TRUMP_LIGHTING.progress(flow.trumpTime());
		if (p >= 1 && lit > 0) {
			float gw = cw * TRUMP_GLOW_W, gh = ch * TRUMP_GLOW_H;
			paint.glow(x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh, TRUMP_GLOW, TRUMP_GLOW.a * lit);
		}
		Card trump = scene.match().trumpCard();
		art.flip(trump, x, y, cw, ch, p);
		if (p >= 1) {
			suitPuff(trump.suit(), x + cw / 2, y + ch / 2, (flow.trumpTime() - TableFlow.TRUMP_FLIP_TIME) / PUFF_LEN);
		}
		if (p >= 1) {
			Glints.draw(batch, assets.glow, trump, x, y, cw, ch, cw, 0, scene.time(), lit);
		}
		if (p >= 1 && lit > 0) {
			post.light((x + cw / 2) / layout.w, (y + ch / 2) / layout.h, 0.3f, lit);
		}
		motes.draw(batch, assets.glow);
	}

	private static final float PICKED_EMBLEM = 0.72f;
	private Suit shownPick;
	private float pickedAt;

	private void pickedTrump(float x, float y, float cw, float ch) {
		Suit s = scene.match().pickedTrump();
		if (s != shownPick) {
			shownPick = s;
			pickedAt = scene.time();
		}
		if (s == null) {
			return;
		}
		float since = scene.time() - pickedAt, drop = PICK_DROP.progress(since);
		float e = cw * PICKED_EMBLEM * (1 + PICK_SWELL * (1 - drop)), rise = PICK_RISE * (1 - drop);
		batch.setColor(1, 1, 1, Math.min(1, drop * 3));
		batch.draw(assets.emblem(s), x + (cw - e) / 2, y + (ch - e) / 2 - rise, e, e);
		batch.setColor(Color.WHITE);
		suitPuff(s, x + cw / 2, y + ch / 2, (since - PICK_DROP.end()) / PUFF_LEN);
	}

	void momentum() {
		Match m = scene.match();
		if (!m.rules().momentum) {
			return;
		}
		float now = scene.time();
		for (int seat = 0; seat < m.players(); seat++) {
			int streak = Math.min(MOMENTUM_NOTCHES, m.streak(seat));
			if (m == momentumOf && streak != shownStreak[seat]) {
				changedAt[seat] = now;
			}
			shownStreak[seat] = streak;
			momentumBar(seat, streak, MathUtils.clamp((now - changedAt[seat]) / NOTCH_FILL, 0, 1), now);
		}
		momentumOf = m;
	}

	private void momentumBar(int seat, int lit, float fill, float now) {
		int spot = layout.spot(seat);
		boolean left = spot == 2 || spot == 3;
		float pop = 1 + BAR_POP * (1 - fill), bw = BAR_W * pop, bh = BAR_H * pop;
		float cx = left ? layout.panelX(seat) - BAR_GAP - BAR_W / 2 : layout.panelX(seat) + PlayerPanel.W + BAR_GAP + BAR_W / 2;
		float cy = layout.panelTop(seat) - PlayerPanel.H / 2;
		if (lit >= MOMENTUM_NOTCHES) {
			paint.glow(cx - BAR_W * 2, cy - BAR_H * 0.75f, BAR_W * 4, BAR_H * 1.5f, Palette.EMBER, 0.35f + 0.15f * MathUtils.sin(now * 4));
		}
		batch.draw(assets.momentum[lit], cx - bw / 2, cy - bh / 2, bw, bh);
	}

	private static final Tween TURN_PULSE = Tween.over(0.8f, Ease.LINEAR), BID_GILT = Tween.over(0.9f, Ease.OUT);
	private static final float BAR_W = 22, BAR_H = BAR_W * 144 / 44, BAR_GAP = 5, BAR_POP = 0.25f, NOTCH_FILL = 0.45f;
	private static final int MOMENTUM_NOTCHES = 2;
	private final int[] shownStreak = new int[Seating.SEATS];
	private final float[] changedAt = { -99, -99, -99, -99 };
	private Match momentumOf;

	void seat(int seat) {
		TableFlow flow = scene.flow();
		Match m = scene.match();
		boolean picking = flow.state() == TableState.PICK_DEALER;
		boolean turn = picking ? DealerPick.litSeat(flow.stateTime(), m.dealer(), m.players()) == seat
				: flow.state() == TableState.TURN && m.currentSeat() == seat;
		boolean dealer = m.dealer() == seat && (!picking || DealerPick.settled(flow.stateTime()));
		state.reset().named(scene.placardName(seat)).score(scoreShown(flow, m, seat)).turn(turn).dealer(dealer)
				.doubled(m.onMomentum(seat)).bid(m.bidSeenBy(seat, Seating.YOU)).won(m.tricksWon(seat)).cards(m.handSize()).plain(flow.isDealing());
		appear(seat, m);
		if (seat == Seating.YOU) {
			boolean scored = m.phase() == Match.Phase.HAND_OVER && (flow.state() == TableState.HAND_END || flow.state() == TableState.RESULTS);
			boolean made = scored && m.madeBid(seat);
			float pulse = turn && flow.state() == TableState.TURN ? TURN_PULSE.progress(flow.stateTime()) : 1;
			float gilt = made && flow.state() == TableState.HAND_END ? 1 - BID_GILT.progress(flow.stateTime()) : 0;
			state.accents(pulse < 1 ? (float) Math.sin(Math.PI * pulse) : 0, gilt, scored && !made);
		}
		panel.draw(state, layout.panelX(seat), layout.panelTop(seat), scene.time());
	}

	private void appear(int seat, Match m) {
		int bid = m.bidSeenBy(seat, Seating.YOU);
		float now = scene.time();
		if (bid != shownBid[seat]) {
			boolean reveal = shownBid[seat] == Match.NO_BID && bid != Match.NO_BID && m.rules().blindBids && seat != Seating.YOU;
			int order = ((seat - m.dealer() - 1) % m.players() + m.players()) % m.players();
			bidAt[seat] = bid == Match.NO_BID ? -99 : now + (reveal ? order * REVEAL_STAGGER : 0);
			revealed[seat] = reveal;
			shownBid[seat] = bid;
		}
		float since = now - bidAt[seat];
		float stamp = revealed[seat] ? 0 : STAMP_SIZE * (1 - BID_STAMP.progress(since));
		float flip = revealed[seat] ? BID_FLIP.progress(since) : 1;
		state.appear(stamp, flip);
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
