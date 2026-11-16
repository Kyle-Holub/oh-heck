package ohheck;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

/** Music, sound effects and game speed, saved on the device. */
public class OptionsScreen extends BaseScreen {

	private static final Color DIMMED = new Color(0.35f, 0.35f, 0.35f, 1f);
	private static final Color ROW = new Color(0f, 0f, 0f, 0.6f);
	private static final Color OFF = new Color(0.35f, 0.35f, 0.35f, 1f);

	private final List<Rectangle> rows = new ArrayList<Rectangle>();
	private Rectangle backButton = new Rectangle();

	public OptionsScreen(OhHeckGame game) {
		super(game);
	}

	@Override
	public void render(float delta) {
		shownFor += Math.min(delta, 0.1f) * game.options.speed;
		if (game.options.autoplayHuman && shownFor > 1.5f) {
			game.newGame();
			return;
		}
		Settings s = game.settings;
		ScreenUtils.clear(Color.BLACK);
		begin();
		batch.setColor(DIMMED);
		batch.draw(assets.table, 0, 0, w, h);
		batch.setColor(Color.WHITE);

		BitmapFont t = serif(2.2f);
		t.setColor(GOLD);
		centred(t, "Options", w / 2, h - 40);

		String[] labels = { "Music", "Sound effects", "Game speed" };
		String[] values = { s.music() ? "On" : "Off", s.sound() ? "On" : "Off", s.fast() ? "Fast" : "Normal" };
		boolean[] on = { s.music(), s.sound(), true };
		rows.clear();
		float rw = 460, rh = 62, x = (w - rw) / 2, y = h - 130 - rh;
		for (int i = 0; i < labels.length; i++) {
			rect(x, y, rw, rh, ROW);
			BitmapFont f = serif(1.6f);
			f.setColor(Color.WHITE);
			glyphs.setText(f, labels[i]);
			f.draw(batch, labels[i], x + 24, y + rh / 2 + glyphs.height / 2);
			float vw = 120;
			rect(x + rw - vw - 14, y + 10, vw, rh - 20, on[i] ? BUTTON : OFF);
			BitmapFont v = ui(1.4f);
			v.setColor(Color.WHITE);
			glyphs.setText(v, values[i]);
			v.draw(batch, values[i], x + rw - 14 - vw / 2 - glyphs.width / 2, y + rh / 2 + glyphs.height / 2);
			rows.add(new Rectangle(x, y, rw, rh));
			y -= rh + 14;
		}
		BitmapFont hint = ui(1f);
		hint.setColor(Color.LIGHT_GRAY);
		centred(hint, "Tap a row to change it", w / 2, y + rh - 6);
		backButton = button("Back", 70, h - 52, 110, 40, BUTTON);
		batch.end();
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
			return;
		}
		Settings s = game.settings;
		if (rows.size() < 3) {
			return;
		}
		if (rows.get(0).contains(x, y)) {
			s.setMusic(!s.music());
		} else if (rows.get(1).contains(x, y)) {
			s.setSound(!s.sound());
			game.audio.play(Audio.Effect.PLAY_CARD);
		} else if (rows.get(2).contains(x, y)) {
			s.setFast(!s.fast());
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
