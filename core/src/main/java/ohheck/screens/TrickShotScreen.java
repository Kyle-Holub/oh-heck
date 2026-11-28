package ohheck.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

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
import ohheck.table.BidPopup;
import ohheck.table.PlayerPanel;
import ohheck.table.SeatState;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * A trick shot: every hand face up around the table, the goal in the header. Its title and goal pop up on the felt
 * when it opens and fade, and the bids wait for them. Every card glides to where it belongs: from a hand into
 * the trick, and from a finished trick, after a moment, to its winner. The others reply one card
 * at a time; nothing can be tapped while a trick is being gathered.
 */
public class TrickShotScreen extends BaseScreen {

	private static final float YOURS = 1.0f, THEIRS = 0.72f, TRICK = 0.75f, PILE = 0.3f;
	private static final float OTHER_STEP = 0.55f, TRICK_HOLD = 0.8f, GATHER = 0.35f;
	private static final float TOP_HAND_Y = 320, SIDE_HAND_DROP = 20, TRICK_DROP = 24, LABEL_GAP = 16, LIFT = 10;
	private static final float BUTTON_W = 170, BUTTON_H = 40, PLACARD_H = 50, PLACARD_PAD = 9, TARGET_FADE = 0.55f;
	private static final float INTRO_HOLD = 2.2f, INTRO_W = 660, INTRO_H = 150;
	private static final Tween GLIDE = Tween.over(0.28f, Ease.OUT), BANNER_IN = Tween.over(0.4f, Ease.OUT),
			INTRO_IN = Tween.over(0.3f, Ease.OUT), INTRO_OUT = Tween.over(0.5f, Ease.IN);

	private final MenuChrome chrome = new MenuChrome(paint);
	private final PlayerPanel panel = new PlayerPanel(paint);
	private final IntPredicate legalBid = b -> this.play.match().isLegalBid(b);
	private final BidPopup bidPopup;
	private final SeatState placardState = new SeatState();
	private final CardArt art;
	private final Sparks sparks = new Sparks();
	private final Rectangle backButton = new Rectangle(), restartButton = new Rectangle();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>(), bidRects = new ArrayList<Rectangle>();
	private final float[] fromX = new float[Card.COUNT], fromY = new float[Card.COUNT], fromW = new float[Card.COUNT],
			toX = new float[Card.COUNT], toY = new float[Card.COUNT], toW = new float[Card.COUNT], movedAt = new float[Card.COUNT];
	private final boolean[] placed = new boolean[Card.COUNT], drawn = new boolean[Card.COUNT];
	private final Vector2 spot = new Vector2();
	private TrickShotPlay play;
	private float finalAt = -1;
	private static final float SLOW = 0.35f, SLOW_SPAN = 0.7f, SLOW_ZOOM = 0.07f, WOBBLE = 12, WOBBLE_LEN = 0.8f;
	private float time, untilOther, trickDoneAt = -99, overAt = -1, lastMotion = -99, introAt = -99;
	private String goal = "";
	private boolean recorded;

	public TrickShotScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
		bidPopup = new BidPopup(paint, art);
	}

	public void open(TrickShot s) {
		play = new TrickShotPlay(s);
		goal = s.goalText();
		reset();
	}

	private void reset() {
		untilOther = OTHER_STEP;
		trickDoneAt = -99;
		overAt = -1;
		finalAt = -1;
		recorded = false;
		introAt = time;
		for (int i = 0; i < Card.COUNT; i++) {
			placed[i] = false;
		}
	}

	private boolean introducing() {
		return time < introAt + INTRO_HOLD + INTRO_OUT.end();
	}

	private boolean gathering() {
		return time < trickDoneAt + TRICK_HOLD + GATHER;
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		if (play.over() && finalAt < 0) {
			finalAt = time;
		}
		if (slowing()) {
			dt *= SLOW;
		}
		shownFor += dt;
		time += dt;
		sparks.step(dt);
		if (gathering() || introducing()) {
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
				sparks.burst(w / 2, h / 2, 160);
			}
		}
		if (play.solved() && !recorded) {
			recorded = true;
			game.records().campaign().solve(play.shot().id);
			game.records().saveCampaign();
		}
	}

	private boolean slowing() {
		return finalAt >= 0 && play.solved() && time - finalAt < SLOW_SPAN;
	}

	private void noteTrickDone(int sizeBefore) {
		if (play.match().currentTrick().isEmpty() && sizeBefore > 0) {
			trickDoneAt = time;
		}
	}

	@Override
	public boolean animating() {
		return introducing() || gathering() || play.othersToMove() || GLIDE.active(time - lastMotion) || sparks.active()
				|| overAt >= 0 && time - overAt < WOBBLE_LEN;
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		float zoomIn = slowing() ? MathUtils.sin((time - finalAt) / SLOW_SPAN * MathUtils.PI) : 0;
		kickZoom = 1 - SLOW_ZOOM * zoomIn;
		kickY = -TRICK_DROP * zoomIn;
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		Match m = play.match();
		float top = h - MenuChrome.HEADER_H;
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
		if (m.phase() == Match.Phase.BIDDING && play.yourTurn() && !introducing()) {
			drawBids(m);
		}
		sparks.draw(batch, assets.glow);
		drawButtons();
		drawIntro();
		chrome.header(goal, backButton, w, h);
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
			return spot.set(w / 2 - handWidth(n, THEIRS) / 2, Math.min(TOP_HAND_Y, top - 116));
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
		if (seat == 2) {
			placard(m, seat, x + handWidth(hand.size(), THEIRS) + LABEL_GAP, y + cardH(THEIRS) / 2 + PLACARD_H / 2);
		} else {
			placard(m, seat, x, y - 6);
		}
	}

	private void placard(Match m, int seat, float x, float top) {
		float iw = PlayerPanel.iconWidth(m.handSize()), ih = iw * PlayerPanel.ICON_ASPECT;
		float pw = PLACARD_PAD * 2 + m.handSize() * (iw + PlayerPanel.ICON_GAP) - PlayerPanel.ICON_GAP, y = top - PLACARD_H;
		if (seat == play.shot().rival) {
			paint.fade(TARGET_FADE);
			paint.frame(assets.panelTurn, x - 4, y - 4, pw + 8, PLACARD_H + 8);
			paint.fade(1f);
		}
		paint.frame(assets.panel, x, y, pw, PLACARD_H);
		int seen = m.bidSeenBy(seat, Seating.YOU);
		if (seen != Match.NO_BID) {
			placardState.reset().bid(seen).won(m.tricksWon(seat)).cards(m.handSize());
			panel.drawTricks(placardState, x + PLACARD_PAD, y + (PLACARD_H - ih) / 2);
		}
	}

	private void drawTrump(Match m, float top) {
		Card trump = m.trumpCard();
		float tw = cardW(0.7f), th = cardH(0.7f), x = w - 20 - tw, y = top - 16 - th;
		if (trump == null) {
			batch.draw(assets.cardSlot, x, y, tw, th);
		} else {
			art.flip(trump, x, y, tw, th, 1);
		}
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
			float dx = s == 1 ? -cw * 1.1f : s == 3 ? cw * 1.1f : 0, dy = s == 0 ? -ch * 0.55f : s == 2 ? ch * 0.55f : 0;
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
		float right = x0 + cw + gap * Math.max(0, hand.size() - 1) + LABEL_GAP;
		placard(m, Seating.YOU, right, 14 + ch / 2 + PLACARD_H / 2);
	}

	private void drawBids(Match m) {
		bidPopup.draw(w / 2, h / 2 - TRICK_DROP, m.handSize(), legalBid, -1, 1f, bidRects);
	}

	private void drawIntro() {
		if (!introducing()) {
			return;
		}
		float t = time - introAt;
		float a = t < INTRO_HOLD ? INTRO_IN.progress(t) : 1 - INTRO_OUT.progress(t - INTRO_HOLD);
		float px = (w - INTRO_W) / 2, py = h / 2 - TRICK_DROP - INTRO_H / 2;
		paint.fade(a);
		paint.frame(assets.panel, px, py, INTRO_W, INTRO_H);
		paint.fade(1f);
		paint.centred(paint.serif(1.6f, Palette.GOLD, a), play.shot().title, w / 2, py + INTRO_H - 30);
		paint.centred(paint.serif(1.15f, Palette.CREAM, a), goal, w / 2, py + 56);
	}

	private void drawButtons() {
		float x = w - 20 - BUTTON_W / 2, y = 14;
		if (overAt >= 0) {
			boolean solved = play.solved();
			float a = BANNER_IN.progress(time - overAt), k = Math.min(1, (time - overAt) / WOBBLE_LEN);
			float wobble = solved ? 0 : MathUtils.sin((time - overAt) * 22) * WOBBLE * (1 - k);
			paint.centred(paint.serif(1.6f + 0.4f * (1 - a), solved ? Palette.GOLD_LIGHT : Palette.RED, a),
					solved ? "Shot made!" : "Not this time", w / 2 + wobble, h / 2 - TRICK_DROP + 12);
			paint.button(restartButton, solved ? nextLabel() : "Try again", x, y, BUTTON_W, BUTTON_H, assets.button);
		} else {
			paint.button(restartButton, "Restart", x, y, BUTTON_W, BUTTON_H, assets.buttonAlt);
		}
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
		if (introducing()) {
			introAt = Math.min(introAt, time - INTRO_HOLD);
		} else if (backButton.contains(x, y)) {
			back();
		} else if (restartButton.contains(x, y)) {
			if (overAt >= 0 && play.solved()) {
				if (nextTrickShot() != null) {
					open(nextTrickShot());
				} else {
					back();
				}
			} else {
				play.restart();
				reset();
			}
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
