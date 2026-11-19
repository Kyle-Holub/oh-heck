package ohheck.table;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.Glints;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.TableView;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.Palette;

/**
 * Every card on the table, each as one {@link CardSprite} for the whole game. Each frame
 * this works out where each card belongs (the dealer's deck, a hand, a fan, the trick, the
 * winner gathering it in, under your finger) and the sprite glides there from wherever it
 * is: a card you play leaves from your hand or your finger, an opponent's from their fan,
 * and your hand closes up after it. Then they're drawn back to front.
 */
final class CardSprites {

	private static final int HUMAN = TableLayout.HUMAN;
	/** A card played flies in to the trick, dropping onto it; a card moving about the hand settles. */
	private static final Tween PLAYED = Tween.over(TrickView.PLAY_FLIGHT, Ease.OUT), SETTLE = Tween.over(0.15f, Ease.OUT);
	private static final float PLAY_ARC = 30;
	/** Dealt: flying from the dealer; then your cards turning face up. */
	private static final Tween DEALT = Tween.over(DealAnimation.DEAL_FLIGHT, Ease.OUT),
			TURNING = Tween.over(DealAnimation.FLIP_TIME, Ease.LINEAR);
	/** The winner gathering the trick in (from the start of the gathering). */
	private static final Tween COLLECT = Tween.over(TrickView.COLLECT_TIME, Ease.IN);
	/** Lifts (for shadows): a dealt card in the air, a card on the trick, one gathered in, one under your finger. */
	private static final float DEAL_LIFT = 30, TRICK_LIFT = 5, COLLECT_LIFT = 23, HAND_LIFT = 5, HELD_LIFT = 0;

	/** Back to front: the fans, your hand, the trick, cards in the air from the deal, the card you hold. */
	private static final int FAN = 0, HAND = 1, TRICK = 2, DEALING = 3, HELD = 4;

	private final CardSprite[] sprites = new CardSprite[52];
	/** This frame: each sprite's layer and order within it, and the hand's tap-area width (for glints). */
	private final int[] layer = new int[52], order = new int[52];
	/** Your cards' visible width (the next card covers the rest), for their glints. */
	private final float[] glintW = new float[52];
	private final boolean[] seen = new boolean[52], dim = new boolean[52];
	private final CardSprite[] drawOrder = new CardSprite[52];
	private final CardSprite.Pose pose = new CardSprite.Pose(), dealer = new CardSprite.Pose();
	/** The winning card of a completed trick (framed), or -1. */
	private int framed = -1;

	CardSprites() {
		for (int i = 0; i < 52; i++) {
			sprites[i] = new CardSprite(cardOf(i));
		}
	}

	private static Card cardOf(int index) {
		for (Card c : allCards()) {
			if (TableView.index(c) == index) {
				return c;
			}
		}
		throw new IllegalStateException();
	}

	private static Iterable<Card> allCards() {
		java.util.List<Card> all = new java.util.ArrayList<Card>();
		for (ohheck.rules.Suit s : ohheck.rules.Suit.values()) {
			for (ohheck.rules.Rank r : ohheck.rules.Rank.values()) {
				all.add(Card.of(r, s));
			}
		}
		return all;
	}

	private CardSprite sprite(Card c) {
		return sprites[TableView.index(c)];
	}

	/** Puts each card where it belongs at game time now, for the table as it is (its hand laid out already). */
	void update(TableScene s, float now) {
		java.util.Arrays.fill(seen, false);
		framed = -1;
		TableFlow flow = s.flow();
		Match m = s.match();
		TableLayout table = s.layout();
		if (flow.state() != TableState.GAME_OVER) { // (at the end the toss takes the cards)
			if (flow.isDealing()) {
				Vector2 d = table.seatAnchor(flow.deal.dealer());
				dealer.set(d.x - TableLayout.backW() / 2, d.y - TableLayout.backH() / 2, TableLayout.backW(),
						TableLayout.backH(), 0, 0, DEAL_LIFT);
			}
			for (int seat = 1; seat < m.players(); seat++) {
				fan(flow, m, table, seat, now);
			}
			hand(s, flow, m, table, now);
			if (flow.state() == TableState.TRICK) {
				gathering(m, table, flow.stateTime(), now);
			} else {
				trick(m.currentTrick(), m.currentTrickSeats(), m, table, now);
			}
		}
		for (int i = 0; i < 52; i++) {
			if (!seen[i] && sprites[i].visible()) {
				sprites[i].show(false);
			}
		}
	}

	private void place(CardSprite sp, int layer, int order) {
		int i = TableView.index(sp.card);
		seen[i] = true;
		this.layer[i] = layer;
		this.order[i] = order;
		dim[i] = false;
		if (!sp.visible()) {
			sp.show(true);
		}
	}

	/**
	 * While dealing, the j-th card to a seat: hidden until it leaves the dealer, then flying
	 * face down to rest (backdated to when it left); if it's yours, turning face up once it
	 * lands. rest is its place face down. Returns seconds since it left, or -1 if it hasn't.
	 */
	private float dealt(TableFlow flow, int seat, int j, CardSprite sp, CardSprite.Pose rest, float now) {
		float since = flow.deal.dealTime() - flow.deal.startOf(seat, j);
		if (since < 0) {
			return -1;
		}
		if (!sp.visible()) {
			sp.snap(dealer);
		}
		if (seat == HUMAN && since >= DealAnimation.DEAL_FLIGHT) {
			rest.flip = 1;
			sp.moveTo(rest, TURNING, 0, now, since - DealAnimation.DEAL_FLIGHT);
		} else {
			sp.moveTo(rest, DEALT, 0, now, since);
		}
		place(sp, since < DealAnimation.DEAL_FLIGHT ? DEALING : seat == HUMAN ? HAND : FAN, j);
		return since;
	}

	private void fan(TableFlow flow, Match m, TableLayout table, int seat, float now) {
		List<Card> hand = m.hand(seat);
		int total = hand.size();
		float y = table.fanOrigin(seat).y;
		for (int i = 0; i < total; i++) {
			CardSprite sp = sprite(hand.get(i));
			pose.set(table.fanCardX(seat, i, total), y, TableLayout.backW(), TableLayout.backH(), 0, 0, 0);
			if (flow.isDealing()) {
				dealt(flow, seat, i, sp, pose, now);
			} else {
				place(sp, FAN, i);
				sp.moveTo(pose, SETTLE, 0, now, 0);
			}
		}
	}

	private void hand(TableScene s, TableFlow flow, Match m, TableLayout table, float now) {
		List<Card> order = s.handOrder();
		int n = order.size();
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		boolean myTurn = s.humanCanPlay();
		List<Card> legal = myTurn ? m.legalPlays(HUMAN) : java.util.Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			Card c = order.get(i);
			CardSprite sp = sprite(c);
			int idx = TableView.index(c);
			boolean canPlay = legal.contains(c);
			if (s.isDragging() && c == s.pressedCard()) { // under your finger
				place(sp, HELD, 0);
				sp.snap(pose.set(s.dragX() - cw / 2, s.dragY() - ch / 2, cw, ch, 0, 1, HELD_LIFT));
				glintW[idx] = cw;
				continue;
			}
			float x = table.handStart + i * table.handGap, y = TableLayout.HAND_Y + (canPlay ? TableLayout.LIFT : 0);
			pose.set(x, y, cw, ch, 0, 1, y - TableLayout.HAND_Y + HAND_LIFT);
			if (flow.isDealing()) {
				pose.flip = 0;
				pose.lift = HAND_LIFT;
				if (dealt(flow, HUMAN, i, sp, pose, now) < 0) {
					continue;
				}
			} else {
				place(sp, HAND, i);
				sp.moveTo(pose, SETTLE, 0, now, 0);
			}
			dim[idx] = myTurn && !canPlay;
			glintW[idx] = i == n - 1 ? cw : table.handGap;
		}
	}

	/** The trick in progress: each card flies to its place as it's played. */
	private void trick(List<Card> cards, List<Integer> seats, Match m, TableLayout table, float now) {
		for (int i = 0; i < cards.size(); i++) {
			Card c = cards.get(i);
			int seat = seats.get(i);
			CardSprite sp = sprite(c);
			Vector2 slot = table.trickSlot(seat);
			place(sp, TRICK, i);
			sp.moveTo(pose.set(slot.x, slot.y, Assets.CARD_W, Assets.CARD_H, TrickView.tilt(c, seat, m.handIndex()), 1,
					TRICK_LIFT), PLAYED, PLAY_ARC, now, 0);
		}
	}

	/** A completed trick: on show, the winner's card framed, then gathered in by the winner. */
	private void gathering(Match m, TableLayout table, float elapsed, float now) {
		if (elapsed >= TrickView.TRICK_PAUSE) {
			return; // gone to the winner
		}
		float gatherFrom = TrickView.TRICK_PAUSE - TrickView.COLLECT_TIME;
		List<Card> cards = m.lastTrick();
		trick(cards, m.lastTrickSeats(), m, table, now);
		int winner = m.lastTrickWinner();
		if (elapsed < gatherFrom) {
			for (int i = 0; i < cards.size(); i++) {
				if (m.lastTrickSeats().get(i) == winner) {
					framed = TableView.index(cards.get(i));
				}
			}
			return;
		}
		Vector2 sink = table.seatAnchor(winner);
		float cw = Assets.CARD_W, ch = Assets.CARD_H;
		pose.set(sink.x - cw / 4, sink.y - ch / 4, cw / 2, ch / 2, 0, 1, COLLECT_LIFT);
		for (Card c : cards) {
			sprite(c).moveTo(pose, COLLECT, 0, now, elapsed - gatherFrom);
		}
	}

	/** Whether any card is still on its way. */
	boolean moving(float now) {
		for (int i = 0; i < 52; i++) {
			if (seen[i] && sprites[i].moving(now)) {
				return true;
			}
		}
		return false;
	}

	/** Draws the cards back to front. */
	void draw(SpriteBatch batch, Assets assets, CardArt art, Match m, boolean trumpFaceUp, float now, float fade) {
		int n = 0;
		for (int i = 0; i < 52; i++) {
			if (seen[i]) {
				drawOrder[n++] = sprites[i];
			}
		}
		java.util.Arrays.sort(drawOrder, 0, n, (a, b) -> {
			int ia = TableView.index(a.card), ib = TableView.index(b.card);
			return layer[ia] != layer[ib] ? layer[ia] - layer[ib] : order[ia] - order[ib];
		});
		for (int k = 0; k < n; k++) {
			CardSprite sp = drawOrder[k];
			int i = TableView.index(sp.card);
			CardSprite.Pose p = sp.pose(now);
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
