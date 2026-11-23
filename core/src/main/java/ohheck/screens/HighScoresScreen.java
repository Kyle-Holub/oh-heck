package ohheck.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.campaign.Badge;
import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.players.Difficulty;
import ohheck.records.HighScores;
import ohheck.records.Stats;
import ohheck.rules.GameMode;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Numbers;
import ohheck.ui.Palette;
import ohheck.ui.Scroller;
import ohheck.ui.SideTabs;
import ohheck.ui.Wording;

/*
 * Your records: the high scores, the statistics or the badges. A switch at the top of the panel
 * picks which; the difficulty tabs down the left filter the scores and statistics. High scores are
 * kept per game mode, picked by a row of buttons under the switch.
 */
public class HighScoresScreen extends BaseScreen {

	private static final float BADGE = 24, PAD = 22, FRAME = 6;
	private static final float BADGE_ROW = 56, SWITCH_H = 40, SWITCH_ROOM = 58, MODE_H = 34, MODE_ROOM = 44, MODE_GAP = 8;
	private static final float SCORE_ROW = 32, SCORE_HEADS_H = 34, STAT_ROW = 42, SECTION_H = 50;
	private static final float RESET_W = 230, RESET_CONFIRM_TIME = 3f;
	private static final float[] SCORE_COLS = { 0.07f, 0.2f, 0.38f, 0.58f };
	private static final Color DIVIDER = new Color(0.68f, 0.6f, 0.49f, 0.25f);

	private final MenuChrome chrome = new MenuChrome(paint);
	private final Rectangle backButton = new Rectangle(), resetButton = new Rectangle(), page = new Rectangle(),
			inside = new Rectangle();
	private final Rectangle[] switches = { new Rectangle(), new Rectangle(), new Rectangle() };
	private final SideTabs filters;
	private final int[] filterOf = new int[Stats.ALL + 1];
	private final Scroller scroller = new Scroller();
	private final Color highlight = new Color();
	private final List<HighScores.Entry> shown = new ArrayList<HighScores.Entry>();
	private final Rectangle[] modeButtons = new Rectangle[GameMode.values().length];
	private GameMode mode = GameMode.CLASSIC;
	private enum View {
		SCORES("High scores", "highscores"), STATS("Statistics", "statistics"), BADGES("Badges", "badges");

		final String label, tag;

		View(String label, String tag) {
			this.label = label;
			this.tag = tag;
		}
	}

	private View view = View.SCORES;
	private int filter = Stats.ALL, rowsFor = -1, scoresFor = -1, newestAt = -1;
	private String[][] games, hands;
	private float confirmReset;
	private List<HighScores.Entry> list;
	private HighScores.Entry newest;

	public HighScoresScreen(GameHost game) {
		super(game);
		filterOf[0] = Stats.ALL;
		for (Difficulty d : Difficulty.values()) {
			filterOf[d.ordinal() + 1] = d.ordinal();
		}
		String[] names = new String[filterOf.length];
		Texture[] badges = new Texture[filterOf.length];
		for (int i = 0; i < filterOf.length; i++) {
			names[i] = filterName(filterOf[i]);
			badges[i] = filterOf[i] == Stats.ALL ? null : assets.levelBadge[filterOf[i]];
		}
		filters = new SideTabs(paint, "Difficulty", names, badges);
		for (int i = 0; i < modeButtons.length; i++) {
			modeButtons[i] = new Rectangle();
		}
	}

	public void highlight(int position) {
		newestAt = position;
	}

	public void showStatistics() {
		view(View.STATS);
	}

	public void showBadges() {
		view(View.BADGES);
	}

	@Override
	public void show() {
		super.show();
		list = game.records().highScores().entries();
		newest = newestAt >= 0 && newestAt < list.size() ? list.get(newestAt) : null;
		mode = newest != null ? newest.mode() : game.settings().mode();
		rowsFor = -1;
		scoresFor = -1;
		confirmReset = 0;
		filter = Stats.ALL;
		filters.choose(0);
		view(View.SCORES);
	}

	private void view(View v) {
		view = v;
		scroller.reset();
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
		filters.draw(w, h, page);
		paint.frame(assets.panel, page.x, page.y, page.width, page.height);
		drawSwitch();
		float top = SWITCH_ROOM;
		if (view == View.SCORES) {
			drawModes(page.y + page.height - SWITCH_ROOM - MODE_H);
			top += MODE_ROOM;
		}
		inside.set(page.x + FRAME, page.y + FRAME, page.width - 2 * FRAME, page.height - top - FRAME);
		float content = view == View.STATS ? statsHeight() : view == View.BADGES ? badgesHeight() : scoresHeight();
		scroller.update(frameDelta(), Gdx.input.isTouched(), content - inside.height);
		if (clipTo(inside)) {
			float listTop = inside.y + inside.height + scroller.offset();
			if (view == View.STATS) {
				drawStats(listTop);
			} else if (view == View.BADGES) {
				drawBadges(listTop);
			} else {
				drawScores(listTop);
			}
			unclip();
		}
		scroller.drawBar(paint, inside, content);
		chrome.header("High scores", backButton, w, h);
		chrome.headerButton(resetButton, confirmReset > 0 ? "Tap to confirm" : "Reset statistics", RESET_W, confirmReset > 0,
				w, h);
		batch.end();
	}

	private static String filterName(int which) {
		return which == Stats.ALL ? "All games" : Difficulty.values()[which].label;
	}

	private void drawSwitch() {
		View[] all = View.values();
		float third = (page.width - 2 * PAD) / all.length, y = page.y + page.height - PAD / 2 - SWITCH_H;
		for (View v : all) {
			switchButton(switches[v.ordinal()], v.label, page.x + PAD + third * (v.ordinal() + 0.5f), y, third, view == v);
		}
	}

	private void switchButton(Rectangle area, String label, float cx, float y, float bw, boolean on) {
		paint.frame(on ? assets.button : assets.buttonAlt, cx - bw / 2, y, bw, SWITCH_H);
		paint.textIn(paint.serif(1.3f, on ? Palette.CREAM : Palette.MUTED), label, cx - bw / 2, y, bw, SWITCH_H);
		area.set(cx - bw / 2, y, bw, SWITCH_H);
	}

	private void drawModes(float y) {
		GameMode[] modes = GameMode.values();
		float bw = (page.width - 2 * PAD - (modes.length - 1) * MODE_GAP) / modes.length, x = page.x + PAD;
		for (GameMode m : modes) {
			boolean on = m == mode;
			paint.frame(on ? assets.button : assets.buttonAlt, x, y, bw, MODE_H);
			paint.textIn(paint.ui(1f, on ? Palette.CREAM : Palette.MUTED), m.label, x, y, bw, MODE_H);
			modeButtons[m.ordinal()].set(x, y, bw, MODE_H);
			x += bw + MODE_GAP;
		}
	}

	private void filterScores() {
		int key = filter * GameMode.values().length + mode.ordinal();
		if (scoresFor == key) {
			return;
		}
		shown.clear();
		for (HighScores.Entry e : list) {
			if (e.mode() == mode && (filter == Stats.ALL || e.level().ordinal() == filter)) {
				shown.add(e);
			}
		}
		scoresFor = key;
	}

	private float scoresHeight() {
		filterScores();
		return SCORE_HEADS_H + Math.max(1, shown.size()) * SCORE_ROW + PAD;
	}

	private float column(int c) {
		return page.x + page.width * SCORE_COLS[c];
	}

	private void drawScores(float top) {
		filterScores();
		BitmapFont f = paint.ui(1.15f, Palette.MUTED);
		float y = top - 8;
		if (shown.isEmpty()) {
			String note = filter == Stats.ALL ? "No " + mode.label + " high scores yet"
					: "No " + mode.label + " high scores at " + filterName(filter) + " yet";
			paint.centred(f, note, page.x + page.width / 2, y - 6);
			return;
		}
		paint.text(f, "#", column(0), y, true);
		paint.text(f, "Level", column(1), y, true);
		paint.text(f, "Score", column(2), y, true);
		paint.text(f, "Finished", column(3), y, true);
		paint.text(f, mode == GameMode.CUSTOM ? "Rules" : "Date", page.x + page.width - PAD, y, false);
		y = top - SCORE_HEADS_H;
		float pulse = 0.5f + 0.5f * MathUtils.sin(shownFor * 3f);
		for (int i = 0; i < shown.size(); i++) {
			HighScores.Entry e = shown.get(i);
			if (e == newest) {
				paint.rect(page.x + PAD / 2, y - SCORE_ROW, page.width - PAD, SCORE_ROW,
						highlight.set(0.97f, 0.85f, 0.56f, 0.10f + 0.08f * pulse));
			}
			drawScoreRow(i, e, y - 7);
			y -= SCORE_ROW;
		}
	}

	private void drawScoreRow(int i, HighScores.Entry e, float y) {
		Color c = e == newest ? Palette.GOLD_LIGHT : i == 0 ? Palette.GOLD : Palette.CREAM;
		BitmapFont f = paint.ui(1.15f, c);
		paint.text(f, Numbers.text(i + 1), column(0), y, true);
		batch.draw(assets.levelBadge[e.level().ordinal()], column(1) - BADGE / 2, y - 18, BADGE, BADGE);
		paint.text(f, Numbers.text(e.score()), column(2), y, true);
		f.setColor(e.place() == 1 ? Palette.GOLD : c);
		paint.text(f, e.place() == 1 ? "Won" : Wording.ordinal(e.place()), column(3), y, true);
		f.setColor(e == newest ? Palette.GOLD_LIGHT : Palette.MUTED);
		paint.text(f, mode == GameMode.CUSTOM ? e.rules().describe() : e.date(), page.x + page.width - PAD, y, false);
	}

	private void statRows() {
		if (rowsFor != filter) {
			Stats.Totals t = game.records().stats().totals(filter);
			games = Wording.gameRows(t);
			hands = Wording.handRows(t);
			rowsFor = filter;
		}
	}

	private float statsHeight() {
		statRows();
		return 2 * SECTION_H + (games.length + hands.length) * STAT_ROW + PAD;
	}

	private void drawStats(float top) {
		statRows();
		drawSection("Hands", hands, drawSection("Games", games, top));
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

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
		} else if (resetButton.contains(x, y)) {
			reset();
		} else if (switchAt(x, y) != null) {
			if (switchAt(x, y) != view) {
				view(switchAt(x, y));
			}
		} else if (view == View.SCORES && modeAt(x, y) != null) {
			mode = modeAt(x, y);
			scroller.reset();
		} else {
			int tab = filters.tabAt(x, y);
			if (tab >= 0 && filterOf[tab] != filter) {
				filters.choose(tab);
				filter = filterOf[tab];
				scroller.reset();
			}
		}
	}

	private View switchAt(float x, float y) {
		for (View v : View.values()) {
			if (switches[v.ordinal()].contains(x, y)) {
				return v;
			}
		}
		return null;
	}

	private float badgesHeight() {
		return (Chapter.values().length + Badge.values().length) * BADGE_ROW + PAD;
	}

	private void drawBadges(float top) {
		Campaign progress = game.records().campaign();
		float y = top - 8;
		for (Chapter c : Chapter.values()) {
			drawBadge(y, "Beat " + c.champion().name, c.ready ? "Win chapter " + c.number() + "'s duel" : "Coming soon",
					progress.cleared(c));
			y -= BADGE_ROW;
		}
		for (Badge b : Badge.values()) {
			drawBadge(y, b.title, b.how, progress.has(b));
			y -= BADGE_ROW;
		}
	}

	private void drawBadge(float top, String title, String how, boolean earned) {
		float x = page.x + PAD, icon = BADGE_ROW - 14;
		batch.setColor(1, 1, 1, earned ? 1 : 0.25f);
		batch.draw(assets.badge, x, top - icon - 4, icon, icon);
		batch.setColor(Color.WHITE);
		paint.serif(1.3f, earned ? Palette.GOLD_LIGHT : Palette.MUTED).draw(batch, title, x + icon + 14, top - 8);
		paint.ui(0.95f, earned ? Palette.CREAM : Palette.MUTED).draw(batch, how, x + icon + 14, top - 30);
	}

	private GameMode modeAt(float x, float y) {
		for (GameMode m : GameMode.values()) {
			if (modeButtons[m.ordinal()].contains(x, y)) {
				return m;
			}
		}
		return null;
	}

	private void reset() {
		if (confirmReset > 0) {
			game.records().resetStats();
			rowsFor = -1;
			confirmReset = 0;
		} else {
			confirmReset = RESET_CONFIRM_TIME;
		}
	}

	@Override
	protected void back() {
		newestAt = -1;
		game.back();
	}

	@Override
	public String stateTag() {
		return view.tag;
	}
}
