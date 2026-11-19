package ohheck;

import java.util.ArrayDeque;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

import ohheck.ui.Assets;

/* The back stack of screens, and the short dark fade each new screen rises out of. */
final class Screens {

	static final float FADE = 0.2f, FADE_FROM = 0.55f;

	private final Game game;
	private final ArrayDeque<Screen> backStack = new ArrayDeque<Screen>();
	private final Matrix4 whole = new Matrix4().setToOrtho2D(0, 0, 1, 1);
	private final Color tint = new Color();
	private float sinceChange = FADE;

	Screens(Game game) {
		this.game = game;
	}

	void open(Screen s) {
		if (game.getScreen() != null) {
			backStack.push(game.getScreen());
		}
		show(s);
	}

	void back(Screen home) {
		show(backStack.isEmpty() ? home : backStack.pop());
	}

	void replaceAll(Screen s) {
		backStack.clear();
		show(s);
	}

	private void show(Screen s) {
		if (s != game.getScreen()) {
			sinceChange = 0;
		}
		game.setScreen(s);
	}

	boolean changing() {
		return sinceChange < FADE;
	}

	void drawChange(SpriteBatch batch, Assets assets, float delta) {
		if (!changing()) {
			return;
		}
		float dark = FADE_FROM * (1 - sinceChange / FADE);
		sinceChange += delta;
		batch.setProjectionMatrix(whole);
		batch.begin();
		batch.setColor(tint.set(0, 0, 0, dark));
		batch.draw(assets.white, 0, 0, 1, 1);
		batch.setColor(Color.WHITE);
		batch.end();
	}
}
