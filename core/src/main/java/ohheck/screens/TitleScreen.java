package ohheck.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.fx.FallingCards;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;
import ohheck.ui.TapAreas;

/** The main menu, over a shower of falling cards. */
public class TitleScreen extends BaseScreen {


	private final FallingCards cards;

	// The opening sequence, once per launch: music over black, then cards begin to fall,
	// the title slowly fades in on its own, and after a pause the menu appears.
	/**
	 * The opening: from black the gold sparkles rise out of the dark, the title fades in
	 * among them, and once it's there the cards start to fall; then the menu, and once
	 * it's fully shown, the music.
	 */
	private static final float SCENE_START = 0.4f, SCENE_FADE = 0.9f, TITLE_START = 1.0f, TITLE_FADE = 1.6f,
			CARDS_START = TITLE_START + TITLE_FADE, MENU_START = 5.0f, MENU_FADE = 0.8f,
			INTRO_END = MENU_START + MENU_FADE;
	/** The scene rises out of black, the title fades in, then the menu. */
	private static final Tween SCENE_IN = Tween.over(SCENE_FADE, Ease.LINEAR).after(SCENE_START),
			TITLE_IN = Tween.over(TITLE_FADE, Ease.SMOOTH).after(TITLE_START),
			MENU_IN = Tween.over(MENU_FADE, Ease.SMOOTH).after(MENU_START);
	private float introT;
	private final List<String> items = new ArrayList<String>();
	private final List<Rectangle> hits = new ArrayList<Rectangle>();
	private final Color black = new Color();

	public TitleScreen(GameHost game) {
		super(game);
		Long seed = game.options().seed;
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
		if (introT >= INTRO_END) {
			game.audio().startMusic(); // (during the opening it starts with the menu)
		}
	}

	@Override
	public boolean animating() {
		return true; // the falling cards
	}

	@Override
	protected void update(float step) {
		advance(step * game.options().speed);
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Color.BLACK);
		begin();
		if (introT >= SCENE_START) {
			drawScene();
		}
		float titleY = drawTitle();
		paint.fade(MENU_IN.progress(introT));
		TapAreas.sizeTo(hits, items.size());
		if (paint.fade() > 0) {
			drawMenu(titleY);
		}
		paint.fade(1f);
		batch.end();
	}

	/** Moves the intro on; true if the screenshot run has gone on to another screen. */
	private boolean advance(float dt) {
		introT += dt;
		if (introT >= SCENE_START) {
			cards.setFalling(introT >= CARDS_START);
			cards.update(dt, w, h);
		}
		if (introT >= INTRO_END && introT - dt < INTRO_END) {
			game.audio().startMusic(); // the menu is fully shown: in comes the music
		}
		if (introT >= INTRO_END) {
			shownFor += dt;
		}
		return game.tour() != null && game.tour().step(this, shownFor);
	}

	/** A dim spotlit backdrop behind the shower, rising out of black. */
	private void drawScene() {
		batch.setColor(0.5f, 0.5f, 0.5f, 1f);
		batch.draw(assets.table, 0, 0, w, h);
		batch.setColor(Color.WHITE);
		cards.draw(batch, w / 2, h / 2);
		paint.rect(0, 0, w, h, Palette.SHADE);
		float dark = 1f - SCENE_IN.progress(introT);
		if (dark > 0) {
			paint.rect(0, 0, w, h, black.set(0, 0, 0, dark));
		}
	}

	/** The title, as in the original game, fading in slowly on its own; returns its bottom edge. */
	private float drawTitle() {
		float tw = assets.titleText.getWidth() * 0.85f, th = assets.titleText.getHeight() * 0.85f;
		float titleY = h - 30 - th;
		float titleA = TITLE_IN.progress(introT);
		batch.setColor(1f, 1f, 1f, titleA);
		batch.draw(assets.titleText, (w - tw) / 2, titleY, tw, th);
		batch.setColor(Color.WHITE);
		return titleY;
	}

	/** "alpha-1.0.9  (dev-0042)", made the first time it's drawn. */
	private String versionLine;

	/** The menu buttons under the title, the version and the copyright, all at the current fade. */
	private void drawMenu(float titleY) {
		float bw = 270, bh = 46, gap = 10;
		float y = titleY - 18 - bh + (1 - paint.fade()) * -12;
		for (int i = 0; i < items.size(); i++) {
			paint.button(hits.get(i), items.get(i), w / 2, y, bw, bh, i == 0 ? assets.button : assets.buttonAlt);
			y -= bh + gap;
		}
		BitmapFont f = paint.ui(0.9f);
		f.setColor(Palette.MUTED.r, Palette.MUTED.g, Palette.MUTED.b, paint.fade());
		if (versionLine == null) {
			versionLine = game.build().isEmpty() ? game.version() : game.version() + "  (" + game.build() + ")";
		}
		f.draw(batch, versionLine, 10, 22);
		String c = "\u00a92026 Kyle Holub";
		glyphs.setText(f, c);
		f.draw(batch, c, w - 10 - glyphs.width, 22);
	}


	/** 0 before start, rising to 1 over length. */




	@Override
	protected void tap(float x, float y) {
		if (introT < INTRO_END) {
			// a tap during the opening skips to the menu, and the music
			introT = Math.max(introT, INTRO_END);
			game.audio().startMusic();
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
			game.play(); // (the tutorial first, the very first time)
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
