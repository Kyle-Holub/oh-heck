package ohheck;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

/** How to play, music, sound effects, game speed and visual effects, saved on the device. */
public class OptionsScreen extends BaseScreen {


	private final List<Rectangle> rows = new ArrayList<Rectangle>();
	private Rectangle backButton = new Rectangle();

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

		String[] labels = { "How to play", "Music", "Sound effects", "Game speed", "Visual effects" };
		String[] values = { "Read", s.music() ? "On" : "Off", s.sound() ? "On" : "Off", Settings.SPEEDS[s.speed()],
				s.effects() ? "On" : "Off" };
		boolean[] on = { true, s.music(), s.sound(), true, s.effects() };
		rows.clear();
		float rw = 460, rh = 52, x = (w - rw) / 2, y = h - 106 - rh;
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
			rows.add(new Rectangle(x, y, rw, rh));
			y -= rh + 10;
		}
		BitmapFont hint = ui(1f);
		hint.setColor(Palette.MUTED);
		centred(hint, "Tap a row to change it", w / 2, y + rh - 6);
		backButton = button("Back", 70, h - 52, 110, 40, assets.buttonAlt);
		batch.end();
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		Settings s = game.settings;
		if (rows.size() < 5) {
			return;
		}
		if (rows.get(0).contains(x, y)) {
			game.showRules();
		} else if (rows.get(1).contains(x, y)) {
			s.setMusic(!s.music());
			game.audio.startMusic(); // starts or stops it to match
		} else if (rows.get(2).contains(x, y)) {
			s.setSound(!s.sound());
			game.audio.play(Audio.Effect.PLAY_CARD);
		} else if (rows.get(3).contains(x, y)) {
			s.setSpeed((s.speed() + 1) % Settings.SPEEDS.length);
		} else if (rows.get(4).contains(x, y)) {
			s.setEffects(!s.effects());
		}
	}

	@Override
	protected void back() {
		game.showTitle();
	}

	@Override
	public String stateTag() {
		return "options";
	}
}
