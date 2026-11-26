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

/*
 * Settings, a page per category: tabs down the left, the chosen page's settings on the right.
 * Two screens are built this way: Options, and the setup before a custom game, whose header
 * carries the button that deals it.
 */
public class OptionsScreen extends BaseScreen {

	private static final float PAGE_TOP = 18, FRAME = 6, DEAL_W = 140;
	private static final Color DIVIDER = new Color(0.68f, 0.6f, 0.49f, 0.25f);

	private final MenuChrome chrome = new MenuChrome(paint);
	private final Rectangle backButton = new Rectangle(), page = new Rectangle(), inside = new Rectangle();
	private final Scroller scroller = new Scroller();
	private final Rectangle dealButton = new Rectangle();
	private final String title, tag;
	private final List<OptionsPage> pages;
	private final Runnable deal;
	private final SideTabs tabs;
	private OptionItem dragging;

	private OptionsScreen(GameHost game, String title, String tag, Runnable deal, OptionsPage... pages) {
		super(game);
		this.title = title;
		this.tag = tag;
		this.deal = deal;
		this.pages = Arrays.asList(pages);
		String[] names = new String[this.pages.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = this.pages.get(i).name;
		}
		tabs = new SideTabs(paint, null, names, null);
	}

	public static OptionsScreen setup(GameHost game) {
		Settings s = game.settings();
		return new OptionsScreen(game, "Custom game", "setup", game::newGame,
				new OptionsPage("Table", new DifficultyOption(s)),
				new OptionsPage("Rules",
						OptionItem.choice("Game mode", "Changing a rule below makes the game Custom.",
								() -> s.mode().label, () -> s.setMode(s.mode().next())),
						OptionItem.choice("Hand progression", "How many cards each hand deals, first to last.",
								() -> s.rules().order.label(s.rules().mostCards),
								() -> s.customize(r -> r.withOrder(r.order.next()))),
						OptionItem.toggle("Marathon", "Hands grow to " + RuleSet.MOST_CARDS + " cards, the whole deck.",
								() -> s.rules().mostCards == RuleSet.MOST_CARDS,
								() -> s.customize(r -> r.withMostCards(r.mostCards == RuleSet.MOST_CARDS ? RuleSet.USUAL_CARDS : RuleSet.MOST_CARDS))),
						OptionItem.toggle("Big bids", "Bigger bids are worth more.",
								() -> s.rules().bigBids, () -> s.customize(r -> r.withBigBids(!r.bigBids))),
						OptionItem.toggle("Blind bids", "Bids are made unseen and shown together; the dealer may bid anything.",
								() -> s.rules().blindBids, () -> s.customize(r -> r.withBlindBids(!r.blindBids)))));
	}

	public static OptionsScreen options(GameHost game) {
		Settings s = game.settings();
		return new OptionsScreen(game, "Options", "options", null,
				new OptionsPage("Preferences",
						OptionItem.heading("Sound"),
						OptionItem.toggle("Mute all", "Silences the music and sound effects.", s::muted, () -> {
							s.setMuted(!s.muted());
							game.audio().startMusic();
						}),
						OptionItem.toggle("Music", "The background music.", s::music, () -> {
							s.setMusic(!s.music());
							game.audio().startMusic();
						}),
						OptionItem.toggle("Sound effects", "Cards, shuffles and fanfares.", s::sound, () -> {
							s.setSound(!s.sound());
							game.audio().play(Audio.Effect.PLAY_CARD);
						}),
						OptionItem.heading("Display"),
						OptionItem.choice("Card back", "Won by clearing campaign chapters.", () -> s.cardBack().label, () -> {
							s.setCardBack(s.cardBack().nextOwned(game.records().campaign()));
							game.assets().dress(s.cardBack(), s.felt());
						}),
						OptionItem.choice("Table felt", "Won by clearing campaign chapters.", () -> s.felt().label, () -> {
							s.setFelt(s.felt().nextOwned(game.records().campaign()));
							game.assets().dress(s.cardBack(), s.felt());
						}),
						OptionItem.choice("Hand sort", "Trumps on the left, then by suit or by rank.",
								() -> s.handSort().label, () -> s.setHandSort(s.handSort().next())),
						OptionItem.choice("Game speed", "How fast the cards and the other players move.",
								() -> s.fastGame() ? "Fast" : "Standard", () -> s.setFastGame(!s.fastGame())),
						OptionItem.toggle("Visual effects", "Glow, vignette and film grain.", s::effects,
								() -> s.setEffects(!s.effects()))),
				new OptionsPage("Tutorial",
						OptionItem.link("Practice round", "Learn the game by playing a short guided round.", () -> game.playPractice(false)),
						OptionItem.link("Roster", "The players you can meet at the table.", game::showPlayers),
						OptionItem.confirmed("Start fresh", "Forget everything, as a new player: tutorial, campaign, scores, settings.",
								game::startFresh)));
	}

	@Override
	public void show() {
		super.show();
		for (OptionsPage p : pages) {
			for (OptionItem item : p.items) {
				item.shown();
			}
		}
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
		chrome.header(title, backButton, w, h);
		if (deal != null) {
			chrome.headerButton(dealButton, "Deal", DEAL_W, true, w, h);
		}
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
			if (i < p.items.size() - 1 && item.dividedBelow() && p.items.get(i + 1).dividedBelow()) {
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
		if (deal != null && dealButton.contains(x, y)) {
			deal.run();
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
		return tag;
	}
}
