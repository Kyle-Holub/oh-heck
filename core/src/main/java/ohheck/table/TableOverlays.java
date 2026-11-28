package ohheck.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.PracticeCoach;
import ohheck.campaign.PracticeRound;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Suit;
import ohheck.ui.CardArt;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/* Panels and buttons drawn over the table. */
final class TableOverlays {

	private static final float HINT_TIME = 2.2f;

	private static final float MENU_BUTTON_W = 80, MENU_BUTTON_H = 38;
	private static final float PAUSE_BUTTON_W = 260, PAUSE_BUTTON_H = 56, PAUSE_GAP = 14;
	private static final float GAME_OVER_W = 420, GAME_OVER_H = 306, GAME_OVER_ROW = 27;
	private static final float END_BUTTON_W = 170, END_BUTTON_H = 42;
	private static final float HINT_H = 40, HINT_PAD = 16;

	private static final float PAUSE_LIFT = 20, PAUSED_TITLE_RISE = 36;
	private static final float MENU_BUTTON_Y = 10;
	private static final float HAND_END_FADE = 0.3f, HAND_END_SCALE = 0.95f;
	private static final Tween HAND_END_IN = Tween.over(HAND_END_FADE, Ease.LINEAR);
	private static final float LABEL_BIG = 2.6f, LABEL_SMALL = 1.6f, LABEL_OPPONENT = 1.9f, LABEL_BESIDE = 16,
			LABEL_SHADOW = 2;
	private static final float TRICKS_RISE = 12, TRICKS_INSET = 18, TAP_NOTE_GAP = 16;
	private static final float GAME_OVER_LIFT = 6, GAME_OVER_ART_DROP = 50, GAME_OVER_ART_SCALE = 0.66f,
			GAME_OVER_RESULT_DROP = 94, GAME_OVER_ROWS_DROP = 130, GAME_OVER_PLACE_X = 60, GAME_OVER_NAME_X = 120,
			GAME_OVER_SCORE_FROM_RIGHT = 140, NEW_BEST_FROM_RIGHT = 128, END_BUTTONS_RISE = 16, END_BUTTONS_GAP = 16;

	private final OverlayTargets targets;

	private final Painter paint;
	private final TableScene scene;
	private final TableLayout layout;
	private final CardArt art;
	private final BidPopup bidPopup;
	private final ScoreCard scoreCard;
	private final TrickReview review;
	private final IntPredicate legalBid = this::bidOk;
	private String hint = "";
	private float hintTime;

	private Match match;
	private final List<Integer> ranked = new ArrayList<Integer>();
	private final Comparator<Integer> byScore = new Comparator<Integer>() {
		@Override
		public int compare(Integer a, Integer b) {
			return match.score(b) - match.score(a);
		}
	};

	TableOverlays(Painter paint, TableScene scene, TableLayout layout, CardArt art, OverlayTargets targets) {
		this.targets = targets;
		this.paint = paint;
		this.scene = scene;
		this.layout = layout;
		this.art = art;
		this.bidPopup = new BidPopup(paint, art);
		this.scoreCard = new ScoreCard(paint);
		this.review = new TrickReview(paint, art, scene, layout, targets);
	}

	void setMatch(Match match) {
		this.match = match;
	}

	void hint(String s) {
		hint = s;
		hintTime = HINT_TIME;
	}

	void update(float dt) {
		hintTime -= dt;
		bidPopup.step(dt);
	}

	void drawHint() {
		if (hintTime <= 0) {
			return;
		}
		BitmapFont f = paint.ui(1.15f);
		paint.glyphs.setText(f, hint);
		float pw = paint.glyphs.width + 2 * HINT_PAD, ph = HINT_H;
		float px = (layout.w - pw) / 2, py = layout.hintY();
		paint.frame(paint.assets.panel, px, py, pw, ph);
		f.setColor(Palette.GOLD);
		paint.textIn(f, hint, px, py, pw, ph);
	}

	void drawMenuButton() {
		Rectangle menuButton = targets.button(OverlayTargets.Button.MENU).set(layout.w - TableLayout.EDGE - MENU_BUTTON_W, MENU_BUTTON_Y, MENU_BUTTON_W, MENU_BUTTON_H);
		paint.frame(paint.assets.buttonAlt, menuButton.x, menuButton.y, menuButton.width, menuButton.height);
		BitmapFont f = paint.ui(1.2f, Palette.CREAM);
		paint.textIn(f, "Menu", menuButton.x, menuButton.y, menuButton.width, menuButton.height);
		Rectangle win = targets.button(OverlayTargets.Button.FORCE_WIN);
		if (scene.inCampaign()) {
			win.set(menuButton.x - 10 - MENU_BUTTON_W, MENU_BUTTON_Y, MENU_BUTTON_W, MENU_BUTTON_H);
			paint.frame(paint.assets.button, win.x, win.y, win.width, win.height);
			paint.textIn(f, "Win", win.x, win.y, win.width, win.height);
		} else {
			win.set(0, 0, 0, 0);
		}
	}

	void drawMenu() {
		float w = layout.w, h = layout.h;
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		float bw = PAUSE_BUTTON_W, bh = PAUSE_BUTTON_H, gap = PAUSE_GAP;
		MenuItem[] items = MenuItem.ALL;
		float total = items.length * bh + (items.length - 1) * gap;
		float top = h / 2 + total / 2 + PAUSE_LIFT;
		BitmapFont title = paint.ui(1.5f, Palette.GOLD);
		paint.text(title, "Paused", w / 2, top + PAUSED_TITLE_RISE, true);
		BitmapFont f = paint.ui(1.4f);
		for (int i = 0; i < items.length; i++) {
			Rectangle r = targets.menuItem(items[i]).set((w - bw) / 2, top - (i + 1) * bh - i * gap, bw, bh);
			paint.frame(i == 0 ? paint.assets.button : paint.assets.buttonAlt, r.x, r.y, r.width, r.height);
			f.setColor(Palette.CREAM);
			paint.textIn(f, items[i].label, r.x, r.y, r.width, r.height);
		}
	}

	private boolean bidOk(int b) {
		return match.isLegalBid(b) && scene.bidAllowed(b);
	}

	void refuseBid(int b) {
		bidPopup.refuse(b);
	}

	boolean refusing() {
		return bidPopup.refusing();
	}

	private static final float PICK_W = 470, PICK_H = 190, PICK_BUTTON = 82, PICK_GAP = 16, PICK_EMBLEM = 0.62f;
	static final String PICK_TITLE = "Pick trump", SUDDEN_DEATH = "Sudden death";

	void drawPickTrump() {
		float cx = layout.w / 2, py = layout.centreY() - PICK_H / 2;
		paint.frame(paint.assets.panel, cx - PICK_W / 2, py, PICK_W, PICK_H);
		paint.text(paint.serif(1.3f, Palette.GOLD_LIGHT), PICK_TITLE, cx, py + PICK_H - 34, true);
		float rowW = 4 * PICK_BUTTON + 3 * PICK_GAP, bx = cx - rowW / 2, by = py + 22;
		for (Suit s : Suit.values()) {
			Rectangle r = targets.suit(s).set(bx + s.ordinal() * (PICK_BUTTON + PICK_GAP), by, PICK_BUTTON, PICK_BUTTON);
			paint.frame(paint.assets.button, r.x, r.y, r.width, r.height);
			paint.pressable(r);
			float e = PICK_BUTTON * PICK_EMBLEM;
			paint.batch.draw(paint.assets.emblem(s), r.x + (r.width - e) / 2, r.y + (r.height - e) / 2, e, e);
		}
	}

	private static final float WIDOW_W = 470, WIDOW_H = 236, WIDOW_CARD = 0.62f, WIDOW_GAP = 24, WIDOW_RAISE = 12;
	static final String WIDOW_TITLE = "The widow", WIDOW_HELP = "Tap one, then a card of yours to swap", KEEP = "Keep my hand";

	void drawWidow() {
		float cx = layout.w / 2, py = layout.centreY() - WIDOW_H / 2 + 10;
		paint.frame(paint.assets.panel, cx - WIDOW_W / 2, py, WIDOW_W, WIDOW_H);
		paint.text(paint.serif(1.3f, Palette.GOLD_LIGHT), WIDOW_TITLE, cx, py + WIDOW_H - 30, true);
		float cw = TableLayout.handCardW() * WIDOW_CARD, ch = TableLayout.handCardH() * WIDOW_CARD, top = py + WIDOW_H - 60;
		for (int i = 0; i < 2; i++) {
			Card c = match.widow().get(i);
			boolean chosen = targets.widowChoice() == i;
			float x = cx + (i == 0 ? -WIDOW_GAP / 2 - cw : WIDOW_GAP / 2), y = top - ch + (chosen ? WIDOW_RAISE : 0);
			Rectangle r = targets.widowCard(i).set(x, y, cw, ch);
			if (chosen) {
				paint.frame(paint.assets.panelTurn, r.x - 5, r.y - 5, r.width + 10, r.height + 10);
			}
			art.shadow(x, y, cw, ch, 3);
			art.flip(c, x, y, cw, ch, 1);
			paint.pressable(r);
		}
		paint.text(paint.ui(0.85f, Palette.CREAM), WIDOW_HELP, cx, top - ch - 14, true);
		paint.button(targets.button(OverlayTargets.Button.KEEP_HAND), KEEP, cx, py + 14, 200, 40, paint.assets.buttonAlt);
	}

	void drawBidPanel() {
		bidPopup.draw(layout.w / 2, layout.centreY(), match.handSize(), legalBid, -1, 1f, targets.bids());
	}

	static final float VERSUS_FROM = 0.35f, VERSUS_LENGTH = 3.8f, VERSUS_SLAM = 1.2f;
	private static final float VERSUS_TILT = 12, VERSUS_HABIT_W = 380, SEAL_SIZE = 170, RISERS = 44;
	private static final Tween PANELS_IN = Tween.over(0.55f, Ease.OUT).after(VERSUS_FROM),
			STAMP = Tween.over(0.22f, Ease.OUT).after(VERSUS_SLAM), PARTING = Tween.over(0.75f, Ease.IN).after(VERSUS_LENGTH - 0.75f);
	private static final Color YOUR_SIDE = new Color(0.17f, 0.1f, 0.06f, 1f);
	private final Color theme = new Color(), themeDeep = new Color(), themeSide = new Color();

	void drawVersus(float since) {
		float w = layout.w, h = layout.h, mid = h / 2, in = PANELS_IN.progress(since), part = PARTING.progress(since);
		float shown = 1 - part;
		theme.set(Palette.SPARKLE);
		themeDeep.set(theme).mul(0.14f, 0.14f, 0.14f, 1);
		themeSide.set(theme).mul(0.34f, 0.34f, 0.34f, 1);
		paint.rect(0, 0, w, h, flashTint.set(themeDeep).mul(1, 1, 1, shown));
		paint.glow(w / 2 - w * 0.6f, mid - h * 0.7f, w * 1.2f, h * 1.4f, theme, 0.22f * shown);
		risingSparkles(since, shown);
		float away = (1 - in) * (w + 300) + part * (w + 300);
		side(YOUR_SIDE, -away, mid, false, 1 - part * 0.5f);
		side(themeSide, away, mid, true, 1 - part * 0.5f);
		gilding(mid, Math.min(in, 1 - part));
		float names = Math.min(in, 1 - PARTING.progress(since + 0.3f));
		paint.text(paint.serif(2.6f, Palette.GOLD_LIGHT, names), scene.name(1), w * 0.7f + away, mid + h * 0.3f, true);
		paint.ui(1.05f, Palette.CREAM, names).draw(paint.batch, scene.habit(1), w * 0.7f - VERSUS_HABIT_W / 2 + away, mid + h * 0.3f - 52,
				VERSUS_HABIT_W, Align.center, true);
		paint.text(paint.serif(2.6f, Palette.GOLD_LIGHT, names), scene.name(Seating.YOU), w * 0.3f - away, mid - h * 0.2f, true);
		paint.text(paint.ui(1.05f, Palette.CREAM, names), CHALLENGER, w * 0.3f - away, mid - h * 0.2f - 52, true);
		if (since >= VERSUS_SLAM) {
			float stamp = STAMP.progress(since), size = SEAL_SIZE * (1.9f - 0.9f * stamp) * (1 + part * 0.6f), a = Math.min(1, stamp * 3) * (1 - part);
			paint.glow(w / 2 - size * 1.2f, mid - size * 1.2f, size * 2.4f, size * 2.4f, theme, 0.45f * a);
			paint.batch.setColor(Math.min(1, theme.r * 1.15f), Math.min(1, theme.g * 1.15f), Math.min(1, theme.b * 1.15f), a);
			paint.batch.draw(paint.assets.sealLocked, w / 2 - size / 2, mid - size / 2, size, size);
			paint.batch.setColor(Color.WHITE);
			float letters = 2.4f * size / SEAL_SIZE;
			paint.text(paint.serif(letters, VS_SHADOW, a), VS, w / 2 + 2, mid + 22 * size / SEAL_SIZE - 2, true);
			paint.text(paint.serif(letters, Palette.GOLD_LIGHT, a), VS, w / 2, mid + 22 * size / SEAL_SIZE, true);
		}
	}

	private final Color flashTint = new Color();
	private final float[] riserColour = new float[3];

	private void risingSparkles(float since, float alpha) {
		if (alpha <= 0) {
			return;
		}
		paint.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < RISERS; i++) {
			float speed = 30 + (i * 37) % 50, x = (i * 173.7f) % layout.w, y = (since * speed + i * 97.3f) % (layout.h + 40) - 20;
			float dot = 5 + (i % 4) * 3, twinkle = 0.5f + 0.5f * MathUtils.sin(since * (2 + i % 3) + i);
			Palette.sparkle(0.6f + (i % 5) * 0.2f, 0, riserColour);
			paint.batch.setColor(riserColour[0], riserColour[1], riserColour[2], alpha * twinkle * 0.8f);
			paint.batch.draw(paint.assets.glow, x + MathUtils.sin(since + i) * 10 - dot / 2, y - dot / 2, dot, dot);
		}
		paint.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		paint.batch.setColor(Color.WHITE);
	}

	private void side(Color c, float dx, float edge, boolean above, float alpha) {
		float bw = layout.w * 2, bh = layout.h;
		paint.batch.setColor(c.r, c.g, c.b, alpha);
		paint.batch.draw(paint.assets.white, layout.w / 2 - bw / 2 + dx, above ? edge : edge - bh, bw / 2, above ? 0 : bh, bw, bh, 1, 1,
				VERSUS_TILT, 0, 0, 1, 1, false, false);
		paint.batch.setColor(Color.WHITE);
	}

	private void gilding(float mid, float a) {
		if (a <= 0) {
			return;
		}
		float bw = layout.w * 2, cos = MathUtils.cosDeg(VERSUS_TILT), sin = MathUtils.sinDeg(VERSUS_TILT);
		rule(mid, bw, GILD_W, Palette.GOLD, a);
		rule(mid + GILD_W, bw, 2, GILD_DARK, a);
		rule(mid - GILD_W, bw, 2, GILD_DARK, a);
		paint.batch.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, a);
		for (float d = -layout.w; d <= layout.w; d += GILD_STEP) {
			float x = layout.w / 2 + d * cos, y = mid + d * sin;
			paint.batch.draw(paint.assets.white, x - GILD_DIAMOND / 2, y - GILD_DIAMOND / 2, GILD_DIAMOND / 2, GILD_DIAMOND / 2, GILD_DIAMOND,
					GILD_DIAMOND, 1, 1, 45 + VERSUS_TILT, 0, 0, 1, 1, false, false);
		}
		paint.batch.setColor(Color.WHITE);
	}

	private void rule(float y, float width, float thick, Color c, float a) {
		paint.batch.setColor(c.r, c.g, c.b, a);
		paint.batch.draw(paint.assets.white, layout.w / 2 - width / 2, y - thick / 2, width / 2, thick / 2, width, thick, 1, 1, VERSUS_TILT,
				0, 0, 1, 1, false, false);
	}

	private static final String VS = "VS", CHALLENGER = "Challenger";
	private static final float GILD_W = 6, GILD_STEP = 110, GILD_DIAMOND = 12;
	private static final Color VS_SHADOW = new Color(0.12f, 0.05f, 0.03f, 1), GILD_DARK = new Color(0.55f, 0.4f, 0.17f, 1);

	private static final float TIP_W = 440, TIP_PAD = 18, TIP_GAP = 18, TIP_RING = 6, TIP_LINE = 3;
	private static final String TIP_TAP = "Tap to continue";
	private final Rectangle anchor = new Rectangle();

	void drawTip(PracticeCoach.Tip tip) {
		boolean pointed = anchorOf(tip);
		float hx = anchor.x - TIP_RING, hy = anchor.y - TIP_RING, hw = anchor.width + 2 * TIP_RING, hh = anchor.height + 2 * TIP_RING;
		paint.fade(0.5f);
		if (pointed) {
			paint.rect(0, 0, layout.w, hy, Palette.BACKDROP);
			paint.rect(0, hy + hh, layout.w, layout.h - hy - hh, Palette.BACKDROP);
			paint.rect(0, hy, hx, hh, Palette.BACKDROP);
			paint.rect(hx + hw, hy, layout.w - hx - hw, hh, Palette.BACKDROP);
		} else {
			paint.rect(0, 0, layout.w, layout.h, Palette.BACKDROP);
		}
		paint.fade(1f);
		if (pointed) {
			paint.rect(hx, hy, hw, TIP_LINE, Palette.GOLD);
			paint.rect(hx, hy + hh - TIP_LINE, hw, TIP_LINE, Palette.GOLD);
			paint.rect(hx, hy, TIP_LINE, hh, Palette.GOLD);
			paint.rect(hx + hw - TIP_LINE, hy, TIP_LINE, hh, Palette.GOLD);
		}
		BitmapFont f = paint.ui(1.05f, Palette.CREAM);
		paint.glyphs.setText(f, tip.text(), Palette.CREAM, TIP_W - 2 * TIP_PAD, Align.left, true);
		float bh = paint.glyphs.height + 2 * TIP_PAD + 26, bw = TIP_W;
		float cx = pointed ? anchor.x + anchor.width / 2 : layout.w / 2;
		float by = !pointed ? layout.h / 2 - bh / 2
				: anchor.y + anchor.height / 2 > layout.h / 2 ? anchor.y - TIP_GAP - TIP_RING - bh : anchor.y + anchor.height + TIP_GAP + TIP_RING;
		float bx = MathUtils.clamp(cx - bw / 2, 12, layout.w - 12 - bw);
		by = MathUtils.clamp(by, 12, layout.h - 12 - bh);
		paint.frame(paint.assets.panelTurn, bx, by, bw, bh);
		f.draw(paint.batch, tip.text(), bx + TIP_PAD, by + bh - TIP_PAD, bw - 2 * TIP_PAD, Align.left, true);
		paint.text(paint.ui(0.9f, Palette.GOLD), TIP_TAP, bx + bw - TIP_PAD, by + TIP_PAD + 8, false);
	}

	private boolean anchorOf(PracticeCoach.Tip tip) {
		switch (tip.anchor()) {
		case DEALER:
			anchor.set(layout.panelX(match.dealer()), layout.panelTop(match.dealer()) - PlayerPanel.H, PlayerPanel.W, PlayerPanel.H);
			return true;
		case TALLY:
			anchor.set(10, layout.h - 58, 150, 50);
			return true;
		case TRACKER:
			anchor.set(10, layout.h - 100, 150, 38);
			return true;
		case TRUMP:
			anchor.set(layout.trumpX(), layout.trumpY(), layout.trumpW(), layout.trumpH());
			return true;
		case CARD:
			int i = scene.handOrder().indexOf(tip.card());
			if (i < 0 || i >= scene.handRects().size()) {
				return false;
			}
			anchor.set(scene.handRects().get(i));
			return true;
		case BID:
			Rectangle b = targets.bid(tip.number());
			if (b == null || b.width <= 0) {
				return false;
			}
			anchor.set(b);
			return true;
		case SCORE_ROW:
			anchor.set(layout.w / 2 - ScoreCard.W / 2, layout.centreY() - ScoreCard.H / 2, ScoreCard.W, ScoreCard.H);
			return true;
		default:
			return false;
		}
	}

	void drawDealerPick(float t) {
		if (dealsOf != match) {
			dealsOf = match;
			String who = scene.inPractice() ? PracticeRound.position(match.dealer()) : scene.name(match.dealer());
			deals = match.dealer() == Seating.YOU ? "You deal" : Character.toUpperCase(who.charAt(0)) + who.substring(1) + " deals";
		}
		float cx = layout.w / 2, cy = layout.centreY() + 20;
		if (!DealerPick.settled(t)) {
			float fly = Ease.IN.apply(MathUtils.clamp((t - DealerPick.LANDED) / DealerPick.FLIGHT, 0, 1));
			float tx = layout.panelX(match.dealer()) + DEALER_CHIP_X, ty = layout.panelTop(match.dealer()) - DEALER_CHIP_Y;
			float appear = COIN_APPEAR.progress(t);
			float x = MathUtils.lerp(cx, tx, fly), y = MathUtils.lerp(cy, ty, fly), size = MathUtils.lerp(DEALER_CHIP, 26, fly) * (0.3f + 0.7f * appear);
			float p = MathUtils.clamp((t - DealerPick.APPEAR) / (DealerPick.LANDED - DealerPick.APPEAR), 0, 1);
			float angle = DEALER_SPINS * MathUtils.PI * (1 - (1 - p) * (1 - p));
			gatheringSparks(cx, cy, t);
			paint.glow(x - size * 1.6f, y - size * 1.6f, size * 3.2f, size * 3.2f, Palette.EMBER, 0.5f * (1 - fly) * appear);
			coin(x, y, size, angle, appear);
		}
		if (DealerPick.landed(t)) {
			paint.centred(paint.serif(1.6f, Palette.GOLD, Math.min(1, (t - DealerPick.LANDED) * 3)), deals, cx, cy - DEALER_CHIP / 2 - 18);
		}
	}

	private static final float DEALER_CHIP = 96, DEALER_SPINS = 8, DEALER_CHIP_X = 40, DEALER_CHIP_Y = 22, COIN_THICK = 0.12f;
	private static final int COIN_LAYERS = 12;

	private void coin(float cx, float cy, float size, float angle, float alpha) {
		float face = size * Math.max(0.03f, Math.abs(MathUtils.cos(angle))), half = size * COIN_THICK / 2 * MathUtils.sin(angle);
		for (int i = 0; i <= COIN_LAYERS; i++) {
			float k = i / (float) COIN_LAYERS, shade = 0.42f + 0.16f * (i % 2) + 0.12f * k;
			paint.batch.setColor(shade, shade * 0.78f, shade * 0.4f, alpha);
			paint.batch.draw(paint.assets.dealerIcon, cx - half + 2 * half * k - face / 2, cy - size / 2, face, size);
		}
		paint.batch.setColor(1, 1, 1, alpha);
		paint.batch.draw(paint.assets.dealerIcon, cx + half - face / 2, cy - size / 2, face, size);
		paint.batch.setColor(Color.WHITE);
	}

	private void gatheringSparks(float cx, float cy, float t) {
		float gather = COIN_APPEAR.progress(t), fade = 1 - SPARKS_FADE.progress(t);
		if (fade <= 0) {
			return;
		}
		paint.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < GATHERING; i++) {
			float angle = i * MathUtils.PI2 / GATHERING + t * 3 + (1 - gather) * 2, r = DEALER_CHIP * (0.45f + 1.4f * (1 - gather)) + (i % 3) * 8;
			float dot = 7 + (i % 4) * 3;
			Palette.sparkle(0.6f + (i % 5) * 0.2f, 0, sparkColour);
			paint.batch.setColor(sparkColour[0], sparkColour[1], sparkColour[2], fade * Math.min(1, t * 4) * (0.6f + 0.4f * MathUtils.sin(t * 9 + i)));
			paint.batch.draw(paint.assets.glow, cx + MathUtils.cos(angle) * r - dot / 2, cy + MathUtils.sin(angle) * r - dot / 2, dot, dot);
		}
		paint.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		paint.batch.setColor(Color.WHITE);
	}

	private static final int GATHERING = 28;
	private static final Tween COIN_APPEAR = Tween.over(DealerPick.APPEAR, Ease.OUT), SPARKS_FADE = Tween.over(0.5f, Ease.SMOOTH).after(DealerPick.APPEAR);
	private final float[] sparkColour = new float[3];
	private String deals = "";
	private Match dealsOf;

	void drawHandEnd(float t) {
		float a = HAND_END_IN.progress(t);
		art.art(match.madeBid(Seating.YOU) ? paint.assets.bidTricksText : paint.assets.setText, layout.centreX(),
				layout.centreY(), HAND_END_SCALE, a);
		drawHandLabels(0f, a);
	}

	void drawHandLabels(float drop, float alpha) {
		for (int seat = 0; seat < match.players(); seat++) {
			if (seat == Seating.YOU && drop <= 0) {
				continue;
			}
			boolean made = match.madeBid(seat);
			String s = Wording.handResult(made);
			float scale, x, y, a = alpha;
			if (seat == Seating.YOU) {
				float p = Ease.OUT.apply(drop);
				BitmapFont f = paint.serif(LABEL_BIG);
				paint.glyphs.setText(f, s);
				float sx = layout.w / 2 - paint.glyphs.width / 2, sy = layout.centreY() + paint.glyphs.height / 2;
				f = paint.serif(LABEL_SMALL);
				paint.glyphs.setText(f, s);
				float ex = layout.panelX(Seating.YOU) + TableLayout.PANEL_W + LABEL_BESIDE;
				float ey = layout.panelTop(Seating.YOU) - TableLayout.PANEL_H / 2 + paint.glyphs.height / 2;
				scale = MathUtils.lerp(LABEL_BIG, LABEL_SMALL, p);
				x = MathUtils.lerp(sx, ex, p);
				y = MathUtils.lerp(sy, ey, p);
				a *= MathUtils.clamp(drop * 3f, 0, 1);
			} else {
				scale = LABEL_OPPONENT;
				paint.glyphs.setText(paint.serif(scale), s);
				Vector2 o = layout.fanOrigin(seat);
				x = layout.spot(seat) == 3 ? o.x - paint.glyphs.width : o.x;
				y = o.y + TableLayout.backH() / 2 + paint.glyphs.height / 2;
			}
			BitmapFont f = paint.serif(scale);
			f.setColor(0f, 0f, 0f, 0.8f * a);
			f.draw(paint.batch, s, x + LABEL_SHADOW, y - LABEL_SHADOW);
			Color c = made ? Palette.GOLD : Palette.RED;
			f.setColor(c.r, c.g, c.b, a);
			f.draw(paint.batch, s, x, y);
		}
	}

	void drawHandResults(float t) {
		float py = scoreCard.draw(layout.w / 2, layout.centreY(), match.suddenDeath() ? SUDDEN_DEATH : Wording.handResults(match.handIndex() + 1), resultRows(), 1f, t);
		float right = layout.w / 2 + ScoreCard.W / 2 - TRICKS_INSET;
		paint.button(targets.button(OverlayTargets.Button.TRICKS), TRICKS, right - TRICKS_W / 2, py + TRICKS_RISE, TRICKS_W, TRICKS_H,
				paint.assets.buttonAlt);
		BitmapFont s = paint.ui(1f, Palette.GOLD);
		Rectangle win = targets.button(OverlayTargets.Button.FORCE_WIN);
		Rectangle menu = win.width > 0 ? win : targets.button(OverlayTargets.Button.MENU);
		paint.glyphs.setText(s, TAP_NOTE);
		paint.text(s, TAP_NOTE, menu.x - TAP_NOTE_GAP, menu.y + (menu.height + paint.glyphs.height) / 2, false);
	}

	void drawTrickReview() {
		if (targets.review() != OverlayTargets.REVIEW_CLOSED) {
			review.draw();
		}
	}


	private static final String TAP_NOTE = "Tap to continue", TRICKS = "Tricks";
	private static final float TRICKS_W = 104, TRICKS_H = 36;

	private final List<ScoreCard.Row> resultRows = new ArrayList<ScoreCard.Row>();
	private Match resultsOf;
	private int resultsHand = -1;

	private List<ScoreCard.Row> resultRows() {
		if (resultsOf != match || resultsHand != match.handIndex()) {
			resultsOf = match;
			resultsHand = match.handIndex();
			resultRows.clear();
			for (int seat : ranked()) {
				resultRows.add(new ScoreCard.Row(match.placeOf(seat), scene.name(seat), match.madeBid(seat), match.bid(seat),
						match.tricksWon(seat), match.lastHandPoints(seat), match.score(seat)));
			}
		}
		return resultRows;
	}

	static final float NEW_BEST_LENGTH = 1.6f;
	private static final int PODIUM_PLACES = 3;
	private static final float PODIUM_W = 150, PODIUM_STEP_X = 200, PODIUM_BASE = 0.2f,
			ROLL_FROM = 0.3f,
			ROLL_STEP = 0.28f, ROLL_SPIN = 28, BEST_FLASH = 0.45f;
	private static final String[] PLACE_CAPS = { "", "FIRST", "SECOND", "THIRD", "FOURTH" };
	private static final String WINNER_IS = "And the winner is\u2026";
	private static final Tween PODIUM_UP = Tween.over(GameEnd.RISE, Ease.OUT);
	private final StringBuilder rolled = new StringBuilder();

	void drawPodium(float t) {
		int n = match.players();
		paint.fade(Math.min(1, t * 3));
		paint.rect(0, 0, layout.w, layout.h, Palette.BACKDROP);
		paint.fade(1f);
		List<Integer> order = ranked();
		int shown = Math.min(PODIUM_PLACES, order.size());
		float span = (shown - 1) * PODIUM_STEP_X, base = layout.h * PODIUM_BASE, cw = PODIUM_W, ch = cw * 98 / 73;
		for (int i = 0; i < shown; i++) {
			float k = PODIUM_UP.progress(t - GameEnd.risesAt(i + 1, n));
			if (k <= 0) {
				continue;
			}
			int s = order.get(i);
			float cx = layout.w / 2 - span / 2 + i * PODIUM_STEP_X, y = base - (1 - k) * (layout.h * 0.8f);
			podiumCard(s, match.placeOf(s), cx - cw / 2, y, cw, ch, t);
		}
		float waiting = t - GameEnd.risesAt(2, n) - GameEnd.RISE;
		if (waiting > 0 && t < GameEnd.risesAt(1, n)) {
			paint.text(paint.serif(1.6f, Palette.GOLD, Math.min(1, waiting * 4)), WINNER_IS, layout.w / 2, layout.h * 0.86f, true);
		}
		if (scene.podiumDone()) {
			paint.text(paint.ui(1.1f, Palette.GOLD), TAP_NOTE, layout.w / 2, layout.h * 0.07f, true);
		}
	}

	private void podiumCard(int seat, int place, float x, float y, float cw, float ch, float t) {
		boolean first = place == 1;
		if (first) {
			paint.glow(x - cw * 0.6f, y - ch * 0.35f, cw * 2.2f, ch * 1.7f, Palette.EMBER, 0.45f + 0.15f * MathUtils.sin(t * 3));
		}
		art.shadow(x, y, cw, ch, 6);
		paint.batch.draw(paint.assets.campaignFace, x, y, cw, ch);
		float cx = x + cw / 2;
		paint.text(paint.ui(0.85f, Palette.GOLD), PLACE_CAPS[place], cx, y + ch - 16, true);
		paint.numeral(place, cx, y + ch * 0.6f, cw * 0.3f, first ? Palette.GOLD_LIGHT : Palette.GOLD);
		paint.text(paint.serif(1.3f, Palette.GOLD_LIGHT), scene.name(seat), cx, y + ch * 0.34f, true);
		paint.text(paint.ui(1.0f, Palette.CREAM), Numbers.text(match.score(seat)), cx, y + ch * 0.2f, true);
	}

	private final Vector2 bestSpot = new Vector2();
	private static final Color EMBOSS_SHADE = new Color(0.12f, 0.07f, 0.02f, 0.9f), EMBOSS_SHINE = new Color(1f, 0.95f, 0.75f, 0.7f);

	Vector2 bestSpot() {
		return bestSpot;
	}

	float bestLocksAt() {
		return ROLL_FROM + (Numbers.text(match.score(Seating.YOU)).length() - 1) * ROLL_STEP;
	}

	private void emboss(BitmapFont f, CharSequence digits, float x, float y) {
		f.setColor(EMBOSS_SHADE);
		paint.text(f, digits, x + 1, y - 2, false);
		f.setColor(EMBOSS_SHINE);
		paint.text(f, digits, x - 1, y + 1, false);
		f.setColor(Palette.GOLD);
		paint.text(f, digits, x, y, false);
	}

	private CharSequence rolling(int score, float t) {
		String digits = Numbers.text(score);
		rolled.setLength(0);
		for (int i = 0; i < digits.length(); i++) {
			char c = digits.charAt(i);
			boolean locked = t >= ROLL_FROM + i * ROLL_STEP || c < '0' || c > '9';
			rolled.append(locked ? c : (char) ('0' + ((int) (t * ROLL_SPIN) + i * 3) % 10));
		}
		return rolled;
	}

	void drawGameOver(int newHighScore, float since) {
		float pw = GAME_OVER_W, ph = GAME_OVER_H;
		float px = (layout.w - pw) / 2, py = layout.h / 2 - GAME_OVER_LIFT - ph / 2;
		paint.frame(paint.assets.panel, px, py, pw, ph);
		art.art(paint.assets.gameOverText, layout.w / 2, py + ph - GAME_OVER_ART_DROP, GAME_OVER_ART_SCALE, 1f);

		String result = winnersLine();
		BitmapFont f = paint.ui(1.5f, Palette.GOLD);
		paint.text(f, result, layout.w / 2, py + ph - GAME_OVER_RESULT_DROP, true);

		BitmapFont t = paint.ui(1.2f);
		float y = py + ph - GAME_OVER_ROWS_DROP;
		for (int s : ranked()) {
			t.setColor(Palette.MUTED);
			t.draw(paint.batch, Wording.ordinal(match.placeOf(s)), px + GAME_OVER_PLACE_X, y);
			t.setColor(match.placeOf(s) == 1 ? Palette.GOLD : Palette.CREAM);
			t.draw(paint.batch, scene.name(s), px + GAME_OVER_NAME_X, y);
			boolean best = s == Seating.YOU && newHighScore == 0;
			float sx = px + pw - GAME_OVER_SCORE_FROM_RIGHT;
			if (best) {
				float locked = since - ROLL_FROM - (Numbers.text(match.score(s)).length() - 1) * ROLL_STEP;
				if (locked >= 0 && locked < BEST_FLASH) {
					paint.glow(sx - 90, y - 50, 120, 80, Palette.SPARKLE, 0.8f * (1 - locked / BEST_FLASH));
				}
			}
			if (best) {
				bestSpot.set(sx - 30, y - 12);
				emboss(t, rolling(match.score(s), since), sx, y);
			} else {
				paint.text(t, Numbers.text(match.score(s)), sx, y, false);
			}
			if (best) {
				BitmapFont n = paint.ui(0.95f, Palette.GOLD_LIGHT);
				n.draw(paint.batch, "New best!", px + pw - NEW_BEST_FROM_RIGHT, y - 2);
			}
			y -= GAME_OVER_ROW;
		}
		float bw = END_BUTTON_W, bh = END_BUTTON_H, by = py + END_BUTTONS_RISE;
		paint.button(targets.button(OverlayTargets.Button.CONTINUE), "Continue", layout.w / 2 - bw / 2 - END_BUTTONS_GAP / 2, by, bw, bh,
				paint.assets.button);
		paint.button(targets.button(OverlayTargets.Button.MAIN_MENU), "Main menu", layout.w / 2 + bw / 2 + END_BUTTONS_GAP / 2, by, bw, bh,
				paint.assets.buttonAlt);
	}

	private String winnersLine() {
		if (winners == null || winnersOf != match) {
			List<Integer> leaders = match.leaders();
			List<String> names = new ArrayList<String>();
			for (int seat : leaders) {
				names.add(scene.name(seat));
			}
			winners = Wording.winners(names, leaders.size() == 1 && leaders.get(0) == Seating.YOU);
			winnersOf = match;
		}
		return winners;
	}

	private String winners;
	private Match winnersOf;

	List<Integer> ranked() {
		ranked.clear();
		for (int s = 0; s < match.players(); s++) {
			ranked.add(s);
		}
		Collections.sort(ranked, byScore);
		return ranked;
	}
}
