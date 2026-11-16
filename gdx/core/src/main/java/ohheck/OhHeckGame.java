package ohheck;

import com.badlogic.gdx.Game;

/** The libGDX entry point shared by Android and desktop. */
public class OhHeckGame extends Game {

	private final GameOptions options;
	private Assets assets;

	public OhHeckGame() {
		this(new GameOptions());
	}

	public OhHeckGame(GameOptions options) {
		this.options = options;
	}

	@Override
	public void create() {
		assets = new Assets();
		setScreen(new PlayScreen(assets, options));
	}

	@Override
	public void dispose() {
		if (getScreen() != null) {
			getScreen().dispose();
		}
		assets.dispose();
	}
}
