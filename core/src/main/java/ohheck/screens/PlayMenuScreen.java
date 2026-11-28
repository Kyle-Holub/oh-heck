package ohheck.screens;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * Where to play, as a row of cards dealt onto the felt: carry on the game in progress, the
 * campaign, a custom game, or the high scores. Each card rises in a beat after the one before.
 * Arriving from the title, someone new to the game is first asked whether to learn it in a practice round.
 */
public class PlayMenuScreen extends BaseScreen {

	private static final float CARD_W = 186, CARD_H = CARD_W * 98 / 73, GAP = 24, ICON = 84, RISE = 40, STAGGER = 0.08f;
	private static final Tween RISING = Tween.over(0.35f, Ease.OUT), ASK_IN = Tween.over(0.3f, Ease.OUT).after(0.55f);
	private static final float ASK_W = 560, ASK_H = 190, ASK_BUTTON_W = 250;
	private static final String ASK_TITLE = "New to Oh Heck?", ASK_LINE = "A short practice round teaches the game as you play.";
	private final Rectangle askPanel = new Rectangle(), teachButton = new Rectangle(), knowButton = new Rectangle();
	private boolean asking;


	private enum Choice {
		CONTINUE("Continue", "Back to your game"), CAMPAIGN("Campaign", ""), CUSTOM("Custom game", "Your table, your rules"),
		SCORES("High scores", "Scores and badges");

		final String title, note;

		Choice(String title, String note) {
			this.title = title;
			this.note = note;
		}
	}

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final Rectangle backButton = new Rectangle();
	private final Rectangle[] cards = new Rectangle[Choice.values().length];
	private String campaignNote = "";

	public PlayMenuScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
		for (int i = 0; i < cards.length; i++) {
			cards[i] = new Rectangle();
		}
	}

	public void askIfNew() {
		asking = !game.settings().knowsHowToPlay() && !game.options().autoplayHuman;
	}

	@Override
	public void show() {
		super.show();
		shownFor = 0;
		campaignNote = campaignNote(game.records().campaign());
	}

	private static String campaignNote(Campaign progress) {
		for (Chapter c : Chapter.values()) {
			if (progress.open(c) && !progress.cleared(c)) {
				return "Chapter " + c.number() + ": " + c.mode.label;
			}
		}
		return "Every chapter cleared";
	}

	@Override
	protected void update(float step) {
		shownFor += step * game.options().speed;

	}

	@Override
	public boolean animating() {
		return !RISING.done(shownFor - STAGGER * cards.length) || asking && !ASK_IN.done(shownFor);
	}

	private boolean shown(Choice c) {
		return c != Choice.CONTINUE || game.hasGameInProgress();
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		int n = 0;
		for (Choice c : Choice.values()) {
			n += shown(c) ? 1 : 0;
		}
		float span = n * CARD_W + (n - 1) * GAP, x = (w - span) / 2, base = (h - MenuChrome.HEADER_H) / 2 - CARD_H / 2;
		int k = 0;
		for (Choice c : Choice.values()) {
			Rectangle r = cards[c.ordinal()];
			if (!shown(c)) {
				r.set(0, 0, 0, 0);
				continue;
			}
			float p = RISING.progress(shownFor - k * STAGGER), y = base - (1 - p) * RISE;
			r.set(x, y, CARD_W, CARD_H);
			drawCard(c, x, y, p);
			x += CARD_W + GAP;
			k++;
		}
		chrome.header("Play", backButton, w, h);
		if (asking) {
			drawAsk(ASK_IN.progress(shownFor));
		}
		batch.end();
	}

	private void drawCard(Choice c, float x, float y, float alpha) {
		art.shadow(x, y, CARD_W, CARD_H, 6);
		batch.setColor(1, 1, 1, alpha);
		batch.draw(assets.campaignFace, x, y, CARD_W, CARD_H);
		batch.draw(icon(c), x + CARD_W / 2 - ICON / 2, y + CARD_H / 2 - ICON / 2 + 6, ICON, ICON);
		batch.setColor(1, 1, 1, 1);
		paint.textIn(paint.serif(1.2f, Palette.GOLD_LIGHT, alpha), c.title, x, y + CARD_H - 50, CARD_W, 28);
		String note = c == Choice.CAMPAIGN ? campaignNote : c.note;
		paint.textIn(paint.ui(0.8f, Palette.CREAM, alpha), note, x, y + 24, CARD_W, 18);
	}

	private Texture icon(Choice c) {
		switch (c) {
		case CONTINUE:
			return assets.menuContinue;
		case CAMPAIGN:
			return assets.menuCampaign;
		case CUSTOM:
			return assets.menuCustom;
		default:
			return assets.menuScores;
		}
	}

	@Override
	protected void tap(float x, float y) {
		if (asking) {
			if (!ASK_IN.done(shownFor)) {
				return;
			}
			if (teachButton.contains(x, y)) {
				asking = false;
				game.playPractice(true);
			} else if (knowButton.contains(x, y)) {
				asking = false;
				game.settings().setKnowsHowToPlay(true);
			} else if (!askPanel.contains(x, y)) {
				asking = false;
			}
			return;
		}
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		if (cards[Choice.CONTINUE.ordinal()].contains(x, y)) {
			game.continueGame();
		} else if (cards[Choice.CAMPAIGN.ordinal()].contains(x, y)) {
			game.showCampaign();
		} else if (cards[Choice.CUSTOM.ordinal()].contains(x, y)) {
			game.showGameSetup();
		} else if (cards[Choice.SCORES.ordinal()].contains(x, y)) {
			game.showHighScores(-1);
		}
	}

	@Override
	protected void back() {
		if (asking) {
			asking = false;
		} else {
			game.back();
		}
	}

	private void drawAsk(float a) {
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		float pw = ASK_W, ph = ASK_H, px = (w - pw) / 2, py = (h - ph) / 2 - (1 - a) * RISE;
		askPanel.set(px, py, pw, ph);
		paint.fade(a);
		paint.frame(assets.panel, px, py, pw, ph);
		paint.text(paint.serif(1.7f, Palette.GOLD, a), ASK_TITLE, w / 2, py + ph - 24, true);
		paint.text(paint.ui(1.1f, Palette.CREAM, a), ASK_LINE, w / 2, py + ph - 80, true);
		paint.button(teachButton, "Teach me", w / 2 - ASK_BUTTON_W / 2 - 8, py + 20, ASK_BUTTON_W, 46, assets.button);
		paint.button(knowButton, "I know how to play", w / 2 + ASK_BUTTON_W / 2 + 8, py + 20, ASK_BUTTON_W, 46, assets.buttonAlt);
		paint.fade(1f);
	}

	@Override
	public String stateTag() {
		return asking ? "ask" : "play";
	}
}
