package ohheck.screens;

import java.util.EnumMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;

import ohheck.platform.Settings;
import ohheck.players.Difficulty;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

/* The difficulty setting: a slider across the levels, and who you'll meet at the one chosen. */
final class DifficultyOption extends OptionItem {

	private static final float HEIGHT = 160, LABEL_ROOM = 40, KNOB = 34, GRAB = 30;
	private static final Color TRACK = new Color(0.36f, 0.29f, 0.21f, 1f);
	private static final Difficulty[] LEVELS = Difficulty.values();

	private final Settings settings;
	private final Map<Difficulty, String> notes = new EnumMap<Difficulty, String>(Difficulty.class);
	private final Rectangle slider = new Rectangle();
	private float firstStop, stopStep;

	DifficultyOption(Settings settings) {
		super("Difficulty", "Seats a new table from your next game.");
		this.settings = settings;
		for (Difficulty d : LEVELS) {
			notes.put(d, Wording.tableDescription(d));
		}
	}

	@Override
	float height() {
		return HEIGHT;
	}

	@Override
	void draw(Painter paint, float x, float y, float w) {
		super.draw(paint, x, y, w);
		drawSlider(paint, x + PAD + LABEL_ROOM, x + w - PAD - LABEL_ROOM, y + 82, y);
		String note = notes.get(settings.difficulty());
		BitmapFont f = paint.ui(0.9f, Palette.GOLD_LIGHT);
		paint.glyphs.setText(f, note);
		if (paint.glyphs.width > w - 2 * PAD) {
			f = paint.ui(0.9f * (w - 2 * PAD) / paint.glyphs.width, Palette.GOLD_LIGHT);
		}
		f.draw(paint.batch, note, x + PAD, y + 26);
	}

	@Override
	void drawControl(Painter paint, float right, float y, float h) {
	}

	private void drawSlider(Painter paint, float x0, float x1, float ty, float rowY) {
		firstStop = x0;
		stopStep = (x1 - x0) / (LEVELS.length - 1);
		slider.set(x0 - GRAB, rowY + 36, x1 - x0 + 2 * GRAB, 70);
		int level = settings.difficulty().ordinal();
		float knobX = x0 + level * stopStep;
		paint.rect(x0, ty - 2, x1 - x0, 4, TRACK);
		paint.rect(x0, ty - 2, knobX - x0, 4, Palette.GOLD);
		for (int i = 0; i < LEVELS.length; i++) {
			float sx = x0 + i * stopStep;
			paint.rect(sx - 1.5f, ty - 7, 3, 14, i <= level ? Palette.GOLD : TRACK);
			paint.text(paint.ui(0.9f, i == level ? Palette.GOLD_LIGHT : Palette.MUTED), LEVELS[i].label, sx, ty - 24, true);
		}
		paint.batch.draw(paint.assets.levelBadge[level], knobX - KNOB / 2, ty - KNOB / 2, KNOB, KNOB);
	}

	@Override
	void tap(float x, float y) {
		if (slider.contains(x, y)) {
			dragTo(x);
		}
	}

	@Override
	boolean grabs(float x, float y) {
		return slider.contains(x, y);
	}

	@Override
	void dragTo(float x) {
		int i = Math.round((x - firstStop) / stopStep);
		Difficulty level = LEVELS[Math.max(0, Math.min(LEVELS.length - 1, i))];
		if (level != settings.difficulty()) {
			settings.setDifficulty(level);
		}
	}
}
