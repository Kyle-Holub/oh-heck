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
		float pw = screen.glyphs.width + 32, ph = 40;
		float px = (table.w - pw) / 2, py = table.hintY();
		screen.frame(screen.assets.panel, px, py, pw, ph);
		f.setColor(Palette.GOLD);
		f.draw(screen.batch, hint, px + 16, py + ph / 2 + 8);
	}

	// ---- the pause menu and its button

	void drawMenuButton() {
		menuButton.set(table.w - 92, 10, 80, 38);
		screen.frame(screen.assets.buttonAlt, menuButton.x, menuButton.y, menuButton.width, menuButton.height);
		BitmapFont f = screen.ui(1.2f);
		f.setColor(Palette.CREAM);
		screen.text(f, "Menu", menuButton.x + menuButton.width / 2, menuButton.y + 27, true);
	}

	void drawMenu() {
		float w = table.w, h = table.h;
		screen.rect(0, 0, w, h, Palette.BACKDROP);
		float bw = 260, bh = 56, gap = 14;
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
			screen.text(f, MENU[i], w / 2, r.y + bh / 2 + 10, true);
		}
	}

	// ---- bidding

	void drawBidPanel() {
		int n = match.handSize();
		float bs = 58, gap = 8;
		float rowW = (n + 1) * bs + n * gap;
		float pw = Math.max(rowW + 40, 440), ph = 176;
		float px = (table.w - pw) / 2, py = table.centreY() - ph / 2;
		screen.frame(screen.assets.panel, px, py, pw, ph);

		art.art(screen.assets.makeBidText, table.w / 2, py + ph - 36, 0.52f, 1f);
		BitmapFont s = screen.ui(1f);
		s.setColor(Palette.MUTED);
		int forbidden = match.forbiddenBid();
		String note = "Bids so far: " + match.bidsSoFar() + " of " + n
				+ (forbidden >= 0 ? "   -   as dealer you can't bid " + forbidden : "");
		screen.text(s, note, table.w / 2, py + ph - 60, true);

		BaseScreen.sizeTo(bidButtons, n + 1);
		float bx = (table.w - rowW) / 2, by = py + 20;
		BitmapFont bf = screen.ui(1.7f);
		for (int b = 0; b <= n; b++) {
			Rectangle r = bidButtons.get(b).set(bx + b * (bs + gap), by, bs, bs);
			boolean ok = match.isLegalBid(b);
			screen.frame(ok ? screen.assets.button : screen.assets.buttonOff, r.x, r.y, r.width, r.height);
			bf.setColor(ok ? Palette.CREAM : Palette.MUTED);
			screen.text(bf, String.valueOf(b), r.x + bs / 2, r.y + bs / 2 + 12, true);
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
		BitmapFont f = screen.assets.serif;
		for (int seat = 0; seat < 4; seat++) {
			if (seat == TableLayout.HUMAN && drop <= 0) {
				continue;
			}
			boolean made = match.madeBid(seat);
			String s = made ? "Bid tricks!" : "Set!";
			float scale, x, y, a = alpha;
			if (seat == TableLayout.HUMAN) {
				float p = Interpolation.pow2Out.apply(drop);
				f.getData().setScale(2.6f * Assets.SERIF_BASE);
				screen.glyphs.setText(f, s);
				float sx = table.w / 2 - screen.glyphs.width / 2, sy = table.centreY() + screen.glyphs.height / 2;
				f.getData().setScale(1.6f * Assets.SERIF_BASE);
				screen.glyphs.setText(f, s);
				float ex = table.panelX(TableLayout.HUMAN) + TableLayout.PANEL_W + 16;
				float ey = table.panelTop(TableLayout.HUMAN) - TableLayout.PANEL_H / 2 + screen.glyphs.height / 2;
				scale = MathUtils.lerp(2.6f, 1.6f, p);
				x = MathUtils.lerp(sx, ex, p);
				y = MathUtils.lerp(sy, ey, p);
				a *= MathUtils.clamp(drop * 3f, 0, 1);
			} else {
				scale = 1.9f;
				f.getData().setScale(scale * Assets.SERIF_BASE);
				screen.glyphs.setText(f, s);
				Vector2 o = table.fanOrigin(seat);
				x = seat == 3 ? o.x - screen.glyphs.width : o.x;
				y = o.y + TableLayout.backH() / 2 + screen.glyphs.height / 2;
			}
			f.getData().setScale(scale * Assets.SERIF_BASE);
			f.setColor(0f, 0f, 0f, 0.8f * a);
			f.draw(screen.batch, s, x + 2, y - 2);
			Color c = made ? Palette.GOLD : Palette.RED;
			f.setColor(c.r, c.g, c.b, a);
			f.draw(screen.batch, s, x, y);
		}
	}

	void drawHandResults() {
		float pw = 470, ph = 220;
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
			y -= 26;
			int pts = match.lastHandPoints(seat);
			t.setColor(Palette.MUTED);
			t.draw(screen.batch, HighScores.ordinal(place(seat)), px + cols[0], y);
			t.setColor(match.madeBid(seat) ? Palette.CREAM : Palette.RED);
			t.draw(screen.batch, PlayScreen.NAMES[seat] + (match.madeBid(seat) ? "" : "  (set)"), px + cols[1], y);
			t.draw(screen.batch, String.valueOf(match.bid(seat)), px + cols[2], y);
			t.draw(screen.batch, String.valueOf(match.tricksWon(seat)), px + cols[3], y);
			t.draw(screen.batch, (pts > 0 ? "+" : "") + pts, px + cols[4], y);
			t.setColor(place(seat) == 1 ? Palette.GOLD : Palette.CREAM);
			t.draw(screen.batch, String.valueOf(match.score(seat)), px + cols[5], y);
		}
		BitmapFont s = screen.ui(1f);
		s.setColor(Palette.GOLD);
		screen.text(s, "Tap to continue", table.w / 2, py + 26, true);
	}

	private static final float[] RESULT_COLS = { 22, 62, 185, 245, 310, 395 };
	private static final String[] RESULT_HEAD = { "", "Player", "Bid", "Won", "Points", "Total" };

	// ---- the end of the game

	/** Final scores; newHighScore is where your game landed in the high score table, or -1. */
	void drawGameOver(int newHighScore) {
		float pw = 420, ph = 306;
		float px = (table.w - pw) / 2, py = table.h / 2 - 6 - ph / 2; // clear of the top panel
		screen.frame(screen.assets.panel, px, py, pw, ph);
		art.art(screen.assets.gameOverText, table.w / 2, py + ph - 50, 0.66f, 1f);

		List<Integer> leaders = match.leaders();
		sb.setLength(0);
		for (int i = 0; i < leaders.size(); i++) {
			if (i > 0) {
				sb.append(i == leaders.size() - 1 ? " and " : ", ");
			}
			sb.append(PlayScreen.NAMES[leaders.get(i)]);
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
			t.draw(screen.batch, PlayScreen.NAMES[s], px + 120, y);
			screen.text(t, String.valueOf(match.score(s)), px + pw - 140, y, false);
			if (s == TableLayout.HUMAN && newHighScore >= 0) {
				BitmapFont n = screen.ui(0.95f);
				n.setColor(Palette.GOLD_LIGHT);
				n.draw(screen.batch, newHighScore == 0 ? "New best!" : "High score!", px + pw - 128, y - 2);
			}
			y -= 27;
		}
		float bw = 170, bh = 42, by = py + 16;
		screen.button(playAgainButton, "Play again", table.w / 2 - bw / 2 - 8, by, bw, bh, screen.assets.button);
		screen.button(highScoresButton, "High scores", table.w / 2 + bw / 2 + 8, by, bw, bh, screen.assets.buttonAlt);
	}

	// ---- standings

	/** Seats from highest score to lowest; ties keep seat order. (Reused list.) */
	List<Integer> ranked() {
		ranked.clear();
		for (int s = 0; s < 4; s++) {
			ranked.add(s); // small Integers are cached: no garbage
		}
		Collections.sort(ranked, byScore);
		return ranked;
	}

	/** 1 for the lead; players on the same score share a place. */
	int place(int seat) {
		int p = 1;
		for (int s = 0; s < 4; s++) {
			if (match.score(s) > match.score(seat)) {
				p++;
			}
		}
		return p;
	}
}
