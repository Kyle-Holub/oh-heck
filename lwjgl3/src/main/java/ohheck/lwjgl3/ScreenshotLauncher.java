package ohheck.lwjgl3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
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
import rules.Bot;
import rules.Difficulty;
import rules.Match;
import rules.Player;
import rules.Rules;
import rules.Stats;

/**
 * Plays a seeded game with the bot in every seat, saving a screenshot the
 * first time each interesting state appears, then exits. Game time advances a
 * fixed step per frame, so it runs as fast as frames can be drawn. Run headless on CI
 * under Xvfb so the layout can be reviewed without a phone.
 */
public class ScreenshotLauncher {

	/** Prefixes of states to capture, as reported by the screens' stateTag(). */
	private static final String[] WANTED = { "intro", "title", "highscores", "statistics", "options", "players", "rules", "shuffle:1", "deal:3", "bid:3",
			"trick:4", "collect:4", "trickwon:", "handend:4", "handover:4", "deal:7", "bidding:7", "menu:7", "play:8:",
			"tossing", "gameover" };

	/** Hands played instantly: nothing on the list happens in them. */
	private static final Integer[] SKIP = { 2, 5, 6 };

	/** Each frame advances the game this far; screens cap a frame at 0.1 s. */
	private static final float STEP = 0.1f;

	/** Frames to wait in a state before capturing it, so animations are under way. */
	private static int settleFrames(String state) {
		if (state.equals("intro")) {
			return 10; // 2 s in: sparkles, title fading in, no cards or menu yet
		}
		if (state.equals("title")) {
			return 10; // cards falling, before the scripted run moves on at ~17 frames
		}
		if (state.equals("rules")) {
			return 8; // scrolled down a little
		}
		if (state.equals("tossing")) {
			return 8; // the final hand's cards in the air
		}
		if (state.equals("gameover")) {
			return 14; // (after 1.2 s of tossing) the winner's fireworks bursting
		}
		if (state.startsWith("handover")) {
			return 12; // the made/set labels have dropped beside the names
		}
		if (state.startsWith("shuffle")) {
			return 9; // mid-riffle
		}
		if (state.startsWith("deal")) {
			return 8; // about a third of the cards dealt
		}
		return 2;
	}

	public static void main(String[] args) {
		final String outDir = args.length > 0 ? args[0] : "screenshots";
		final Set<String> wanted = new HashSet<String>(Arrays.asList(WANTED));
		final int[] frames = { 0 };
		final String[] seen = { "" };
		final int[] inState = { 0 };
		final long start = System.nanoTime();

		GameOptions o = new GameOptions();
		o.seed = 20261118L; // a game the scripted player wins, to show the fireworks
		o.autoplayHuman = true;
		o.persist = false;
		o.speed = 2f;
		o.fixedStep = STEP; // game time per frame, not wall-clock time: runs as fast as frames are drawn
		o.skipHands.addAll(Arrays.asList(SKIP));
		o.stats = someStats();
		o.frameListener = new GameOptions.FrameListener() {
			@Override
			public void onFrame(String state) {
				frames[0]++;
				if (!state.equals(seen[0])) {
					seen[0] = state;
					inState[0] = 0;
				}
				inState[0]++;
				if (inState[0] == settleFrames(state)) {
					for (String w : WANTED) {
						if (state.startsWith(w) && wanted.remove(w)) {
							save(outDir + "/" + String.format("%02d-", WANTED.length - wanted.size()) + state.replace(':', '-') + ".png");
							break;
						}
					}
				}
				boolean finished = wanted.isEmpty() || (state.equals("gameover") && inState[0] > settleFrames(state));
				if (finished || frames[0] > 20000) {
					System.out.printf("Screenshots: %d frames in %.1f s, %d states not seen: %s%n", frames[0],
							(System.nanoTime() - start) / 1e9, wanted.size(), wanted);
					Gdx.app.exit();
				}
			}
		};
		Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.config(1560, 720);
		config.disableAudio(true);
		config.useVsync(false);
		config.setForegroundFPS(0); // as fast as possible
		new Lwjgl3Application(new OhHeckGame(o), config);
	}

	/** Statistics from a dozen quick games between bots, so the statistics page has numbers to show. */
	private static Stats someStats() {
		Stats stats = new Stats();
		Random rng = new Random(7);
		for (int g = 0; g < 12; g++) {
			Difficulty level = Difficulty.values()[g % 3];
			int[] players = Player.draw(level, rng);
			Bot[] bots = { Bot.of(Difficulty.NORMAL, rng), Player.ALL.get(players[0]).bot(rng),
					Player.ALL.get(players[1]).bot(rng), Player.ALL.get(players[2]).bot(rng) };
			Match m = new Match(4, rng, rng.nextInt(4));
			stats.startGame();
			while (m.phase() != Match.Phase.GAME_OVER) {
				int s = m.currentSeat();
				if (m.phase() == Match.Phase.BIDDING) {
					m.placeBid(s, bots[s].chooseBid(m.view(s)));
				} else if (m.phase() == Match.Phase.PLAYING) {
					m.playCard(s, bots[s].choosePlay(m.view(s)));
				} else {
					stats.countHand(m.handIndex(), level, m.bid(0), m.tricksWon(0));
					m.nextHand();
				}
			}
			int[] finals = new int[4];
			for (int s = 0; s < 4; s++) {
				finals[s] = m.score(s);
			}
			stats.countGame(level, finals, Rules.numberOfHands(), players);
		}
		return stats;
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
