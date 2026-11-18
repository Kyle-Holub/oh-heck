package ohheck;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Difficulty;

/**
 * The settings, saved on the device, in two columns: the game (difficulty, hand sizes,
 * speed, and the players you might meet) and sound and display. How to play sits in the header.
 */
public class OptionsScreen extends BaseScreen {

	private static final Color HEADER = new Color(0.06f, 0.04f, 0.03f, 0.97f);
	private static final float HEADER_H = 56, COLUMN_W = 440, COLUMN_GAP = 34;

	private final Rectangle backButton = new Rectangle(), rulesButton = new Rectangle();

	/** One setting below the difficulty: its name, its value on a button, and what a tap on the row does. */
	private abstract class Row {
		final String label;
		final Rectangle bounds = new Rectangle();

		Row(String label) {
			this.label = label;
		}

		abstract String value();

		/** Whether the value is shown lit (on) or greyed (off). */
		boolean on() {
			return true;
		}

		abstract void tap();
	}

	/** The game column (under the difficulty), and the sound and display column. */
	private final List<Row> gameRows = new ArrayList<Row>(), displayRows = new ArrayList<Row>();
	{
		final Settings s = game.settings;
		gameRows.add(new Row("Hand sizes") { // (from the next new game)
			@Override
			String value() {
				return s.handOrder().label;
			}

			@Override
			void tap() {
				s.setHandOrder(s.handOrder().flipped());
			}
		});
		gameRows.add(new Row("Game speed") {
			@Override
			String value() {
				return Settings.SPEEDS[s.speed()];
			}

			@Override
			void tap() {
				s.setSpeed((s.speed() + 1) % Settings.SPEEDS.length);
			}
		});
		gameRows.add(new Row("The players") {
			@Override
			String value() {
				return "View";
			}

			@Override
			void tap() {
				game.showPlayers();
			}
		});
		displayRows.add(new Row("Music") {
			@Override
			String value() {
				return s.music() ? "On" : "Off";
			}

			@Override
			boolean on() {
				return s.music();
			}

			@Override
			void tap() {
				s.setMusic(!s.music());
				game.audio.startMusic(); // starts or stops it to match
			}
		});
		displayRows.add(new Row("Sound effects") {
			@Override
			String value() {
				return s.sound() ? "On" : "Off";
			}

			@Override
			boolean on() {
				return s.sound();
			}

			@Override
			void tap() {
				s.setSound(!s.sound());
				game.audio.play(Audio.Effect.PLAY_CARD);
			}
		});
		displayRows.add(new Row("Visual effects") {
			@Override
			String value() {
				return s.effects() ? "On" : "Off";
			}

			@Override
			boolean on() {
				return s.effects();
			}

			@Override
			void tap() {
				s.setEffects(!s.effects());
			}
		});
	}

	public OptionsScreen(OhHeckGame game) {
		super(game);
	}

	@Override
	public void render(float delta) {
		shownFor += Math.min(delta, 0.1f) * game.options.speed;
		if (game.options.autoplayHuman && shownFor > 1.5f) {
			game.showPlayers(); // (the screenshot run goes on from there to How to play)
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

		// the header: back, the title, and How to play (gold lettering, set apart from the settings)
		rect(0, h - HEADER_H, w, HEADER_H, HEADER);
		rect(0, h - HEADER_H - 2, w, 2, Palette.GOLD);
		BitmapFont t = serif(1.6f);
		t.setColor(GOLD);
		centred(t, "Options", w / 2, h - 16);
		button(backButton, "Back", 70, h - HEADER_H + 8, 110, HEADER_H - 16, assets.buttonAlt);
		float bw = 190, bh = HEADER_H - 16;
		rulesButton.set(w - 20 - bw, h - HEADER_H + 8, bw, bh);
		frame(assets.buttonAlt, rulesButton.x, rulesButton.y, bw, bh);
		BitmapFont rb = serif(1.5f);
		rb.setColor(GOLD);
		textIn(rb, "How to play", rulesButton.x, rulesButton.y, bw, bh);
		batch.end();
	}

	/** A column's name, in gold above it. */
	private void heading(String name, float x, float y) {
		BitmapFont f = serif(1.35f);
		f.setColor(GOLD);
		f.draw(batch, name, x + 6, y);
	}

	private void drawRow(Row row, float x, float y, float rw, float rh) {
		frame(assets.panel, x, y, rw, rh);
		BitmapFont f = serif(1.6f);
		f.setColor(Palette.CREAM);
		glyphs.setText(f, row.label);
		f.draw(batch, row.label, x + 24, y + rh / 2 + glyphs.height / 2);
		float vw = 120;
		boolean on = row.on();
		frame(on ? assets.button : assets.buttonOff, x + rw - vw - 14, y + 7, vw, rh - 14);
		BitmapFont v = ui(1.4f);
		v.setColor(on ? Palette.CREAM : Palette.MUTED);
		textIn(v, row.value(), x + rw - vw - 14, y, vw, rh);
		row.bounds.set(x, y, rw, rh);
	}

	// ---- difficulty: a slider with three stops, its knob the level's badge

	private static final float ROW_H = 40, ROW_GAP = 6, DIFFICULTY_H = 112, KNOB = 34;
	/** Who you'll likely meet at each setting, shown under the slider. */
	private static final String[] LEVEL_NOTES = {
			"Mostly easy-going players, and an expert now and then.",
			"A mix: some easy-going, mostly solid, the odd expert.",
			"Mostly experts, who read the bids and sink the leader." };
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
		stopStep = (x1 - x0) / 2;
		slider.set(x0 - 30, y + 48, x1 - x0 + 60, DIFFICULTY_H - 48);
		int level = s.difficulty();
		float knobX = x0 + level * stopStep;
		rect(x0, ty - 2, x1 - x0, 4, TRACK);
		rect(x0, ty - 2, knobX - x0, 4, GOLD);
		BitmapFont l = ui(0.9f);
		for (int i = 0; i < 3; i++) {
			float sx = x0 + i * stopStep;
			rect(sx - 1.5f, ty - 7, 3, 14, i <= level ? GOLD : TRACK);
			l.setColor(i == level ? Palette.GOLD_LIGHT : Palette.MUTED);
			text(l, Difficulty.values()[i].label, sx, y + 60, true);
		}
		BitmapFont n = ui(0.85f);
		glyphs.setText(n, LEVEL_NOTES[level]);
		if (glyphs.width > rw - 48) { // keep it on one line inside the row
			n = ui(0.85f * (rw - 48) / glyphs.width);
		}
		n.setColor(Palette.MUTED);
		n.draw(batch, LEVEL_NOTES[level], x + 24, y + 38);
		n.draw(batch, "Seats a new table from your next game.", x + 24, y + 20);
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
		if (rulesButton.contains(x, y)) {
			game.showTutorial();
			return;
		}
		for (List<Row> rows : java.util.Arrays.asList(gameRows, displayRows)) {
			for (Row row : rows) {
				if (row.bounds.contains(x, y)) { // (empty until drawn)
					row.tap();
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
