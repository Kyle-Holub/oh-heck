package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.Briefing;
import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.campaign.TrickShots;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * The campaign as a hand of chapter cards: open chapters face up with a pip for each step done,
 * the rest face down, the chapter in play lifted and lit. A tapped chapter lays its three steps
 * (table, trick shots, duel) on the felt like a trick. A chapter, and each step, opens with a
 * briefing of what is about to happen.
 */
public class CampaignScreen extends BaseScreen {

	private static final float CARD_W = 142, CARD_H = CARD_W * 98 / 73, CARD_GAP = 22, ARC = 6, LIFT = 26;
	private static final float STEP_W = 136, STEP_H = STEP_W * 98 / 73, STEP_GAP = 36, PIP = 16, PIP_GAP = 24, EMBLEM = 46, ICON = 64;
	private static final float GLOW_W = 1.9f, GLOW_H = 1.6f;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f);
	private static final Tween LAYING = Tween.over(0.35f, Ease.OUT);
	private static final float BRIEF_W = 560, BRIEF_H = 290, BRIEF_PAD = 28, BRIEF_BUTTON_W = 160;
	private static final int TABLE = 0, SHOTS = 1, DUEL = 2;
	private static final String[] STEPS = { "Table", "Trick shots", "Duel" };

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final Rectangle backButton = new Rectangle();
	private final Rectangle[] chapterCards = new Rectangle[Chapter.values().length];
	private final Rectangle[] stepCards = new Rectangle[STEPS.length];
	private final Rectangle briefPanel = new Rectangle(), goButton = new Rectangle(), stayButton = new Rectangle();
	private Chapter chosen;
	private float chosenFor, briefedFor;
	private Briefing briefing;
	private Runnable afterBriefing;

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
		briefing = null;
	}

	public void brief(Briefing b, Runnable then) {
		briefing = b;
		afterBriefing = then;
		briefedFor = 0;
	}

	public void briefChapter(Chapter c) {
		brief(Briefing.chapter(c), () -> openChapter(c));
	}

	public void briefTable(Chapter c) {
		brief(Briefing.table(c), () -> game.playCampaign(c, false));
	}

	public void go() {
		Runnable then = afterBriefing;
		briefing = null;
		then.run();
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
		briefedFor += step * game.options().speed;
	}

	@Override
	public boolean animating() {
		return chosen != null && !LAYING.done(chosenFor) || briefing != null && !LAYING.done(briefedFor);
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
		if (briefing != null) {
			drawBriefing();
		}
		chrome.header(chosen == null ? "Campaign" : "Chapter " + chosen.number(), backButton, w, h);
		batch.end();
	}

	private void drawBriefing() {
		float a = LAYING.progress(briefedFor);
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		float pw = Math.min(BRIEF_W, w - 40), ph = BRIEF_H, px = (w - pw) / 2, py = (h - MenuChrome.HEADER_H - ph) / 2 - (1 - a) * 30;
		briefPanel.set(px, py, pw, ph);
		paint.frame(assets.panel, px, py, pw, ph);
		paint.centred(paint.serif(1.5f, Palette.GOLD_LIGHT, a), briefing.title(), w / 2, py + ph - 22);
		paint.ui(1.05f, Palette.CREAM, a).draw(batch, briefing.body(), px + BRIEF_PAD, py + ph - 70, pw - 2 * BRIEF_PAD, Align.left, true);
		paint.button(goButton, briefing.go(), w / 2 + BRIEF_BUTTON_W / 2 + 8, py + 18, BRIEF_BUTTON_W, 42, assets.button);
		paint.button(stayButton, "Back", w / 2 - BRIEF_BUTTON_W / 2 - 8, py + 18, BRIEF_BUTTON_W, 42, assets.buttonAlt);
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
		batch.draw(assets.campaignFace, x, y, CARD_W, CARD_H);
		float cx = x + CARD_W / 2;
		paint.centred(paint.ui(0.8f, Palette.GOLD), "CHAPTER", cx, y + CARD_H - 20);
		paint.numeral(c.number(), cx, y + CARD_H - 56, 40, Palette.GOLD_LIGHT);
		batch.draw(assets.emblem(c.emblem), cx - EMBLEM / 2, y + CARD_H / 2 - EMBLEM / 2 - 12, EMBLEM, EMBLEM);
		paint.centred(paint.serif(1.3f, Palette.GOLD_LIGHT), c.mode.label, cx, y + 58);
		pips(cx, y + 18, progress.tableWon(c), progress.allMade(c), progress.cleared(c));
	}

	private void pips(float cx, float y, boolean... done) {
		for (int i = 0; i < done.length; i++) {
			float px = cx + (i - (done.length - 1) / 2f) * PIP_GAP - PIP / 2;
			batch.draw(done[i] ? assets.pipOn : assets.pipOff, px, y, PIP, PIP);
		}
	}

	private void drawChapter(Campaign progress, Chapter c) {
		float top = h - MenuChrome.HEADER_H;
		paint.centred(paint.serif(1.5f, Palette.GOLD_LIGHT), c.mode.label, w / 2, top - 26);
		paint.centred(paint.ui(1.0f, Palette.MUTED), "Win the table, sharpen up with trick shots, then beat "
				+ c.champion().name + " in a duel", w / 2, top - 58);
		float span = STEPS.length * STEP_W + (STEPS.length - 1) * STEP_GAP, x0 = (w - span) / 2;
		float laid = LAYING.progress(chosenFor), y = (top - 80) / 2 - STEP_H / 2 + 20 - (1 - laid) * 40;
		for (int i = 0; i < STEPS.length; i++) {
			float x = x0 + i * (STEP_W + STEP_GAP);
			stepCards[i].set(x, y, STEP_W, STEP_H);
			art.shadow(x, y, STEP_W, STEP_H, 4);
			boolean faceUp = i == TABLE || progress.shotsOpen(c);
			batch.setColor(1, 1, 1, laid);
			if (faceUp) {
				drawStepFace(progress, c, i, x, y, laid);
			} else {
				batch.draw(assets.back(), x, y, STEP_W, STEP_H);
				batch.setColor(Color.WHITE);
				paint.textIn(paint.ui(0.9f, Palette.MUTED, laid), "Win the table first", x - 20, y - 26, STEP_W + 40, 20);
			}
			batch.setColor(Color.WHITE);
		}
	}

	private void drawStepFace(Campaign progress, Chapter c, int step, float x, float y, float alpha) {
		batch.draw(assets.campaignFace, x, y, STEP_W, STEP_H);
		batch.draw(step == TABLE ? assets.stepTable : step == SHOTS ? assets.stepShots : assets.stepDuel,
				x + STEP_W / 2 - ICON / 2, y + STEP_H / 2 - ICON / 2 + 4, ICON, ICON);
		batch.setColor(Color.WHITE);
		paint.textIn(paint.serif(1.25f, Palette.GOLD_LIGHT, alpha), STEPS[step], x, y + STEP_H - 46, STEP_W, 28);
		boolean done = step == TABLE ? progress.tableWon(c) : step == SHOTS ? progress.allMade(c) : progress.cleared(c);
		paint.textIn(paint.ui(0.9f, done ? Palette.GOLD : Palette.CREAM, alpha), stepNote(progress, c, step), x, y + 18, STEP_W, 20);
	}

	private String stepNote(Campaign progress, Chapter c, int step) {
		if (step == SHOTS) {
			return progress.madeIn(c) + " of " + TrickShots.of(c).size() + " made";
		}
		boolean won = step == TABLE ? progress.tableWon(c) : progress.cleared(c);
		return won ? "Won" : "Play";
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
		if (briefing != null) {
			if (goButton.contains(x, y)) {
				go();
			} else if (stayButton.contains(x, y) || !briefPanel.contains(x, y)) {
				briefing = null;
			}
			return;
		}
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		Campaign progress = game.records().campaign();
		if (chosen == null) {
			for (int i = 0; i < chapterCards.length; i++) {
				Chapter c = Chapter.values()[i];
				if (chapterCards[i].contains(x, y) && progress.open(c)) {
					briefChapter(c);
					return;
				}
			}
			return;
		}
		Chapter c = chosen;
		if (stepCards[TABLE].contains(x, y)) {
			briefTable(c);
		} else if (stepCards[SHOTS].contains(x, y) && progress.shotsOpen(c)) {
			brief(Briefing.trickShots(c, progress), () -> game.playTrickShot(progress.nextTrickShot(c)));
		} else if (stepCards[DUEL].contains(x, y) && progress.duelOpen(c)) {
			brief(Briefing.duel(c), () -> game.playCampaign(c, true));
		}
	}

	@Override
	protected void back() {
		if (briefing != null) {
			briefing = null;
		} else if (chosen != null) {
			chosen = null;
		} else {
			game.back();
		}
	}

	@Override
	public String stateTag() {
		if (briefing != null) {
			return briefing.tag();
		}
		return chosen == null ? "campaign" : "chapter:" + chosen.number();
	}
}
