package ohheck.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import ohheck.fx.Corridor;
import ohheck.fx.FallingCards;
import ohheck.fx.Sparks;
import ohheck.platform.Audio;
import ohheck.players.Player;
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

	private static final float PLAQUE_W = 200, PLAQUE_H = 300, PLAQUE_GAP = 54, PADLOCK = 46, RISE_STAGGER = 0.08f, LIFT = 26, ICON = 64,
			STAGE_ICON = 70;
	private static final String MAP_LINE = "Four taverns, four champions. Win each chapter's duel to open the road ahead.";
	private static final float GLOW_W = 1.9f, GLOW_H = 1.6f, GILT = 1.3f, FIT_PAD = 12, STAGE_TEXT = 1.1f, STAGE_TEXT_LEAST = 0.75f,
			STAGE_TEXT_ROOM = 30;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f);
	private static final Tween LAYING = Tween.over(0.35f, Ease.OUT);
	private static final float CAPTION_H = 112, STAGE_CAPTION_H = 136, CAPTION_W = 860, BIG_W = 170, BIG_H = BIG_W * 98 / 73, SMALL_W = 120,
			SMALL_H = SMALL_W * 98 / 73, BIG_ICON = 84,
			BUTTON_ROOM = 64, PLAYER_GAP = 24, PANEL_GAP = 34, HABIT_W = 640, HABIT_H = 62;
	private static final int SHOTS = 1, DUEL = 3, CHAMPION = 4;
	private static final String[] STEPS = { "Table", "Trick shots", "Final table", "Duel" },
			STAGE_LABELS = { "STAGE 1", "STAGE 2", "STAGE 3", "STAGE 4" };
	private static final Stage[] STAGE_AT = { Stage.TABLE, null, Stage.FINAL, Stage.DUEL };

	private static final float UNFOLD_LENGTH = 2.4f, TILT_FROM = 82f, DROP = 0.95f, MIST_LIFT = 1.5f, MIST_DEPTH = 700f, FAR = 1400f,
			NEAR = 700f;
	private static final Tween LOOK_DOWN = Tween.over(1.7f, Ease.SMOOTH).after(0.3f), MAP_LIGHT = Tween.over(0.9f, Ease.SMOOTH).after(0.3f),
			MIST_IN = Tween.over(0.5f, Ease.SMOOTH), INKING = Tween.over(0.7f, Ease.SMOOTH).after(1.9f),
			SEALS = Tween.over(0.4f, Ease.OUT).after(2.0f);
	private static final float ENTER_WAIT = UNFOLD_LENGTH + 0.3f, ENTER_LENGTH = 2.6f, OPEN_AT = 1.75f, ZOOM_SCALE = 2.6f, SMALL_SEAL = 104,
			TWIST_TURN = 900f, TWIST_SPIN = 0.35f, TWIST_SHRINK = 0.75f, SWIRL_SPEED = 1.5f, FOG_FROM = 0.3f, FOG_TO = 1.4f, ARRIVE = 0.45f;
	private static final int SWIRLERS = 56, TWIRL_GRID = 12;
	private static final Tween ZOOM_IN = Tween.over(0.7f, Ease.SMOOTH), DARKEN = Tween.over(0.6f, Ease.SMOOTH).after(0.1f),
			SPREAD = Tween.over(0.9f, Ease.OUT).after(0.35f), TWIST = Tween.over(0.95f, Ease.IN).after(0.85f),
			FOG_IN = Tween.over(0.7f, Ease.SMOOTH).after(1.2f), FOG_OUT = Tween.over(0.85f, Ease.SMOOTH).after(OPEN_AT),
			HALL_IN = Tween.over(0.85f, Ease.SMOOTH).after(OPEN_AT);
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
	private final Rectangle backButton = new Rectangle(), goButton = new Rectangle(), infoButton = new Rectangle(), opening = new Rectangle();
	private final Rectangle[] arches = new Rectangle[STEPS.length];
	private final Rectangle[] playerCards = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };
	private final StringBuilder pageCount = new StringBuilder(), stageText = new StringBuilder();
	private final List<Briefing> info = new ArrayList<Briefing>();
	private final Sparks sparks = new Sparks();
	private final Matrix4 zoom = new Matrix4(), still = new Matrix4(), doorwayFit = new Matrix4();
	private final Color flash = new Color();
	private Chapter chosen, entering, revealing, jigglingSeal;
	private int page, rival, door = -1, jigglingArch = -1;
	private float chosenFor, briefedFor, unfoldFor = UNFOLD_LENGTH, enterFor, arrivedFor = ARRIVE, doorFor, walkFor = -1,
			jiggleFor = JIGGLE.end(), revealFor = REVEAL_LEN;
	private String beatLine = "";
	private Briefing briefing;
	private Runnable afterBriefing;

	public CampaignScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
		map = new ChapterMap(paint);
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

	public void openChapter(Chapter c) {
		chosen = c;
		beatLine = c == null ? "" : "Win the table, then beat " + c.champion().name + " at the final table and in a duel";
		chosenFor = 0;
		briefing = null;
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
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		shownFor += dt;
		chosenFor += dt;
		briefedFor += dt;
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
			if (enterFor >= ENTER_LENGTH) {
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
		if (briefing != null) {
			drawBriefing();
		}
		chrome.header(chosen == null ? "Campaign" : "Chapter " + chosen.number(), backButton, w, h);
		if (chosen != null && briefing == null && door < 0) {
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
		if (crossed(was, now, TWIST.start())) {
			game.audio().play(Audio.Effect.WHOOSH);
		}
		if (crossed(was, now, OPEN_AT)) {
			openChapter(entering);
		}
	}

	private static boolean crossed(float was, float now, float at) {
		return was < at && now >= at;
	}

	private void drawSealing() {
		float t = enterFor, zoomIn = ZOOM_IN.progress(t), twist = TWIST.progress(t);
		float dark = t < OPEN_AT ? DARKEN.progress(t) : 1 - HALL_IN.progress(t);
		paint.rect(0, 0, w, h, flash.set(0, 0, 0, dark));
		Rectangle m = map.marker(entering);
		float cx = MathUtils.lerp(m.x + m.width / 2, w / 2, zoomIn), cy = MathUtils.lerp(m.y + m.height / 2, h / 2, zoomIn);
		float size = SMALL_SEAL * (1 + (ZOOM_SCALE - 1) * zoomIn), shrunk = size * (1 - TWIST_SHRINK * twist);
		if (twist < 1) {
			drawTwirled(assets.seal, cx, cy, shrunk, twist, 1 - twist * twist);
			drawTwirled(assets.emblem(entering.emblem), cx, cy, shrunk * 0.45f, twist, 1 - twist * twist);
		}
		float fogIn = FOG_IN.progress(t), fogA = fogIn * (1 - FOG_OUT.progress(t));
		if (fogA > 0) {
			float grow = MathUtils.lerp(FOG_FROM, FOG_TO, fogIn);
			batch.setTransformMatrix(zoom.idt().translate(cx, cy, 0).scale(grow, grow, 1));
			fog.drawMist(batch, 0, 0, fogA);
			batch.setTransformMatrix(still);
		}
		float glow = twist * (1 - HALL_IN.progress(t)), glowSize = size * (1 + twist);
		paint.glow(cx - glowSize, cy - glowSize, glowSize * 2, glowSize * 2, Palette.LAMP, 0.35f * glow);
		drawSwirl(cx, cy, size, t, twist);
	}

	private void drawTwirled(Texture texture, float cx, float cy, float size, float twist, float alpha) {
		float colour = flash.set(1, 1, 1, alpha).toFloatBits();
		for (int j = 0; j < TWIRL_GRID; j++) {
			for (int i = 0; i < TWIRL_GRID; i++) {
				twirled(0, i, j, cx, cy, size, twist, colour);
				twirled(5, i + 1, j, cx, cy, size, twist, colour);
				twirled(10, i + 1, j + 1, cx, cy, size, twist, colour);
				twirled(15, i, j + 1, cx, cy, size, twist, colour);
				batch.draw(texture, twirl, 0, 20);
			}
		}
	}

	private void twirled(int o, int i, int j, float cx, float cy, float size, float twist, float colour) {
		float u = (float) i / TWIRL_GRID, v = (float) j / TWIRL_GRID, px = (u - 0.5f) * size, py = (v - 0.5f) * size;
		float r = Math.min(1, (float) Math.hypot(px, py) / (size / 2)), inner = 1 - r;
		float turn = (twist * TWIST_TURN * inner * inner + twist * TWIST_TURN * TWIST_SPIN) * MathUtils.degRad;
		float cos = MathUtils.cos(turn), sin = MathUtils.sin(turn);
		twirl[o] = cx + px * cos - py * sin;
		twirl[o + 1] = cy + px * sin + py * cos;
		twirl[o + 2] = colour;
		twirl[o + 3] = u;
		twirl[o + 4] = 1 - v;
	}

	private final float[] twirl = new float[20];

	private void drawSwirl(float cx, float cy, float size, float t, float twist) {
		float a = SPREAD.progress(t) * (1 - HALL_IN.progress(t));
		if (a <= 0) {
			return;
		}
		float[] rgb = swirlColour;
		float spread = SPREAD.progress(t), out = HALL_IN.progress(t), whirl = twist * TWIST_TURN * MathUtils.degRad * 0.8f;
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < SWIRLERS; i++) {
			float lane = 0.5f + 0.9f * spread * (0.6f + 0.4f * ((i * 7) % 10) / 10f);
			float angle = i * MathUtils.PI2 / SWIRLERS + t * SWIRL_SPEED * (1 + (i % 3) * 0.15f) + whirl * (1.3f - lane * 0.4f);
			float r = size * lane * (1 - 0.45f * twist) + out * size * 1.6f;
			float dot = 8 + (i % 4) * 4;
			Palette.sparkle(0.6f + (i % 5) * 0.2f, 0, rgb);
			batch.setColor(rgb[0], rgb[1], rgb[2], a * (0.55f + 0.45f * MathUtils.sin(t * 7 + i)));
			batch.draw(assets.glow, cx + MathUtils.cos(angle) * r - dot / 2, cy + MathUtils.sin(angle) * r - dot / 2, dot, dot);
		}
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
	}

	private final float[] swirlColour = new float[3];

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
			float zoomIn = ZOOM_IN.progress(enterFor), scale = 1 + (ZOOM_SCALE - 1) * zoomIn, mx = m.x + m.width / 2, my = m.y + m.height / 2;
			batch.setTransformMatrix(zoom.idt().translate(MathUtils.lerp(mx, w / 2, zoomIn), MathUtils.lerp(my, h / 2, zoomIn), 0)
					.scale(scale, scale, 1).translate(-mx, -my, 0));
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
			batch.setTransformMatrix(zoom.idt().translate(MathUtils.lerp(cx, w / 2, pan), MathUtils.lerp(cy, h / 2, pan), 0).scale(scale, scale, 1)
					.translate(-cx, -cy, 0));
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
		paint.ui(stageTextSize(boxW - 44), Palette.CREAM, a).draw(batch, stageText, 52, 12 + STAGE_CAPTION_H - 18, boxW - 44, Align.left, true);
		paint.button(goButton, briefing.go(), w - 30 - bw / 2, 12 + (STAGE_CAPTION_H - 44) / 2, bw, 44, assets.button);
	}

	private void panel(Briefing.Page p, float cx, float cy, float a) {
		Texture icon = switch (p.picture()) {
		case TABLE -> assets.stepTable;
		case WIN -> assets.badge;
		case SHOTS -> assets.stepShots;
		case CHAMPION -> championBadge();
		default -> null;
		};
		if (icon != null) {
			smallCard(cx, cy, icon, p.figure(), a, p.picture() == Briefing.Picture.CHAMPION);
			return;
		}
		float x = cx - SMALL_W / 2, y = cy - SMALL_H / 2;
		art.shadow(x, y, SMALL_W, SMALL_H, 4);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, SMALL_W, SMALL_H);
		batch.setColor(Color.WHITE);
		fit(true, 2.6f, Palette.GOLD_LIGHT, a, p.figure(), x, cy - 20, SMALL_W, 40);
		String heading = p.heading().toUpperCase();
		BitmapFont f = paint.ui(0.85f, Palette.GOLD, a);
		paint.glyphs.setText(f, heading);
		if (paint.glyphs.width > SMALL_W - FIT_PAD) {
			paint.ui(0.7f, Palette.GOLD, a).draw(batch, heading, x + FIT_PAD / 2, y + 52, SMALL_W - FIT_PAD, Align.center, true);
		} else {
			paint.textIn(f, heading, x, y + 14, SMALL_W, 20);
		}
	}

	private void drawPicture(Briefing.Page p, float cy, float a) {
		Chapter c = briefing.chapter();
		switch (p.picture()) {
		case CHAPTER:
			bigCard(w / 2, cy, assets.emblem(c.emblem), p.figure(), a, false);
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
				smallCard(rx, ry, assets.levelBadge[pl.level.ordinal()], pl.name, a, i == CHAMPION);
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
			bigCard(w / 2, cy, championBadge(), p.figure(), a, true);
			break;
		}
	}

	private void fit(boolean serif, float size, Color c, float a, String s, float x, float y, float bw, float bh) {
		float room = bw - FIT_PAD;
		BitmapFont f = serif ? paint.serif(size, c, a) : paint.ui(size, c, a);
		paint.glyphs.setText(f, s);
		if (paint.glyphs.width > room) {
			float smaller = size * room / paint.glyphs.width;
			f = serif ? paint.serif(smaller, c, a) : paint.ui(smaller, c, a);
		}
		paint.textIn(f, s, x, y, bw, bh);
	}

	private float stageTextSize(float width) {
		if (stageSize == 0) {
			stageSize = STAGE_TEXT;
			while (stageSize > STAGE_TEXT_LEAST) {
				paint.glyphs.setText(paint.ui(stageSize), stageText, 0, stageText.length(), Palette.CREAM, width, Align.left, true, null);
				if (paint.glyphs.height <= STAGE_CAPTION_H - STAGE_TEXT_ROOM) {
					break;
				}
				stageSize -= 0.05f;
			}
		}
		return stageSize;
	}

	private float stageSize;

	private Texture championBadge() {
		return assets.levelBadge[briefing.chapter().champion().level.ordinal()];
	}

	private void icon(Texture icon, float cx, float cy, float size, boolean gilt) {
		batch.draw(icon, cx - size / 2, cy - size / 2, size, size);
		if (gilt) {
			float ring = size * GILT;
			batch.draw(assets.giltRing, cx - ring / 2, cy - ring / 2, ring, ring);
		}
	}

	private void bigCard(float cx, float cy, Texture icon, String label, float a, boolean gilt) {
		float x = cx - BIG_W / 2, y = cy - BIG_H / 2;
		glow(x, y, BIG_W, BIG_H);
		art.shadow(x, y, BIG_W, BIG_H, 8);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, BIG_W, BIG_H);
		icon(icon, cx, cy + 6, gilt ? BIG_ICON / GILT : BIG_ICON, gilt);
		batch.setColor(Color.WHITE);
		fit(true, 1.3f, Palette.GOLD_LIGHT, a, label, x, y + 22, BIG_W, 28);
	}

	private void smallCard(float cx, float cy, Texture icon, String label, float a, boolean gilt) {
		float x = cx - SMALL_W / 2, y = cy - SMALL_H / 2;
		art.shadow(x, y, SMALL_W, SMALL_H, 4);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, SMALL_W, SMALL_H);
		icon(icon, cx, cy + 8, gilt ? ICON / GILT : ICON, gilt);
		batch.setColor(Color.WHITE);
		fit(true, 1.1f, Palette.GOLD_LIGHT, a, label, x, y + 16, SMALL_W, 24);
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

	private String stepNote(Campaign progress, Chapter c, int step) {
		if (step == SHOTS) {
			return progress.madeIn(c) + " of " + TrickShots.of(c).size() + " made";
		}
		return stepDone(progress, c, step) ? "Won" : "Play";
	}

	private Texture stepIcon(int step) {
		return step == SHOTS ? assets.stepShots : STAGE_AT[step] == Stage.DUEL ? assets.stepDuel : assets.stepTable;
	}

	public void brief(Briefing b, Runnable then) {
		briefing = b;
		afterBriefing = then;
		briefedFor = 0;
		page = 0;
		rival = 0;
		stageText.setLength(0);
		stageSize = 0;
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

	public void briefTable(Chapter c) {
		game.settings().seeFirstTime(c.name() + "." + Stage.TABLE.name());
		brief(Briefing.table(c), () -> game.playCampaign(c, Stage.TABLE));
	}

	public void go() {
		Runnable then = afterBriefing;
		briefing = null;
		then.run();
	}

	public void reveal(Chapter next) {
		chosen = null;
		briefing = null;
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
		if (briefing != null) {
			return briefing.tag();
		}
		return chosen == null ? "campaign" : "chapter:" + chosen.number();
	}
}
