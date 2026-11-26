package ohheck.table;

import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.Glints;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.Palette;

/*
 * Every card on the table: sets each sprite's target from the game each frame, then draws them
 * back to front. A played card squashes as it lands (yours hardest), a trump cutting another suit's lead
 * trails gold, the card under your finger rises and tilts, and a card you may not play shakes its head.
 */
final class CardSprites {

	private static final Tween PLAYED = Tween.over(TrickView.PLAY_FLIGHT, Ease.OUT), SETTLE = Tween.over(0.15f, Ease.OUT);
	private static final float PLAY_ARC = 30;
	private static final Tween DEALT = Tween.over(DealAnimation.DEAL_FLIGHT, Ease.OUT),
			TURNING = Tween.over(DealAnimation.FLIP_TIME, Ease.LINEAR);
	private static final Tween COLLECT = Tween.over(TrickView.COLLECT_TIME, Ease.IN);
	private static final float DEAL_LIFT = 30, TRICK_LIFT = 5, COLLECT_LIFT = 23, HAND_LIFT = 5, HELD_LIFT = 0;

	private static final int FAN = 0, HAND = 1, TRICK = 2, DEALING = 3, HELD = 4;
	private static final Tween SLAP = Tween.over(0.16f, Ease.LINEAR), REFUSED = Tween.over(0.4f, Ease.LINEAR);
	private static final float SLAP_YOURS = 0.1f, SLAP_THEIRS = 0.05f, HOVER_RISE = 16, HOVER_TILT = -4, REFUSE_SHAKE = 8,
			STREAK_STEP = 0.025f, STREAK_GLOW = 1.4f;
	private static final int STREAK_GHOSTS = 5;
	private static final float RAKE_STAGGER = 0.03f;

	private final CardSprite[] sprites = new CardSprite[Card.COUNT];
	private final int[] layer = new int[Card.COUNT], order = new int[Card.COUNT];
	private final float[] glintW = new float[Card.COUNT];
	private final boolean[] seen = new boolean[Card.COUNT], dim = new boolean[Card.COUNT], cuts = new boolean[Card.COUNT];
	private final float[] slap = new float[Card.COUNT];
	private final CardSprite[] drawOrder = new CardSprite[Card.COUNT];
	private final CardSprite.Pose pose = new CardSprite.Pose(), dealer = new CardSprite.Pose();
	private int framed = -1;

	CardSprites() {
		for (int i = 0; i < Card.COUNT; i++) {
			sprites[i] = new CardSprite(Card.ofIndex(i));
		}
	}

	private CardSprite sprite(Card c) {
		return sprites[c.index()];
	}

	void update(TableScene s, float now) {
		Arrays.fill(seen, false);
		framed = -1;
		TableFlow flow = s.flow();
		Match m = s.match();
		TableLayout layout = s.layout();
		if (flow.state() != TableState.GAME_OVER) {
			if (flow.isDealing()) {
				Vector2 d = layout.dealSpot(flow.deal().dealer());
				float dw = Assets.CARD_W * DealAnimation.DECK_SCALE, dh = Assets.CARD_H * DealAnimation.DECK_SCALE;
				dealer.set(d.x - dw / 2, d.y - dh / 2, dw, dh, 0, 0, DEAL_LIFT);
			}
			for (int seat = 1; seat < m.players(); seat++) {
				fan(flow, m, layout, seat, now);
			}
			hand(s, flow, m, layout, now);
			if (flow.state() == TableState.TRICK) {
				gathering(m, layout, flow.stateTime(), now);
			} else {
				trick(m.currentTrick(), m.currentTrickSeats(), m, layout, now);
			}
		}
		for (int i = 0; i < Card.COUNT; i++) {
			if (!seen[i] && sprites[i].visible()) {
				sprites[i].show(false);
			}
		}
	}

	private void place(CardSprite sp, int layer, int order) {
		int i = sp.card.index();
		seen[i] = true;
		this.layer[i] = layer;
		this.order[i] = order;
		dim[i] = false;
		if (!sp.visible()) {
			sp.show(true);
		}
	}

	private float dealt(TableFlow flow, int seat, int j, CardSprite sp, CardSprite.Pose rest, float now) {
		float since = flow.deal().dealTime() - flow.deal().startOf(seat, j);
		if (flow.deal().isShuffling() || since < 0) {
			return -1;
		}
		if (!sp.visible()) {
			sp.snap(dealer);
		}
		if (seat == Seating.YOU && since >= DealAnimation.DEAL_FLIGHT) {
			rest.flip = 1;
			sp.moveTo(rest, TURNING, 0, now, since - DealAnimation.DEAL_FLIGHT);
		} else {
			sp.moveTo(rest, DEALT, 0, now, since);
		}
		place(sp, since < DealAnimation.DEAL_FLIGHT ? DEALING : seat == Seating.YOU ? HAND : FAN, j);
		return since;
	}

	private void fan(TableFlow flow, Match m, TableLayout layout, int seat, float now) {
		List<Card> hand = m.hand(seat);
		int total = hand.size();
		float y = layout.fanOrigin(seat).y;
		for (int i = 0; i < total; i++) {
			CardSprite sp = sprite(hand.get(i));
			pose.set(layout.fanCardX(seat, i, total), y, TableLayout.backW(), TableLayout.backH(), 0, 0, 0);
			if (flow.isDealing()) {
				dealt(flow, seat, i, sp, pose, now);
			} else {
				place(sp, FAN, i);
				sp.moveTo(pose, SETTLE, 0, now, 0);
			}
		}
	}

	private void hand(TableScene s, TableFlow flow, Match m, TableLayout layout, float now) {
		List<Card> order = s.handOrder();
		int n = order.size();
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		boolean myTurn = s.humanCanPlay();
		List<Card> legal = myTurn ? m.legalPlays(Seating.YOU) : java.util.Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			Card c = order.get(i);
			CardSprite sp = sprite(c);
			int idx = c.index();
			boolean canPlay = legal.contains(c) && s.playable(c);
			if (s.isDragging() && c == s.pressedCard()) {
				place(sp, HELD, 0);
				sp.snap(pose.set(s.dragX() - cw / 2, s.dragY() - ch / 2, cw, ch, 0, 1, HELD_LIFT));
				glintW[idx] = cw;
				continue;
			}
			float x = layout.handStart + i * layout.handGap, y = TableLayout.HAND_Y + (canPlay ? TableLayout.LIFT : 0);
			boolean hovered = c == s.hoveredCard();
			if (hovered) {
				y += HOVER_RISE;
			}
			float refusing = c == s.refusedCard() ? REFUSED.progress(s.refusedFor()) : 1;
			if (refusing < 1) {
				x += (float) Math.sin(s.refusedFor() * 45) * REFUSE_SHAKE * (1 - refusing);
			}
			pose.set(x, y, cw, ch, hovered ? HOVER_TILT : 0, 1, y - TableLayout.HAND_Y + HAND_LIFT);
			if (flow.isDealing()) {
				pose.flip = 0;
				pose.lift = HAND_LIFT;
				if (dealt(flow, Seating.YOU, i, sp, pose, now) < 0) {
					continue;
				}
			} else if (refusing < 1) {
				place(sp, HAND, i);
				sp.snap(pose);
			} else {
				place(sp, HAND, i);
				sp.moveTo(pose, SETTLE, 0, now, 0);
			}
			dim[idx] = myTurn && !canPlay;
			glintW[idx] = i == n - 1 ? cw : layout.handGap;
		}
	}

	private void trick(List<Card> cards, List<Integer> seats, Match m, TableLayout layout, float now) {
		for (int i = 0; i < cards.size(); i++) {
			Card c = cards.get(i);
			int seat = seats.get(i);
			CardSprite sp = sprite(c);
			Vector2 slot = layout.trickSlot(seat);
			place(sp, TRICK, i);
			slap[c.index()] = seat == Seating.YOU ? SLAP_YOURS : SLAP_THEIRS;
			cuts[c.index()] = i > 0 && c.suit() == m.trump() && cards.get(0).suit() != m.trump();
			sp.moveTo(pose.set(slot.x, slot.y, Assets.CARD_W, Assets.CARD_H, TrickView.tilt(c, seat, m.handIndex()), 1,
					TRICK_LIFT), PLAYED, PLAY_ARC, now, 0);
		}
	}

	private void gathering(Match m, TableLayout layout, float elapsed, float now) {
		if (elapsed >= TrickView.TRICK_PAUSE) {
			return;
		}
		float gatherFrom = TrickView.TRICK_PAUSE - TrickView.COLLECT_TIME;
		List<Card> cards = m.lastTrick();
		int winner = m.lastTrickWinner();
		if (elapsed < gatherFrom) {
			trick(cards, m.lastTrickSeats(), m, layout, now);
			for (int i = 0; i < cards.size(); i++) {
				if (m.lastTrickSeats().get(i) == winner) {
					framed = cards.get(i).index();
				}
			}
			return;
		}
		Vector2 sink = layout.seatAnchor(winner);
		float cw = Assets.CARD_W, ch = Assets.CARD_H;
		pose.set(sink.x - cw / 4, sink.y - ch / 4, cw / 2, ch / 2, 0, 1, COLLECT_LIFT);
		for (int i = 0; i < cards.size(); i++) {
			CardSprite sp = sprite(cards.get(i));
			place(sp, TRICK, i);
			sp.moveTo(pose, COLLECT, 0, now, elapsed - gatherFrom - i * RAKE_STAGGER);
		}
	}

	private void streak(SpriteBatch batch, Assets assets, CardSprite sp, float now) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int g = STREAK_GHOSTS; g >= 1; g--) {
			CardSprite.Pose p = sp.pose(now - g * STREAK_STEP);
			float a = 0.5f * (1 - g / (float) (STREAK_GHOSTS + 1)), gw = p.w * STREAK_GLOW, gh = p.h * STREAK_GLOW;
			batch.setColor(Palette.SPARKLE.r, Palette.SPARKLE.g, Palette.SPARKLE.b, a);
			batch.draw(assets.glow, p.x + p.w / 2 - gw / 2, p.y + p.h / 2 - gh / 2, gw, gh);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	boolean moving(float now) {
		for (int i = 0; i < Card.COUNT; i++) {
			if (seen[i] && sprites[i].moving(now)) {
				return true;
			}
		}
		return false;
	}

	void draw(SpriteBatch batch, Assets assets, CardArt art, Match m, boolean trumpFaceUp, float now, float fade) {
		int n = 0;
		for (int i = 0; i < Card.COUNT; i++) {
			if (seen[i]) {
				drawOrder[n++] = sprites[i];
			}
		}
		java.util.Arrays.sort(drawOrder, 0, n, (a, b) -> {
			int ia = a.card.index(), ib = b.card.index();
			return layer[ia] != layer[ib] ? layer[ia] - layer[ib] : order[ia] - order[ib];
		});
		for (int k = 0; k < n; k++) {
			CardSprite sp = drawOrder[k];
			int i = sp.card.index();
			if (layer[i] == TRICK && cuts[i] && sp.moving(now)) {
				streak(batch, assets, sp, now);
			}
			CardSprite.Pose p = sp.pose(now);
			if (layer[i] == TRICK) {
				float landed = sp.landedFor(now);
				if (landed >= 0 && landed < SLAP.end()) {
					float bump = (float) Math.sin(Math.PI * SLAP.progress(landed)) * slap[i];
					float sw = p.w * (1 + bump), sh = p.h * (1 - bump);
					p.x -= (sw - p.w) / 2;
					p.y -= (sh - p.h) / 2;
					p.w = sw;
					p.h = sh;
				}
			}
			if (p.lift > 0.5f && layer[i] != FAN) {
				art.shadow(p.x, p.y, p.w, p.h, p.lift);
			}
			if (i == framed) {
				batch.setColor(1f, 1f, 1f, fade);
				assets.panelTurn.draw(batch, p.x - 7, p.y - 7, p.w / 2 + 7, p.h / 2 + 7, p.w + 14, p.h + 14, 1f, 1f, p.rot);
				batch.setColor(Color.WHITE);
			}
			batch.setColor(dim[i] ? Palette.CARD_DIM : Color.WHITE);
			art.flip(sp.card, p.x, p.y, p.w, p.h, p.flip, p.rot);
			batch.setColor(Color.WHITE);
			boolean glints = layer[i] == HAND ? trumpFaceUp : layer[i] == TRICK || layer[i] == HELD;
			if (glints && p.flip >= 1 && sp.card.suit() == m.trump()) {
				Glints.draw(batch, assets.glow, sp.card, p.x, p.y, p.w, p.h, layer[i] == HAND ? glintW[i] : p.w, p.rot, now,
						dim[i] ? 0.5f : layer[i] == TRICK ? fade : 1f);
			}
		}
	}
}
