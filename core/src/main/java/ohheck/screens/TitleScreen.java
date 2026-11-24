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

/* The main menu, over falling cards. */
public class TitleScreen extends BaseScreen {

	private final FallingCards cards;

	private static final float SCENE_START = 0.4f, SCENE_FADE = 0.9f, TITLE_START = 1.0f, TITLE_FADE = 1.6f,
			CARDS_START = TITLE_START + TITLE_FADE, MENU_START = 5.0f, MENU_FADE = 0.8f,
			INTRO_END = MENU_START + MENU_FADE;
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
		items.add("Play");
		items.add("Options");
		items.add("Quit");
	}

	public void replayIntro() {
		introT = 0;
	}

	@Override
	public void show() {
		super.show();
		if (introT > 0) {
			introT = Math.max(introT, INTRO_END);
		}
		buildItems();
		if (introT >= INTRO_END) {
			game.audio().startMusic();
		}
	}

	@Override
	public boolean animating() {
		return true;
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

	private void advance(float dt) {
		introT += dt;
		if (introT >= SCENE_START) {
			cards.setFalling(introT >= CARDS_START);
			cards.update(dt, w, h);
		}
		if (introT >= INTRO_END && introT - dt < INTRO_END) {
			game.audio().startMusic();
		}
		if (introT >= INTRO_END) {
			shownFor += dt;
		}
	}

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

	private float drawTitle() {
		float tw = assets.titleText.getWidth() * 0.85f, th = assets.titleText.getHeight() * 0.85f;
		float titleY = h - 30 - th;
		float titleA = TITLE_IN.progress(introT);
		batch.setColor(1f, 1f, 1f, titleA);
		batch.draw(assets.titleText, (w - tw) / 2, titleY, tw, th);
		batch.setColor(Color.WHITE);
		return titleY;
	}

	private String versionLine;

	private void drawMenu(float titleY) {
		float bw = 270, bh = 46, gap = 10;
		float y = titleY - 18 - bh + (1 - paint.fade()) * -12;
		for (int i = 0; i < items.size(); i++) {
			paint.button(hits.get(i), items.get(i), w / 2, y, bw, bh, i == 0 ? assets.button : assets.buttonAlt);
			y -= bh + gap;
		}
		BitmapFont f = paint.ui(0.9f, Palette.MUTED, paint.fade());
		if (versionLine == null) {
			versionLine = game.build().isEmpty() ? game.version() : game.version() + "  (" + game.build() + ")";
		}
		f.draw(batch, versionLine, 10, 22);
		String c = "\u00a92026 Kyle Holub";
		glyphs.setText(f, c);
		f.draw(batch, c, w - 10 - glyphs.width, 22);
	}

	@Override
	protected void tap(float x, float y) {
		if (introT < INTRO_END) {
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
		if (item.equals("Play")) {
			game.showPlayMenu();
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
