package ohheck.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Phases;
import ohheck.anim.Tween;
import ohheck.campaign.Chapter;
import ohheck.campaign.Cosmetic;
import ohheck.fx.Fireworks;
import ohheck.fx.Sparks;
import ohheck.platform.Audio;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;

/*
 * The fanfare for clearing a chapter: fireworks and a title, then each prize lies face down and
 * is turned over in turn, shaking, flashing and flipping to show what was won (its card back, its
 * felt, its champion's badge). Once all are shown the new look is put on. A tap hurries the next prize along.
 */
public class ChapterClearedScreen extends BaseScreen {

	private static final float INTRO = 1.4f, CARD_W = 130, CARD_H = CARD_W * 98 / 73, CARD_GAP = 40, POP = 0.22f;
	private static final float BUTTON_W = 190, BUTTON_H = 44;
	private static final float SHAKE_LEN = 0.9f, FLASH_LEN = 0.15f, FLIP_LEN = 0.35f, HOLD_LEN = 0.9f, FLIP_AT = SHAKE_LEN + FLASH_LEN;
	private static final Phases PRIZE = new Phases(SHAKE_LEN, FLASH_LEN, FLIP_LEN, HOLD_LEN);
	private static final int SHAKE = 0, FLASH = 1, FLIP = 2;
	private static final Tween TITLE_IN = Tween.over(0.6f, Ease.OUT), BUTTONS_IN = Tween.over(0.4f, Ease.OUT);
	private static final Color GLOW = new Color(1f, 0.85f, 0.45f, 0.5f), FLASH_TINT = new Color(1f, 0.9f, 0.72f, 1f);
	private static final float FLASH_PEAK = 0.35f;

	private record Prize(String kind, String name, Texture face, boolean felt) {
	}

	private final CardArt art;
	private final Fireworks fireworks = new Fireworks();
	private final Sparks sparks = new Sparks();
	private final List<Prize> prizes = new ArrayList<Prize>();
	private final Rectangle continueButton = new Rectangle();
	private Chapter chapter;
	private Cosmetic look;
	private String title = "", beaten = "", lookLine = "";
	private float time;
	private int flipped;
	private boolean finale;

	public ChapterClearedScreen(GameHost game) {
		super(game);
		art = new CardArt(batch, assets);
	}

	public void open(Chapter c) {
		chapter = c;
		prizes.clear();
		look = null;
		for (Cosmetic k : Cosmetic.values()) {
			if (k.wonBy == c) {
				look = k;
				prizes.add(new Prize("NEW CARD BACK", k.label, assets.backOf(k), false));
				prizes.add(new Prize("NEW TABLE FELT", k.label, assets.feltOf(k), true));
			}
		}
		prizes.add(new Prize("NEW BADGE", "Beat " + c.champion().name, assets.badge, false));
		title = "Chapter " + c.number() + " cleared!";
		beaten = "You beat " + c.champion().name + ", the " + c.mode.label + " champion";
		lookLine = look == null ? "" : "Your cards and table are now " + look.label + ". Change them in Options.";
		time = 0;
		flipped = 0;
		finale = false;
	}

	private float revealT() {
		return time - INTRO;
	}

	private float doneAt() {
		return INTRO + prizes.size() * PRIZE.length();
	}

	private boolean revealed() {
		return time >= doneAt();
	}

	@Override
	public void show() {
		super.show();
		fireworks.show(w, h);
		game.audio().play(Audio.Effect.WIN);
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		shownFor += dt;
		time += dt;
		fireworks.update(dt);
		sparks.step(dt);
		while (flipped < prizes.size() && revealT() >= flipped * PRIZE.length() + FLIP_AT + FLIP_LEN / 2) {
			sparks.burst(slotX(flipped), cardY() + CARD_H / 2, 90);
			game.audio().play(Audio.Effect.PLAY_CARD);
			flipped++;
		}
		if (revealed() && !finale) {
			finale = true;
			if (look != null) {
				game.settings().setCardBack(look);
				game.settings().setFelt(look);
				game.assets().dress(look, look);
			}
			fireworks.show(w, h);
			game.audio().play(Audio.Effect.WIN);
		}
	}

	@Override
	public boolean animating() {
		return !revealed() || fireworks.active() || sparks.active() || BUTTONS_IN.active(time - doneAt());
	}

	private float slotX(int i) {
		float span = prizes.size() * CARD_W + (prizes.size() - 1) * CARD_GAP;
		return (w - span) / 2 + i * (CARD_W + CARD_GAP) + CARD_W / 2;
	}

	private float cardY() {
		return h / 2 - 10 - CARD_H / 2;
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		fireworks.draw(batch, assets.glow);
		float a = TITLE_IN.progress(time);
		paint.centred(paint.serif(2.4f + (1 - a) * 1.2f, Palette.GOLD_LIGHT, a), title, w / 2, h - 34);
		paint.centred(paint.ui(1.1f, Palette.CREAM, a), beaten, w / 2, h - 92);
		for (int i = 0; i < prizes.size(); i++) {
			drawPrize(i);
		}
		sparks.draw(batch, assets.glow);
		drawFlash();
		drawButtons();
		batch.end();
	}

	private void drawPrize(int i) {
		Prize p = prizes.get(i);
		float t = revealT() - i * PRIZE.length(), cx = slotX(i), y = cardY();
		int phase = t < 0 ? -1 : PRIZE.at(t);
		float into = t < 0 ? 0 : PRIZE.into(t);
		float shake = phase == SHAKE ? into / SHAKE_LEN : 0;
		float flip = phase < FLIP ? 0 : phase == FLIP ? Ease.SMOOTH.apply(into / FLIP_LEN) : 1;
		float pop = phase == SHAKE ? shake * POP : phase == FLASH ? POP : phase == FLIP ? POP * (1 - flip * 0.5f)
				: phase > FLIP ? POP * 0.5f * (1 - Math.min(1, into / 0.3f)) : 0;
		float lit = phase == FLASH ? 1 : phase >= SHAKE && phase <= FLIP ? 0.4f + shake * 0.6f : phase > FLIP ? Math.max(0, 1 - into / 0.6f) : 0;
		float cw = CARD_W * (1 + pop), ch = CARD_H * (1 + pop);
		float wobble = phase == SHAKE ? MathUtils.sin(into * 38f) * 7f * shake : 0;
		float cy = y + CARD_H / 2;
		if (lit > 0) {
			glow(cx, cy, cw, ch, lit);
		}
		art.shadow(cx - cw / 2, cy - ch / 2, cw, ch, 6);
		float sx = Math.abs(MathUtils.cos(flip * MathUtils.PI));
		float fw = cw * Math.max(0.02f, sx);
		if (flip < 0.5f) {
			batch.draw(assets.back(), cx - fw / 2 + wobble, cy - ch / 2, fw / 2, ch / 2, fw, ch, 1, 1, wobble * -0.6f);
		} else {
			drawFace(p, cx - fw / 2, cy - ch / 2, fw, ch);
		}
		if (flip >= 1) {
			float la = Math.min(1, into / 0.3f + (phase > FLIP ? 1 : 0));
			paint.centred(paint.ui(0.85f, Palette.GOLD, la), p.kind, cx, y - 14);
			paint.centred(paint.serif(1.3f, Palette.GOLD_LIGHT, la), p.name, cx, y - 40);
		}
	}

	private void drawFace(Prize p, float x, float y, float fw, float fh) {
		if (p.felt) {
			batch.draw(assets.campaignFace, x, y, fw, fh);
			float in = fw * 0.08f;
			x += in;
			y += in;
			fw -= 2 * in;
			fh -= 2 * in;
			int sw = p.face.getWidth() / 3, sh = sw * 98 / 73;
			batch.draw(p.face, x, y, fw, fh, (p.face.getWidth() - sw) / 2, (p.face.getHeight() - sh) / 2, sw, sh, false, false);
		} else if (p.face == assets.badge) {
			batch.draw(assets.campaignFace, x, y, fw, fh);
			float s = Math.min(fw, fh) * 0.62f;
			batch.draw(assets.badge, x + fw / 2 - s / 2, y + fh / 2 - s / 2, s, s);
		} else {
			batch.draw(p.face, x, y, fw, fh);
		}
	}

	private void glow(float cx, float cy, float cw, float ch, float alpha) {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		float gw = cw * 2.2f, gh = ch * 1.8f, pulse = 0.85f + 0.15f * MathUtils.sin(time * 9f);
		batch.setColor(GLOW.r, GLOW.g, GLOW.b, GLOW.a * alpha * pulse);
		batch.draw(assets.glow, cx - gw / 2, cy - gh / 2, gw, gh);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
	}

	private void drawFlash() {
		float t = revealT();
		if (t < 0 || revealed()) {
			return;
		}
		float within = t % PRIZE.length();
		if (PRIZE.at(within) == FLASH) {
			paint.fade(FLASH_PEAK * (1 - PRIZE.into(within) / FLASH_LEN));
			paint.rect(0, 0, w, h, FLASH_TINT);
			paint.fade(1f);
		}
	}

	private void drawButtons() {
		if (!revealed()) {
			continueButton.set(0, 0, 0, 0);
			paint.text(paint.ui(0.9f, Palette.MUTED), "Tap to hurry", w - 16, 24, false);
			return;
		}
		float a = BUTTONS_IN.progress(time - doneAt());
		paint.centred(paint.ui(0.95f, Palette.MUTED, a), lookLine, w / 2, h - 122);
		paint.fade(a);
		paint.button(continueButton, "Continue", w / 2, 18, BUTTON_W, BUTTON_H, assets.button);
		paint.fade(1f);
	}

	@Override
	protected void tap(float x, float y) {
		if (!revealed()) {
			hurry();
		} else if (continueButton.contains(x, y)) {
			back();
		}
	}

	private void hurry() {
		float t = revealT();
		if (t < 0) {
			time = INTRO;
			return;
		}
		int i = (int) (t / PRIZE.length());
		float landed = i * PRIZE.length() + FLIP_AT + FLIP_LEN;
		time = INTRO + (t < landed ? landed : Math.min(prizes.size() * PRIZE.length(), landed + PRIZE.length()));
	}

	@Override
	protected void back() {
		if (!revealed()) {
			time = doneAt();
		} else {
			game.back();
		}
	}

	@Override
	public String stateTag() {
		return revealed() ? "cleared" : "clearing";
	}
}
