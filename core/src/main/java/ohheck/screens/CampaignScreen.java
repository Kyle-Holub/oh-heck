package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.Briefing;
import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.campaign.Stage;
import ohheck.campaign.TrickShots;
import ohheck.players.Player;
import ohheck.rules.Card;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * The campaign as a hand of chapter cards: open chapters face up with a pip for each step done,
 * the rest face down, the chapter in play lifted and lit. A tapped chapter lays its four steps
 * (table, trick shots, final table, duel) on the felt like a trick. A chapter, and each step, opens with a
 * briefing of what is about to happen: a few pages in the tutorial's style, a picture above and a caption below.
 */
public class CampaignScreen extends BaseScreen {

	private static final float CARD_W = 142, CARD_H = CARD_W * 98 / 73, CARD_GAP = 22, ARC = 6, LIFT = 26;
	private static final float STEP_W = 136, STEP_H = STEP_W * 98 / 73, STEP_GAP = 36, PIP = 16, PIP_GAP = 24, EMBLEM = 46, ICON = 64;
	private static final float GLOW_W = 1.9f, GLOW_H = 1.6f;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f);
	private static final Tween LAYING = Tween.over(0.35f, Ease.OUT);
	private static final float CAPTION_H = 92, CAPTION_W = 860, BIG_W = 170, BIG_H = BIG_W * 98 / 73, SMALL_W = 120,
			SMALL_H = SMALL_W * 98 / 73, BIG_ICON = 84, DECK_W = 96,
			BUTTON_ROOM = 64, PLAYER_GAP = 24;
	private static final Card[] SHORT_DECK = { Card.parse("9H"), Card.parse("10H"), Card.parse("JH"), Card.parse("QH"),
			Card.parse("KH"), Card.parse("AH") };
	private static final int SHOTS = 1, CHAMPION = 4;
	private static final String[] STEPS = { "Table", "Trick shots", "Final table", "Duel" };
	private static final Stage[] STAGE_AT = { Stage.TABLE, null, Stage.FINAL, Stage.DUEL };

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final Rectangle backButton = new Rectangle();
	private final Rectangle[] chapterCards = new Rectangle[Chapter.values().length];
	private final Rectangle[] stepCards = new Rectangle[STEPS.length];
	private final Rectangle goButton = new Rectangle();
	private final Rectangle[] playerCards = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };
	private final StringBuilder pageCount = new StringBuilder();
	private Chapter chosen;
	private float chosenFor, briefedFor;
	private int page, rival;
	private String beatLine = "";
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
		beatLine = c == null ? "" : "Win the table, then beat " + c.champion().name + " at the final table and in a duel";
		chosenFor = 0;
		briefing = null;
	}

	public void brief(Briefing b, Runnable then) {
		briefing = b;
		afterBriefing = then;
		briefedFor = 0;
		page = 0;
		rival = 0;
	}

	public void briefChapter(Chapter c) {
		brief(Briefing.chapter(c), () -> openChapter(c));
	}

	public void briefTable(Chapter c) {
		brief(Briefing.table(c), () -> game.playCampaign(c, Stage.TABLE));
	}

	private void briefStep(Campaign progress, Chapter c, int step) {
		Stage s = STAGE_AT[step];
		if (s == null) {
			brief(Briefing.trickShots(c, progress), () -> game.playTrickShot(progress.nextTrickShot(c)));
		} else {
			brief(s == Stage.TABLE ? Briefing.table(c) : s == Stage.FINAL ? Briefing.finalTable(c) : Briefing.duel(c),
					() -> game.playCampaign(c, s));
		}
	}

	private boolean stepOpen(Campaign progress, Chapter c, int step) {
		return STAGE_AT[step] == null ? progress.shotsOpen(c) : progress.open(c, STAGE_AT[step]);
	}

	private boolean stepDone(Campaign progress, Chapter c, int step) {
		return STAGE_AT[step] == null ? progress.allMade(c) : progress.won(c, STAGE_AT[step]);
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
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		Briefing.Page p = briefing.pages().get(page);
		float cw = Math.min(w - 60, CAPTION_W), x = (w - cw) / 2, y = 12;
		float midY = (y + CAPTION_H + BUTTON_ROOM + h - MenuChrome.HEADER_H) / 2 - (1 - a) * 30;
		paint.fade(a);
		drawPicture(p, midY, a);
		paint.frame(assets.panel, x, y, cw, CAPTION_H);
		paint.fade(1f);
		Player shown = rival > 0 ? player(briefing.chapter(), rival) : null;
		paint.serif(1.35f, Palette.GOLD, a).draw(batch, shown != null ? shown.name : p.heading(), x + 22, y + CAPTION_H - 12);
		paint.ui(1.1f, Palette.CREAM, a).draw(batch, shown != null ? shown.trait : p.line(), x + 22, y + CAPTION_H - 44, cw - 44, Align.left, true);
		BitmapFont m = paint.ui(0.9f, Palette.MUTED, a);
		pageCount.setLength(0);
		pageCount.append(page + 1).append(" / ").append(briefing.pages().size());
		paint.text(m, pageCount.toString(), x + cw - 20, y + CAPTION_H - 14, false);
		if (page == briefing.pages().size() - 1) {
			paint.button(goButton, briefing.go(), w / 2, y + CAPTION_H + 14, 220, 44, assets.button);
		} else {
			goButton.set(0, 0, 0, 0);
			paint.text(m, "Tap to continue", x + cw - 20, y + 22, false);
		}
	}

	private void drawPicture(Briefing.Page p, float cy, float a) {
		Chapter c = briefing.chapter();
		switch (p.picture()) {
		case CHAPTER:
			bigCard(w / 2, cy, assets.emblem(c.emblem), p.figure(), a);
			paint.centred(paint.ui(0.9f, Palette.GOLD, a), "CHAPTER", w / 2, cy + BIG_H / 2 - 20);
			break;
		case SCORING:
			paint.centred(paint.serif(3.2f, Palette.GOLD_LIGHT, a), p.figure(), w / 2, cy + 30);
			break;
		case HANDS:
			int n = c.rules().mostCards;
			float hw = DECK_W * 0.8f, hh = hw * 98 / 73, fan = hw * 0.42f, hx = w / 2 - (hw + fan * (n - 1)) / 2;
			for (int i = 0; i < n; i++) {
				float off = i - (n - 1) / 2f;
				batch.setColor(1, 1, 1, a);
				batch.draw(assets.back(), hx + i * fan, cy - hh / 2 - 30 - off * off * 1.5f, hw / 2, hh / 2, hw, hh, 1, 1, -off * 3f);
			}
			batch.setColor(Color.WHITE);
			paint.centred(paint.serif(2.4f, Palette.GOLD_LIGHT, a), p.figure(), w / 2, cy + hh / 2 + 10);
			break;
		case RIVALS:
			for (int i = 1; i < playerCards.length; i++) {
				float rx = w / 2 + (i - 2.5f) * (SMALL_W + PLAYER_GAP), ry = cy + (i == rival ? LIFT : 0);
				playerCards[i].set(rx - SMALL_W / 2, ry - SMALL_H / 2, SMALL_W, SMALL_H);
				if (i == rival) {
					glow(rx - SMALL_W / 2, ry - SMALL_H / 2, SMALL_W, SMALL_H);
				}
				Player pl = player(c, i);
				smallCard(rx, ry, i == CHAMPION ? assets.stepDuel : assets.levelBadge[pl.level.ordinal()], pl.name, a);
			}
			break;
		case TABLE:
			bigCard(w / 2, cy, assets.stepTable, p.figure(), a);
			break;
		case WIN:
			bigCard(w / 2, cy, assets.badge, p.figure(), a);
			break;
		case SHOTS:
			bigCard(w / 2, cy, assets.stepShots, p.figure(), a);
			break;
		case CHAMPION:
			bigCard(w / 2, cy, assets.stepDuel, p.figure(), a);
			break;
		case SHORT_DECK:
			float dh = DECK_W * 98 / 73, step = DECK_W * 0.7f, x0 = w / 2 - (DECK_W + step * (SHORT_DECK.length - 1)) / 2;
			for (int i = 0; i < SHORT_DECK.length; i++) {
				float x = x0 + i * step, y = cy - dh / 2 + (i - 2.5f) * (i - 2.5f) * -3;
				art.shadow(x, y, DECK_W, dh, 4);
				art.flip(SHORT_DECK[i], x, y, DECK_W, dh, 1);
			}
			break;
		case MOMENTUM:
			glow(w / 2 - BIG_W / 2, cy - BIG_H / 2, BIG_W, BIG_H);
			paint.centred(paint.serif(5f, Palette.GOLD_LIGHT, a), p.figure(), w / 2, cy + 48);
			break;
		}
	}

	private Texture stepIcon(int step) {
		return step == SHOTS ? assets.stepShots : STAGE_AT[step] == Stage.DUEL ? assets.stepDuel : assets.stepTable;
	}

	private void bigCard(float cx, float cy, Texture icon, String label, float a) {
		float x = cx - BIG_W / 2, y = cy - BIG_H / 2;
		glow(x, y, BIG_W, BIG_H);
		art.shadow(x, y, BIG_W, BIG_H, 8);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, BIG_W, BIG_H);
		batch.draw(icon, cx - BIG_ICON / 2, cy - BIG_ICON / 2 + 6, BIG_ICON, BIG_ICON);
		batch.setColor(Color.WHITE);
		paint.textIn(paint.serif(1.3f, Palette.GOLD_LIGHT, a), label, x, y + 22, BIG_W, 28);
	}

	private void smallCard(float cx, float cy, Texture icon, String label, float a) {
		float x = cx - SMALL_W / 2, y = cy - SMALL_H / 2;
		art.shadow(x, y, SMALL_W, SMALL_H, 4);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, SMALL_W, SMALL_H);
		batch.draw(icon, cx - ICON / 2, cy - ICON / 2 + 8, ICON, ICON);
		batch.setColor(Color.WHITE);
		paint.textIn(paint.serif(1.1f, Palette.GOLD_LIGHT, a), label, x, y + 16, SMALL_W, 24);
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
		pips(cx, y + 18, progress.won(c, Stage.TABLE), progress.allMade(c), progress.won(c, Stage.FINAL), progress.cleared(c));
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
		paint.centred(paint.ui(1.0f, Palette.MUTED), beatLine, w / 2, top - 58);
		float span = STEPS.length * STEP_W + (STEPS.length - 1) * STEP_GAP, x0 = (w - span) / 2;
		float laid = LAYING.progress(chosenFor), y = (top - 80) / 2 - STEP_H / 2 + 20 - (1 - laid) * 40;
		for (int i = 0; i < STEPS.length; i++) {
			float x = x0 + i * (STEP_W + STEP_GAP);
			stepCards[i].set(x, y, STEP_W, STEP_H);
			art.shadow(x, y, STEP_W, STEP_H, 4);
			boolean faceUp = stepOpen(progress, c, i);
			batch.setColor(1, 1, 1, laid);
			if (faceUp) {
				drawStepFace(progress, c, i, x, y, laid);
			} else {
				batch.draw(assets.back(), x, y, STEP_W, STEP_H);
				batch.setColor(Color.WHITE);
				paint.textIn(paint.ui(0.9f, Palette.MUTED, laid), i == STEPS.length - 1 ? "Win the final table" : "Win the table first",
						x - 20, y - 26, STEP_W + 40, 20);
			}
			batch.setColor(Color.WHITE);
		}
	}

	private void drawStepFace(Campaign progress, Chapter c, int step, float x, float y, float alpha) {
		batch.draw(assets.campaignFace, x, y, STEP_W, STEP_H);
		batch.draw(stepIcon(step),
				x + STEP_W / 2 - ICON / 2, y + STEP_H / 2 - ICON / 2 + 4, ICON, ICON);
		batch.setColor(Color.WHITE);
		paint.textIn(paint.serif(1.25f, Palette.GOLD_LIGHT, alpha), STEPS[step], x, y + STEP_H - 46, STEP_W, 28);
		boolean done = stepDone(progress, c, step);
		paint.textIn(paint.ui(0.9f, done ? Palette.GOLD : Palette.CREAM, alpha), stepNote(progress, c, step), x, y + 18, STEP_W, 20);
	}

	private String stepNote(Campaign progress, Chapter c, int step) {
		if (step == SHOTS) {
			return progress.madeIn(c) + " of " + TrickShots.of(c).size() + " made";
		}
		return stepDone(progress, c, step) ? "Won" : "Play";
	}

	private void glow(float x, float y, float cw, float ch) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		float gw = cw * GLOW_W, gh = ch * GLOW_H, pulse = 0.8f + 0.2f * MathUtils.sin(shownFor * 2f);
		batch.setColor(GLOW.r, GLOW.g, GLOW.b, GLOW.a * pulse);
		batch.draw(assets.glow, x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
	}

	private int rivalAt(float x, float y) {
		for (int i = 1; i < playerCards.length; i++) {
			if (playerCards[i].contains(x, y)) {
				return i;
			}
		}
		return 0;
	}

	private static Player player(Chapter c, int i) {
		return i == CHAMPION ? c.champion() : c.rivals.at(i);
	}

	@Override
	protected void tap(float x, float y) {
		if (briefing != null) {
			int tapped = briefing.pages().get(page).picture() == Briefing.Picture.RIVALS ? rivalAt(x, y) : 0;
			if (backButton.contains(x, y)) {
				briefing = null;
			} else if (tapped > 0) {
				rival = tapped == rival ? 0 : tapped;
			} else if (page < briefing.pages().size() - 1) {
				page++;
				rival = 0;
				briefedFor = LAYING.end() / 2;
			} else if (goButton.contains(x, y)) {
				go();
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
		for (int i = 0; i < STEPS.length; i++) {
			if (stepCards[i].contains(x, y) && stepOpen(progress, c, i)) {
				briefStep(progress, c, i);
			}
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
