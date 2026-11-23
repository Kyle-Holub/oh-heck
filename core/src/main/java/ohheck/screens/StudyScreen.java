package ohheck.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.campaign.Campaign;
import ohheck.campaign.Studies;
import ohheck.campaign.Study;
import ohheck.campaign.StudyPlay;
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
 * A study: every hand face up around the table, the goal above, your undos as three small cards
 * that turn over as they are spent. The others reply one card at a time, and a finished trick
 * stays on the table a moment before it is gathered.
 */
public class StudyScreen extends BaseScreen {

	private static final float YOURS = 1.0f, THEIRS = 0.72f, TRICK = 0.75f, OTHER_STEP = 0.55f, TRICK_HOLD = 1.0f;
	private static final float TOP_HAND_Y = 300, SIDE_HAND_DROP = 20, TRICK_DROP = 24, LABEL_GAP = 16;
	private static final float BUTTON_W = 120, BUTTON_H = 40, UNDO_CARD = 0.34f, BID_SIZE = 46, BID_GAP = 10;

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final Rectangle backButton = new Rectangle(), undoButton = new Rectangle(), restartButton = new Rectangle(),
			nextButton = new Rectangle();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>(), bidRects = new ArrayList<Rectangle>();
	private StudyPlay play;
	private float untilOther, holding;
	private boolean recorded;

	public StudyScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
	}

	public void open(Study s) {
		play = new StudyPlay(s);
		reset();
	}

	private void reset() {
		untilOther = OTHER_STEP;
		holding = 0;
		recorded = false;
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		shownFor += dt;
		if (holding > 0) {
			holding -= dt;
			return;
		}
		if (play.othersToMove()) {
			untilOther -= dt;
			if (untilOther <= 0) {
				untilOther = OTHER_STEP;
				int before = play.match().currentTrick().size();
				play.stepOthers();
				holdIfTrickDone(before);
			}
		}
		if (play.solved() && !recorded) {
			recorded = true;
			Campaign c = game.records().campaign();
			c.solve(play.study().id);
			game.records().saveCampaign();
		}
	}

	private void holdIfTrickDone(int sizeBefore) {
		if (play.match().currentTrick().isEmpty() && sizeBefore > 0) {
			holding = TRICK_HOLD;
		}
	}

	@Override
	public boolean animating() {
		return holding > 0 || play.othersToMove();
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		Match m = play.match();
		float top = h - MenuChrome.HEADER_H;
		paint.centred(paint.serif(1.2f, Palette.GOLD_LIGHT), play.study().goalText(name(play.study().rival)), w / 2,
				top - 18);
		drawUndos(top);
		drawSeat(m, 1, 20, h / 2 - SIDE_HAND_DROP);
		drawSeat(m, 2, w / 2 - handWidth(m.hand(2).size(), THEIRS) / 2, Math.min(TOP_HAND_Y, top - 136));
		drawSeat(m, 3, w - 20 - handWidth(Math.max(1, m.hand(3).size()), THEIRS), h / 2 - SIDE_HAND_DROP);
		drawTrump(m, top);
		drawTrick(m);
		drawYourHand(m);
		if (m.phase() == Match.Phase.BIDDING && play.yourTurn()) {
			drawBids(m);
		}
		drawButtons();
		chrome.header(play.study().title, backButton, w, h);
		batch.end();
	}

	private String name(int seat) {
		return seat == Seating.YOU ? "You" : seat < 0 ? "" : play.study().chapter.rivals.at(seat).name;
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

	private void drawSeat(Match m, int seat, float x, float y) {
		List<Card> hand = m.hand(seat);
		for (int i = 0; i < hand.size(); i++) {
			art.flip(hand.get(i), x + i * cardW(THEIRS) * 0.62f, y, cardW(THEIRS), cardH(THEIRS), 1);
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
		List<Card> cards = holding > 0 ? m.lastTrick() : m.currentTrick();
		List<Integer> seats = holding > 0 ? m.lastTrickSeats() : m.currentTrickSeats();
		float cw = cardW(TRICK), ch = cardH(TRICK), cx = w / 2, cy = h / 2 - TRICK_DROP;
		for (int i = 0; i < cards.size(); i++) {
			int s = seats.get(i);
			float dx = s == 1 ? -cw * 0.95f : s == 3 ? cw * 0.95f : 0, dy = s == 0 ? -ch * 0.4f : s == 2 ? ch * 0.4f : 0;
			art.shadow(cx + dx - cw / 2, cy + dy - ch / 2, cw, ch, 3);
			art.flip(cards.get(i), cx + dx - cw / 2, cy + dy - ch / 2, cw, ch, 1);
		}
	}

	private void drawYourHand(Match m) {
		List<Card> hand = m.hand(Seating.YOU);
		while (handRects.size() < hand.size()) {
			handRects.add(new Rectangle());
		}
		float cw = cardW(YOURS), ch = cardH(YOURS), gap = cw * 0.9f, x0 = w / 2 - (cw + gap * (hand.size() - 1)) / 2;
		List<Card> legal = play.yourTurn() && m.phase() == Match.Phase.PLAYING ? m.legalPlays(Seating.YOU) : null;
		for (int i = 0; i < hand.size(); i++) {
			float x = x0 + i * gap, y = 14 + (legal != null && legal.contains(hand.get(i)) ? 10 : 0);
			handRects.get(i).set(x, y, cw, ch);
			art.flip(hand.get(i), x, y, cw, ch, 1);
		}
		paint.ui(0.95f, Palette.CREAM).draw(batch, "You  " + seatLine(m, Seating.YOU), x0, 14 + ch + 30);
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

	private void drawUndos(float top) {
		float cw = cardW(UNDO_CARD), ch = cardH(UNDO_CARD), x = 20, y = top - 16 - ch;
		for (int i = 0; i < StudyPlay.UNDOS; i++) {
			float cx = x + i * (cw + 6);
			if (i < play.undosLeft()) {
				batch.draw(assets.cardBlank, cx, y, cw, ch);
			} else {
				batch.draw(assets.back(), cx, y, cw, ch);
			}
		}
		paint.ui(0.85f, Palette.MUTED).draw(batch, "Undos", x, y - 6);
	}

	private void drawButtons() {
		float x = w - 20 - BUTTON_W / 2, y = 14;
		if (play.over() && holding <= 0) {
			boolean solved = play.solved();
			paint.centred(paint.serif(1.6f, solved ? Palette.GOLD_LIGHT : Palette.RED), solved ? "Solved!" : "Not this time",
					w / 2, h / 2 - TRICK_DROP + 12);
			paint.button(nextButton, solved ? nextLabel() : "Try again", x, y + BUTTON_H + 10, BUTTON_W, BUTTON_H, assets.button);
		} else {
			nextButton.set(0, 0, 0, 0);
		}
		paint.button(undoButton, "Undo", x, y, BUTTON_W, BUTTON_H, play.canUndo() ? assets.buttonAlt : assets.buttonOff);
		paint.button(restartButton, "Restart", x - BUTTON_W - 10, y, BUTTON_W, BUTTON_H, assets.buttonAlt);
	}

	private Study nextStudy() {
		List<Study> all = Studies.of(play.study().chapter);
		int at = all.indexOf(play.study());
		return at + 1 < all.size() ? all.get(at + 1) : null;
	}

	private String nextLabel() {
		return nextStudy() != null ? "Next study" : "Chapter";
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
		} else if (nextButton.contains(x, y)) {
			if (!play.solved()) {
				open(play.study());
			} else if (nextStudy() != null) {
				open(nextStudy());
			} else {
				back();
			}
		} else if (undoButton.contains(x, y) && play.canUndo()) {
			play.undo();
			reset();
		} else if (restartButton.contains(x, y)) {
			play.restart();
			reset();
		} else if (play.yourTurn() && holding <= 0) {
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
				holdIfTrickDone(before);
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
		return "study:" + play.study().id;
	}
}
