package ohheck.screens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.platform.Audio;
import ohheck.platform.Settings;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;
import ohheck.ui.TapAreas;

/* The settings, a page per category: tabs down the left, the chosen page's settings on the right. */
public class OptionsScreen extends BaseScreen implements TourStop {

	private static final float MAX_W = 940, MARGIN = 24, GAP = 18, TABS_W = 210, TAB_H = 54, TAB_GAP = 10, PAGE_TOP = 18;
	private static final Color DIVIDER = new Color(0.68f, 0.6f, 0.49f, 0.25f);

	private final MenuChrome chrome = new MenuChrome(paint);
	private final Rectangle backButton = new Rectangle(), page = new Rectangle();
	private final List<Rectangle> tabs = new ArrayList<Rectangle>();
	private final List<OptionsPage> pages;
	private int current;
	private OptionItem dragging;

	public OptionsScreen(GameHost game) {
		super(game);
		Settings s = game.settings();
		pages = Arrays.asList(
				new OptionsPage("Game",
						new DifficultyOption(s),
						OptionItem.choice("Hand progression", "How many cards each hand deals, first to last.",
								() -> s.handOrder().label(), () -> s.setHandOrder(s.handOrder().flipped())),
						OptionItem.choice("Game speed", "How fast the cards and the other players move.",
								() -> s.fastGame() ? "Fast" : "Standard", () -> s.setFastGame(!s.fastGame()))),
				new OptionsPage("Sound & display",
						OptionItem.toggle("Music", "The background music.", s::music, () -> {
							s.setMusic(!s.music());
							game.audio().startMusic();
						}),
						OptionItem.toggle("Sound effects", "Cards, shuffles and fanfares.", s::sound, () -> {
							s.setSound(!s.sound());
							game.audio().play(Audio.Effect.PLAY_CARD);
						}),
						OptionItem.toggle("Visual effects", "Glow, vignette and film grain.", s::effects,
								() -> s.setEffects(!s.effects()))),
				new OptionsPage("Learn",
						OptionItem.link("How to play", "The rules, a step at a time.", game::showTutorial),
						OptionItem.link("Roster", "The players you can meet at the table.", game::showPlayers)));
	}

	@Override
	public ScreenshotTour.Stop tourStop() {
		return ScreenshotTour.Stop.OPTIONS;
	}

	@Override
	protected void update(float step) {
		shownFor += step * game.options().speed;
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
		drawPage(pages.get(current));
		chrome.header("Options", backButton, w, h);
		batch.end();
	}

	private void drawTabs(float left, float top) {
		TapAreas.sizeTo(tabs, pages.size());
		for (int i = 0; i < pages.size(); i++) {
			Rectangle tab = tabs.get(i).set(left, top - (i + 1) * TAB_H - i * TAB_GAP, TABS_W, TAB_H);
			boolean chosen = i == current;
			paint.frame(chosen ? assets.button : assets.buttonAlt, tab.x, tab.y, tab.width, tab.height);
			paint.textIn(paint.serif(1.3f, chosen ? Palette.CREAM : Palette.MUTED), pages.get(i).name, tab.x, tab.y,
					tab.width, tab.height);
		}
	}

	private void drawPage(OptionsPage p) {
		paint.frame(assets.panel, page.x, page.y, page.width, page.height);
		float y = page.y + page.height - PAGE_TOP;
		for (int i = 0; i < p.items.size(); i++) {
			OptionItem item = p.items.get(i);
			y -= item.height();
			item.draw(paint, page.x, y, page.width);
			if (i < p.items.size() - 1) {
				paint.rect(page.x + OptionItem.PAD, y, page.width - 2 * OptionItem.PAD, 1, DIVIDER);
			}
		}
	}

	@Override
	protected void touchDown(float x, float y) {
		super.touchDown(x, y);
		dragging = null;
		for (OptionItem item : pages.get(current).items) {
			if (item.grabs(x, y)) {
				dragging = item;
				item.dragTo(x);
			}
		}
	}

	@Override
	protected void touchDragged(float x, float y) {
		if (dragging != null) {
			dragging.dragTo(x);
		} else {
			super.touchDragged(x, y);
		}
	}

	@Override
	protected void touchUp(float x, float y) {
		if (dragging != null) {
			dragging = null;
			return;
		}
		super.touchUp(x, y);
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(x, y)) {
				current = i;
				return;
			}
		}
		for (OptionItem item : pages.get(current).items) {
			if (item.bounds.contains(x, y)) {
				item.tap(x, y);
				return;
			}
		}
	}

	@Override
	protected void back() {
		game.back();
	}

	@Override
	public String stateTag() {
		return "options";
	}
}
