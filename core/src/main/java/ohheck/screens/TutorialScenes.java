package ohheck.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import ohheck.fx.Glints;
import ohheck.platform.Settings;
import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;
import ohheck.table.BidPopup;
import ohheck.table.Counters;
import ohheck.table.PlayerPanel;
import ohheck.table.ScoreCard;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/* The tutorial's scenes, drawn from the scene's clock. */
final class TutorialScenes extends TutorialStage {

	private static final String BIDS_LINE = LEFT + " bid 1.  " + RIGHT + " bid 1.";
	private static final float ZERO_SCORED = 3.6f, CARD_AT = 7.4f;
	private static final String RESULTS_TITLE = Wording.handResults(1);
	private static final List<ScoreCard.Row> SCORE_ROWS = scoreRows();

	private static List<ScoreCard.Row> scoreRows() {
		RuleSet r = RuleSet.STANDARD;
		String[] names = { NAMES[0], LEFT, RIGHT };
		int[] bids = { 2, 0, 1 }, won = { 2, 0, 2 };
		List<ScoreCard.Row> rows = new ArrayList<ScoreCard.Row>();
		for (int i = 0; i < names.length; i++) {
			int points = r.score(bids[i], won[i]), place = 1;
			for (int j = 0; j < names.length; j++) {
				place += r.score(bids[j], won[j]) > points ? 1 : 0;
			}
			rows.add(new ScoreCard.Row(place, names[i], bids[i] == won[i], bids[i], won[i], points, points));
		}
		rows.sort((a, b) -> a.place() - b.place());
		return rows;
	}

	private static final String[] DEAL_LINES = { "", "Hand 1: 1 card each", "Hand 2: 2 cards each", "Hand 3: 3 cards each" };

	private final BidPopup bidPopup;
	private final ScoreCard scoreCard;

	TutorialScenes(Painter paint, Settings settings) {
		super(paint, settings);
		bidPopup = new BidPopup(paint, art);
		scoreCard = new ScoreCard(paint);
	}

	private static final Suit[] ACE_SUITS = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };

	void welcome() {
		art.art(assets.titleText, cx(), cy() + 70, 0.62f, at(0, 0.8f));
		float open = at(0.9f, 1.9f), sway = MathUtils.sin(clock.shown() * 1.1f) * 2f;
		float pivotX = cx(), pivotY = cy() - CH - 30;
		for (int i = 0; i < 4; i++) {
			float k = at(0.7f + i * 0.08f, 1.2f + i * 0.08f);
			if (k <= 0) {
				continue;
			}
			float rot = (i - 1.5f) * -21f * open + sway * open;
			float rx = MathUtils.sinDeg(-rot) * (CH + 10), ry = MathUtils.cosDeg(rot) * (CH + 10);
			float x = pivotX + rx - CW / 2, y = pivotY + ry - CH / 2 + (1 - k) * -40;
			Card c = card(Rank.ACE, ACE_SUITS[i]);
			batch.setColor(1, 1, 1, k);
			art.shadow(x, y, CW, CH, 6);
			batch.draw(assets.face(c), x, y, CW / 2, CH / 2, CW, CH, 1, 1, rot);
			batch.setColor(Color.WHITE);
			if (open >= 1) {
				Glints.draw(batch, assets.glow, c, x, y, CW, CH, CW, rot, clock.shown() + i * 0.7f, 0.8f);
			}
		}
		if (passed(2.0f) && settings.effects()) {
			sparks.burst(cx(), cy() - 20, 60);
		}
	}

	private static final Rank[] HIGH_TO_LOW = { Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN, Rank.NINE,
			Rank.EIGHT, Rank.SEVEN, Rank.SIX, Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO };

	void ranks() {
		float step = Math.min(64, (w - 120 - CW) / 12), x0 = cx() - (12 * step + CW) / 2, y = cy() - CH / 2;
		for (int i = 0; i < 13; i++) {
			float a = 0.1f + i * 0.07f;
			Card c = card(HIGH_TO_LOW[i], Suit.SPADES);
			float x = x0 + i * step;
			float shimmer = 1 - Math.min(1, Math.abs(t - (1.9f + i * 0.12f)) / 0.18f);
			glow(x, y, shimmer);
			flying(c, cx() - CW / 2, y - 60, x, y + shimmer * 8, a, a + 0.4f, 1, at(a, a + 0.1f));
		}
		label("HIGHEST", x0 + CW / 2, y + CH + 26, at(1.4f, 1.8f));
		label("LOWEST", x0 + 12 * step + CW / 2, y + CH + 26, at(1.4f, 1.8f));
	}

	void aTrick() {
		int[] order = { 1, 3, 0 };
		Card[] cards = { card(Rank.SEVEN, Suit.HEARTS), card(Rank.KING, Suit.HEARTS), card(Rank.NINE, Suit.HEARTS) };
		boolean taken = t >= 3.4f;
		seat(0, plain());
		seat(1, plain());
		seat(3, taken ? bidOf(1).won(1) : plain());
		float gone = at(2.8f, 3.4f), y = rowY(false);
		for (int i = 0; i < 3; i++) {
			float a = 0.3f + i * 0.5f;
			boolean king = i == 1;
			float lift = king ? at(2.0f, 2.4f) * 10 : 0;
			float x = MathUtils.lerp(rowX(i, 3), fromX(3), gone), cy = MathUtils.lerp(y, fromY(3), gone) + lift;
			if (t >= a + 0.45f) {
				if (king) {
					glow(x, cy, at(2.0f, 2.4f) * (1 - gone));
				}
				card(cards[i], x, cy, 2 + lift, 1, 1 - gone);
			} else {
				flying(cards[i], fromX(order[i]), fromY(order[i]), rowX(i, 3), y, a, a + 0.45f, 1, 1);
			}
		}
	}

	void followSuit() {
		Card led = card(Rank.TEN, Suit.HEARTS);
		Card[] hand = { card(Rank.FOUR, Suit.HEARTS), card(Rank.QUEEN, Suit.CLUBS), card(Rank.EIGHT, Suit.SPADES) };
		float y = rowY(true);
		flying(led, fromX(3), fromY(3), rowX(0, 2), y, 0.2f, 0.6f, 1, 1);
		seat(3, plain());
		float dim = at(0.9f, 1.3f);
		for (int i = 0; i < hand.length; i++) {
			float x = handX(i, 3), hy = handY();
			if (i == 0) {
				float lift = at(1.0f, 1.3f) * 12;
				if (t < 1.6f) {
					glow(x, hy + lift, at(1.0f, 1.3f));
					card(hand[i], x, hy + lift, 2 + lift, 1, 1);
				} else {
					flying(hand[i], x, hy + lift, rowX(1, 2), y, 1.6f, 2.2f, 1, 1);
				}
			} else {
				card(hand[i], x, hy, 2, 1 - 0.55f * dim, 1);
			}
		}
	}

	void cantFollow() {
		Card led = card(Rank.JACK, Suit.DIAMONDS);
		Card[] hand = { card(Rank.SIX, Suit.HEARTS), card(Rank.KING, Suit.CLUBS), card(Rank.FIVE, Suit.SPADES) };
		float y = rowY(true);
		seat(1, plain());
		seat(3, plain());
		flying(led, fromX(3), fromY(3), rowX(0, 3), y, 0.2f, 0.6f, 1, 1);
		float play = 1.6f;
		for (int i = 0; i < hand.length; i++) {
			float x = handX(i, 3), hy = handY();
			if (i == 1 && t >= play) {
				flying(hand[i], x, hy, rowX(1, 3), y, play, play + 0.5f, 1 - 0.4f * at(3.0f, 3.3f), 1);
			} else {
				glow(x, hy, at(0.8f, 1.2f) * (1 - at(1.4f, 1.6f)) * 0.7f);
				card(hand[i], x, hy, 2, 1, 1);
			}
		}
		flying(card(Rank.NINE, Suit.DIAMONDS), fromX(1), fromY(1), rowX(2, 3), y, 2.3f, 2.7f, 1, 1);
		if (t >= 3.0f) {
			glow(rowX(0, 3), y, at(3.0f, 3.3f));
			card(led, rowX(0, 3), y, 2, 1, 1);
		}
	}

	void trump() {
		Card trumpCard = card(Rank.FIVE, Suit.SPADES);
		float tx = cx() + CW * 2.2f, ty = rowY(false) + CH + 30;
		float p = at(0.3f, 0.9f);
		float lit = at(0.9f, 1.5f);
		glow(tx, ty, lit * 0.8f);
		art.shadow(tx, ty, CW, CH, 4);
		art.flip(trumpCard, tx, ty, CW, CH, p);
		if (p >= 1) {
			Glints.draw(batch, assets.glow, trumpCard, tx, ty, CW, CH, CW, 0, clock.shown(), 1f);
		}
		Card ace = card(Rank.ACE, Suit.DIAMONDS), two = card(Rank.TWO, Suit.SPADES);
		boolean won = t >= 4.2f;
		seat(0, won ? bidOf(1).won(1) : plain());
		seat(1, plain());
		float gone = at(3.6f, 4.2f), y = rowY(false);
		float ax = MathUtils.lerp(rowX(0, 2), fromX(0), gone), ay = MathUtils.lerp(y, fromY(0), gone);
		float bx = MathUtils.lerp(rowX(1, 2), fromX(0), gone), by = MathUtils.lerp(y, fromY(0), gone);
		if (t >= 1.8f) {
			card(ace, ax, ay, 2, 1, 1 - gone);
		} else {
			flying(ace, fromX(1), fromY(1), rowX(0, 2), y, 1.4f, 1.8f, 1, 1);
		}
		if (t >= 2.4f) {
			float lift = at(2.6f, 3.0f) * 10;
			glow(bx, by + lift, at(2.6f, 3.0f) * (1 - gone));
			card(two, bx, by + lift, 2 + lift, 1, 1 - gone);
			if (gone <= 0) {
				Glints.draw(batch, assets.glow, two, bx, by + lift, CW, CH, CW, 0, clock.shown(), 1f);
			}
		} else {
			flying(two, fromX(0), fromY(0), rowX(1, 2), y, 2.0f, 2.4f, 1, 1);
		}
	}

	private static final float DEAL_SCALE = 0.8f;

	void theDeal() {
		float[] starts = { 0.2f, 1.6f, 3.3f };
		float dw = CW * DEAL_SCALE, dh = CH * DEAL_SCALE, gap = dw + 12;
		float deckX = w - 60 - CW, deckY = cy() - CH / 2 - 20;
		art.stack(deckX + CW / 2, deckY + CH / 2, 12, CW, CH, 0);
		float[] rowYs = { seatTop(0) - PlayerPanel.H / 2 - dh / 2, seatTop(1) - PlayerPanel.H / 2 - dh / 2 };
		int dealer = Math.max(0, clock.count(starts) - 1), before = Math.max(0, dealer - 1);
		float move = dealer > 0 ? at(starts[dealer] - 0.1f, starts[dealer] + CHIP_MOVE) : 1;
		float chipX = cx() - 1.5f * gap - CHIP_BESIDE - MathUtils.sin(move * MathUtils.PI) * 40;
		float chipY = MathUtils.lerp(rowYs[before % 2], rowYs[dealer % 2], move) + dh / 2 - CHIP / 2;
		batch.draw(assets.dealerIcon, chipX, chipY, CHIP, CHIP);
		for (int round = 0; round < 3; round++) {
			int n = round + 1;
			float a = starts[round], end = round < 2 ? starts[round + 1] - 0.15f : 99f;
			if (t < a || t >= end) {
				continue;
			}
			BitmapFont f = paint.serif(1.6f, Palette.GOLD, at(a, a + 0.3f) * (1 - at(5.0f, 5.3f)));
			paint.centred(f, DEAL_LINES[n], cx(), h - 70);
			for (int k = 0; k < n * 2; k++) {
				int who = k % 2, j = k / 2;
				float ka = a + k * 0.12f;
				if (t < ka) {
					break;
				}
				float kk = at(ka, ka + 0.35f);
				float x0 = cx() - (n * gap - 12) / 2;
				float x = MathUtils.lerp(deckX, x0 + j * gap, kk), y = MathUtils.lerp(deckY, rowYs[who], kk);
				art.shadow(x, y, dw, dh, 4);
				batch.draw(assets.back(), x, y, dw, dh);
			}
		}
		BitmapFont f = paint.serif(1.6f, Palette.GOLD, at(5.2f, 5.6f));
		paint.centred(f, "\u2026 and so on, up to " + HANDS, cx(), h - 70);
	}

	private static final float CHIP = 30, CHIP_MOVE = 0.45f, CHIP_BESIDE = 50;

	private float popupOverHand() {
		return handY() + CH + 12 + BidPopup.H / 2;
	}

	private static final float HOOK_RISE = 30;

	private final List<Rectangle> bidButtons = new ArrayList<Rectangle>();
	private final IntPredicate anyBid = b -> true, notOne = b -> b != 1;

	void bidding() {
		Card[] hand = { card(Rank.ACE, Suit.SPADES), card(Rank.KING, Suit.HEARTS), card(Rank.FOUR, Suit.CLUBS) };
		int[] seats = { 0, 1, 3 }, bids = { 2, 1, 0 };
		float[] when = { 6.4f, 7.2f, 7.9f };
		for (int i = 0; i < 3; i++) {
			boolean bid = t >= when[i];
			seat(seats[i], (bid ? bidOf(bids[i]) : waiting()).turn(i == 0 && !bid).cards(3));
		}
		float counting = 1 - at(4.0f, 4.3f);
		for (int i = 0; i < 3; i++) {
			float x = handX(i, 3), y = handY();
			float g = i < 2 ? at(0.5f + i * 1.0f, 0.9f + i * 1.0f) * counting : 0;
			glow(x, y, g);
			card(hand[i], x, y, 2, i == 2 ? 1 - 0.4f * at(0.5f, 0.9f) * counting : 1, 1);
			if (i < 2 && g > 0) {
				paint.numeral(i + 1, x + CW / 2, y + CH + 22, 30, tint.set(1, 1, 1, g));
			}
		}
		BitmapFont f = paint.ui(1.05f, Palette.GOLD_LIGHT, at(1.8f, 2.2f));
		f.draw(batch, "The Ace of spades and a King:\ntwo likely winners", handX(2, 3) + CW + 24, handY() + CH * 0.65f);
		bidPopup.draw(cx(), popupOverHand(), 3, anyBid, t >= 5.2f ? 2 : -1, at(4.4f, 4.8f) * (1 - at(6.2f, 6.4f)), bidButtons);
	}

	private static final Card[] ZERO_HAND = { card(Rank.THREE, Suit.CLUBS), card(Rank.TWO, Suit.DIAMONDS) };
	private static final Card[][] ZERO_TRICKS = {
			{ card(Rank.KING, Suit.CLUBS), ZERO_HAND[0], card(Rank.NINE, Suit.CLUBS) },
			{ card(Rank.ACE, Suit.DIAMONDS), ZERO_HAND[1], card(Rank.EIGHT, Suit.DIAMONDS) } };
	private static final int[] PLAY_ORDER = { 3, 0, 1 }, SLOTS = { 2, 1, 0 };
	private static final int WINNER = 3;
	private static final float[] ZERO_TRICK_STARTS = { 3.4f, 5.6f };
	private static final float CARD_GAP = 0.4f, GATHER_FROM = 1.4f, GATHER_TO = 2.0f, HAND_END_AT = 7.8f, ZERO_BID_AT = 2.3f;
	private static final Card[] SET_HAND = { card(Rank.QUEEN, Suit.HEARTS), card(Rank.JACK, Suit.CLUBS) };
	private static final Card[][] SET_TRICKS = {
			{ card(Rank.KING, Suit.HEARTS), SET_HAND[0], card(Rank.FOUR, Suit.HEARTS) },
			{ card(Rank.ACE, Suit.CLUBS), SET_HAND[1], card(Rank.FIVE, Suit.CLUBS) } };
	private static final float[] SET_TRICK_STARTS = { 1.0f, 3.2f };
	private static final float SET_AT = 5.4f;

	void biddingZero() {
		int taken = 0;
		for (float start : ZERO_TRICK_STARTS) {
			taken += t >= start + GATHER_TO ? 1 : 0;
		}
		seat(0, (t >= ZERO_BID_AT ? bidOf(0) : waiting().turn(true)).cards(2));
		seat(1, bidOf(1).cards(2));
		seat(3, bidOf(2).won(taken).cards(2));
		playTricks(ZERO_HAND, ZERO_TRICKS, ZERO_TRICK_STARTS);
		bidPopup.draw(cx(), popupOverHand(), 2, anyBid, t >= 0.9f ? 0 : -1, at(0.2f, 0.5f) * (1 - at(ZERO_BID_AT, ZERO_BID_AT + 0.3f)),
				bidButtons);
		float shown = at(HAND_END_AT, HAND_END_AT + 0.3f);
		if (passed(HAND_END_AT) && settings.effects()) {
			sparks.burst(cx(), cy(), 110);
		}
		art.art(assets.bidTricksText, cx(), cy(), 0.8f, shown);
	}

	void gettingSet() {
		int taken = 0;
		for (float start : SET_TRICK_STARTS) {
			taken += t >= start + GATHER_TO ? 1 : 0;
		}
		seat(0, bidOf(1).cards(2));
		seat(1, bidOf(0).cards(2));
		seat(3, bidOf(2).won(taken).cards(2));
		playTricks(SET_HAND, SET_TRICKS, SET_TRICK_STARTS);
		art.art(assets.setText, cx(), cy(), 0.8f, at(SET_AT, SET_AT + 0.3f));
	}

	private void playTricks(Card[] hand, Card[][] tricks, float[] starts) {
		for (int k = 0; k < hand.length; k++) {
			if (t < starts[k] + CARD_GAP) {
				card(hand[k], handX(k, hand.length), handY(), 2, 1, 1);
			}
		}
		for (int k = 0; k < tricks.length; k++) {
			playTrick(k, hand.length, tricks[k], starts[k]);
		}
	}

	private void playTrick(int k, int handSize, Card[] trick, float start) {
		float gone = at(start + GATHER_FROM, start + GATHER_TO), y = rowY(true);
		if (gone >= 1) {
			return;
		}
		for (int i = 0; i < PLAY_ORDER.length; i++) {
			int seat = PLAY_ORDER[i];
			float a = start + i * CARD_GAP;
			float x0 = seat == 0 ? handX(k, handSize) : fromX(seat), y0 = seat == 0 ? handY() : fromY(seat);
			if (t >= a + CARD_GAP) {
				float x = MathUtils.lerp(rowX(SLOTS[i], 3), fromX(WINNER), gone), cy = MathUtils.lerp(y, fromY(WINNER), gone);
				card(trick[i], x, cy, 2, 1, 1 - gone);
			} else {
				flying(trick[i], x0, y0, rowX(SLOTS[i], 3), y, a, a + CARD_GAP, 1, 1);
			}
		}
	}


	void dealersHook() {
		seat(1, (t >= 0.3f ? bidOf(1) : waiting()).cards(3));
		seat(3, (t >= 0.8f ? bidOf(1) : waiting()).cards(3));
		seat(0, waiting().turn(true).dealer(true).cards(3));
		float alpha = at(1.6f, 1.9f), below = cy() + HOOK_RISE - BidPopup.H / 2;
		bidPopup.draw(cx(), cy() + HOOK_RISE, 3, notOne, -1, alpha, bidButtons);
		BitmapFont f = paint.ui(1.15f, Palette.CREAM, at(2.4f, 2.8f));
		paint.centred(f, BIDS_LINE, cx(), below - 16);
		f.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, at(3.2f, 3.6f));
		paint.centred(f, "3 tricks, and 2 bid already, so you can't bid 1.", cx(), below - 44);
	}

	void counters() {
		float scale = 1.6f, left = cx() - 250, top = h - 80;
		int done = clock.count(0.5f, 1.0f, 1.5f);
		counterArt.tally(left, top, RuleSet.STANDARD.numberOfHands(), done, Counters.pulse(clock.shown()), scale);
		BitmapFont f = paint.ui(1.05f, Palette.CREAM, at(1.6f, 2.0f));
		f.draw(batch, "Each tally represents the round number", left + 150 * scale, top - 12);
		int[] totals = { 0, 1, 3, 4, 5 };
		float[] when = { 0, 3.4f, 4.2f, 5.0f, 5.8f };
		int total = totals[clock.count(when) - 1];
		float ty = top - Counters.TALLY_H * scale - Counters.TRACKER_BELOW_TALLY * scale;
		float trackerAlpha = at(2.6f, 3.0f);
		if (trackerAlpha > 0) {
			batch.setColor(1, 1, 1, trackerAlpha);
			float th = counterArt.tracker(left - Counters.TALLY_W * scale / 2, ty, 400, 4, total, scale);
			batch.setColor(Color.WHITE);
			f.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, at(3.0f, 3.4f));
			f.draw(batch, "Each card represents a bid", left + 150 * scale, ty - 8);
			f.setColor(Palette.RED.r, Palette.RED.g, Palette.RED.b, at(6.0f, 6.4f));
			f.draw(batch, "A red card indicates there are more bids than tricks", left + 150 * scale, ty - th - 6);
		}
	}

	void scoring() {
		int mine = clock.count(0.8f, 1.6f), his = clock.count(4.8f, 5.6f);
		seat(0, bidOf(2).won(mine).score(t >= 2.2f ? RuleSet.STANDARD.score(2, 2) : 0).cards(3));
		seat(3, bidOf(1).won(his).score(t >= 6.4f ? RuleSet.STANDARD.score(1, 2) : 0).cards(3));
		seat(1, bidOf(0).score(t >= ZERO_SCORED ? RuleSet.STANDARD.score(0, 0) : 0).cards(3));
		float rx = seatX(1) + PlayerPanel.W / 2;
		if (passed(ZERO_SCORED)) {
			sparks.burst(rx, seatTop(1) - PlayerPanel.H / 2, 70);
		}
		floating(Numbers.signed(RuleSet.STANDARD.score(0, 0)), rx, seatTop(1) + 10, ZERO_SCORED, Palette.GOLD_LIGHT);
		float yx = seatX(0) + PlayerPanel.W / 2, yy = seatTop(0) + 10;
		if (passed(2.2f)) {
			sparks.burst(yx, yy - PlayerPanel.H / 2, 70);
		}
		floating(Numbers.signed(RuleSet.STANDARD.score(2, 2)), yx, yy, 2.2f, Palette.GOLD_LIGHT);
		float hx = seatX(3) + PlayerPanel.W / 2;
		floating(Numbers.text(RuleSet.STANDARD.score(1, 2)), hx, seatTop(3) + 10, 6.4f, Palette.RED);
		scoreCard.draw(cx(), cy(), RESULTS_TITLE, SCORE_ROWS, at(CARD_AT, CARD_AT + 0.4f));
	}

	private static final Suit[] LINE_SUITS = { Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS };
	private static final Card[] LINE = new Card[Card.COUNT];
	static {
		int i = 0;
		for (Suit suit : LINE_SUITS) {
			for (int r = HIGH_TO_LOW.length - 1; r >= 0; r--) {
				LINE[i++] = card(HIGH_TO_LOW[r], suit);
			}
		}
	}
	private static final float LINE_SCALE = 0.75f, LINE_DEAL = 0.022f, LINE_FLIGHT = 0.35f, FLIP_FROM = 1.6f,
			FLIP_STEP = 0.02f, FLIP_TIME = 0.25f, WAVE_FROM = 3.0f, WAVE_HEIGHT = 14, WAVE_SPEED = 3.2f, WAVE_LENGTH = 0.28f;

	void ready() {
		art.art(assets.titleText, cx(), cy() + 70, 0.62f, 0.25f + 0.75f * at(0, 1.2f));
		float cw = CW * LINE_SCALE, ch = CH * LINE_SCALE;
		float step = (w - 80 - cw) / (LINE.length - 1), x0 = (w - step * (LINE.length - 1) - cw) / 2;
		float baseY = cy() - ch + 4, wave = at(WAVE_FROM, WAVE_FROM + 1f);
		for (int i = 0; i < LINE.length; i++) {
			float arrive = i * LINE_DEAL, k = at(arrive, arrive + LINE_FLIGHT);
			if (t < arrive) {
				break;
			}
			float x = MathUtils.lerp(-cw - 20, x0 + i * step, k);
			float y = baseY - (1 - k) * 60 + wave * WAVE_HEIGHT * MathUtils.sin(clock.shown() * WAVE_SPEED - i * WAVE_LENGTH);
			float flip = at(FLIP_FROM + i * FLIP_STEP, FLIP_FROM + i * FLIP_STEP + FLIP_TIME);
			art.shadow(x, y, cw, ch, 3);
			art.flip(LINE[i], x, y, cw, ch, flip);
		}
	}
}
