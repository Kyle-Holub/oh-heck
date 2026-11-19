package ohheck;

import java.util.ArrayDeque;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

import ohheck.ui.Assets;

/**
 * Which screen is showing, the way back, and the change between them: each new screen rises
 * out of a dark tint over a moment, so screens don't snap from one to the next.
 */
final class Screens {

	/** How long a new screen takes to come up, and how dark it starts. */
	static final float FADE = 0.2f, FADE_FROM = 0.55f;

	private final Game game;
	private final ArrayDeque<Screen> backStack = new ArrayDeque<Screen>();
	private final Matrix4 whole = new Matrix4().setToOrtho2D(0, 0, 1, 1);
	private final Color tint = new Color();
	/** Seconds since the screen changed. */
	private float sinceChange = FADE;

	Screens(Game game) {
		this.game = game;
	}

	/** Goes to s, remembering this screen for Back. */
	void open(Screen s) {
		if (game.getScreen() != null) {
			backStack.push(game.getScreen());
		}
		show(s);
	}

	/** Back to the screen before (or home, if there's none). */
	void back(Screen home) {
		show(backStack.isEmpty() ? home : backStack.pop());
	}

	/** Goes to s, forgetting the way back (the title, a new table). */
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

	/** Whether a change is still coming up (so the frame rate stays up). */
	boolean changing() {
		return sinceChange < FADE;
	}

	/** After the screen has drawn: the tint over it while it comes up. */
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
