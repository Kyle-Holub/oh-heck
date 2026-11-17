package ohheck;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

/** Your ten best finished games. */
public class HighScoresScreen extends BaseScreen {

	private final Rectangle backButton = new Rectangle();
	private final Color highlight = new Color();
	private List<HighScores.Entry> list;
	private int newest = -1;

	public HighScoresScreen(OhHeckGame game) {
		super(game);
	}

	/** Picks out the entry at this position (just set), or -1 for none. */
	void highlight(int position) {
		newest = position;
	}

	@Override
	public void show() {
		super.show();
		list = game.highScores().entries(); // read once, not every frame
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f);
		shownFor += dt * game.options.speed;
		if (game.options.autoplayHuman && shownFor > 1.5f) {
			game.showOptions(false);
			return;
		}
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		BitmapFont t = serif(2.2f);
		t.setColor(GOLD);
		centred(t, "High scores", w / 2, h - 40);

		float pw = 480, rowH = 30, ph = 66 + Math.max(1, list.size()) * rowH;
		float px = (w - pw) / 2, top = h - 100;
		frame(assets.panel, px, top - ph, pw, ph);

		BitmapFont f = ui(1.15f);
		float[] cols = { px + 40, px + 170, px + 290, px + pw - 30 };
		float y = top - 18;
		if (list.isEmpty()) {
			f.setColor(Palette.MUTED);
			centred(f, "Finish a game to set your first high score", w / 2, y - 22);
		} else {
			f.setColor(Palette.MUTED);
			text(f, "#", cols[0], y, true);
			text(f, "Score", cols[1], y, true);
			text(f, "Finished", cols[2], y, true);
			text(f, "Date", cols[3], y, false);
			y -= 40;
			float pulse = 0.5f + 0.5f * MathUtils.sin(shownFor * 3f);
			for (int i = 0; i < list.size(); i++) {
				HighScores.Entry e = list.get(i);
				if (i == newest) {
					rect(px + 12, y - rowH + 8, pw - 24, rowH, highlight.set(0.97f, 0.85f, 0.56f, 0.10f + 0.08f * pulse));
				}
				Color c = i == newest ? Palette.GOLD_LIGHT : i == 0 ? GOLD : Palette.CREAM;
				f.setColor(c);
				text(f, String.valueOf(i + 1), cols[0], y, true);
				text(f, String.valueOf(e.score), cols[1], y, true);
				f.setColor(e.place == 1 ? GOLD : c);
				text(f, e.place == 1 ? "Won" : HighScores.ordinal(e.place), cols[2], y, true);
				f.setColor(i == newest ? Palette.GOLD_LIGHT : Palette.MUTED);
				text(f, e.date, cols[3], y, false);
				y -= rowH;
			}
		}
		button(backButton, "Back", 70, h - 52, 110, 40, assets.buttonAlt);
		batch.end();
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
		}
	}

	@Override
	protected void back() {
		newest = -1;
		game.showTitle();
	}

	@Override
	public String stateTag() {
		return "highscores";
	}
}
