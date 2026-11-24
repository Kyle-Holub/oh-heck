package ohheck.lwjgl3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.OhHeckGame;
import ohheck.campaign.Chapter;
import ohheck.campaign.TrickShots;
import ohheck.platform.GameOptions;
import ohheck.players.Bot;
import ohheck.players.Difficulty;
import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.records.Stats;
import ohheck.rules.Match;
import ohheck.screens.CampaignScreen;
import ohheck.screens.HighScoresScreen;
import ohheck.screens.PlayersScreen;
import ohheck.screens.TutorialScreen;

/*
 * Plays a seeded game with a bot in your seat at fixed time steps, saving a screenshot the
 * first time each wanted state appears. CI runs it under Xvfb.
 */
public class ScreenshotLauncher {

	private static final String[] WANTED = { "intro", "title", "play", "setup", "highscores", "statistics", "badges", "options", "players", "campaign", "brief:chapter", "chapter:1", "brief:table", "shot:lone-ace", "cleared", "tutorial:1", "tutorial:3", "tutorial:5", "tutorial:6", "tutorial:8", "tutorial:9", "tutorial:10", "tutorial:11", "tutorial:13", "shuffle:1", "deal:3", "bid:3",
			"trick:4", "collect:4", "trickwon:", "handend:4", "handover:4", "deal:4", "deal:7", "bidding:7", "menu:7", "play:8:",
			"tossing", "gameover" };

	private static String[] only(String[] prefixes) {
		java.util.List<String> kept = new java.util.ArrayList<String>();
		for (String w : WANTED) {
			for (String p : prefixes) {
				if (!p.trim().isEmpty() && w.startsWith(p.trim())) {
					kept.add(w);
					break;
				}
			}
		}
		return kept.toArray(new String[0]);
	}

	private static final Integer[] SKIP = { 2, 5, 6 };

	private static final float STEP = 0.1f, SPEED = 2f;

	private static int settleFrames(String state) {
		if (state.equals("intro")) {
			return 10;
		}
		if (state.startsWith("tutorial")) {
			return 19;
		}
		if (state.equals("title")) {
			return 10;
		}
		if (state.equals("tossing")) {
			return 8;
		}
		if (state.equals("gameover")) {
			return 6;
		}
		if (state.startsWith("handover")) {
			return 12;
		}
		if (state.startsWith("shuffle")) {
			return 9;
		}
		if (state.startsWith("deal")) {
			return 8;
		}
		return 2;
	}

	public static void main(String[] args) {
		String outDir = args.length > 0 ? args[0] : "screenshots";
		String[] list = args.length > 1 && !args[1].trim().isEmpty() ? only(args[1].split(",")) : WANTED;
		if (list.length == 0) {
			System.err.println("No screenshot states start with \"" + args[1] + "\". They are: " + Arrays.toString(WANTED));
			System.exit(1);
		}
		Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.config(1560, 720);
		config.disableAudio(true);
		config.useVsync(false);
		config.setForegroundFPS(0);
		Capture capture = new Capture(outDir, list);
		OhHeckGame game = new OhHeckGame(scriptedRun(capture));
		capture.walk = new Walk(game);
		new Lwjgl3Application(game, config);
	}

	private static GameOptions scriptedRun(GameOptions.FrameListener capture) {
		GameOptions o = new GameOptions();
		o.seed = 20261201L;
		o.autoplayHuman = true;
		o.persist = false;
		o.speed = SPEED;
		o.fixedStep = STEP;
		o.skipHands.addAll(Arrays.asList(SKIP));
		o.stats = someStats();
		o.frameListener = capture;
		return o;
	}

	private static final class Capture implements GameOptions.FrameListener {
		private static final int MAX_FRAMES = 20000;

		private final String outDir;
		private final String[] list;
		private final Set<String> wanted;
		private final long start = System.nanoTime();
		private int frames, framesInState;
		private String state = "";
		Walk walk;

		Capture(String outDir, String[] list) {
			this.outDir = outDir;
			this.list = list;
			this.wanted = new HashSet<String>(Arrays.asList(list));
		}

		@Override
		public void onFrame(String now) {
			walk.onFrame(now, STEP * SPEED);
			frames++;
			if (!now.equals(state)) {
				state = now;
				framesInState = 0;
			}
			framesInState++;
			if (framesInState == settleFrames(state)) {
				captureIfWanted();
			}
			boolean finished = wanted.isEmpty() || (state.equals("gameover") && framesInState > settleFrames(state));
			if (finished || frames > MAX_FRAMES) {
				System.out.printf("Screenshots: %d frames in %.1f s, %d states not seen: %s%n", frames,
						(System.nanoTime() - start) / 1e9, wanted.size(), wanted);
				Gdx.app.exit();
			}
		}

		private void captureIfWanted() {
			for (String w : list) {
				if (state.startsWith(w) && wanted.remove(w)) {
					save(outDir + "/" + String.format("%02d-", list.length - wanted.size()) + state.replace(':', '-') + ".png");
					return;
				}
			}
		}
	}

	private static Stats someStats() {
		Stats stats = new Stats();
		Random rng = new Random(7);
		for (int g = 0; g < 12; g++) {
			Difficulty level = Difficulty.values()[g % 3];
			Seating players = Player.draw(level, rng);
			Bot[] bots = { Bot.of(Difficulty.NORMAL, rng), players.at(1).bot(rng), players.at(2).bot(rng),
					players.at(3).bot(rng) };
			Match m = new Match(4, rng, rng.nextInt(4));
			stats.startGame();
			while (m.phase() != Match.Phase.GAME_OVER) {
				int s = m.currentSeat();
				if (m.phase() == Match.Phase.BIDDING) {
					m.placeBid(s, bots[s].chooseBid(m.view(s)));
				} else if (m.phase() == Match.Phase.PLAYING) {
					m.playCard(s, bots[s].choosePlay(m.view(s)));
				} else {
					stats.countHand(m.handIndex(), level, m.bid(0), m.tricksWon(0), m.lastHandPoints(0));
					m.nextHand();
				}
			}
			int[] finals = new int[4];
			for (int s = 0; s < 4; s++) {
				finals[s] = m.score(s);
			}
			stats.countGame(level, finals, m.rules().numberOfHands(), players);
		}
		return stats;
	}

	private static void save(String path) {
		int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
		byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
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

	private static final class Walk {
		private static final int TUTORIAL_STEPS = 13;

		private final List<String> states = new ArrayList<String>();
		private final List<Float> waits = new ArrayList<Float>();
		private final List<Consumer<Screen>> actions = new ArrayList<Consumer<Screen>>();
		private int next;
		private float waited;

		Walk(OhHeckGame game) {
			then("title", 3.5f, s -> game.showPlayMenu());
			then("play", 1.5f, s -> game.showGameSetup());
			then("setup", 1.5f, s -> game.showHighScores(-1));
			then("highscores", 1.5f, s -> ((HighScoresScreen) s).showStatistics());
			then("statistics", 1.5f, s -> ((HighScoresScreen) s).showBadges());
			then("badges", 1.5f, s -> game.showOptions());
			then("options", 1.5f, s -> game.showPlayers());
			then("players", 1.0f, s -> ((PlayersScreen) s).scrollToEnd());
			then("players", 1.2f, s -> game.showCampaign());
			then("campaign", 1.5f, s -> ((CampaignScreen) s).briefChapter(Chapter.ONE));
			then("brief:chapter", 1.5f, s -> ((CampaignScreen) s).go());
			then("chapter:1", 1.5f, s -> ((CampaignScreen) s).briefTable(Chapter.ONE));
			then("brief:table", 1.5f, s -> game.playTrickShot(TrickShots.ALL.get(0)));
			then("shot:lone-ace", 3.5f, s -> game.showChapterCleared(Chapter.ONE));
			then("cleared", 1.5f, s -> game.showTutorial());
			for (int step = 1; step < TUTORIAL_STEPS; step++) {
				then("tutorial:" + step, 4.0f, s -> ((TutorialScreen) s).next());
			}
			then("tutorial:" + TUTORIAL_STEPS, 4.0f, s -> game.newGame());
		}

		private void then(String state, float seconds, Consumer<Screen> action) {
			states.add(state);
			waits.add(seconds);
			actions.add(action);
		}

		void onFrame(String state, float elapsed) {
			if (next >= actions.size() || !state.equals(states.get(next))) {
				return;
			}
			waited += elapsed;
			if (waited >= waits.get(next)) {
				waited = 0;
				actions.get(next++).accept(((Game) Gdx.app.getApplicationListener()).getScreen());
			}
		}
	}
}
