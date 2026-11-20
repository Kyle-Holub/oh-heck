package ohheck.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
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
import ohheck.ui.MenuChrome;
import ohheck.ui.Numbers;
import ohheck.ui.Palette;
import ohheck.ui.Scroller;
import ohheck.ui.Wording;

/*
 * Your records, a page at a time: the high scores, or the statistics for every game or one
 * difficulty. Tabs down the left, the chosen page in a panel on the right that scrolls.
 */
public class HighScoresScreen extends BaseScreen implements TourStop {

	private static final float MAX_W = 940, MARGIN = 24, GAP = 18, TABS_W = 210, TAB_H = 50, TAB_GAP = 8;
	private static final float GROUP_LABEL_H = 30, BADGE = 24, BADGE_INSET = 14, PAD = 22, SCROLLBAR_W = 4, FRAME = 6;
	private static final float SCORE_ROW = 32, SCORE_HEADS_H = 40, STAT_ROW = 42, SECTION_H = 50, RESET_ROOM = 76;
	private static final float RESET_W = 260, RESET_H = 42, RESET_CONFIRM_TIME = 3f;
	private static final float[] SCORE_COLS = { 0.07f, 0.2f, 0.38f, 0.58f };
	private static final Color DIVIDER = new Color(0.68f, 0.6f, 0.49f, 0.25f), SCROLLBAR = new Color(0.89f, 0.72f, 0.37f, 0.5f);
	private static final int SCORES = 0;

	private final MenuChrome chrome = new MenuChrome(paint);
	private final Rectangle backButton = new Rectangle(), page = new Rectangle(), inside = new Rectangle(),
			resetButton = new Rectangle();
	private final List<Rectangle> tabs = new ArrayList<Rectangle>();
	private final Scroller scroller = new Scroller();
	private final Color highlight = new Color();
	private final int[] statsOf;
	private int current = SCORES, rowsFor = -1;
	private String[][] games, hands;
	private float confirmReset;
	private List<HighScores.Entry> list;
	private int newest = -1;

	public HighScoresScreen(GameHost game) {
		super(game);
		statsOf = new int[Stats.ALL + 2];
		statsOf[1] = Stats.ALL;
		for (Difficulty d : Difficulty.values()) {
			statsOf[d.ordinal() + 2] = d.ordinal();
		}
		for (int i = 0; i < statsOf.length; i++) {
			tabs.add(new Rectangle());
		}
	}

	public void highlight(int position) {
		newest = position;
	}

	@Override
	public ScreenshotTour.Stop tourStop() {
		return ScreenshotTour.Stop.HIGH_SCORES;
	}

	@Override
	public void secondHalf(boolean on) {
		if (on && current == SCORES) {
			choose(1);
		}
	}

	@Override
	public void show() {
		super.show();
		list = game.records().highScores().entries();
		rowsFor = -1;
		confirmReset = 0;
		choose(SCORES);
	}

	private void choose(int tab) {
		current = tab;
		scroller.reset();
		confirmReset = 0;
	}

	private boolean onStats() {
		return current != SCORES;
	}

	@Override
	protected void drag(float dy) {
		scroller.drag(dy);
	}

	@Override
	protected void update(float step) {
		shownFor += step * game.options().speed;
		confirmReset = Math.max(0, confirmReset - step);
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		float width = Math.min(MAX_W, w - 2 * MARGIN), left = (w - width) / 2;
		float top = h - MenuChrome.HEADER_H - GAP, bottom = MARGIN;
		drawTabs(left, top);
		page.set(left + TABS_W + GAP, bottom, width - TABS_W - GAP, top - bottom);
		paint.frame(assets.panel, page.x, page.y, page.width, page.height);
		float content = onStats() ? statsHeight() : scoresHeight();
		scroller.update(frameDelta(), Gdx.input.isTouched(), content - page.height);
		inside.set(page.x + FRAME, page.y + FRAME, page.width - 2 * FRAME, page.height - 2 * FRAME);
		if (clipTo(inside)) {
			if (onStats()) {
				drawStats(page.y + page.height + scroller.offset());
			} else {
				drawScores(page.y + page.height + scroller.offset());
			}
			unclip();
		}
		drawScrollbar(content);
		chrome.header("High scores", backButton, w, h);
		batch.end();
	}

	private void drawTabs(float left, float top) {
		float y = top;
		for (int i = 0; i < tabs.size(); i++) {
			if (i == 1) {
				paint.ui(0.95f, Palette.MUTED).draw(batch, "Statistics", left + 6, y - 8);
				y -= GROUP_LABEL_H;
			}
			Rectangle tab = tabs.get(i).set(left, y - TAB_H, TABS_W, TAB_H);
			boolean chosen = i == current;
			paint.frame(chosen ? assets.button : assets.buttonAlt, tab.x, tab.y, tab.width, tab.height);
			BitmapFont f = paint.serif(1.3f, chosen ? Palette.CREAM : Palette.MUTED);
			boolean level = i > 1;
			if (level) {
				batch.draw(assets.levelBadge[statsOf[i]], tab.x + BADGE_INSET, tab.y + (TAB_H - BADGE) / 2, BADGE, BADGE);
			}
			paint.textIn(f, tabName(i), tab.x, tab.y, tab.width, tab.height);
			y -= TAB_H + TAB_GAP;
		}
	}

	private String tabName(int tab) {
		if (tab == SCORES) {
			return "High scores";
		}
		return statsOf[tab] == Stats.ALL ? "All games" : Difficulty.values()[statsOf[tab]].label;
	}

	private float scoresHeight() {
		return SCORE_HEADS_H + Math.max(1, list.size()) * SCORE_ROW + PAD;
	}

	private float column(int c) {
		return page.x + page.width * SCORE_COLS[c];
	}

	private void drawScores(float top) {
		BitmapFont f = paint.ui(1.15f, Palette.MUTED);
		float y = top - PAD + 4;
		if (list.isEmpty()) {
			paint.centred(f, "Finish a game to set your first high score", page.x + page.width / 2, y - 6);
			return;
		}
		paint.text(f, "#", column(0), y, true);
		paint.text(f, "Level", column(1), y, true);
		paint.text(f, "Score", column(2), y, true);
		paint.text(f, "Finished", column(3), y, true);
		paint.text(f, "Date", page.x + page.width - PAD, y, false);
		y = top - SCORE_HEADS_H;
		float pulse = 0.5f + 0.5f * MathUtils.sin(shownFor * 3f);
		for (int i = 0; i < list.size(); i++) {
			if (i == newest) {
				paint.rect(page.x + PAD / 2, y - SCORE_ROW, page.width - PAD, SCORE_ROW,
						highlight.set(0.97f, 0.85f, 0.56f, 0.10f + 0.08f * pulse));
			}
			drawScoreRow(i, list.get(i), y - 7);
			y -= SCORE_ROW;
		}
	}

	private void drawScoreRow(int i, HighScores.Entry e, float y) {
		Color c = i == newest ? Palette.GOLD_LIGHT : i == 0 ? Palette.GOLD : Palette.CREAM;
		BitmapFont f = paint.ui(1.15f, c);
		paint.text(f, Numbers.text(i + 1), column(0), y, true);
		if (e.level() != null) {
			batch.draw(assets.levelBadge[e.level().ordinal()], column(1) - BADGE / 2, y - 18, BADGE, BADGE);
		}
		paint.text(f, Numbers.text(e.score()), column(2), y, true);
		f.setColor(e.place() == 1 ? Palette.GOLD : c);
		paint.text(f, e.place() == 1 ? "Won" : Wording.ordinal(e.place()), column(3), y, true);
		f.setColor(i == newest ? Palette.GOLD_LIGHT : Palette.MUTED);
		paint.text(f, e.date(), page.x + page.width - PAD, y, false);
	}

	private void statRows() {
		int which = statsOf[current];
		if (rowsFor != which) {
			Stats.Totals t = game.records().stats().totals(which);
			games = Wording.gameRows(t);
			hands = Wording.handRows(t);
			rowsFor = which;
		}
	}

	private float statsHeight() {
		statRows();
		return 2 * SECTION_H + (games.length + hands.length) * STAT_ROW + RESET_ROOM;
	}

	private void drawStats(float top) {
		statRows();
		float y = drawSection("Games", games, top);
		y = drawSection("Hands", hands, y);
		paint.button(resetButton, confirmReset > 0 ? "Tap again to reset" : "Reset statistics", page.x + page.width / 2,
				y - RESET_ROOM + (RESET_ROOM - RESET_H) / 2, RESET_W, RESET_H, confirmReset > 0 ? assets.button : assets.buttonAlt);
	}

	private float drawSection(String title, String[][] rows, float top) {
		paint.serif(1.4f, Palette.GOLD).draw(batch, title, page.x + PAD, top - 14);
		float y = top - SECTION_H;
		BitmapFont f = paint.ui(1.2f);
		for (int i = 0; i < rows.length; i++) {
			float text = y - (STAT_ROW - 18) / 2;
			f.setColor(Palette.CREAM);
			f.draw(batch, rows[i][0], page.x + PAD, text);
			f.setColor(Palette.GOLD_LIGHT);
			paint.text(f, rows[i][1], page.x + page.width - PAD, text, false);
			y -= STAT_ROW;
			if (i < rows.length - 1) {
				paint.rect(page.x + PAD, y, page.width - 2 * PAD, 1, DIVIDER);
			}
		}
		return y;
	}

	private void drawScrollbar(float content) {
		if (content <= page.height) {
			return;
		}
		float track = page.height - 2 * PAD, bar = Math.max(30, track * page.height / content);
		float travel = content - page.height, at = travel <= 0 ? 0 : scroller.offset() / travel;
		paint.rect(page.x + page.width - PAD / 2 - SCROLLBAR_W, page.y + PAD + (track - bar) * (1 - at), SCROLLBAR_W, bar,
				SCROLLBAR);
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(x, y)) {
				if (i != current) {
					choose(i);
				}
				return;
			}
		}
		if (onStats() && page.contains(x, y) && resetButton.contains(x, y)) {
			if (confirmReset > 0) {
				game.records().resetStats();
				rowsFor = -1;
				confirmReset = 0;
			} else {
				confirmReset = RESET_CONFIRM_TIME;
			}
		}
	}

	@Override
	protected void back() {
		newest = -1;
		game.back();
	}

	@Override
	public String stateTag() {
		return onStats() ? "statistics" : "highscores";
	}
}
