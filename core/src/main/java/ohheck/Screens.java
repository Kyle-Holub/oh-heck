package ohheck;

import java.util.ArrayDeque;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

import ohheck.platform.Audio;
import ohheck.ui.Assets;

/*
 * The back stack of screens, and the change between them: a dark band narrowing away like a card turned on
 * its edge, with a soft whoosh.
 */
final class Screens {

	static final float FADE = 0.22f, FADE_FROM = 0.6f;

	private final OhHeckGame game;
	private final ArrayDeque<Screen> backStack = new ArrayDeque<Screen>();
	private final Matrix4 whole = new Matrix4().setToOrtho2D(0, 0, 1, 1);
	private final Color tint = new Color();
	private float sinceChange = FADE;

	Screens(OhHeckGame game) {
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
			if (game.getScreen() != null) {
				game.audio().play(Audio.Effect.WHOOSH);
			}
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
		float p = sinceChange / FADE, edge = (float) Math.cos(p * Math.PI / 2);
		sinceChange += delta;
		batch.setProjectionMatrix(whole);
		batch.begin();
		batch.setColor(tint.set(0, 0, 0, FADE_FROM * edge));
		batch.draw(assets.white, 0.5f - edge / 2, 0, edge, 1);
		batch.setColor(Color.WHITE);
		batch.end();
	}
}
