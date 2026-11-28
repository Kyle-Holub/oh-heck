package ohheck.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.Briefing;
import ohheck.campaign.Campaign;
import ohheck.campaign.Chapter;
import ohheck.campaign.Stage;
import ohheck.campaign.TrickShots;
import ohheck.fx.Corridor;
import ohheck.fx.FallingCards;
import ohheck.fx.Sparks;
import ohheck.platform.Audio;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;

/*
 * The campaign. Arriving from the Play menu, the eye looks out into mist and sparkles, then drops through them to a
 * parchment map lying on the table (ChapterMap): a tavern for each chapter, marked by a wax seal, the trail between
 * them inking itself in. A tapped tavern's seal draws the eye in as the map goes dark and sparkles gather round it,
 * then twists away into itself like a whirlpool, the sparkles turning with it, and becomes a fog through which the
 * chapter fades in: a hall of arched doors, one for each stage (table, trick shots, final table, duel). A
 * tapped door swings open on a dark doorway of mist and sparkles and the camera dives through; the duel's door leads
 * instead into a torchlit corridor walked to the champion. A chapter, and each stage, opens with a briefing of what
 * is about to happen on a first visit: a few pages in the tutorial's style, a picture above and a caption below.
 * Locked seals and doors jiggle their padlocks.
 */
public class CampaignScreen extends BaseScreen {

	private static final float PLAQUE_W = 200, PLAQUE_H = 300, PLAQUE_GAP = 54, PADLOCK = 46, RISE_STAGGER = 0.08f, STAGE_ICON = 70;
	private static final String MAP_LINE = "Four taverns, four champions. Win each chapter's duel to open the road ahead.";
	private static final float GLOW_W = 1.9f, GLOW_H = 1.6f;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f);
	private static final int SHOTS = 1, DUEL = 3;
	private static final String[] STEPS = { "Table", "Trick shots", "Final table", "Duel" },
			STAGE_LABELS = { "STAGE 1", "STAGE 2", "STAGE 3", "STAGE 4" };
	private static final Stage[] STAGE_AT = { Stage.TABLE, null, Stage.FINAL, Stage.DUEL };

	private static final float UNFOLD_LENGTH = 2.4f, TILT_FROM = 82f, DROP = 0.95f, MIST_LIFT = 1.5f, MIST_DEPTH = 700f, FAR = 1400f,
			NEAR = 700f;
	private static final Tween LOOK_DOWN = Tween.over(1.7f, Ease.SMOOTH).after(0.3f), MAP_LIGHT = Tween.over(0.9f, Ease.SMOOTH).after(0.3f),
			MIST_IN = Tween.over(0.5f, Ease.SMOOTH), INKING = Tween.over(0.7f, Ease.SMOOTH).after(1.9f),
			SEALS = Tween.over(0.4f, Ease.OUT).after(2.0f);
	private static final float ENTER_WAIT = UNFOLD_LENGTH + 0.3f, ARRIVE = 0.45f;
	private static final float DOOR_LENGTH = 1.0f, DIVE_SCALE = 9f, DOOR_SWING = 80f;
	private static final Tween DOORS = Tween.over(0.5f, Ease.SMOOTH), DIVE = Tween.over(0.6f, Ease.IN).after(DOOR_LENGTH - 0.6f),
			DOOR_FLASH = Tween.over(0.2f, Ease.IN).after(DOOR_LENGTH - 0.2f), PAN = Tween.over(0.7f, Ease.SMOOTH).after(0.2f);
	private static final float WALK_LENGTH = 2.4f;
	private static final Tween WALK = Tween.over(WALK_LENGTH - 0.1f, Ease.SMOOTH).after(0.1f), WALK_IN = Tween.over(0.5f, Ease.OUT),
			WALK_FLASH = Tween.over(0.3f, Ease.IN).after(WALK_LENGTH - 0.3f);
	private static final Tween JIGGLE = Tween.over(0.45f, Ease.LINEAR);
	private static final float JIGGLE_TURNS = 7, JIGGLE_DEGREES = 14, JIGGLE_SHIFT = 5;
	private static final float DOORWAY_DEPTH = 2000f, DOORWAY_FILL = 0.9f, STREAK_STEP = 1 / 30f, DOORWAY_INSET = 12, DOORWAY_ARCH = 55,
			DOORWAY_SOFT = 46;
	private static final Color DOORWAY_DARK = new Color(0.05f, 0.04f, 0.04f, 1f);
	private static final float REVEAL_WAIT = 0.9f, REVEAL_LEN = 1.6f, REVEAL_FLIP_FROM = 0.35f, REVEAL_FLIP = 0.5f;
	private static final Tween RISE_IN = Tween.over(0.45f, Ease.OUT);
	private static final String INFO = "Chapter info", NEXT = "Next", DONE = "Done";
	private static final float INFO_W = 210;

	private final MenuChrome chrome = new MenuChrome(paint);
	private final CardArt art;
	private final ChapterMap map;
	private final Corridor corridor;
	private final FallingCards doorway, fog;
	private final SealWhirl whirl;
	private final Rectangle backButton = new Rectangle(), goButton = new Rectangle(), infoButton = new Rectangle(), opening = new Rectangle();
	private final Rectangle[] arches = new Rectangle[STEPS.length];
	private final List<Briefing> info = new ArrayList<Briefing>();
	private final Sparks sparks = new Sparks();
	private final Matrix4 zoom = new Matrix4(), still = new Matrix4(), doorwayFit = new Matrix4();
	private final Color flash = new Color();
	private Chapter chosen, entering, revealing, jigglingSeal;
	private int page, rival, door = -1, jigglingArch = -1;
	private float chosenFor, briefedFor, unfoldFor = UNFOLD_LENGTH, enterFor, arrivedFor = ARRIVE, doorFor, walkFor = -1,
			jiggleFor = JIGGLE.end(), revealFor = REVEAL_LEN;
	private String beatLine = "", header = "", shotsNote = "";
	private final BriefingView briefing;

	public CampaignScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
		map = new ChapterMap(paint);
		whirl = new SealWhirl(paint);
		briefing = new BriefingView(paint, art);
		corridor = new Corridor(assets);
		Long seed = game.options().seed;
		doorway = new FallingCards(assets, seed == null ? new Random() : new Random(seed), true);
		doorway.setFalling(false);
		fog = new FallingCards(assets, seed == null ? new Random() : new Random(seed + 1), true);
		fog.setFalling(false);
		for (int i = 0; i < arches.length; i++) {
			arches[i] = new Rectangle();
		}
	}

	public void unfoldMap() {
		chosen = null;
		unfoldFor = 0;
		game.audio().play(Audio.Effect.PAPER);
	}

	public void brief(Briefing b, Runnable then) {
		briefing.show(b, then);
	}

	public void go() {
		briefing.go();
	}

	public void openChapter(Chapter c) {
		chosen = c;
		beatLine = c == null ? "" : "Win the table, then beat " + c.champion().name + " at the final table and in a duel";
		header = c == null ? "" : "Chapter " + c.number();
		noteShots();
		chosenFor = 0;
		briefing.close();
	}

	private void noteShots() {
		shotsNote = chosen == null ? "" : game.records().campaign().madeIn(chosen) + " of " + TrickShots.of(chosen).size() + " made";
	}

	public void enter(Chapter c) {
		entering = c;
		enterFor = 0;
		fog.fly(0, 0);
	}

	public void enterOnArrival(Chapter c) {
		entering = c;
		enterFor = -ENTER_WAIT;
	}

	private void unseal() {
		Chapter c = entering;
		entering = null;
		if (game.settings().seeFirstTime(c.name())) {
			brief(Briefing.chapter(c), () -> openChapter(c));
		}
	}

	public void openStage(int step) {
		door = step;
		doorFor = 0;
		doorway.fly(0, 0);
		game.audio().play(Audio.Effect.DOOR);
	}

	private void throughTheDoor() {
		int step = door;
		door = -1;
		if (step == DUEL) {
			walkFor = 0;
			corridor.walk(0);
			game.audio().play(Audio.Effect.CRACKLE);
		} else {
			arrivedFor = 0;
			openStep(game.records().campaign(), chosen, step);
		}
	}

	private void endOfTheCorridor() {
		walkFor = -1;
		arrivedFor = 0;
		openStep(game.records().campaign(), chosen, DUEL);
	}

	private void openStep(Campaign progress, Chapter c, int step) {
		String intro = c.name() + "." + (STAGE_AT[step] == null ? "SHOTS" : STAGE_AT[step].name());
		if (game.settings().seeFirstTime(intro)) {
			brief(stepBriefing(progress, c, step), () -> playStep(progress, c, step));
		} else {
			playStep(progress, c, step);
		}
	}

	private boolean busy() {
		return entering != null || door >= 0 || walkFor >= 0 || !LOOK_DOWN.done(unfoldFor);
	}

	@Override
	public void show() {
		super.show();
		game.audio().startMusic();
		chosenFor = 0;
		walkFor = -1;
		door = -1;
		noteShots();
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		shownFor += dt;
		chosenFor += dt;
		briefing.update(dt);
		unfoldFor += dt;
		arrivedFor += dt;
		jiggleFor += dt;
		float before = revealFor;
		revealFor += dt;
		sparks.step(dt);
		if (!LOOK_DOWN.done(unfoldFor)) {
			float depth = LOOK_DOWN.progress(unfoldFor) * MIST_DEPTH;
			fog.fly(depth, depth - LOOK_DOWN.progress(unfoldFor - STREAK_STEP) * MIST_DEPTH);
			fog.update(dt, w, h);
		}
		if (entering != null) {
			float was = enterFor;
			enterFor += dt;
			sealMoments(was, enterFor);
			fog.update(dt, w, h);
			if (enterFor >= SealWhirl.LENGTH) {
				unseal();
			}
		}
		if (door >= 0) {
			doorFor += dt;
			float depth = DIVE.progress(doorFor) * DOORWAY_DEPTH;
			doorway.fly(depth, depth - DIVE.progress(doorFor - STREAK_STEP) * DOORWAY_DEPTH);
			doorway.update(dt, w, w * PLAQUE_H / PLAQUE_W);
			if (doorFor >= DOOR_LENGTH) {
				throughTheDoor();
			}
		}
		if (walkFor >= 0) {
			walkFor += dt;
			corridor.update(dt);
			corridor.walk(WALK.progress(walkFor));
			if (walkFor >= WALK_LENGTH) {
				endOfTheCorridor();
			}
		}
		float half = REVEAL_FLIP_FROM + REVEAL_FLIP / 2;
		if (revealing != null && before < half && revealFor >= half) {
			Rectangle r = map.marker(revealing);
			sparks.burst(r.x + r.width / 2, r.y + r.height / 2, 90);
			game.audio().play(Audio.Effect.LATCH);
		}
	}

	@Override
	public boolean animating() {
		return true;
	}

	@Override
	protected void draw() {
		if (walkFor >= 0) {
			drawCorridor();
			return;
		}
		ScreenUtils.clear(Color.BLACK);
		begin();
		Campaign progress = game.records().campaign();
		float top = h - MenuChrome.HEADER_H;
		map.layout(w, top);
		if (chosen == null) {
			drawMap(progress, top);
		} else {
			drawHall(progress, chosen, top);
		}
		if (briefing.showing()) {
			briefing.draw(w, h);
		}
		chrome.header(chosen == null ? "Campaign" : header, backButton, w, h);
		if (chosen != null && !briefing.showing() && door < 0) {
			paint.button(infoButton, INFO, w - 30 - INFO_W / 2, h - MenuChrome.HEADER_H + (MenuChrome.HEADER_H - 44) / 2, INFO_W, 44,
					assets.buttonAlt);
		} else {
			infoButton.set(0, 0, 0, 0);
		}
		if (entering != null && enterFor >= 0) {
			drawSealing();
		}
		drawFlashes();
		batch.end();
	}

	private void sealMoments(float was, float now) {
		if (crossed(was, now, SealWhirl.TWIST.start())) {
			game.audio().play(Audio.Effect.WHOOSH);
		}
		if (crossed(was, now, SealWhirl.OPENS)) {
			openChapter(entering);
		}
	}

	private Matrix4 lookInto(float x, float y, float centring, float scale) {
		return zoom.idt().translate(MathUtils.lerp(x, w / 2, centring), MathUtils.lerp(y, h / 2, centring), 0).scale(scale, scale, 1)
				.translate(-x, -y, 0);
	}

	private static boolean crossed(float was, float now, float at) {
		return was < at && now >= at;
	}

	private void drawSealing() {
		Rectangle m = map.marker(entering);
		whirl.draw(enterFor, m.x + m.width / 2, m.y + m.height / 2, w, h, assets.emblem(entering.emblem), fog);
	}

	private void drawFlashes() {
		float light = door >= 0 ? DOOR_FLASH.progress(doorFor) : 0;
		if (door >= 0) {
			float glow = DIVE.progress(doorFor) * (1 - light), size = w * (0.2f + glow * 1.2f);
			paint.glow(w / 2 - size / 2, h / 2 - size * 0.35f, size, size * 0.7f, Palette.LAMP, 0.45f * glow);
		}
		if (light > 0) {
			paint.rect(0, 0, w, h, flash.set(Palette.FLASH).mul(1, 1, 1, light));
		} else if (arrivedFor < ARRIVE) {
			float fade = 1 - arrivedFor / ARRIVE;
			paint.rect(0, 0, w, h, flash.set(Palette.FLASH).mul(1, 1, 1, fade * fade));
		}
	}

	private void drawMap(Campaign progress, float top) {
		float down = LOOK_DOWN.progress(unfoldFor), tilt = TILT_FROM * (1 - down), distance = MathUtils.lerp(FAR, NEAR, down);
		boolean looking = !LOOK_DOWN.done(unfoldFor);
		if (looking) {
			float table = 0.4f * MAP_LIGHT.progress(unfoldFor), drop = -(1 - down) * h * DROP;
			batch.setColor(table, table, table, 1);
			batch.draw(assets.table, 0, drop, w, h);
			batch.setColor(Color.WHITE);
			map.drawMap(tilt, distance, MAP_LIGHT.progress(unfoldFor), drop);
			float mistY = h / 2 + down * h * MIST_LIFT;
			if (MIST_IN.progress(unfoldFor) > 0) {
				fog.drawMist(batch, w / 2, mistY);
				fog.draw(batch, w / 2, mistY);
			}
			paint.rect(0, 0, w, h, flash.set(0, 0, 0, 1 - MIST_IN.progress(unfoldFor)));
			return;
		}
		if (entering != null && enterFor >= 0) {
			Rectangle m = map.marker(entering);
			batch.setTransformMatrix(lookInto(m.x + m.width / 2, m.y + m.height / 2, SealWhirl.ZOOM_IN.progress(enterFor), SealWhirl.scale(enterFor)));
		}
		map.drawMap(0, NEAR, 1, 0);
		float seals = SEALS.progress(unfoldFor), pulse = 0.8f + 0.2f * MathUtils.sin(shownFor * 2f);
		map.drawTrail(progress, INKING.progress(unfoldFor));
		Chapter lit = current(progress);
		for (Chapter c : Chapter.values()) {
			if (c == entering && enterFor >= 0) {
				continue;
			}
			boolean showing = c == revealing && revealFor < REVEAL_LEN;
			float opened = !progress.unlocked(c) ? 0 : showing ? MathUtils.clamp((revealFor - REVEAL_FLIP_FROM) / REVEAL_FLIP, 0, 1) : 1;
			if (progress.open(c)) {
				paint.pressable(map.marker(c));
			}
			map.drawMarker(progress, c, seals, opened, c == jigglingSeal ? JIGGLE.raw(jiggleFor) : 1, c == lit, pulse);
		}
		paint.centred(paint.ui(1.0f, Palette.MAP_INK, seals), MAP_LINE, w / 2, top - 30);
		if (entering == null) {
			sparks.draw(batch, assets.glow);
		}
		batch.setTransformMatrix(still);
	}

	private void drawHall(Campaign progress, Chapter c, float top) {
		if (door >= 0) {
			Rectangle a = arches[door];
			float pan = PAN.progress(doorFor), scale = 1 + (DIVE_SCALE - 1) * DIVE.progress(doorFor);
			float cx = a.x + a.width / 2, cy = a.y + a.height * 0.45f;
			batch.setTransformMatrix(lookInto(cx, cy, pan, scale));
		}
		batch.draw(assets.table, 0, 0, w, h);
		paint.centred(paint.serif(1.5f, Palette.GOLD_LIGHT), c.mode.label, w / 2, top - 22);
		paint.centred(paint.ui(0.95f, Palette.MUTED), beatLine, w / 2, top - 52);
		float span = STEPS.length * PLAQUE_W + (STEPS.length - 1) * PLAQUE_GAP, x0 = (w - span) / 2, base = (top - PLAQUE_H) / 2 - 14;
		int lit = -1;
		for (int i = 0; i < STEPS.length && lit < 0; i++) {
			if (stepOpen(progress, c, i) && !stepDone(progress, c, i)) {
				lit = i;
			}
		}
		for (int i = 0; i < STEPS.length; i++) {
			float rise = RISE_IN.progress(chosenFor - i * RISE_STAGGER), x = x0 + i * (PLAQUE_W + PLAQUE_GAP), y = base - (1 - rise) * 30;
			boolean open = stepOpen(progress, c, i);
			arches[i].set(x, y, PLAQUE_W, PLAQUE_H);
			paint.pressable(arches[i]);
			if (i == lit) {
				glow(x, y, PLAQUE_W, PLAQUE_H);
			}
			if (i == door) {
				drawOpeningDoors(x, y, rise);
				archFace(progress, c, i, x, y, true, rise * Math.max(0, 1 - 3 * DOORS.progress(doorFor)));
				continue;
			}
			batch.setColor(1, 1, 1, rise);
			batch.draw(open ? assets.plaque : assets.plaqueLocked, x, y, PLAQUE_W, PLAQUE_H);
			batch.setColor(Color.WHITE);
			archFace(progress, c, i, x, y, open, rise);
		}
		batch.setTransformMatrix(still);
	}

	private void archFace(Campaign progress, Chapter c, int step, float x, float y, boolean open, float alpha) {
		float cx = x + PLAQUE_W / 2, iy = y + PLAQUE_H * 0.46f;
		paint.centred(paint.ui(0.8f, Palette.GOLD, alpha), STAGE_LABELS[step], cx, y + PLAQUE_H - 40);
		paint.centred(paint.serif(1.4f, open ? Palette.GOLD_LIGHT : Palette.MUTED, alpha), STEPS[step], cx, y + PLAQUE_H - 66);
		if (open) {
			batch.setColor(1, 1, 1, alpha);
			batch.draw(stepIcon(step), cx - STAGE_ICON / 2, iy - STAGE_ICON / 2, STAGE_ICON, STAGE_ICON);
			batch.setColor(Color.WHITE);
			boolean done = stepDone(progress, c, step);
			paint.centred(paint.ui(0.95f, done ? Palette.GOLD : Palette.CREAM, alpha), stepNote(progress, c, step), cx, y + 62);
			return;
		}
		float shake = step == jigglingArch ? 1 - JIGGLE.progress(jiggleFor) : 0;
		float swing = MathUtils.sin(JIGGLE.raw(jiggleFor) * JIGGLE_TURNS * MathUtils.PI2) * shake;
		int tw = assets.padlock.getWidth(), th = assets.padlock.getHeight();
		batch.setColor(1, 1, 1, alpha);
		batch.draw(assets.padlock, cx - PADLOCK / 2 + swing * JIGGLE_SHIFT, iy - PADLOCK / 2, PADLOCK / 2, PADLOCK, PADLOCK, PADLOCK, 1, 1,
				swing * JIGGLE_DEGREES, 0, 0, tw, th, false, false);
		batch.setColor(Color.WHITE);
		paint.textIn(paint.ui(0.85f, Palette.MUTED, alpha), step == DUEL ? "Win the final table" : "Win the table first", x + 16, y + 52,
				PLAQUE_W - 32, 20);
	}

	private void drawOpeningDoors(float x, float y, float alpha) {
		float open = DOORS.progress(doorFor), leaf = PLAQUE_W / 2 * MathUtils.cosDeg(open * DOOR_SWING);
		int tw = assets.plaque.getWidth(), th = assets.plaque.getHeight();
		batch.setColor(DOORWAY_DARK.r, DOORWAY_DARK.g, DOORWAY_DARK.b, alpha);
		batch.draw(assets.plaque, x, y, PLAQUE_W, PLAQUE_H);
		float fit = PLAQUE_W * DOORWAY_FILL / w;
		boolean clipped = clipTo(opening.set(x + DOORWAY_INSET, y + DOORWAY_INSET, PLAQUE_W - 2 * DOORWAY_INSET,
				PLAQUE_H - DOORWAY_INSET - DOORWAY_ARCH));
		doorwayFit.set(batch.getTransformMatrix()).translate(x + PLAQUE_W / 2, y + PLAQUE_H / 2, 0).scale(fit, fit, 1);
		batch.setTransformMatrix(doorwayFit);
		doorway.drawMist(batch, 0, 0);
		doorway.draw(batch, 0, 0);
		batch.setTransformMatrix(zoom);
		softenEdges(opening, alpha);
		if (clipped) {
			unclip();
		}
		float shade = 1 - 0.45f * open;
		batch.setColor(shade, shade, shade, alpha);
		batch.draw(assets.plaque, x, y, leaf, PLAQUE_H, 0, 0, tw / 2, th, false, false);
		batch.draw(assets.plaque, x + PLAQUE_W - leaf, y, leaf, PLAQUE_H, tw / 2, 0, tw / 2, th, false, false);
		batch.setColor(Color.WHITE);
	}

	private void softenEdges(Rectangle r, float alpha) {
		int tw = assets.fadeDown.getWidth(), th = assets.fadeDown.getHeight();
		batch.setColor(DOORWAY_DARK.r, DOORWAY_DARK.g, DOORWAY_DARK.b, alpha);
		batch.draw(assets.fadeDown, r.x, r.y + r.height - DOORWAY_SOFT, r.width, DOORWAY_SOFT);
		batch.draw(assets.fadeDown, r.x + DOORWAY_SOFT, r.y, 0, 0, r.height, DOORWAY_SOFT, 1, 1, 90, 0, 0, tw, th, false, false);
		batch.draw(assets.fadeDown, r.x + r.width - DOORWAY_SOFT, r.y + r.height, 0, 0, r.height, DOORWAY_SOFT, 1, 1, -90, 0, 0, tw, th,
				false, false);
		batch.setColor(Color.WHITE);
	}

	private void drawCorridor() {
		ScreenUtils.clear(Color.BLACK);
		begin();
		corridor.draw(batch, w, h);
		float in = 1 - WALK_IN.progress(walkFor);
		paint.rect(0, 0, w, h, flash.set(Palette.FLASH).mul(1, 1, 1, Math.max(in * in, WALK_FLASH.progress(walkFor))));
		batch.end();
	}

	private void glow(float x, float y, float cw, float ch) {
		float gw = cw * GLOW_W, gh = ch * GLOW_H, pulse = 0.8f + 0.2f * MathUtils.sin(shownFor * 2f);
		paint.glow(x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh, GLOW, GLOW.a * pulse);
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
			briefing.close();
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

	private String stepNote(Campaign progress, Chapter c, int step) {
		if (step == SHOTS) {
			return shotsNote;
		}
		return stepDone(progress, c, step) ? "Won" : "Play";
	}

	private Texture stepIcon(int step) {
		return step == SHOTS ? assets.stepShots : STAGE_AT[step] == Stage.DUEL ? assets.stepDuel : assets.stepTable;
	}

	public void briefTable(Chapter c) {
		game.settings().seeFirstTime(c.name() + "." + Stage.TABLE.name());
		brief(Briefing.table(c), () -> game.playCampaign(c, Stage.TABLE));
	}

	public void reveal(Chapter next) {
		chosen = null;
		briefing.close();
		revealing = next;
		revealFor = -REVEAL_WAIT;
	}

	private Chapter current(Campaign progress) {
		for (Chapter c : Chapter.values()) {
			if (progress.open(c) && !progress.cleared(c)) {
				return c;
			}
		}
		return null;
	}

	private void jiggle() {
		jiggleFor = 0;
		game.audio().play(Audio.Effect.TANKARD);
		game.audio().buzz(Audio.Buzz.DOUBLE);
	}

	@Override
	protected void tap(float x, float y) {
		if (walkFor >= 0) {
			endOfTheCorridor();
			return;
		}
		if (busy()) {
			return;
		}
		if (briefing.showing()) {
			briefing.tap(x, y, backButton.contains(x, y));
			return;
		}
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		Campaign progress = game.records().campaign();
		if (chosen == null) {
			for (Chapter c : Chapter.values()) {
				if (map.marker(c).contains(x, y)) {
					if (progress.open(c)) {
						enter(c);
					} else {
						jigglingSeal = c;
						jiggle();
					}
					return;
				}
			}
			return;
		}
		if (infoButton.contains(x, y)) {
			showInfo(progress, chosen);
			return;
		}
		for (int i = 0; i < STEPS.length; i++) {
			if (arches[i].contains(x, y)) {
				if (stepOpen(progress, chosen, i)) {
					openStage(i);
				} else {
					jigglingArch = i;
					jiggle();
				}
				return;
			}
		}
	}

	@Override
	protected void back() {
		if (busy()) {
			return;
		}
		if (briefing.showing()) {
			briefing.back();
		} else if (chosen != null) {
			chosen = null;
		} else {
			game.back();
		}
	}

	@Override
	public String stateTag() {
		if (!LOOK_DOWN.done(unfoldFor)) {
			return "unfolding";
		}
		if (entering != null && enterFor >= 0) {
			return "entering";
		}
		if (door >= 0) {
			return "door";
		}
		if (walkFor >= 0) {
			return "corridor";
		}
		if (briefing.showing()) {
			return briefing.tag();
		}
		return chosen == null ? "campaign" : "chapter:" + chosen.number();
	}
}
