package ohheck.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.TrickShot;
import ohheck.campaign.TrickShotPlay;
import ohheck.campaign.TrickShots;
import ohheck.fx.Sparks;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Numbers;
import ohheck.ui.Palette;

/*
 * A trick shot: every hand face up around the table, the goal above. Every card glides to where it belongs: from a hand into
 * the trick, and from a finished trick, after a moment, to its winner. The others reply one card
 * at a time; nothing can be tapped while a trick is being gathered.
 */
public class TrickShotScreen extends BaseScreen {

	private static final float YOURS = 1.0f, THEIRS = 0.72f, TRICK = 0.75f, PILE = 0.3f;
	private static final float OTHER_STEP = 0.55f, TRICK_HOLD = 0.8f, GATHER = 0.35f;
	private static final float TOP_HAND_Y = 300, SIDE_HAND_DROP = 20, TRICK_DROP = 24, LABEL_GAP = 16, LIFT = 10;
	private static final float BUTTON_W = 120, BUTTON_H = 40, BID_SIZE = 46, BID_GAP = 10;
	private static final Tween GLIDE = Tween.over(0.28f, Ease.OUT), BANNER_IN = Tween.over(0.4f, Ease.OUT);

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final Sparks sparks = new Sparks();
	private final Rectangle backButton = new Rectangle(), restartButton = new Rectangle(),
			nextButton = new Rectangle();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>(), bidRects = new ArrayList<Rectangle>();
	private final float[] fromX = new float[Card.COUNT], fromY = new float[Card.COUNT], fromW = new float[Card.COUNT],
			toX = new float[Card.COUNT], toY = new float[Card.COUNT], toW = new float[Card.COUNT], movedAt = new float[Card.COUNT];
	private final boolean[] placed = new boolean[Card.COUNT], drawn = new boolean[Card.COUNT];
	private final Vector2 spot = new Vector2();
	private TrickShotPlay play;
	private float time, untilOther, trickDoneAt = -99, overAt = -1, lastMotion = -99;
	private boolean recorded;

	public TrickShotScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
	}

	public void open(TrickShot s) {
		play = new TrickShotPlay(s);
		reset();
	}

	private void reset() {
		untilOther = OTHER_STEP;
		trickDoneAt = -99;
		overAt = -1;
		recorded = false;
		for (int i = 0; i < Card.COUNT; i++) {
			placed[i] = false;
		}
	}

	private boolean gathering() {
		return time < trickDoneAt + TRICK_HOLD + GATHER;
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		shownFor += dt;
		time += dt;
		sparks.step(dt);
		if (gathering()) {
			return;
		}
		if (play.othersToMove()) {
			untilOther -= dt;
			if (untilOther <= 0) {
				untilOther = OTHER_STEP;
				int before = play.match().currentTrick().size();
				play.stepOthers();
				noteTrickDone(before);
			}
		}
		if (play.over() && overAt < 0) {
			overAt = time;
			if (play.solved()) {
				sparks.burst(w / 2, h / 2, 120);
			}
		}
		if (play.solved() && !recorded) {
			recorded = true;
			game.records().campaign().solve(play.shot().id);
			game.records().saveCampaign();
		}
	}

	private void noteTrickDone(int sizeBefore) {
		if (play.match().currentTrick().isEmpty() && sizeBefore > 0) {
			trickDoneAt = time;
		}
	}

	@Override
	public boolean animating() {
		return gathering() || play.othersToMove() || GLIDE.active(time - lastMotion) || sparks.active()
				|| overAt >= 0 && BANNER_IN.active(time - overAt);
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		Match m = play.match();
		float top = h - MenuChrome.HEADER_H;
		paint.centred(paint.serif(1.2f, Palette.GOLD_LIGHT), play.shot().goalText(name(play.shot().rival)), w / 2, top - 18);
		drawTrump(m, top);
		for (int i = 0; i < Card.COUNT; i++) {
			drawn[i] = false;
		}
		for (int seat = 1; seat < m.players(); seat++) {
			drawSeat(m, seat, top);
		}
		drawYourHand(m);
		drawTrick(m);
		for (int i = 0; i < Card.COUNT; i++) {
			placed[i] &= drawn[i];
		}
		if (m.phase() == Match.Phase.BIDDING && play.yourTurn()) {
			drawBids(m);
		}
		sparks.draw(batch, assets.glow);
		drawButtons();
		chrome.header(play.shot().title, backButton, w, h);
		batch.end();
	}

	private void card(Card c, float x, float y, float cw) {
		int i = c.index();
		if (!placed[i]) {
			fromX[i] = toX[i] = x;
			fromY[i] = toY[i] = y;
			fromW[i] = toW[i] = cw;
			movedAt[i] = -99;
			placed[i] = true;
		} else if (x != toX[i] || y != toY[i] || cw != toW[i]) {
			float p = GLIDE.progress(time - movedAt[i]);
			fromX[i] = MathUtils.lerp(fromX[i], toX[i], p);
			fromY[i] = MathUtils.lerp(fromY[i], toY[i], p);
			fromW[i] = MathUtils.lerp(fromW[i], toW[i], p);
			toX[i] = x;
			toY[i] = y;
			toW[i] = cw;
			movedAt[i] = time;
			lastMotion = time;
		}
		float p = GLIDE.progress(time - movedAt[i]);
		float cx = MathUtils.lerp(fromX[i], toX[i], p), cy = MathUtils.lerp(fromY[i], toY[i], p), w = MathUtils.lerp(fromW[i], toW[i], p);
		float ch = w * Assets.CARD_H / Assets.CARD_W;
		art.shadow(cx, cy, w, ch, 3);
		art.flip(c, cx, cy, w, ch, 1);
		drawn[i] = true;
	}

	private String name(int seat) {
		return seat == Seating.YOU ? "You" : seat < 0 ? "" : play.shot().chapter.rivals.at(seat).name;
	}

	private static float cardW(float scale) {
		return Assets.CARD_W * scale;
	}

	private static float cardH(float scale) {
		return Assets.CARD_H * scale;
	}

	private static float handWidth(int n, float scale) {
		return cardW(scale) + Math.max(0, n - 1) * cardW(scale) * 0.62f;
	}

	private Vector2 handStart(int seat, int n, float top) {
		if (seat == 1) {
			return spot.set(20, h / 2 - SIDE_HAND_DROP);
		}
		if (seat == 2) {
			return spot.set(w / 2 - handWidth(n, THEIRS) / 2, Math.min(TOP_HAND_Y, top - 136));
		}
		return spot.set(w - 20 - handWidth(Math.max(1, n), THEIRS), h / 2 - SIDE_HAND_DROP);
	}

	private void drawSeat(Match m, int seat, float top) {
		List<Card> hand = m.hand(seat);
		Vector2 at = handStart(seat, hand.size(), top);
		float x = at.x, y = at.y;
		for (int i = 0; i < hand.size(); i++) {
			card(hand.get(i), x + i * cardW(THEIRS) * 0.62f, y, cardW(THEIRS));
		}
		paint.ui(1.0f, Palette.CREAM).draw(batch, name(seat), x, y + cardH(THEIRS) + LABEL_GAP);
		paint.ui(0.9f, Palette.MUTED).draw(batch, seatLine(m, seat), x, y - 6);
	}

	private String seatLine(Match m, int seat) {
		return m.hasBid(seat) ? "bid " + Numbers.text(m.bid(seat)) + ", won " + Numbers.text(m.tricksWon(seat)) : "";
	}

	private void drawTrump(Match m, float top) {
		Card trump = m.trumpCard();
		float tw = cardW(0.7f), th = cardH(0.7f), x = w - 20 - tw, y = top - 16 - th;
		if (trump == null) {
			batch.draw(assets.cardSlot, x, y, tw, th);
		} else {
			art.flip(trump, x, y, tw, th, 1);
		}
		paint.textIn(paint.ui(0.85f, Palette.MUTED), "Trump", x, y - 20, tw, 16);
	}

	private void drawTrick(Match m) {
		boolean finished = gathering();
		List<Card> cards = finished ? m.lastTrick() : m.currentTrick();
		List<Integer> seats = finished ? m.lastTrickSeats() : m.currentTrickSeats();
		boolean toWinner = finished && time >= trickDoneAt + TRICK_HOLD;
		float cw = cardW(TRICK), ch = cardH(TRICK), cx = w / 2, cy = h / 2 - TRICK_DROP;
		for (int i = 0; i < cards.size(); i++) {
			if (toWinner) {
				Vector2 pile = pile(m, m.lastTrickWinner());
				card(cards.get(i), pile.x, pile.y, cardW(PILE));
				continue;
			}
			int s = seats.get(i);
			float dx = s == 1 ? -cw * 0.95f : s == 3 ? cw * 0.95f : 0, dy = s == 0 ? -ch * 0.4f : s == 2 ? ch * 0.4f : 0;
			card(cards.get(i), cx + dx - cw / 2, cy + dy - ch / 2, cw);
		}
	}

	private Vector2 pile(Match m, int seat) {
		if (seat == Seating.YOU) {
			return spot.set(w / 2 - cardW(PILE) / 2, 30);
		}
		Vector2 at = handStart(seat, Math.max(1, m.hand(seat).size()), h - MenuChrome.HEADER_H);
		return spot.set(at.x + cardW(THEIRS) / 2 - cardW(PILE) / 2, at.y + cardH(THEIRS) / 2 - cardH(PILE) / 2);
	}

	private void drawYourHand(Match m) {
		List<Card> hand = m.hand(Seating.YOU);
		while (handRects.size() < hand.size()) {
			handRects.add(new Rectangle());
		}
		float cw = cardW(YOURS), ch = cardH(YOURS), gap = cw * 0.9f, x0 = w / 2 - (cw + gap * (hand.size() - 1)) / 2;
		List<Card> legal = play.yourTurn() && m.phase() == Match.Phase.PLAYING && !gathering() ? m.legalPlays(Seating.YOU) : null;
		for (int i = 0; i < hand.size(); i++) {
			float x = x0 + i * gap, y = 14 + (legal != null && legal.contains(hand.get(i)) ? LIFT : 0);
			handRects.get(i).set(x, y, cw, ch);
			card(hand.get(i), x, y, cw);
		}
		paint.ui(0.95f, Palette.CREAM).draw(batch, "You  " + seatLine(m, Seating.YOU), w / 2 - 2 * cw, 14 + ch + 30);
	}

	private void drawBids(Match m) {
		int n = m.handSize() + 1;
		while (bidRects.size() < n) {
			bidRects.add(new Rectangle());
		}
		float span = n * BID_SIZE + (n - 1) * BID_GAP, x = w / 2 - span / 2, y = h / 2 - BID_SIZE / 2;
		for (int b = 0; b < n; b++) {
			Rectangle r = bidRects.get(b).set(x + b * (BID_SIZE + BID_GAP), y, BID_SIZE, BID_SIZE);
			paint.frame(m.isLegalBid(b) ? assets.button : assets.buttonOff, r.x, r.y, r.width, r.height);
			paint.numeral(b, r.x + r.width / 2, r.y + r.height / 2, BID_SIZE * 0.7f, Color.WHITE);
		}
	}

	private void drawButtons() {
		float x = w - 20 - BUTTON_W / 2, y = 14;
		if (overAt >= 0) {
			boolean solved = play.solved();
			float a = BANNER_IN.progress(time - overAt);
			paint.centred(paint.serif(1.6f + 0.4f * (1 - a), solved ? Palette.GOLD_LIGHT : Palette.RED, a),
					solved ? "Shot made!" : "Not this time", w / 2, h / 2 - TRICK_DROP + 12);
			paint.button(nextButton, solved ? nextLabel() : "Try again", x, y + BUTTON_H + 10, BUTTON_W, BUTTON_H, assets.button);
		} else {
			nextButton.set(0, 0, 0, 0);
		}
		paint.button(restartButton, "Restart", x, y, BUTTON_W, BUTTON_H, assets.buttonAlt);
	}

	private TrickShot nextTrickShot() {
		List<TrickShot> all = TrickShots.of(play.shot().chapter);
		int at = all.indexOf(play.shot());
		return at + 1 < all.size() ? all.get(at + 1) : null;
	}

	private String nextLabel() {
		return nextTrickShot() != null ? "Next shot" : "Chapter";
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
		} else if (nextButton.contains(x, y)) {
			if (!play.solved()) {
				open(play.shot());
			} else if (nextTrickShot() != null) {
				open(nextTrickShot());
			} else {
				back();
			}
		} else if (restartButton.contains(x, y)) {
			play.restart();
			reset();
		} else if (play.yourTurn() && !gathering()) {
			tapPlay(x, y);
		}
	}

	private void tapPlay(float x, float y) {
		Match m = play.match();
		if (m.phase() == Match.Phase.BIDDING) {
			for (int b = 0; b <= m.handSize(); b++) {
				if (bidRects.get(b).contains(x, y) && m.isLegalBid(b)) {
					play.move(new Move.Bid(Seating.YOU, b));
					return;
				}
			}
			return;
		}
		List<Card> hand = m.hand(Seating.YOU);
		for (int i = hand.size() - 1; i >= 0; i--) {
			if (handRects.get(i).contains(x, y) && m.legalPlays(Seating.YOU).contains(hand.get(i))) {
				int before = m.currentTrick().size();
				play.move(new Move.Play(Seating.YOU, hand.get(i)));
				noteTrickDone(before);
				untilOther = OTHER_STEP;
				return;
			}
		}
	}

	@Override
	protected void back() {
		game.back();
	}

	@Override
	public String stateTag() {
		return "shot:" + play.shot().id;
	}
}
