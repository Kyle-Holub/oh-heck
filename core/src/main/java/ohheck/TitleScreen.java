package ohheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

/** The main menu, over a shower of falling cards. */
public class TitleScreen extends BaseScreen {


	private final FallingCards cards;
	private final List<String> items = new ArrayList<String>();
	private final List<Rectangle> hits = new ArrayList<Rectangle>();

	public TitleScreen(OhHeckGame game) {
		super(game);
		Long seed = game.options.seed;
		cards = new FallingCards(assets, seed == null ? new Random() : new Random(seed));
	}

	private void buildItems() {
		items.clear();
		if (game.hasGameInProgress()) {
			items.add("Continue");
			items.add("New game");
		} else {
			items.add("Play");
		}
		items.add("How to play");
		items.add("Options");
		items.add("Quit");
	}

	@Override
	public void show() {
		super.show();
		game.audio.startMusic();
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f) * game.options.speed;
		shownFor += dt;
		cards.update(dt, w, h);
		if (game.options.autoplayHuman && shownFor > 3.5f) {
			game.showRules();
			return;
		}

		ScreenUtils.clear(Palette.BG);
		begin();
		// a dim spotlit backdrop behind the shower
		batch.setColor(0.5f, 0.5f, 0.5f, 1f);
		batch.draw(assets.table, 0, 0, w, h);
		batch.setColor(Color.WHITE);
		cards.draw(batch, w / 2, h / 2);
		rect(0, 0, w, h, Palette.SHADE);

		// title, as in the original game
		float tw = assets.titleText.getWidth() * 0.85f, th = assets.titleText.getHeight() * 0.85f;
		float titleY = h - 30 - th;
		batch.draw(assets.titleText, (w - tw) / 2, titleY, tw, th);

		buildItems();
		hits.clear();
		float bw = 270, bh = 46, gap = 10;
		float y = titleY - 18 - bh;
		for (String s : items) {
			hits.add(button(s, w / 2, y, bw, bh, s.equals(items.get(0)) ? assets.button : assets.buttonAlt));
			y -= bh + gap;
		}

		BitmapFont f = ui(0.9f);
		f.setColor(Palette.MUTED);
		f.draw(batch, OhHeckGame.VERSION, 10, 22);
		String c = "©2026 Kyle Holub";
		glyphs.setText(f, c);
		f.draw(batch, c, w - 10 - glyphs.width, 22);
		batch.end();
	}

	@Override
	protected void tap(float x, float y) {
		for (int i = 0; i < hits.size(); i++) {
			if (hits.get(i).contains(x, y)) {
				choose(items.get(i));
				return;
			}
		}
	}

	private void choose(String item) {
		if (item.equals("Continue")) {
			game.continueGame();
		} else if (item.equals("Play") || item.equals("New game")) {
			game.newGame();
		} else if (item.equals("How to play")) {
			game.showRules();
		} else if (item.equals("Options")) {
			game.showOptions();
		} else if (item.equals("Quit")) {
			Gdx.app.exit();
		}
	}

	@Override
	protected void back() {
		Gdx.app.exit();
	}

	@Override
	public String stateTag() {
		return "title";
	}
}
