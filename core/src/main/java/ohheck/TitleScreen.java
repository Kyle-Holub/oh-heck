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

	// The opening sequence, once per launch: music over black, then cards begin to fall,
	// the title slowly fades in on its own, and after a pause the menu appears.
	/**
	 * The opening: black with the music, then the gold sparkles rise out of the dark, the
	 * title fades in among them, and once it's there the cards start to fall; then the menu.
	 */
	private static final float SCENE_START = 0.4f, SCENE_FADE = 0.9f, TITLE_START = 1.0f, TITLE_FADE = 1.6f,
			CARDS_START = TITLE_START + TITLE_FADE, MENU_START = 5.0f, MENU_FADE = 0.8f,
			INTRO_END = MENU_START + MENU_FADE;
	private float introT;
	private final List<String> items = new ArrayList<String>();
	private final List<Rectangle> hits = new ArrayList<Rectangle>();
	private final Color black = new Color();

	public TitleScreen(OhHeckGame game) {
		super(game);
		Long seed = game.options.seed;
		cards = new FallingCards(assets, seed == null ? new Random() : new Random(seed), true);
	}

	private void buildItems() {
		items.clear();
		if (game.hasGameInProgress()) {
			items.add("Continue");
			items.add("New game");
		} else {
			items.add("Play");
		}
		items.add("High scores");
		items.add("Options");
		items.add("Quit");
	}

	@Override
	public void show() {
		super.show();
		if (introT > 0) {
			introT = Math.max(introT, INTRO_END); // the opening plays once per launch
		}
		buildItems(); // Continue appears only with a game to continue; that can't change while shown
		game.audio.startMusic();
	}

	@Override
	boolean animating() {
		return true; // the falling cards
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f) * game.options.speed;
		introT += dt;
		if (introT >= SCENE_START) {
			cards.setFalling(introT >= CARDS_START);
			cards.update(dt, w, h);
		}
		if (introT >= INTRO_END) {
			shownFor += dt;
		}
		if (game.options.autoplayHuman && shownFor > 3.5f) {
			game.showHighScores(-1);
			return;
		}

		ScreenUtils.clear(Color.BLACK);
		begin();
		if (introT >= SCENE_START) {
			// a dim spotlit backdrop behind the shower
			batch.setColor(0.5f, 0.5f, 0.5f, 1f);
			batch.draw(assets.table, 0, 0, w, h);
			batch.setColor(Color.WHITE);
			cards.draw(batch, w / 2, h / 2);
			rect(0, 0, w, h, Palette.SHADE);
			// the scene rises out of black
			float dark = 1f - ramp(introT, SCENE_START, SCENE_FADE);
			if (dark > 0) {
				rect(0, 0, w, h, black.set(0, 0, 0, dark));
			}
		}

		// title, as in the original game, fading in slowly on its own
		float tw = assets.titleText.getWidth() * 0.85f, th = assets.titleText.getHeight() * 0.85f;
		float titleY = h - 30 - th;
		float titleA = smooth(ramp(introT, TITLE_START, TITLE_FADE));
		batch.setColor(1f, 1f, 1f, titleA);
		batch.draw(assets.titleText, (w - tw) / 2, titleY, tw, th);
		batch.setColor(Color.WHITE);

		// then the menu
		fade = smooth(ramp(introT, MENU_START, MENU_FADE));
		sizeTo(hits, items.size());
		if (fade > 0) {
			float bw = 270, bh = 46, gap = 10;
			float y = titleY - 18 - bh + (1 - fade) * -12;
			for (int i = 0; i < items.size(); i++) {
				button(hits.get(i), items.get(i), w / 2, y, bw, bh, i == 0 ? assets.button : assets.buttonAlt);
				y -= bh + gap;
			}
			BitmapFont f = ui(0.9f);
			f.setColor(Palette.MUTED.r, Palette.MUTED.g, Palette.MUTED.b, fade);
			f.draw(batch, game.build.isEmpty() ? OhHeckGame.VERSION : OhHeckGame.VERSION + "  (" + game.build + ")", 10, 22);
			String c = "©2026 Kyle Holub";
			glyphs.setText(f, c);
			f.draw(batch, c, w - 10 - glyphs.width, 22);
		}
		fade = 1f;
		batch.end();
	}

	/** 0 before start, rising to 1 over length. */
	private static float ramp(float t, float start, float length) {
		return Math.max(0f, Math.min(1f, (t - start) / length));
	}

	private static float smooth(float x) {
		return x * x * (3 - 2 * x);
	}

	@Override
	protected void tap(float x, float y) {
		if (introT < INTRO_END) {
			// a tap during the opening skips to the menu
			introT = Math.max(introT, INTRO_END);
			return;
		}
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
		} else if (item.equals("High scores")) {
			game.showHighScores(-1);
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
		return introT < INTRO_END ? "intro" : "title";
	}
}
