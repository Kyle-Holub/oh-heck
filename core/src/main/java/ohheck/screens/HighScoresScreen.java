package ohheck.screens;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.players.Difficulty;
import ohheck.records.HighScores;
import ohheck.records.Stats;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/** Your ten best finished games, and (on its second tab) your statistics, by difficulty or for all. */
public class HighScoresScreen extends BaseScreen {

	private final Rectangle backButton = new Rectangle(), scoresTab = new Rectangle(), statsTab = new Rectangle(),
			resetButton = new Rectangle();
	/** One per difficulty, then All (indexed like Stats' totals). */
	private final Rectangle[] filterButtons = new Rectangle[Stats.ALL + 1];
	{
		for (int i = 0; i < filterButtons.length; i++) {
			filterButtons[i] = new Rectangle();
		}
	}
	/** Showing the statistics (not the high scores); which totals (a difficulty, or Stats.ALL). */
	private boolean showStats;

	/** The Statistics tab (for the screenshot run, which looks at both). */
	void showStatistics() {
		showStats = true;
	}
	private int filter = Stats.ALL;
	/** Seconds left to tap Reset again to confirm, or 0. */
	private float confirmReset;
	private final Color highlight = new Color();
	private List<HighScores.Entry> list;
	private int newest = -1;

	public HighScoresScreen(GameHost game) {
		super(game);
	}

	/** Picks out the entry at this position (just set), or -1 for none. */
	public void highlight(int position) {
		newest = position;
	}

	@Override
	public void show() {
		super.show();
		list = game.records().highScores().entries(); // read once, not every frame
		showStats = false;
		confirmReset = 0;
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f);
		shownFor += dt * game.options().speed;
		if (game.tour() != null && game.tour().step(this, shownFor)) {
			return;
		}
		confirmReset = Math.max(0, confirmReset - dt);
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		BitmapFont t = serif(2.2f);
		t.setColor(GOLD);
		centred(t, showStats ? "Statistics" : "High scores", w / 2, h - TITLE_DROP);
		button(scoresTab, "Scores", w - SCORES_TAB_FROM_RIGHT, h - TABS_DROP, SCORES_TAB_W, TAB_H,
				showStats ? assets.buttonAlt : assets.button);
		button(statsTab, "Statistics", w - STATS_TAB_FROM_RIGHT, h - TABS_DROP, STATS_TAB_W, TAB_H,
				showStats ? assets.button : assets.buttonAlt);
		backButton(backButton, h - TABS_DROP);
		if (showStats) {
			drawStats();
		} else {
			drawScores();
		}
		batch.end();
	}

	// the title and tabs, from the top
	private static final float TITLE_DROP = 40, TABS_DROP = 52, TAB_H = 40;
	private static final float SCORES_TAB_FROM_RIGHT = 250, SCORES_TAB_W = 130, STATS_TAB_FROM_RIGHT = 98, STATS_TAB_W = 160;
	// the score table: its panel, rows, and columns (rank, level, score, place from the left; date from the right)
	private static final float SCORES_W = 480, SCORES_TOP_DROP = 100, SCORES_PAD = 66, SCORE_ROW = 30;
	private static final float SCORE_HEADS_DROP = 18, EMPTY_NOTE_BELOW = 22, FIRST_ROW_BELOW_HEADS = 40;
	private static final float[] SCORE_COLS = { 36, 86, 190, 300 };
	private static final float DATE_FROM_RIGHT = 30, ROW_HIGHLIGHT_INSET = 12, ROW_HIGHLIGHT_RISE = 8;
	private static final float SCORE_BADGE = 24, SCORE_BADGE_DROP = 16;

	private void drawScores() {
		float pw = SCORES_W, rowH = SCORE_ROW, ph = SCORES_PAD + Math.max(1, list.size()) * rowH;
		float px = (w - pw) / 2, top = h - SCORES_TOP_DROP;
		frame(assets.panel, px, top - ph, pw, ph);
		BitmapFont f = ui(1.15f);
		float y = top - SCORE_HEADS_DROP;
		if (list.isEmpty()) {
			f.setColor(Palette.MUTED);
			centred(f, "Finish a game to set your first high score", w / 2, y - EMPTY_NOTE_BELOW);
			return;
		}
		for (int c = 0; c < SCORE_COLS.length; c++) {
			scoreCols[c] = px + SCORE_COLS[c];
		}
		scoreCols[4] = px + pw - DATE_FROM_RIGHT;
		drawScoreHeads(f, y);
		y -= FIRST_ROW_BELOW_HEADS;
		float pulse = 0.5f + 0.5f * MathUtils.sin(shownFor * 3f);
		for (int i = 0; i < list.size(); i++) {
			if (i == newest) { // the game just finished, picked out
				rect(px + ROW_HIGHLIGHT_INSET, y - rowH + ROW_HIGHLIGHT_RISE, pw - 2 * ROW_HIGHLIGHT_INSET, rowH,
						highlight.set(0.97f, 0.85f, 0.56f, 0.10f + 0.08f * pulse));
			}
			drawScoreRow(f, i, list.get(i), y);
			y -= rowH;
		}
	}

	/** The score table's columns: rank, level, score, place, date. */
	private final float[] scoreCols = new float[5];

	private void drawScoreHeads(BitmapFont f, float y) {
		float[] cols = scoreCols;
		f.setColor(Palette.MUTED);
		text(f, "#", cols[0], y, true);
		text(f, "Level", cols[1], y, true);
		text(f, "Score", cols[2], y, true);
		text(f, "Finished", cols[3], y, true);
		text(f, "Date", cols[4], y, false);
	}

	/** Row i of the table: the newest in light gold, the best in gold. */
	private void drawScoreRow(BitmapFont f, int i, HighScores.Entry e, float y) {
		float[] cols = scoreCols;
		float badge = SCORE_BADGE;
		Color c = i == newest ? Palette.GOLD_LIGHT : i == 0 ? GOLD : Palette.CREAM;
		f.setColor(c);
		text(f, String.valueOf(i + 1), cols[0], y, true);
		if (e.level() >= 0 && e.level() < assets.levelBadge.length) {
			batch.draw(assets.levelBadge[e.level()], cols[1] - badge / 2, y - SCORE_BADGE_DROP, badge, badge);
		}
		text(f, String.valueOf(e.score()), cols[2], y, true);
		f.setColor(e.place() == 1 ? GOLD : c);
		text(f, e.place() == 1 ? "Won" : Wording.ordinal(e.place()), cols[3], y, true);
		f.setColor(i == newest ? Palette.GOLD_LIGHT : Palette.MUTED);
		text(f, e.date(), cols[4], y, false);
	}


	// ---- statistics

	// the statistics: the filter buttons (a badge left of each level's name), the two panels, Reset
	private static final float FILTER_W = 152, FILTER_H = 40, FILTERS_DROP = 116, FILTER_GAP = 12;
	private static final float FILTER_BADGE = 24, FILTER_BADGE_X = 10, FILTER_BADGE_Y = 8, FILTER_BADGE_SPACE = 28;
	private static final float STATS_PANEL_W = 430, STATS_GAP = 30, STAT_ROW = 33, PANELS_BELOW_FILTERS = 18;
	private static final float STAT_PANEL_PAD = 52, STAT_TITLE_DROP = 12, STAT_ROWS_DROP = 50, STAT_INSET = 22;
	private static final float RESET_Y = 18, RESET_W = 260, RESET_H = 40;

	/** A filter's label: the level's name, or "All". */
	private static String filterLabel(int which) {
		return which == Stats.ALL ? "All" : Difficulty.values()[which].label;
	}

	private void drawStats() {
		// the filter: one button per difficulty (with its badge), then All
		float fw = FILTER_W, fh = FILTER_H, fy = h - FILTERS_DROP;
		int n = filterButtons.length;
		float fx = w / 2 - (n * fw + (n - 1) * FILTER_GAP) / 2;
		for (int i = 0; i < n; i++) {
			int which = i == 0 ? Stats.ALL : i - 1; // All first, then each level
			float cx = fx + i * (fw + FILTER_GAP) + fw / 2;
			boolean badge = which != Stats.ALL;
			float space = badge ? FILTER_BADGE_SPACE : 0;
			button(filterButtons[which], "", cx, fy, fw, fh, which == filter ? assets.button : assets.buttonAlt);
			BitmapFont f = serif(1.5f);
			f.setColor(Palette.CREAM);
			textIn(f, filterLabel(which), cx - fw / 2 + space, fy, fw - space, fh);
			if (badge) { // its level badge, left of the name
				batch.draw(assets.levelBadge[which], cx - fw / 2 + FILTER_BADGE_X, fy + FILTER_BADGE_Y, FILTER_BADGE, FILTER_BADGE);
			}
		}

		Stats.Totals t = game.records().stats().totals(filter);
		String[][] games = Wording.gameRows(t), hands = Wording.handRows(t);
		float top = fy - PANELS_BELOW_FILTERS, x0 = w / 2 - STATS_PANEL_W - STATS_GAP / 2;
		statPanel("Games", games, x0, top);
		statPanel("Hands", hands, x0 + STATS_PANEL_W + STATS_GAP, top);

		button(resetButton, confirmReset > 0 ? "Tap again to reset" : "Reset statistics", w / 2, RESET_Y, RESET_W, RESET_H,
				confirmReset > 0 ? assets.button : assets.buttonAlt);
	}

	private void statPanel(String title, String[][] rows, float x, float top) {
		float ph = STAT_PANEL_PAD + rows.length * STAT_ROW;
		frame(assets.panel, x, top - ph, STATS_PANEL_W, ph);
		BitmapFont head = serif(1.5f);
		head.setColor(GOLD);
		head.draw(batch, title, x + STAT_INSET, top - STAT_TITLE_DROP);
		BitmapFont f = ui(1.15f);
		float y = top - STAT_ROWS_DROP;
		for (String[] row : rows) {
			f.setColor(Palette.CREAM);
			f.draw(batch, row[0], x + STAT_INSET, y);
			f.setColor(Palette.GOLD_LIGHT);
			text(f, row[1], x + STATS_PANEL_W - STAT_INSET, y, false); // right-aligned
			y -= STAT_ROW;
		}
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
					game.records().resetStats();
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
