package ohheck.screens;

import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.platform.Audio;
import ohheck.platform.Settings;
import ohheck.rules.RuleSet;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;
import ohheck.ui.Scroller;
import ohheck.ui.SideTabs;

/* The settings, a page per category: tabs down the left, the chosen page's settings on the right. */
public class OptionsScreen extends BaseScreen {

	private static final float PAGE_TOP = 18, FRAME = 6;
	private static final Color DIVIDER = new Color(0.68f, 0.6f, 0.49f, 0.25f);

	private final MenuChrome chrome = new MenuChrome(paint);
	private final Rectangle backButton = new Rectangle(), page = new Rectangle(), inside = new Rectangle();
	private final Scroller scroller = new Scroller();
	private final List<OptionsPage> pages;
	private final SideTabs tabs;
	private OptionItem dragging;

	public OptionsScreen(GameHost game) {
		super(game);
		Settings s = game.settings();
		pages = Arrays.asList(
				new OptionsPage("Game",
						new DifficultyOption(s),
						OptionItem.choice("Game mode", "Changing a rule below makes the game Custom.",
								() -> s.mode().label, () -> s.setMode(s.mode().next())),
						OptionItem.choice("Hand progression", "How many cards each hand deals, first to last.",
								() -> s.rules().order.label(s.rules().mostCards),
								() -> s.customize(r -> r.withOrder(r.order.next()))),
						OptionItem.toggle("Marathon", "Hands grow to " + RuleSet.MOST_CARDS + " cards, the whole deck.",
								() -> s.rules().mostCards == RuleSet.MOST_CARDS,
								() -> s.customize(r -> r.withMostCards(r.mostCards == RuleSet.MOST_CARDS ? RuleSet.USUAL_CARDS : RuleSet.MOST_CARDS))),
						OptionItem.toggle("Big bids", "A made bid scores " + RuleSet.STANDARD.made() + " plus its square: bid big.",
								() -> s.rules().bigBids, () -> s.customize(r -> r.withBigBids(!r.bigBids))),
						OptionItem.toggle("Blind bids", "Bids are made unseen and shown together; the dealer may bid anything.",
								() -> s.rules().blindBids, () -> s.customize(r -> r.withBlindBids(!r.blindBids)))),
				new OptionsPage("Sound & display",
						OptionItem.choice("Hand sort", "Trumps on the left, then by suit or by rank.",
								() -> s.handSort().label, () -> s.setHandSort(s.handSort().next())),
						OptionItem.choice("Game speed", "How fast the cards and the other players move.",
								() -> s.fastGame() ? "Fast" : "Standard", () -> s.setFastGame(!s.fastGame())),
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
				new OptionsPage("Tutorial",
						OptionItem.link("How to play", "The rules, a step at a time.", game::showTutorial),
						OptionItem.link("Roster", "The players you can meet at the table.", game::showPlayers)));
		String[] names = new String[pages.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = pages.get(i).name;
		}
		tabs = new SideTabs(paint, null, names, null);
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
		tabs.draw(w, h, page);
		drawPage(pages.get(tabs.chosen()));
		chrome.header("Options", backButton, w, h);
		batch.end();
	}

	private void drawPage(OptionsPage p) {
		paint.frame(assets.panel, page.x, page.y, page.width, page.height);
		inside.set(page.x + FRAME, page.y + FRAME, page.width - 2 * FRAME, page.height - 2 * FRAME);
		float content = PAGE_TOP + FRAME;
		for (OptionItem item : p.items) {
			content += item.height();
		}
		scroller.update(frameDelta(), Gdx.input.isTouched(), content - inside.height);
		if (clipTo(inside)) {
			drawItems(p, page.y + page.height - PAGE_TOP + scroller.offset());
			unclip();
		}
		scroller.drawBar(paint, inside, content);
	}

	private void drawItems(OptionsPage p, float top) {
		float y = top;
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
	protected void drag(float dy) {
		scroller.drag(dy);
	}

	@Override
	protected void touchDown(float x, float y) {
		super.touchDown(x, y);
		dragging = null;
		for (OptionItem item : pages.get(tabs.chosen()).items) {
			if (inside.contains(x, y) && item.grabs(x, y)) {
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
		int tab = tabs.tabAt(x, y);
		if (tab >= 0) {
			tabs.choose(tab);
			scroller.reset();
			return;
		}
		if (!inside.contains(x, y)) {
			return;
		}
		for (OptionItem item : pages.get(tabs.chosen()).items) {
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
