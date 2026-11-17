package ohheck;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Difficulty;

/** How to play, music, sound effects, game speed and visual effects, saved on the device. */
public class OptionsScreen extends BaseScreen {


	private final List<Rectangle> rows = new ArrayList<Rectangle>();
	private final Rectangle backButton = new Rectangle(), rulesButton = new Rectangle();

	public OptionsScreen(OhHeckGame game) {
		super(game);
	}

	@Override
	public void render(float delta) {
		shownFor += Math.min(delta, 0.1f) * game.options.speed;
		if (game.options.autoplayHuman && shownFor > 1.5f) {
			game.showRules();
			return;
		}
		Settings s = game.settings;
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		BitmapFont t = serif(2.2f);
		t.setColor(GOLD);
		centred(t, "Options", w / 2, h - 40);

		String[] labels = { "Music", "Sound effects", "Game speed", "Visual effects" };
		String[] values = { s.music() ? "On" : "Off", s.sound() ? "On" : "Off", Settings.SPEEDS[s.speed()],
				s.effects() ? "On" : "Off" };
		boolean[] on = { s.music(), s.sound(), true, s.effects() };
		sizeTo(rows, labels.length);
		float rw = 460, rh = ROW_H, x = (w - rw) / 2;

		// How to play: its own button, gold lettering, set apart from the settings below
		float bw = 260, bh = 44;
		rulesButton.set((w - bw) / 2, h - 84 - bh, bw, bh);
		frame(assets.buttonAlt, rulesButton.x, rulesButton.y, bw, bh);
		BitmapFont rb = serif(1.6f);
		rb.setColor(GOLD);
		textIn(rb, "How to play", rulesButton.x, rulesButton.y, bw, bh);

		float y = rulesButton.y - 14 - DIFFICULTY_H;
		drawDifficulty(x, y, rw);
		y -= ROW_GAP + rh;
		for (int i = 0; i < labels.length; i++) {
			frame(assets.panel, x, y, rw, rh);
			BitmapFont f = serif(1.6f);
			f.setColor(Palette.CREAM);
			glyphs.setText(f, labels[i]);
			f.draw(batch, labels[i], x + 24, y + rh / 2 + glyphs.height / 2);
			float vw = 120;
			frame(on[i] ? assets.button : assets.buttonOff, x + rw - vw - 14, y + 9, vw, rh - 18);
			BitmapFont v = ui(1.4f);
			v.setColor(on[i] ? Palette.CREAM : Palette.MUTED);
			glyphs.setText(v, values[i]);
			v.draw(batch, values[i], x + rw - 14 - vw / 2 - glyphs.width / 2, y + rh / 2 + glyphs.height / 2);
			rows.get(i).set(x, y, rw, rh);
			y -= rh + ROW_GAP;
		}
		button(backButton, "Back", 70, h - 52, 110, 40, assets.buttonAlt);
		batch.end();
	}

	// ---- difficulty: a slider with three stops, its knob the level's badge

	private static final float ROW_H = 46, ROW_GAP = 7, DIFFICULTY_H = 94, KNOB = 34;
	/** What to expect at each level, shown under the slider. */
	private static final String[] LEVEL_NOTES = {
			"Forgiving: they misjudge bids and slip up. Luck still counts!",
			"Solid: they count cards, trump in and duck unwanted tricks.",
			"Ruthless: they think ahead and gang up on the leader." };
	private static final Color TRACK = new Color(0.36f, 0.29f, 0.21f, 1f);
	private final Rectangle slider = new Rectangle();
	private float stop0, stopStep;
	private boolean sliding;

	private void drawDifficulty(float x, float y, float rw) {
		Settings s = game.settings;
		frame(assets.panel, x, y, rw, DIFFICULTY_H);
		BitmapFont f = serif(1.6f);
		f.setColor(Palette.CREAM);
		f.draw(batch, "Difficulty", x + 24, y + DIFFICULTY_H - 18);
		// the track, gold up to the chosen stop
		float x0 = x + 214, x1 = x + rw - 36, ty = y + 66;
		stop0 = x0;
		stopStep = (x1 - x0) / 2;
		slider.set(x0 - 30, y + 30, x1 - x0 + 60, DIFFICULTY_H - 30);
		int level = s.difficulty();
		float knobX = x0 + level * stopStep;
		rect(x0, ty - 2, x1 - x0, 4, TRACK);
		rect(x0, ty - 2, knobX - x0, 4, GOLD);
		BitmapFont l = ui(0.9f);
		for (int i = 0; i < 3; i++) {
			float sx = x0 + i * stopStep;
			rect(sx - 1.5f, ty - 7, 3, 14, i <= level ? GOLD : TRACK);
			l.setColor(i == level ? Palette.GOLD_LIGHT : Palette.MUTED);
			text(l, Difficulty.values()[i].label, sx, y + 42, true);
		}
		BitmapFont n = ui(0.85f);
		glyphs.setText(n, LEVEL_NOTES[level]);
		if (glyphs.width > rw - 48) { // keep it on one line inside the row
			n = ui(0.85f * (rw - 48) / glyphs.width);
		}
		n.setColor(Palette.MUTED);
		n.draw(batch, LEVEL_NOTES[level], x + 24, y + 20);
		batch.draw(assets.levelBadge[level], knobX - KNOB / 2, ty - KNOB / 2, KNOB, KNOB);
	}

	/** Moves the slider to the stop nearest x. */
	private void slideTo(float x) {
		int level = Math.max(0, Math.min(2, Math.round((x - stop0) / stopStep)));
		if (level != game.settings.difficulty()) {
			game.settings.setDifficulty(level);
		}
	}

	@Override
	protected void touchDown(float x, float y) {
		super.touchDown(x, y);
		sliding = slider.contains(x, y);
		if (sliding) {
			slideTo(x);
		}
	}

	@Override
	protected void touchDragged(float x, float y) {
		if (sliding) {
			slideTo(x);
		} else {
			super.touchDragged(x, y);
		}
	}

	@Override
	protected void touchUp(float x, float y) {
		if (sliding) {
			sliding = false;
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
		Settings s = game.settings;
		if (rulesButton.contains(x, y)) {
			game.showRules();
			return;
		}
		if (rows.size() < 4) {
			return;
		}
		if (rows.get(0).contains(x, y)) {
			s.setMusic(!s.music());
			game.audio.startMusic(); // starts or stops it to match
		} else if (rows.get(1).contains(x, y)) {
			s.setSound(!s.sound());
			game.audio.play(Audio.Effect.PLAY_CARD);
		} else if (rows.get(2).contains(x, y)) {
			s.setSpeed((s.speed() + 1) % Settings.SPEEDS.length);
		} else if (rows.get(3).contains(x, y)) {
			s.setEffects(!s.effects());
		}
	}

	@Override
	protected void back() {
		game.back(); // to the title screen or the paused game, whichever opened it
	}

	@Override
	public String stateTag() {
		return "options";
	}
}
