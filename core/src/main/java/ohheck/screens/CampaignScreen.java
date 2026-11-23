package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.campaign.Studies;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * The campaign as a hand of chapter cards: open chapters face up with a pip for each step done,
 * the rest face down, the chapter in play lifted and lit. A tapped chapter lays its three steps
 * (studies, table, duel) on the felt like a trick.
 */
public class CampaignScreen extends BaseScreen {

	private static final float CARD_W = 142, CARD_H = CARD_W * 98 / 73, CARD_GAP = 22, ARC = 6, LIFT = 26;
	private static final float STEP_W = 130, STEP_H = STEP_W * 98 / 73, STEP_GAP = 36, PIP = 10, PIP_GAP = 20;
	private static final float GLOW_W = 1.9f, GLOW_H = 1.6f;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f), PIP_OFF = new Color(0.17f, 0.13f, 0.1f, 0.25f);
	private static final Tween LAYING = Tween.over(0.35f, Ease.OUT);
	private static final String[] STEPS = { "Studies", "Table", "Duel" };

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final Rectangle backButton = new Rectangle();
	private final Rectangle[] chapterCards = new Rectangle[Chapter.values().length];
	private final Rectangle[] stepCards = new Rectangle[STEPS.length];
	private Chapter chosen;
	private float chosenFor;

	public CampaignScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
		for (int i = 0; i < chapterCards.length; i++) {
			chapterCards[i] = new Rectangle();
		}
		for (int i = 0; i < stepCards.length; i++) {
			stepCards[i] = new Rectangle();
		}
	}

	public void openChapter(Chapter c) {
		chosen = c;
		chosenFor = 0;
	}

	@Override
	public void show() {
		super.show();
		game.audio().startMusic();
	}

	@Override
	protected void update(float step) {
		shownFor += step * game.options().speed;
		chosenFor += step * game.options().speed;
	}

	@Override
	public boolean animating() {
		return chosen != null && !LAYING.done(chosenFor);
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		Campaign progress = game.records().campaign();
		if (chosen == null) {
			drawHand(progress);
		} else {
			drawChapter(progress, chosen);
		}
		chrome.header(chosen == null ? "Campaign" : "Chapter " + chosen.number(), backButton, w, h);
		batch.end();
	}

	private Chapter current(Campaign progress) {
		for (Chapter c : Chapter.values()) {
			if (progress.open(c) && !progress.cleared(c)) {
				return c;
			}
		}
		return null;
	}

	private void drawHand(Campaign progress) {
		Chapter[] all = Chapter.values();
		float span = all.length * CARD_W + (all.length - 1) * CARD_GAP, x0 = (w - span) / 2, base = h / 2 - CARD_H / 2 - 20;
		Chapter lit = current(progress);
		for (int i = 0; i < all.length; i++) {
			Chapter c = all[i];
			float off = i - (all.length - 1) / 2f;
			float x = x0 + i * (CARD_W + CARD_GAP), y = base - ARC * off * off + (c == lit ? LIFT : 0);
			chapterCards[i].set(x, y, CARD_W, CARD_H);
			if (c == lit) {
				glow(x, y, CARD_W, CARD_H);
			}
			art.shadow(x, y, CARD_W, CARD_H, c == lit ? 8 : 4);
			if (progress.open(c)) {
				drawChapterFace(progress, c, x, y);
			} else {
				batch.draw(assets.back(), x, y, CARD_W, CARD_H);
				paint.textIn(paint.ui(0.95f, Palette.MUTED), c.ready ? "Locked" : "Coming soon", x, y - 26, CARD_W, 20);
			}
		}
		paint.centred(paint.ui(1.05f, Palette.MUTED), "Tap a chapter", w / 2, base - 70);
	}

	private void drawChapterFace(Campaign progress, Chapter c, float x, float y) {
		batch.draw(assets.cardBlank, x, y, CARD_W, CARD_H);
		paint.textIn(paint.serif(1.15f, Palette.INK), "Chapter " + c.number(), x, y + CARD_H - 40, CARD_W, 28);
		paint.textIn(paint.serif(1.4f, Palette.INK), c.mode.label, x, y + CARD_H / 2 + 4, CARD_W, 30);
		paint.textIn(paint.ui(0.85f, Palette.INK), "Champion", x, y + CARD_H / 2 - 26, CARD_W, 18);
		paint.textIn(paint.ui(1.0f, Palette.INK), c.champion().name, x, y + CARD_H / 2 - 44, CARD_W, 20);
		boolean[] done = { progress.allSolved(c), progress.tableWon(c), progress.cleared(c) };
		float px = x + CARD_W / 2 - PIP_GAP;
		for (int i = 0; i < done.length; i++) {
			paint.rect(px + i * PIP_GAP - PIP / 2, y + 16, PIP, PIP, done[i] ? Palette.GOLD : PIP_OFF);
		}
	}

	private void drawChapter(Campaign progress, Chapter c) {
		float top = h - MenuChrome.HEADER_H;
		paint.centred(paint.serif(1.5f, Palette.GOLD_LIGHT), c.mode.label, w / 2, top - 26);
		paint.centred(paint.ui(1.0f, Palette.MUTED), "Beat " + c.champion().name + " in a duel to clear the chapter", w / 2,
				top - 58);
		float span = STEPS.length * STEP_W + (STEPS.length - 1) * STEP_GAP, x0 = (w - span) / 2;
		float laid = LAYING.progress(chosenFor), y = (top - 80) / 2 - STEP_H / 2 + 20 - (1 - laid) * 40;
		for (int i = 0; i < STEPS.length; i++) {
			float x = x0 + i * (STEP_W + STEP_GAP);
			stepCards[i].set(x, y, STEP_W, STEP_H);
			art.shadow(x, y, STEP_W, STEP_H, 4);
			boolean faceUp = i < 2 || progress.duelOpen(c);
			batch.setColor(1, 1, 1, laid);
			if (faceUp) {
				batch.draw(assets.cardBlank, x, y, STEP_W, STEP_H);
			} else {
				batch.draw(assets.back(), x, y, STEP_W, STEP_H);
			}
			batch.setColor(Color.WHITE);
			if (faceUp) {
				paint.textIn(paint.serif(1.25f, Palette.INK, laid), STEPS[i], x, y + STEP_H - 44, STEP_W, 28);
				paint.textIn(paint.ui(0.9f, Palette.INK, laid), stepNote(progress, c, i), x, y + 18, STEP_W, 20);
			}
			paint.textIn(paint.ui(0.9f, Palette.MUTED, laid), faceUp ? "" : "Win the table first", x - 20, y - 26, STEP_W + 40, 20);
		}
	}

	private String stepNote(Campaign progress, Chapter c, int step) {
		if (step == 0) {
			return progress.solvedIn(c) + " of " + Studies.of(c).size() + " solved";
		}
		if (step == 1) {
			return progress.tableWon(c) ? "Won" : "Play";
		}
		return progress.cleared(c) ? "Won" : "Play";
	}

	private void glow(float x, float y, float cw, float ch) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		float gw = cw * GLOW_W, gh = ch * GLOW_H, pulse = 0.8f + 0.2f * MathUtils.sin(shownFor * 2f);
		batch.setColor(GLOW.r, GLOW.g, GLOW.b, GLOW.a * pulse);
		batch.draw(assets.glow, x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		Campaign progress = game.records().campaign();
		if (chosen == null) {
			for (int i = 0; i < chapterCards.length; i++) {
				Chapter c = Chapter.values()[i];
				if (chapterCards[i].contains(x, y) && progress.open(c)) {
					openChapter(c);
					return;
				}
			}
			return;
		}
		if (stepCards[0].contains(x, y)) {
			game.playStudy(progress.nextStudy(chosen));
		} else if (stepCards[1].contains(x, y)) {
			game.playCampaign(chosen, false);
		} else if (stepCards[2].contains(x, y) && progress.duelOpen(chosen)) {
			game.playCampaign(chosen, true);
		}
	}

	@Override
	protected void back() {
		if (chosen != null) {
			chosen = null;
		} else {
			game.back();
		}
	}

	@Override
	public String stateTag() {
		return chosen == null ? "campaign" : "chapter:" + chosen.number();
	}
}
