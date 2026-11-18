package ohheck;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Stats;

/** Your ten best finished games, and (on its second tab) your statistics, by difficulty or for all. */
public class HighScoresScreen extends BaseScreen {

	private final Rectangle backButton = new Rectangle(), scoresTab = new Rectangle(), statsTab = new Rectangle(),
			resetButton = new Rectangle();
	private final Rectangle[] filterButtons = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };
	/** Showing the statistics (not the high scores); which totals (a difficulty, or Stats.ALL). */
	private boolean showStats;
	private int filter = Stats.ALL;
	/** Seconds left to tap Reset again to confirm, or 0. */
	private float confirmReset;
	private final Color highlight = new Color();
	private List<HighScores.Entry> list;
	private int newest = -1;

	public HighScoresScreen(OhHeckGame game) {
		super(game);
	}

	/** Picks out the entry at this position (just set), or -1 for none. */
	void highlight(int position) {
		newest = position;
	}

	@Override
	public void show() {
		super.show();
		list = game.highScores().entries(); // read once, not every frame
		showStats = false;
		confirmReset = 0;
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f);
		shownFor += dt * game.options.speed;
		if (game.options.autoplayHuman && shownFor > 1.5f) {
			if (shownFor > 3f) {
				game.showOptions();
				return;
			}
			showStats = true; // the screenshot run looks at both tabs
		}
		confirmReset = Math.max(0, confirmReset - dt);
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		BitmapFont t = serif(2.2f);
		t.setColor(GOLD);
		centred(t, showStats ? "Statistics" : "High scores", w / 2, h - 40);
		button(scoresTab, "Scores", w - 220, h - 52, 130, 40, showStats ? assets.buttonAlt : assets.button);
		button(statsTab, "Statistics", w - 82, h - 52, 130, 40, showStats ? assets.button : assets.buttonAlt);
		button(backButton, "Back", 70, h - 52, 110, 40, assets.buttonAlt);
		if (showStats) {
			drawStats();
		} else {
			drawScores();
		}
		batch.end();
	}

	private void drawScores() {
		float pw = 480, rowH = 30, ph = 66 + Math.max(1, list.size()) * rowH;
		float px = (w - pw) / 2, top = h - 100;
		frame(assets.panel, px, top - ph, pw, ph);

		BitmapFont f = ui(1.15f);
		float[] cols = { px + 36, px + 86, px + 190, px + 300, px + pw - 30 };
		float badge = 24;
		float y = top - 18;
		if (list.isEmpty()) {
			f.setColor(Palette.MUTED);
			centred(f, "Finish a game to set your first high score", w / 2, y - 22);
		} else {
			f.setColor(Palette.MUTED);
			text(f, "#", cols[0], y, true);
			text(f, "Level", cols[1], y, true);
			text(f, "Score", cols[2], y, true);
			text(f, "Finished", cols[3], y, true);
			text(f, "Date", cols[4], y, false);
			y -= 40;
			float pulse = 0.5f + 0.5f * MathUtils.sin(shownFor * 3f);
			for (int i = 0; i < list.size(); i++) {
				HighScores.Entry e = list.get(i);
				if (i == newest) {
					rect(px + 12, y - rowH + 8, pw - 24, rowH, highlight.set(0.97f, 0.85f, 0.56f, 0.10f + 0.08f * pulse));
				}
				Color c = i == newest ? Palette.GOLD_LIGHT : i == 0 ? GOLD : Palette.CREAM;
				f.setColor(c);
				text(f, String.valueOf(i + 1), cols[0], y, true);
				if (e.level >= 0 && e.level < assets.levelBadge.length) {
					batch.draw(assets.levelBadge[e.level], cols[1] - badge / 2, y - 16, badge, badge);
				}
				text(f, String.valueOf(e.score), cols[2], y, true);
				f.setColor(e.place == 1 ? GOLD : c);
				text(f, e.place == 1 ? "Won" : HighScores.ordinal(e.place), cols[3], y, true);
				f.setColor(i == newest ? Palette.GOLD_LIGHT : Palette.MUTED);
				text(f, e.date, cols[4], y, false);
				y -= rowH;
			}
		}
	}

	// ---- statistics

	private static final String[] FILTERS = { "Easy", "Normal", "Hard", "All" };
	private static final float STATS_PANEL_W = 430, STATS_GAP = 30, STAT_ROW = 33;

	private void drawStats() {
		// the filter: one button per difficulty (with its badge), then All
		float fw = 132, fh = 40, fy = h - 116;
		float fx = w / 2 - (4 * fw + 3 * 12) / 2;
		int[] order = { Stats.ALL, 0, 1, 2 };
		for (int i = 0; i < order.length; i++) {
			int which = order[i];
			float cx = fx + i * (fw + 12) + fw / 2;
			button(filterButtons[which], FILTERS[which], cx, fy, fw, fh,
					which == filter ? assets.button : assets.buttonAlt);
			if (which != Stats.ALL) {
				batch.draw(assets.levelBadge[which], cx - fw / 2 + 8, fy + 8, 24, 24);
			}
		}

		Stats.Totals t = game.stats().totals(filter);
		String none = "\u2014";
		boolean g = t.anyGames(), hd = t.anyHands();
		String[][] games = {
				{ "Games played", String.valueOf(t.games) },
				{ "Games won", g ? t.wins + "  (" + percent(t.winRate()) + ")" : none },
				{ "Winning streak", g ? t.streak + "  (best " + t.bestStreak + ")" : none },
				{ "Average score", g ? String.valueOf(Math.round(t.averageFinal())) : none },
				{ "Best / worst score", g ? t.bestFinal + " / " + t.worstFinal : none },
				{ "Perfect games", g ? String.valueOf(t.perfect) : none },
				{ "Nemesis", t.nemesis() > 0 ? PlayScreen.NAMES[t.nemesis()] : none } };
		int misses = t.over + t.under;
		String[][] hands = {
				{ "Hands played", String.valueOf(t.hands) },
				{ "Bids made", hd ? percent(t.madeRate()) + "  (" + t.made + ")" : none },
				{ "Zero bids made", t.zeroBids > 0 ? t.zeroMade + " of " + t.zeroBids : none },
				{ "Misses: over / under", misses > 0 ? t.over + " / " + t.under : none },
				{ "Tricks won", hd ? t.tricks + "  (" + String.format(java.util.Locale.US, "%.1f", t.tricksPerHand()) + " a hand)" : none },
				{ "Best hand", hd ? t.bestHand + " points" : none },
				{ "Worst hand", hd ? t.worstHand + " points" : none } };
		float top = fy - 18, x0 = w / 2 - STATS_PANEL_W - STATS_GAP / 2;
		statPanel("Games", games, x0, top);
		statPanel("Hands", hands, x0 + STATS_PANEL_W + STATS_GAP, top);

		button(resetButton, confirmReset > 0 ? "Tap again to reset" : "Reset statistics", w / 2, 18, 260, 40,
				confirmReset > 0 ? assets.button : assets.buttonAlt);
	}

	private void statPanel(String title, String[][] rows, float x, float top) {
		float ph = 52 + rows.length * STAT_ROW;
		frame(assets.panel, x, top - ph, STATS_PANEL_W, ph);
		BitmapFont head = serif(1.5f);
		head.setColor(GOLD);
		head.draw(batch, title, x + 22, top - 12);
		BitmapFont f = ui(1.15f);
		float y = top - 50;
		for (String[] row : rows) {
			f.setColor(Palette.CREAM);
			f.draw(batch, row[0], x + 22, y);
			f.setColor(Palette.GOLD_LIGHT);
			text(f, row[1], x + STATS_PANEL_W - 22, y, false); // right-aligned
			y -= STAT_ROW;
		}
	}

	private static String percent(double share) {
		return Math.round(share * 100) + "%";
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
		} else if (scoresTab.contains(x, y)) {
			showStats = false;
		} else if (statsTab.contains(x, y)) {
			showStats = true;
		} else if (showStats) {
			for (int i = 0; i < filterButtons.length; i++) {
				if (filterButtons[i].contains(x, y)) {
					filter = i;
				}
			}
			if (resetButton.contains(x, y)) {
				if (confirmReset > 0) {
					game.resetStats();
					confirmReset = 0;
				} else {
					confirmReset = RESET_CONFIRM_TIME;
				}
			}
		}
	}

	/** Seconds the reset button waits for its second tap. */
	private static final float RESET_CONFIRM_TIME = 3f;

	@Override
	protected void back() {
		newest = -1;
		game.back();
	}

	@Override
	public String stateTag() {
		return showStats ? "statistics" : "highscores";
	}
}
