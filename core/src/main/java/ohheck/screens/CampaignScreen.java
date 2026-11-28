package ohheck.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
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
import ohheck.fx.Sparks;
import ohheck.platform.Audio;
import ohheck.players.Player;
import ohheck.rules.Suit;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.CardWipe;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * The campaign as a hand of chapter cards: open chapters face up with a pip for each step done,
 * the rest face down, the chapter in play lifted and lit. A tapped chapter lays its four steps
 * (table, trick shots, final table, duel) on the felt like a trick. A chapter, and each step, opens with a
 * briefing of what is about to happen: a few pages in the tutorial's style, a picture above and a caption below.
 * Entering a chapter swings its doors open on the light behind and dives the camera through the arch into the
 * chapter, where a first visit's briefing waits.
 */
public class CampaignScreen extends BaseScreen {

	private static final float PLAQUE_W = 200, PLAQUE_H = 300, PLAQUE_GAP = 54, PLAQUE_EMBLEM = 58, PADLOCK = 46, PATH_GEM = 18,
			RISE_STAGGER = 0.08f, LIFT = 26;
	private static final String HALL_TITLE = "Four tables, four champions", HALL_LINE = "Win each chapter's duel to open the next door.";
	private static final String[] ROMAN = { "I", "II", "III", "IV" };
	private static final float STEP_W = 136, STEP_H = STEP_W * 98 / 73, STEP_GAP = 36, PIP = 16, PIP_GAP = 24, ICON = 64;
	private static final float GLOW_W = 1.9f, GLOW_H = 1.6f;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f);
	private static final Tween LAYING = Tween.over(0.35f, Ease.OUT);
	private static final float CAPTION_H = 112, STAGE_CAPTION_H = 136, CAPTION_W = 860, BIG_W = 170, BIG_H = BIG_W * 98 / 73, SMALL_W = 120,
			SMALL_H = SMALL_W * 98 / 73, BIG_ICON = 84,
			BUTTON_ROOM = 64, PLAYER_GAP = 24, PANEL_GAP = 34, HABIT_W = 640, HABIT_H = 62;
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
	private float wipeFor = CardWipe.LENGTH, revealFor = REVEAL_LEN, hallFor;
	private static final Tween RISE_IN = Tween.over(0.45f, Ease.OUT);
	private Chapter revealing;
	private final Sparks sparks = new Sparks();
	private static final float REVEAL_WAIT = CardWipe.LENGTH + 0.3f, REVEAL_LEN = 1.6f, REVEAL_FLIP_FROM = 0.35f, REVEAL_FLIP = 0.5f;
	private String beatLine = "";
	private final StringBuilder stageText = new StringBuilder();
	private Briefing briefing;
	private final List<Briefing> info = new ArrayList<Briefing>();
	private final Rectangle infoButton = new Rectangle();
	private static final String INFO = "Chapter info", NEXT = "Next", DONE = "Done";
	private static final float INFO_W = 210;
	private Runnable afterBriefing;
	private static final float ENTER_WAIT = 0.9f, ENTER_LENGTH = 1.0f, DIVE_SCALE = 9f, DOOR_SWING = 80f, ARRIVE = 0.45f;
	private static final Tween DOORS = Tween.over(0.5f, Ease.SMOOTH), DIVE = Tween.over(0.6f, Ease.IN).after(ENTER_LENGTH - 0.6f),
			ENTER_FLASH = Tween.over(0.2f, Ease.IN).after(ENTER_LENGTH - 0.2f);
	private Chapter entering;
	private float enterFor, arrivedFor = ARRIVE;
	private final Matrix4 dive = new Matrix4(), still = new Matrix4();
	private final Color flash = new Color();

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

	public void reveal(Chapter next) {
		chosen = null;
		briefing = null;
		revealing = next;
		revealFor = -REVEAL_WAIT;
	}

	public void wipeIn() {
		wipeFor = 0;
	}

	public void brief(Briefing b, Runnable then) {
		briefing = b;
		afterBriefing = then;
		briefedFor = 0;
		page = 0;
		rival = 0;
		stageText.setLength(0);
		for (Briefing.Page p : b.pages()) {
			stageText.append(stageText.length() > 0 ? " " : "").append(p.line());
		}
	}

	private void closeOrTurnBack() {
		if (!briefing.isStage() && page > 0) {
			page--;
			rival = 0;
		} else {
			briefing = null;
		}
	}

	public void enter(Chapter c) {
		entering = c;
		enterFor = 0;
		game.audio().play(Audio.Effect.CREAK);
	}

	public void enterOnArrival(Chapter c) {
		entering = c;
		enterFor = -ENTER_WAIT;
	}

	private void arrive() {
		Chapter c = entering;
		entering = null;
		arrivedFor = 0;
		openChapter(c);
		if (game.settings().seeFirstTime(c.name())) {
			brief(Briefing.chapter(c), () -> openChapter(c));
		}
	}

	public void briefTable(Chapter c) {
		game.settings().seeFirstTime(c.name() + "." + Stage.TABLE.name());
		brief(Briefing.table(c), () -> game.playCampaign(c, Stage.TABLE));
	}

	private Briefing stepBriefing(Campaign progress, Chapter c, int step) {
		Stage s = STAGE_AT[step];
		return s == null ? Briefing.trickShots(c, progress) : s == Stage.TABLE ? Briefing.table(c) : s == Stage.FINAL ? Briefing.finalTable(c)
				: Briefing.duel(c);
	}

	private void playStep(Campaign progress, Chapter c, int step) {
		Stage s = STAGE_AT[step];
		if (s == null) {
			game.playTrickShot(progress.nextTrickShot(c));
		} else {
			game.playCampaign(c, s);
		}
	}

	private void openStep(Campaign progress, Chapter c, int step) {
		String intro = c.name() + "." + (STAGE_AT[step] == null ? "SHOTS" : STAGE_AT[step].name());
		if (game.settings().seeFirstTime(intro)) {
			brief(stepBriefing(progress, c, step), () -> playStep(progress, c, step));
		} else {
			playStep(progress, c, step);
		}
	}

	private void showInfo(Campaign progress, Chapter c) {
		info.clear();
		info.add(Briefing.chapter(c));
		for (int step = 0; step < STEPS.length; step++) {
			info.add(stepBriefing(progress, c, step));
		}
		nextInfo();
	}

	private void nextInfo() {
		if (info.isEmpty()) {
			briefing = null;
			return;
		}
		Briefing b = info.remove(0);
		brief(new Briefing(b.tag(), b.chapter(), info.isEmpty() ? DONE : NEXT, b.pages()), this::nextInfo);
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
		hallFor = 0;
	}

	@Override
	protected void update(float step) {
		shownFor += step * game.options().speed;
		chosenFor += step * game.options().speed;
		briefedFor += step * game.options().speed;
		wipeFor += step * game.options().speed;
		float before = revealFor;
		revealFor += step * game.options().speed;
		hallFor += step * game.options().speed;
		arrivedFor += step * game.options().speed;
		if (entering != null) {
			float was = enterFor;
			enterFor += step * game.options().speed;
			if (was < 0 && enterFor >= 0) {
				game.audio().play(Audio.Effect.CREAK);
			}
			if (enterFor >= ENTER_LENGTH) {
				arrive();
			}
		}
		if (revealing != null && before < 0 && revealFor >= 0) {
			game.audio().play(Audio.Effect.CREAK);
		}
		sparks.step(step * game.options().speed);
		float half = REVEAL_FLIP_FROM + REVEAL_FLIP / 2;
		if (revealing != null && before < half && revealFor >= half) {
			Rectangle r = chapterCards[revealing.ordinal()];
			sparks.burst(r.x + r.width / 2, r.y + r.height / 2, 90);
		}
	}

	@Override
	public boolean animating() {
		return entering != null || arrivedFor < ARRIVE || chosen == null && briefing == null || chosen != null && !LAYING.done(chosenFor) || briefing != null && !LAYING.done(briefedFor) || wipeFor < CardWipe.LENGTH || revealing != null && revealFor < REVEAL_LEN || sparks.active();
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		Campaign progress = game.records().campaign();
		if (entering != null) {
			Rectangle door = chapterCards[entering.ordinal()];
			float p = DIVE.progress(enterFor), scale = 1 + (DIVE_SCALE - 1) * p;
			float cx = door.x + door.width / 2, cy = door.y + door.height * 0.45f;
			batch.setTransformMatrix(dive.idt().translate(MathUtils.lerp(cx, w / 2, p), MathUtils.lerp(cy, h / 2, p), 0).scale(scale, scale, 1)
					.translate(-cx, -cy, 0));
		}
		batch.draw(assets.table, 0, 0, w, h);
		if (chosen == null) {
			drawHand(progress);
			batch.setTransformMatrix(still);
		} else {
			drawChapter(progress, chosen);
		}
		if (briefing != null) {
			drawBriefing();
		}
		chrome.header(chosen == null ? "Campaign" : "Chapter " + chosen.number(), backButton, w, h);
		if (chosen != null && briefing == null) {
			paint.button(infoButton, INFO, w - 30 - INFO_W / 2, h - MenuChrome.HEADER_H + (MenuChrome.HEADER_H - 44) / 2, INFO_W, 44,
					assets.buttonAlt);
		} else {
			infoButton.set(0, 0, 0, 0);
		}
		CardWipe.draw(batch, assets.back(), w, h, wipeFor);
		if (entering != null) {
			paint.rect(0, 0, w, h, flash.set(Palette.FLASH).mul(1, 1, 1, ENTER_FLASH.progress(enterFor)));
		} else if (arrivedFor < ARRIVE) {
			float fade = 1 - arrivedFor / ARRIVE;
			paint.rect(0, 0, w, h, flash.set(Palette.FLASH).mul(1, 1, 1, fade * fade));
		}
		batch.end();
	}

	private void drawBriefing() {
		float a = LAYING.progress(briefedFor);
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		if (briefing.isStage()) {
			drawStage(a);
			return;
		}
		Briefing.Page p = briefing.pages().get(page);
		float cw = Math.min(w - 60, CAPTION_W), x = (w - cw) / 2, y = 12;
		float midY = (y + CAPTION_H + BUTTON_ROOM + h - MenuChrome.HEADER_H) / 2 - (1 - a) * 30;
		paint.fade(a);
		drawPicture(p, midY, a);
		paint.frame(assets.panel, x, y, cw, CAPTION_H);
		paint.fade(1f);
		paint.serif(1.35f, Palette.GOLD, a).draw(batch, p.heading(), x + 22, y + CAPTION_H - 12);
		paint.ui(1.1f, Palette.CREAM, a).draw(batch, p.line(), x + 22, y + CAPTION_H - 44, cw - 44, Align.left, true);
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

	private void drawStage(float a) {
		List<Briefing.Page> pages = briefing.pages();
		float top = h - MenuChrome.HEADER_H, boxTop = 12 + STAGE_CAPTION_H, rise = (1 - a) * 30;
		paint.centred(paint.serif(1.6f, Palette.GOLD, a), pages.get(0).heading(), w / 2, top - 22 + rise);
		float cy = (boxTop + top - 50) / 2 - rise, span = pages.size() * SMALL_W + (pages.size() - 1) * PANEL_GAP;
		for (int i = 0; i < pages.size(); i++) {
			panel(pages.get(i), (w - span) / 2 + i * (SMALL_W + PANEL_GAP) + SMALL_W / 2, cy, a);
		}
		float bw = 180, boxW = w - 60 - bw - 16;
		paint.fade(a);
		paint.frame(assets.panel, 30, 12, boxW, STAGE_CAPTION_H);
		paint.fade(1f);
		paint.ui(1.1f, Palette.CREAM, a).draw(batch, stageText, 52, 12 + STAGE_CAPTION_H - 18, boxW - 44, Align.left, true);
		paint.button(goButton, briefing.go(), w - 30 - bw / 2, 12 + (STAGE_CAPTION_H - 44) / 2, bw, 44, assets.button);
	}

	private void panel(Briefing.Page p, float cx, float cy, float a) {
		Texture icon = switch (p.picture()) {
		case TABLE -> assets.stepTable;
		case WIN -> assets.badge;
		case SHOTS -> assets.stepShots;
		case CHAMPION -> assets.stepDuel;
		default -> null;
		};
		if (icon != null) {
			smallCard(cx, cy, icon, p.figure(), a);
			return;
		}
		float x = cx - SMALL_W / 2, y = cy - SMALL_H / 2;
		art.shadow(x, y, SMALL_W, SMALL_H, 4);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, SMALL_W, SMALL_H);
		batch.setColor(Color.WHITE);
		paint.textIn(paint.serif(2.6f, Palette.GOLD_LIGHT, a), p.figure(), x, cy - 20, SMALL_W, 40);
		paint.textIn(paint.ui(0.85f, Palette.GOLD, a), p.heading().toUpperCase(), x, y + 14, SMALL_W, 20);
	}

	private void drawPicture(Briefing.Page p, float cy, float a) {
		Chapter c = briefing.chapter();
		switch (p.picture()) {
		case CHAPTER:
			bigCard(w / 2, cy, assets.emblem(c.emblem), p.figure(), a);
			paint.centred(paint.ui(0.9f, Palette.GOLD, a), "CHAPTER", w / 2, cy + BIG_H / 2 - 20);
			break;
		case HOUSE_RULES:
			paint.serif(2.8f, Palette.GOLD_LIGHT, a).draw(batch, p.figure(), 0, cy + 60, w, Align.center, false);
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
			if (rival > 0) {
				Player shown = player(c, rival);
				float bw = Math.min(w - 60, HABIT_W), bx = (w - bw) / 2, by = cy - SMALL_H / 2 - 12 - HABIT_H;
				paint.frame(assets.panel, bx, by, bw, HABIT_H);
				paint.serif(1.2f, Palette.GOLD, a).draw(batch, shown.name, bx + 18, by + HABIT_H - 10);
				paint.ui(1.0f, Palette.CREAM, a).draw(batch, shown.trait, bx + 18, by + HABIT_H - 36, bw - 36, Align.left, true);
			}
			break;
		case CHAMPION:
			bigCard(w / 2, cy, assets.stepDuel, p.figure(), a);
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
		float top = h - MenuChrome.HEADER_H, span = all.length * PLAQUE_W + (all.length - 1) * PLAQUE_GAP, x0 = (w - span) / 2;
		float base = (top - PLAQUE_H) / 2 - 6, pathY = base + PLAQUE_H * 0.42f;
		Chapter lit = current(progress);
		paint.centred(paint.serif(1.5f, Palette.GOLD_LIGHT), HALL_TITLE, w / 2, top - 22);
		paint.centred(paint.ui(0.95f, Palette.MUTED), HALL_LINE, w / 2, top - 52);
		for (int i = 0; i + 1 < all.length; i++) {
			float from = x0 + i * (PLAQUE_W + PLAQUE_GAP) + PLAQUE_W, mid = from + PLAQUE_GAP / 2;
			Color line = progress.cleared(all[i]) ? Palette.GOLD : Palette.MUTED;
			paint.rect(from, pathY - 1, PLAQUE_GAP, 2, line);
			batch.setColor(line);
			batch.draw(assets.emblem(Suit.DIAMONDS), mid - PATH_GEM / 2, pathY - PATH_GEM / 2, PATH_GEM, PATH_GEM);
			batch.setColor(Color.WHITE);
		}
		for (int i = 0; i < all.length; i++) {
			Chapter c = all[i];
			boolean showing = c == revealing && revealFor < REVEAL_LEN;
			float k = showing ? MathUtils.clamp(revealFor / REVEAL_LEN, 0, 1) : 0;
			float rise = RISE_IN.progress(hallFor - i * RISE_STAGGER);
			float x = x0 + i * (PLAQUE_W + PLAQUE_GAP), y = base;
			chapterCards[i].set(x, y, PLAQUE_W, PLAQUE_H);
			if (progress.open(c)) {
				paint.pressable(chapterCards[i]);
			}
			if (showing && revealFor > 0) {
				pourLight(x, y, k);
			}
			if (c == lit || showing && k > 0) {
				glow(x, y, PLAQUE_W, PLAQUE_H);
			}
			float opened = !progress.unlocked(c) ? 0 : showing ? MathUtils.clamp((revealFor - REVEAL_FLIP_FROM) / REVEAL_FLIP, 0, 1) : 1;
			if (c == entering) {
				drawOpeningDoors(x, y, rise);
				plaqueFace(progress, c, x, y, 1, rise * Math.max(0, 1 - 3 * DOORS.progress(enterFor)));
				continue;
			}
			if (opened < 1) {
				batch.setColor(1, 1, 1, rise);
				batch.draw(assets.plaqueLocked, x, y, PLAQUE_W, PLAQUE_H);
			}
			if (opened > 0) {
				batch.setColor(1, 1, 1, rise * opened);
				batch.draw(assets.plaque, x, y, PLAQUE_W, PLAQUE_H);
			}
			batch.setColor(Color.WHITE);
			plaqueFace(progress, c, x, y, opened, rise);
		}
		sparks.draw(batch, assets.glow);
		paint.centred(paint.ui(1.0f, Palette.MUTED), "Tap a chapter", w / 2, base - 34);
	}

	private void plaqueFace(Campaign progress, Chapter c, float x, float y, float opened, float alpha) {
		float cx = x + PLAQUE_W / 2, shut = 1 - opened, ey = y + PLAQUE_H * 0.44f;
		paint.centred(paint.ui(0.8f, Palette.GOLD, alpha), "CHAPTER", cx, y + PLAQUE_H - 40);
		paint.centred(paint.serif(2.4f, opened > 0 ? Palette.GOLD_LIGHT : Palette.MUTED, alpha), ROMAN[c.ordinal()], cx, y + PLAQUE_H - 64);
		if (opened > 0) {
			batch.setColor(1, 1, 1, alpha * opened);
			batch.draw(assets.emblem(c.emblem), cx - PLAQUE_EMBLEM / 2, ey - PLAQUE_EMBLEM / 2, PLAQUE_EMBLEM, PLAQUE_EMBLEM);
			batch.setColor(Color.WHITE);
			paint.centred(paint.serif(1.35f, Palette.GOLD_LIGHT, alpha * opened), c.mode.label, cx, y + 86);
			paint.centred(paint.ui(0.85f, Palette.CREAM, alpha * opened), "vs " + c.champion().name, cx, y + 60);
			batch.setColor(1, 1, 1, alpha * opened);
			pips(cx, y + 22, progress.won(c, Stage.TABLE), progress.allMade(c), progress.won(c, Stage.FINAL), progress.cleared(c));
			batch.setColor(Color.WHITE);
		}
		if (shut > 0) {
			float lock = PADLOCK * (1 + opened * 0.8f);
			batch.setColor(1, 1, 1, alpha * shut);
			batch.draw(assets.padlock, cx - lock / 2, ey - lock / 2, lock, lock);
			batch.setColor(Color.WHITE);
			paint.centred(paint.ui(0.9f, Palette.MUTED, alpha * shut), "Locked", cx, y + 60);
		}
	}

	private void drawOpeningDoors(float x, float y, float alpha) {
		float open = DOORS.progress(enterFor), leaf = PLAQUE_W / 2 * MathUtils.cosDeg(open * DOOR_SWING);
		int tw = assets.plaque.getWidth(), th = assets.plaque.getHeight();
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(Palette.LAMP.r, Palette.LAMP.g, Palette.LAMP.b, open);
		batch.draw(assets.plaque, x, y, PLAQUE_W, PLAQUE_H);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		paint.glow(x - PLAQUE_W, y - PLAQUE_H * 0.6f, PLAQUE_W * 3, PLAQUE_H * 2.2f, Palette.LAMP, 0.25f * open);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(Palette.LAMP.r, Palette.LAMP.g, Palette.LAMP.b, open * DIVE.progress(enterFor));
		batch.draw(assets.plaque, x, y, PLAQUE_W, PLAQUE_H);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		float shade = 1 - 0.45f * open;
		batch.setColor(shade, shade, shade, alpha);
		batch.draw(assets.plaque, x, y, leaf, PLAQUE_H, 0, 0, tw / 2, th, false, false);
		batch.draw(assets.plaque, x + PLAQUE_W - leaf, y, leaf, PLAQUE_H, tw / 2, 0, tw / 2, th, false, false);
		batch.setColor(Color.WHITE);
	}

	private void pourLight(float x, float y, float k) {
		float a = MathUtils.sin(k * MathUtils.PI), bw = PLAQUE_W * (0.6f + 1.6f * k), bh = h * 1.3f;
		paint.glow(x + PLAQUE_W / 2 - bw / 2, y + PLAQUE_H / 2 - bh / 2, bw, bh, Palette.SPARKLE, 0.55f * a);
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
			if (stepOpen(progress, c, i)) {
				paint.pressable(stepCards[i]);
			}
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
		float gw = cw * GLOW_W, gh = ch * GLOW_H, pulse = 0.8f + 0.2f * MathUtils.sin(shownFor * 2f);
		paint.glow(x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh, GLOW, GLOW.a * pulse);
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
		if (entering != null) {
			return;
		}
		if (briefing != null) {
			if (briefing.isStage()) {
				if (backButton.contains(x, y)) {
					briefing = null;
				} else if (goButton.contains(x, y)) {
					go();
				}
				return;
			}
			int tapped = briefing.pages().get(page).picture() == Briefing.Picture.RIVALS ? rivalAt(x, y) : 0;
			if (backButton.contains(x, y)) {
				closeOrTurnBack();
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
					enter(c);
					return;
				}
			}
			return;
		}
		Chapter c = chosen;
		if (infoButton.contains(x, y)) {
			showInfo(progress, c);
			return;
		}
		for (int i = 0; i < STEPS.length; i++) {
			if (stepCards[i].contains(x, y) && stepOpen(progress, c, i)) {
				openStep(progress, c, i);
			}
		}
	}

	@Override
	protected void back() {
		if (entering != null) {
			return;
		}
		if (briefing != null) {
			closeOrTurnBack();
		} else if (chosen != null) {
			chosen = null;
		} else {
			game.back();
		}
	}

	@Override
	public String stateTag() {
		if (entering != null) {
			return "entering";
		}
		if (briefing != null) {
			return briefing.tag();
		}
		return chosen == null ? "campaign" : "chapter:" + chosen.number();
	}
}
