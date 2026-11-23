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
 */
public class PlayMenuScreen extends BaseScreen {

	private static final float CARD_W = 186, CARD_H = CARD_W * 98 / 73, GAP = 24, ICON = 84, RISE = 40, STAGGER = 0.08f;
	private static final Tween RISING = Tween.over(0.35f, Ease.OUT);

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
		return !RISING.done(shownFor - STAGGER * cards.length);
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
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		if (cards[Choice.CONTINUE.ordinal()].contains(x, y)) {
			game.continueGame();
		} else if (cards[Choice.CAMPAIGN.ordinal()].contains(x, y)) {
			game.showCampaign();
		} else if (cards[Choice.CUSTOM.ordinal()].contains(x, y)) {
			game.play();
		} else if (cards[Choice.SCORES.ordinal()].contains(x, y)) {
			game.showHighScores(-1);
		}
	}

	@Override
	protected void back() {
		game.back();
	}

	@Override
	public String stateTag() {
		return "play";
	}
}
