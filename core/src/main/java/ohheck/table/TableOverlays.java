package ohheck.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.players.Seating;
import ohheck.rules.Match;
import ohheck.ui.CardArt;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/** Panels and buttons drawn over the table. */
final class TableOverlays {

	private static final float HINT_TIME = 2.2f;

	private static final float MENU_BUTTON_W = 80, MENU_BUTTON_H = 38;
	private static final float PAUSE_BUTTON_W = 260, PAUSE_BUTTON_H = 56, PAUSE_GAP = 14;
	private static final Color NUMERAL_OFF = new Color(0.55f, 0.52f, 0.5f, 0.6f);
	private static final float BID_BUTTON = 58, BID_GAP = 8, BID_PANEL_MIN_W = 440, BID_PANEL_H = 190;
	private static final float RESULTS_W = 540, RESULTS_H = 220, RESULTS_ROW = 26;
	private static final float GAME_OVER_W = 420, GAME_OVER_H = 306, GAME_OVER_ROW = 27;
	private static final float END_BUTTON_W = 170, END_BUTTON_H = 42;
	private static final float HINT_H = 40, HINT_PAD = 16;

	private static final float PAUSE_LIFT = 20, PAUSED_TITLE_RISE = 36;
	private static final float MENU_BUTTON_Y = 10;
	private static final float BID_TITLE_DROP = 44, BID_TITLE_SCALE = 0.78f, BID_NOTE_DROP = 86, BID_ROW_RISE = 20,
			BID_PANEL_MARGIN = 40, BID_NUMERAL_OF_BUTTON = 0.66f;
	private static final float HAND_END_FADE = 0.3f, HAND_END_SCALE = 0.95f, TAP_NOTE_BELOW = 40;
	private static final Tween HAND_END_IN = Tween.over(HAND_END_FADE, Ease.LINEAR);
	private static final float LABEL_BIG = 2.6f, LABEL_SMALL = 1.6f, LABEL_OPPONENT = 1.9f, LABEL_BESIDE = 16,
			LABEL_SHADOW = 2;
	private static final float RESULTS_TITLE_DROP = 14, RESULTS_HEAD_DROP = 52, RESULTS_TAP_RISE = 26,
			RESULT_NUMERAL_H = 26, RESULT_NUMERAL_X = 8, RESULT_NUMERAL_Y = 8;
	private static final float GAME_OVER_LIFT = 6, GAME_OVER_ART_DROP = 50, GAME_OVER_ART_SCALE = 0.66f,
			GAME_OVER_RESULT_DROP = 94, GAME_OVER_ROWS_DROP = 130, GAME_OVER_PLACE_X = 60, GAME_OVER_NAME_X = 120,
			GAME_OVER_SCORE_FROM_RIGHT = 140, NEW_BEST_FROM_RIGHT = 128, END_BUTTONS_RISE = 16, END_BUTTONS_GAP = 16;

	private final OverlayTargets targets;

	private final Painter paint;
	private final TableScene scene;
	private final TableLayout layout;
	private final CardArt art;
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

	void drawBidPanel() {
		int n = match.handSize();
		float bs = BID_BUTTON, gap = BID_GAP;
		float rowW = (n + 1) * bs + n * gap;
		float pw = Math.max(rowW + BID_PANEL_MARGIN, BID_PANEL_MIN_W), ph = BID_PANEL_H;
		float px = (layout.w - pw) / 2, py = layout.centreY() - ph / 2;
		paint.frame(paint.assets.panel, px, py, pw, ph);

		art.art(paint.assets.makeBidText, layout.w / 2, py + ph - BID_TITLE_DROP, BID_TITLE_SCALE, 1f);
		int forbidden = match.forbiddenBid();
		if (forbidden >= 0) {
			BitmapFont s = paint.ui(1f, Palette.MUTED);
			paint.text(s, Wording.dealerCantBid(forbidden), layout.w / 2, py + ph - BID_NOTE_DROP, true);
		}

		targets.bidsUpTo(n);
		float bx = (layout.w - rowW) / 2, by = py + BID_ROW_RISE;
		for (int b = 0; b <= n; b++) {
			Rectangle r = targets.bid(b).set(bx + b * (bs + gap), by, bs, bs);
			boolean ok = match.isLegalBid(b);
			paint.frame(ok ? paint.assets.button : paint.assets.buttonOff, r.x, r.y, r.width, r.height);
			paint.numeral(b, r.x + r.width / 2, r.y + r.height / 2, bs * BID_NUMERAL_OF_BUTTON, ok ? Color.WHITE : NUMERAL_OFF);
		}
	}

	void drawHandEnd(float t) {
		float a = HAND_END_IN.progress(t);
		art.art(match.madeBid(Seating.YOU) ? paint.assets.bidTricksText : paint.assets.setText, layout.centreX(),
				layout.centreY(), HAND_END_SCALE, a);
		drawHandLabels(0f, a);
		BitmapFont f = paint.ui(1f, Palette.MUTED);
		paint.text(f, "Tap for the scores", layout.w / 2, layout.h / 2 - TAP_NOTE_BELOW, true);
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
				x = seat == 3 ? o.x - paint.glyphs.width : o.x;
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

	void drawHandResults() {
		float pw = RESULTS_W, ph = RESULTS_H;
		float px = (layout.w - pw) / 2, py = layout.centreY() - ph / 2;
		paint.frame(paint.assets.panel, px, py, pw, ph);
		BitmapFont f = paint.ui(1.3f, Palette.CREAM);
		paint.text(f, Wording.handResults(match.handIndex() + 1), layout.w / 2, py + ph - RESULTS_TITLE_DROP, true);

		BitmapFont t = paint.ui(1.1f);
		float[] cols = RESULT_COLS;
		float y = py + ph - RESULTS_HEAD_DROP;
		t.setColor(Palette.MUTED);
		for (int c = 0; c < cols.length; c++) {
			t.draw(paint.batch, RESULT_HEAD[c], px + cols[c], y);
		}
		for (int seat : ranked()) {
			y -= RESULTS_ROW;
			int pts = match.lastHandPoints(seat);
			t.setColor(Palette.MUTED);
			t.draw(paint.batch, Wording.ordinal(match.placeOf(seat)), px + cols[0], y);
			t.setColor(match.madeBid(seat) ? Palette.CREAM : Palette.RED);
			t.draw(paint.batch, scene.name(seat) + (match.madeBid(seat) ? "" : "  (set)"), px + cols[1], y);
			paint.numeral(match.bid(seat), px + cols[2] + RESULT_NUMERAL_X, y - RESULT_NUMERAL_Y, RESULT_NUMERAL_H, Color.WHITE);
			t.draw(paint.batch, Numbers.text(match.tricksWon(seat)), px + cols[3], y);
			t.draw(paint.batch, Numbers.signed(pts), px + cols[4], y);
			t.setColor(match.placeOf(seat) == 1 ? Palette.GOLD : Palette.CREAM);
			t.draw(paint.batch, Numbers.text(match.score(seat)), px + cols[5], y);
		}
		BitmapFont s = paint.ui(1f, Palette.GOLD);
		paint.text(s, "Tap to continue", layout.w / 2, py + RESULTS_TAP_RISE, true);
	}

	private static final float[] RESULT_COLS = { 22, 62, 255, 315, 380, 465 };
	private static final String[] RESULT_HEAD = { "", "Player", "Bid", "Won", "Points", "Total" };

	void drawGameOver(int newHighScore) {
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
			paint.text(t, Numbers.text(match.score(s)), px + pw - GAME_OVER_SCORE_FROM_RIGHT, y, false);
			if (s == Seating.YOU && newHighScore == 0) {
				BitmapFont n = paint.ui(0.95f, Palette.GOLD_LIGHT);
				n.draw(paint.batch, "New best!", px + pw - NEW_BEST_FROM_RIGHT, y - 2);
			}
			y -= GAME_OVER_ROW;
		}
		float bw = END_BUTTON_W, bh = END_BUTTON_H, by = py + END_BUTTONS_RISE;
		paint.button(targets.button(OverlayTargets.Button.PLAY_AGAIN), "Play again", layout.w / 2 - bw / 2 - END_BUTTONS_GAP / 2, by, bw, bh, paint.assets.button);
		paint.button(targets.button(OverlayTargets.Button.HIGH_SCORES), "High scores", layout.w / 2 + bw / 2 + END_BUTTONS_GAP / 2, by, bw, bh, paint.assets.buttonAlt);
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
