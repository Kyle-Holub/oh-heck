package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import rules.Match;

/**
 * Everything drawn over the card table: the bid panel, the end-of-hand
 * message and score table, the final scores, the pause menu and its button,
 * and short hints. Each draw method also sets the tap areas it shows, which
 * {@link PlayScreen} checks.
 */
final class TableOverlays {

	static final String[] MENU = { "Resume", "Options", "New game", "Main menu" };
	private static final float HINT_TIME = 2.2f;

	// sizes of the panels and buttons drawn over the table
	private static final float MENU_BUTTON_W = 80, MENU_BUTTON_H = 38;
	private static final float PAUSE_BUTTON_W = 260, PAUSE_BUTTON_H = 56, PAUSE_GAP = 14;
	/** A bid that can't be made: the gold numeral greyed and faded. */
	private static final Color NUMERAL_OFF = new Color(0.55f, 0.52f, 0.5f, 0.6f);
	private static final float BID_BUTTON = 58, BID_GAP = 8, BID_PANEL_MIN_W = 440, BID_PANEL_H = 190;
	private static final float RESULTS_W = 540, RESULTS_H = 220, RESULTS_ROW = 26;
	private static final float GAME_OVER_W = 420, GAME_OVER_H = 306, GAME_OVER_ROW = 27;
	private static final float END_BUTTON_W = 170, END_BUTTON_H = 42;
	private static final float HINT_H = 40, HINT_PAD = 16;

	final Rectangle menuButton = new Rectangle();
	final List<Rectangle> bidButtons = new ArrayList<Rectangle>();
	final List<Rectangle> menuItems = new ArrayList<Rectangle>();
	final Rectangle playAgainButton = new Rectangle(), highScoresButton = new Rectangle();

	private final PlayScreen screen;
	private final TableLayout table;
	private final CardArt art;
	private String hint = "";
	private float hintTime;

	private Match match;
	private final List<Integer> ranked = new ArrayList<Integer>();
	private final StringBuilder sb = new StringBuilder();
	private final Comparator<Integer> byScore = new Comparator<Integer>() {
		@Override
		public int compare(Integer a, Integer b) {
			return match.score(b) - match.score(a);
		}
	};

	TableOverlays(PlayScreen screen, TableLayout table, CardArt art) {
		this.screen = screen;
		this.table = table;
		this.art = art;
	}

	void setMatch(Match match) {
		this.match = match;
	}

	// ---- hints

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
		BitmapFont f = screen.ui(1.15f);
		screen.glyphs.setText(f, hint);
		float pw = screen.glyphs.width + 2 * HINT_PAD, ph = HINT_H;
		float px = (table.w - pw) / 2, py = table.hintY();
		screen.frame(screen.assets.panel, px, py, pw, ph);
		f.setColor(Palette.GOLD);
		screen.textIn(f, hint, px, py, pw, ph);
	}

	// ---- the pause menu and its button

	void drawMenuButton() {
		menuButton.set(table.w - TableLayout.EDGE - MENU_BUTTON_W, 10, MENU_BUTTON_W, MENU_BUTTON_H);
		screen.frame(screen.assets.buttonAlt, menuButton.x, menuButton.y, menuButton.width, menuButton.height);
		BitmapFont f = screen.ui(1.2f);
		f.setColor(Palette.CREAM);
		screen.textIn(f, "Menu", menuButton.x, menuButton.y, menuButton.width, menuButton.height);
	}

	void drawMenu() {
		float w = table.w, h = table.h;
		screen.rect(0, 0, w, h, Palette.BACKDROP);
		float bw = PAUSE_BUTTON_W, bh = PAUSE_BUTTON_H, gap = PAUSE_GAP;
		float total = MENU.length * bh + (MENU.length - 1) * gap;
		float top = h / 2 + total / 2 + 20;
		BitmapFont title = screen.ui(1.5f);
		title.setColor(Palette.GOLD);
		screen.text(title, "Paused", w / 2, top + 36, true);
		BaseScreen.sizeTo(menuItems, MENU.length);
		BitmapFont f = screen.ui(1.4f);
		for (int i = 0; i < MENU.length; i++) {
			Rectangle r = menuItems.get(i).set((w - bw) / 2, top - (i + 1) * bh - i * gap, bw, bh);
			screen.frame(i == 0 ? screen.assets.button : screen.assets.buttonAlt, r.x, r.y, r.width, r.height);
			f.setColor(Palette.CREAM);
			screen.textIn(f, MENU[i], r.x, r.y, r.width, r.height);
		}
	}

	// ---- bidding

	void drawBidPanel() {
		int n = match.handSize();
		float bs = BID_BUTTON, gap = BID_GAP;
		float rowW = (n + 1) * bs + n * gap;
		float pw = Math.max(rowW + 40, BID_PANEL_MIN_W), ph = BID_PANEL_H;
		float px = (table.w - pw) / 2, py = table.centreY() - ph / 2;
		screen.frame(screen.assets.panel, px, py, pw, ph);

		art.art(screen.assets.makeBidText, table.w / 2, py + ph - 44, 0.78f, 1f);
		int forbidden = match.forbiddenBid();
		if (forbidden >= 0) {
			BitmapFont s = screen.ui(1f);
			s.setColor(Palette.MUTED);
			screen.text(s, "As dealer you can't bid " + forbidden, table.w / 2, py + ph - 86, true);
		}

		BaseScreen.sizeTo(bidButtons, n + 1);
		float bx = (table.w - rowW) / 2, by = py + 20;
		for (int b = 0; b <= n; b++) {
			Rectangle r = bidButtons.get(b).set(bx + b * (bs + gap), by, bs, bs);
			boolean ok = match.isLegalBid(b);
			screen.frame(ok ? screen.assets.button : screen.assets.buttonOff, r.x, r.y, r.width, r.height);
			screen.numeral(b, r.x + r.width / 2, r.y + r.height / 2, bs * 0.66f, ok ? Color.WHITE : NUMERAL_OFF);
		}
	}

	// ---- the end of a hand

	/** "Bid Tricks!" or "You got set!", t seconds in, with how each opponent did. */
	void drawHandEnd(float t) {
		float a = MathUtils.clamp(t / 0.3f, 0, 1);
		art.art(match.madeBid(TableLayout.HUMAN) ? screen.assets.bidTricksText : screen.assets.setText, table.centreX(),
				table.centreY(), 0.95f, a);
		drawHandLabels(0f, a);
		BitmapFont f = screen.ui(1f);
		f.setColor(Palette.MUTED);
		screen.text(f, "Tap for the scores", table.w / 2, table.h / 2 - 40, true);
	}

	/**
	 * "Bid tricks!" / "Set!" for each player. The opponents' sit by their cards;
	 * yours starts as the big art in the middle and, as drop goes 0 to 1, glides
	 * to the right of your panel, where it stays while the scores are up.
	 */
	void drawHandLabels(float drop, float alpha) {
		for (int seat = 0; seat < match.players(); seat++) {
			if (seat == TableLayout.HUMAN && drop <= 0) {
				continue;
			}
			boolean made = match.madeBid(seat);
			String s = made ? "Bid tricks!" : "Set!";
			float scale, x, y, a = alpha;
			if (seat == TableLayout.HUMAN) {
				float p = Interpolation.pow2Out.apply(drop);
				BitmapFont f = screen.serif(2.6f);
				screen.glyphs.setText(f, s);
				float sx = table.w / 2 - screen.glyphs.width / 2, sy = table.centreY() + screen.glyphs.height / 2;
				f = screen.serif(1.6f);
				screen.glyphs.setText(f, s);
				float ex = table.panelX(TableLayout.HUMAN) + TableLayout.PANEL_W + 16;
				float ey = table.panelTop(TableLayout.HUMAN) - TableLayout.PANEL_H / 2 + screen.glyphs.height / 2;
				scale = MathUtils.lerp(2.6f, 1.6f, p);
				x = MathUtils.lerp(sx, ex, p);
				y = MathUtils.lerp(sy, ey, p);
				a *= MathUtils.clamp(drop * 3f, 0, 1);
			} else {
				scale = 1.9f;
				screen.glyphs.setText(screen.serif(scale), s);
				Vector2 o = table.fanOrigin(seat);
				x = seat == 3 ? o.x - screen.glyphs.width : o.x;
				y = o.y + TableLayout.backH() / 2 + screen.glyphs.height / 2;
			}
			BitmapFont f = screen.serif(scale);
			f.setColor(0f, 0f, 0f, 0.8f * a);
			f.draw(screen.batch, s, x + 2, y - 2);
			Color c = made ? Palette.GOLD : Palette.RED;
			f.setColor(c.r, c.g, c.b, a);
			f.draw(screen.batch, s, x, y);
		}
	}

	void drawHandResults() {
		float pw = RESULTS_W, ph = RESULTS_H;
		float px = (table.w - pw) / 2, py = table.centreY() - ph / 2;
		screen.frame(screen.assets.panel, px, py, pw, ph);
		BitmapFont f = screen.ui(1.3f);
		f.setColor(Palette.CREAM);
		screen.text(f, "Hand " + (match.handIndex() + 1) + " results", table.w / 2, py + ph - 14, true);

		BitmapFont t = screen.ui(1.1f);
		float[] cols = RESULT_COLS;
		float y = py + ph - 52;
		t.setColor(Palette.MUTED);
		for (int c = 0; c < cols.length; c++) {
			t.draw(screen.batch, RESULT_HEAD[c], px + cols[c], y);
		}
		for (int seat : ranked()) {
			y -= RESULTS_ROW;
			int pts = match.lastHandPoints(seat);
			t.setColor(Palette.MUTED);
			t.draw(screen.batch, HighScores.ordinal(place(seat)), px + cols[0], y);
			t.setColor(match.madeBid(seat) ? Palette.CREAM : Palette.RED);
			t.draw(screen.batch, screen.name(seat) + (match.madeBid(seat) ? "" : "  (set)"), px + cols[1], y);
			screen.numeral(match.bid(seat), px + cols[2] + 8, y - 8, 26, Color.WHITE);
			t.draw(screen.batch, String.valueOf(match.tricksWon(seat)), px + cols[3], y);
			t.draw(screen.batch, (pts > 0 ? "+" : "") + pts, px + cols[4], y);
			t.setColor(place(seat) == 1 ? Palette.GOLD : Palette.CREAM);
			t.draw(screen.batch, String.valueOf(match.score(seat)), px + cols[5], y);
		}
		BitmapFont s = screen.ui(1f);
		s.setColor(Palette.GOLD);
		screen.text(s, "Tap to continue", table.w / 2, py + 26, true);
	}

	private static final float[] RESULT_COLS = { 22, 62, 255, 315, 380, 465 }; // (room for "Henrietta  (set)")
	private static final String[] RESULT_HEAD = { "", "Player", "Bid", "Won", "Points", "Total" };

	// ---- the end of the game

	/** Final scores; newHighScore is where your game landed in the high score table, or -1. */
	void drawGameOver(int newHighScore) {
		float pw = GAME_OVER_W, ph = GAME_OVER_H;
		float px = (table.w - pw) / 2, py = table.h / 2 - 6 - ph / 2; // clear of the top panel
		screen.frame(screen.assets.panel, px, py, pw, ph);
		art.art(screen.assets.gameOverText, table.w / 2, py + ph - 50, 0.66f, 1f);

		List<Integer> leaders = match.leaders();
		sb.setLength(0);
		for (int i = 0; i < leaders.size(); i++) {
			if (i > 0) {
				sb.append(i == leaders.size() - 1 ? " and " : ", ");
			}
			sb.append(screen.name(leaders.get(i)));
		}
		if (leaders.size() > 1) {
			sb.append(" tie!");
		} else {
			sb.append(leaders.get(0) == TableLayout.HUMAN ? " win the game!" : " wins the game!");
		}
		BitmapFont f = screen.ui(1.5f);
		f.setColor(Palette.GOLD);
		screen.text(f, sb.toString(), table.w / 2, py + ph - 94, true);

		BitmapFont t = screen.ui(1.2f);
		float y = py + ph - 130;
		for (int s : ranked()) {
			t.setColor(Palette.MUTED);
			t.draw(screen.batch, HighScores.ordinal(place(s)), px + 60, y);
			t.setColor(place(s) == 1 ? Palette.GOLD : Palette.CREAM);
			t.draw(screen.batch, screen.name(s), px + 120, y);
			screen.text(t, String.valueOf(match.score(s)), px + pw - 140, y, false);
			if (s == TableLayout.HUMAN && newHighScore == 0) { // only a score that tops the table
				BitmapFont n = screen.ui(0.95f);
				n.setColor(Palette.GOLD_LIGHT);
				n.draw(screen.batch, "New best!", px + pw - 128, y - 2);
			}
			y -= GAME_OVER_ROW;
		}
		float bw = END_BUTTON_W, bh = END_BUTTON_H, by = py + 16;
		screen.button(playAgainButton, "Play again", table.w / 2 - bw / 2 - 8, by, bw, bh, screen.assets.button);
		screen.button(highScoresButton, "High scores", table.w / 2 + bw / 2 + 8, by, bw, bh, screen.assets.buttonAlt);
	}

	// ---- standings

	/** Seats from highest score to lowest; ties keep seat order. (Reused list.) */
	List<Integer> ranked() {
		ranked.clear();
		for (int s = 0; s < match.players(); s++) {
			ranked.add(s); // small Integers are cached: no garbage
		}
		Collections.sort(ranked, byScore);
		return ranked;
	}

	/** 1 for the lead; players on the same score share a place. */
	int place(int seat) {
		int p = 1;
		for (int s = 0; s < match.players(); s++) {
			if (match.score(s) > match.score(seat)) {
				p++;
			}
		}
		return p;
	}
}
