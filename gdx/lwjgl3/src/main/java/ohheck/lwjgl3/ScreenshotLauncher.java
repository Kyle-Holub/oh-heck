package ohheck.lwjgl3;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.GameOptions;
import ohheck.OhHeckGame;

/**
 * Plays a seeded game with the bot in every seat, saving a screenshot the
 * first time each interesting state appears, then exits. Run headless on CI
 * under Xvfb so the layout can be reviewed without a phone.
 */
public class ScreenshotLauncher {

	/** Prefixes of states to capture, as reported by PlayScreen.stateTag(). */
	private static final String[] WANTED = { "title", "rules", "options", "deal:3", "bid:3", "bidconfirm:3", "selected:4:", "trick:4", "collect:4", "trickwon:", "handend:4", "handover:4", "menu:6", "play:8:", "gameover" };

	public static void main(String[] args) {
		final String outDir = args.length > 0 ? args[0] : "screenshots";
		final Set<String> wanted = new HashSet<String>();
		for (String s : WANTED) {
			wanted.add(s);
		}
		final int[] frames = { 0 };
		final String[] seen = { "" };
		final long[] seenAt = { 0 };
		final boolean[] done = { false };

		GameOptions o = new GameOptions();
		o.seed = 20261116L;
		o.autoplayHuman = true;
		o.speed = 2f;
		o.frameListener = new GameOptions.FrameListener() {
			@Override
			public void onFrame(String state) {
				frames[0]++;
				long now = System.nanoTime();
				if (!state.equals(seen[0])) {
					seen[0] = state;
					seenAt[0] = now;
					done[0] = false;
				}
				// let each state settle before capturing it; the menus need a moment longer
				float settle = state.equals("title") ? 1.2f : state.equals("rules") ? 0.75f : 0.12f;
				if (!done[0] && (now - seenAt[0]) / 1e9f >= settle) {
					done[0] = true;
					for (String w : WANTED) {
						if (state.startsWith(w) && wanted.remove(w)) {
							save(outDir + "/" + String.format("%02d-", WANTED.length - wanted.size()) + state.replace(':', '-') + ".png");
							break;
						}
					}
				}
				if (wanted.isEmpty() || (state.equals("gameover") && done[0]) || frames[0] > 60 * 60 * 5) {
					Gdx.app.exit();
				}
			}
		};
		Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.config(1560, 720);
		config.disableAudio(true);
		new Lwjgl3Application(new OhHeckGame(o), config);
	}

	private static void save(String path) {
		int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
		byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
		// make it opaque
		for (int i = 4; i <= pixels.length; i += 4) {
			pixels[i - 1] = (byte) 255;
		}
		Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		BufferUtils.copy(pixels, 0, p.getPixels(), pixels.length);
		FileHandle f = new FileHandle(path);
		PixmapIO.writePNG(f, p);
		p.dispose();
		System.out.println("Saved " + path);
	}
}
