package ohheck;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Difficulty;
import rules.Player;

/**
 * The settings, saved on the device: the game (difficulty, hand sizes, speed) on the left;
 * sound and display on the right, and under them Learn (How to play, and the Roster of players).
 */
public class OptionsScreen extends BaseScreen {

	private static final float COLUMN_W = 440, COLUMN_GAP = 34, SECTION_GAP = 40;

	private final Rectangle backButton = new Rectangle();

	/** One setting below the difficulty: its name, its value on a button, and what a tap on the row does. */
	private static final class Row {
		final String label;
		final Supplier<String> value;
		/** Whether the value is shown lit (on) or greyed (off). */
		final BooleanSupplier on;
		final Runnable tap;
		final Rectangle bounds = new Rectangle();

		Row(String label, Supplier<String> value, BooleanSupplier on, Runnable tap) {
			this.label = label;
			this.value = value;
			this.on = on;
			this.tap = tap;
		}

		/** A row that's always lit, like "Game speed" or "How to play". */
		Row(String label, Supplier<String> value, Runnable tap) {
			this(label, value, () -> true, tap);
		}

		/** An On/Off switch. */
		static Row toggle(String label, BooleanSupplier on, Runnable flip) {
			return new Row(label, () -> on.getAsBoolean() ? "On" : "Off", on, flip);
		}
	}

	/** The game column (under the difficulty), the sound and display column, and Learn under it. */
	private final List<Row> gameRows = new ArrayList<Row>(), displayRows = new ArrayList<Row>(),
			learnRows = new ArrayList<Row>();
	{
		final Settings s = game.settings;
		// (both take effect from the next new game)
		gameRows.add(new Row("Hand progression", () -> s.handOrder().label(), () -> s.setHandOrder(s.handOrder().flipped())));
		gameRows.add(new Row("Game speed", () -> Settings.SPEEDS[s.speed()],
				() -> s.setSpeed((s.speed() + 1) % Settings.SPEEDS.length)));
		displayRows.add(Row.toggle("Music", s::music, () -> {
			s.setMusic(!s.music());
			game.audio.startMusic(); // starts or stops it to match
		}));
		displayRows.add(Row.toggle("Sound effects", s::sound, () -> {
			s.setSound(!s.sound());
			game.audio.play(Audio.Effect.PLAY_CARD);
		}));
		displayRows.add(Row.toggle("Visual effects", s::effects, () -> s.setEffects(!s.effects())));
		learnRows.add(new Row("How to play", () -> "View", game::showTutorial));
		learnRows.add(new Row("Roster", () -> "View", game::showPlayers));
	}

	public OptionsScreen(OhHeckGame game) {
		super(game);
	}

	@Override
	public void render(float delta) {
		shownFor += Math.min(delta, 0.1f) * game.options.speed;
		if (game.tour != null && game.tour.step(this, shownFor)) {
			return;
		}
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		float rh = ROW_H, top = h - HEADER_H - 46;
		float left = (w - 2 * COLUMN_W - COLUMN_GAP) / 2, right = left + COLUMN_W + COLUMN_GAP;
		heading("Game", left, top + 30);
		heading("Sound & display", right, top + 30);
		float y = top - DIFFICULTY_H;
		drawDifficulty(left, y, COLUMN_W);
		for (Row row : gameRows) {
			y -= ROW_GAP + rh;
			drawRow(row, left, y, COLUMN_W, rh);
		}
		y = top;
		for (Row row : displayRows) {
			y -= rh;
			drawRow(row, right, y, COLUMN_W, rh);
			y -= ROW_GAP;
		}
		y -= SECTION_GAP;
		heading("Learn", right, y + 30);
		for (Row row : learnRows) {
			y -= rh;
			drawRow(row, right, y, COLUMN_W, rh);
			y -= ROW_GAP;
		}

		header("Options", backButton);
		batch.end();
	}

	private void drawRow(Row row, float x, float y, float rw, float rh) {
		frame(assets.panel, x, y, rw, rh);
		BitmapFont f = serif(1.6f);
		f.setColor(Palette.CREAM);
		glyphs.setText(f, row.label);
		f.draw(batch, row.label, x + 24, y + rh / 2 + glyphs.height / 2);
		float vw = 140;
		boolean on = row.on.getAsBoolean();
		frame(on ? assets.button : assets.buttonOff, x + rw - vw - 14, y + 7, vw, rh - 14);
		BitmapFont v = ui(1.4f);
		v.setColor(on ? Palette.CREAM : Palette.MUTED);
		textIn(v, row.value.get(), x + rw - vw - 14, y, vw, rh);
		row.bounds.set(x, y, rw, rh);
	}

	// ---- difficulty: a slider with three stops, its knob the level's badge

	private static final float ROW_H = 40, ROW_GAP = 6, DIFFICULTY_H = 112, KNOB = 34;
	/** Who you'll likely meet at each setting, shown under the slider. */
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
		float x0 = x + 214, x1 = x + rw - 36, ty = y + 84;
		stop0 = x0;
		stopStep = (x1 - x0) / (Difficulty.values().length - 1);
		slider.set(x0 - 30, y + 48, x1 - x0 + 60, DIFFICULTY_H - 48);
		int level = s.difficulty().ordinal(); // (the slider's stop)
		float knobX = x0 + level * stopStep;
		rect(x0, ty - 2, x1 - x0, 4, TRACK);
		rect(x0, ty - 2, knobX - x0, 4, GOLD);
		BitmapFont l = ui(0.9f);
		for (int i = 0; i < Difficulty.values().length; i++) {
			float sx = x0 + i * stopStep;
			rect(sx - 1.5f, ty - 7, 3, 14, i <= level ? GOLD : TRACK);
			l.setColor(i == level ? Palette.GOLD_LIGHT : Palette.MUTED);
			text(l, Difficulty.values()[i].label, sx, y + 60, true);
		}
		BitmapFont n = ui(0.85f);
		String note = Player.tableDescription(s.difficulty()); // (from the matchmaking odds)
		glyphs.setText(n, note);
		if (glyphs.width > rw - 48) { // keep it on one line inside the row
			n = ui(0.85f * (rw - 48) / glyphs.width);
		}
		n.setColor(Palette.MUTED);
		n.draw(batch, note, x + 24, y + 38);
		n.draw(batch, "Seats a new table from your next game.", x + 24, y + 20);
		batch.draw(assets.levelBadge[level], knobX - KNOB / 2, ty - KNOB / 2, KNOB, KNOB);
	}

	/** Moves the slider to the stop nearest x. */
	private void slideTo(float x) {
		int n = Difficulty.values().length;
		Difficulty level = Difficulty.values()[Math.max(0, Math.min(n - 1, Math.round((x - stop0) / stopStep)))];
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
		for (List<Row> rows : java.util.Arrays.asList(gameRows, displayRows, learnRows)) {
			for (Row row : rows) {
				if (row.bounds.contains(x, y)) { // (empty until drawn)
					row.tap.run();
					return;
				}
			}
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
