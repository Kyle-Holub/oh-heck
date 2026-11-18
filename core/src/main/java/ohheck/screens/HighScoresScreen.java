package ohheck.screens;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

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
	private final Rectangle[] filterButtons = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };
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
		centred(t, showStats ? "Statistics" : "High scores", w / 2, h - 40);
		button(scoresTab, "Scores", w - 250, h - 52, 130, 40, showStats ? assets.buttonAlt : assets.button);
		button(statsTab, "Statistics", w - 98, h - 52, 160, 40, showStats ? assets.button : assets.buttonAlt);
		backButton(backButton, h - 52);
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
		float y = top - 18;
		if (list.isEmpty()) {
			f.setColor(Palette.MUTED);
			centred(f, "Finish a game to set your first high score", w / 2, y - 22);
			return;
		}
		scoreCols[0] = px + 36;
		scoreCols[1] = px + 86;
		scoreCols[2] = px + 190;
		scoreCols[3] = px + 300;
		scoreCols[4] = px + pw - 30;
		drawScoreHeads(f, y);
		y -= 40;
		float pulse = 0.5f + 0.5f * MathUtils.sin(shownFor * 3f);
		for (int i = 0; i < list.size(); i++) {
			if (i == newest) { // the game just finished, picked out
				rect(px + 12, y - rowH + 8, pw - 24, rowH, highlight.set(0.97f, 0.85f, 0.56f, 0.10f + 0.08f * pulse));
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
		float badge = 24;
		Color c = i == newest ? Palette.GOLD_LIGHT : i == 0 ? GOLD : Palette.CREAM;
		f.setColor(c);
		text(f, String.valueOf(i + 1), cols[0], y, true);
		if (e.level() >= 0 && e.level() < assets.levelBadge.length) {
			batch.draw(assets.levelBadge[e.level()], cols[1] - badge / 2, y - 16, badge, badge);
		}
		text(f, String.valueOf(e.score()), cols[2], y, true);
		f.setColor(e.place() == 1 ? GOLD : c);
		text(f, e.place() == 1 ? "Won" : Wording.ordinal(e.place()), cols[3], y, true);
		f.setColor(i == newest ? Palette.GOLD_LIGHT : Palette.MUTED);
		text(f, e.date(), cols[4], y, false);
	}


	// ---- statistics

	private static final String[] FILTERS = { "Easy", "Normal", "Hard", "All" };
	private static final float STATS_PANEL_W = 430, STATS_GAP = 30, STAT_ROW = 33;

	private void drawStats() {
		// the filter: one button per difficulty (with its badge), then All
		float fw = 152, fh = 40, fy = h - 116;
		float fx = w / 2 - (4 * fw + 3 * 12) / 2;
		int[] order = { Stats.ALL, 0, 1, 2 };
		for (int i = 0; i < order.length; i++) {
			int which = order[i];
			float cx = fx + i * (fw + 12) + fw / 2;
			boolean badge = which != Stats.ALL;
			button(filterButtons[which], "", cx, fy, fw, fh, which == filter ? assets.button : assets.buttonAlt);
			BitmapFont f = serif(1.5f);
			f.setColor(Palette.CREAM);
			textIn(f, FILTERS[which], cx - fw / 2 + (badge ? 28 : 0), fy, fw - (badge ? 28 : 0), fh);
			if (badge) { // its level badge, left of the name
				batch.draw(assets.levelBadge[which], cx - fw / 2 + 10, fy + 8, 24, 24);
			}
		}

		Stats.Totals t = game.records().stats().totals(filter);
		String[][] games = Wording.gameRows(t), hands = Wording.handRows(t);
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
