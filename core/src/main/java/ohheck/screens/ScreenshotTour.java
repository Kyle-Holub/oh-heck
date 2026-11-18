package ohheck.screens;

import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;

/**
 * The screenshot run's walk through the menus, all in one place: Title, High scores
 * (both tabs), Options, Roster (top, then the end of the list), each step of How to
 * play, and on into a game, where {@link Autoplay} takes your seat. Each menu screen
 * calls {@link #step} once a frame; normal play has no tour.
 * Times are seconds the screen has been shown, at game speed.
 */
public final class ScreenshotTour implements GameHost.Tour {

	static final float TITLE = 3.5f, OPTIONS = 1.5f;
	/** High scores: the Statistics tab from, and on to Options after. */
	static final float STATISTICS_FROM = 1.5f, SCORES_LEAVE = 3f;
	/** The Roster: the end of the list from, and on to How to play after. */
	static final float ROSTER_END_FROM = 1.0f, ROSTER_LEAVE = 2.2f;
	/** On each step of How to play. */
	static final float TUTORIAL_STEP = 4.0f;

	private final GameHost game;

	public ScreenshotTour(GameHost game) {
		this.game = game;
	}

	/** Moves the tour along; true if it has gone on to another screen (so this one stops drawing). */
	@Override
	public boolean step(BaseScreen screen, float shown) {
		if (screen instanceof TitleScreen) {
			return after(shown, TITLE, () -> game.showHighScores(-1));
		}
		if (screen instanceof HighScoresScreen) {
			if (shown > STATISTICS_FROM) {
				((HighScoresScreen) screen).showStatistics();
			}
			return after(shown, SCORES_LEAVE, game::showOptions);
		}
		if (screen instanceof OptionsScreen) {
			return after(shown, OPTIONS, game::showPlayers);
		}
		if (screen instanceof PlayersScreen) {
			((PlayersScreen) screen).scrollToEnd(shown > ROSTER_END_FROM);
			return after(shown, ROSTER_LEAVE, game::showTutorial);
		}
		if (screen instanceof TutorialScreen) {
			TutorialScreen t = (TutorialScreen) screen;
			if (shown > TUTORIAL_STEP) {
				if (t.lastStep()) {
					game.newGame();
					return true;
				}
				t.next();
			}
		}
		return false;
	}

	private static boolean after(float shown, float limit, Runnable then) {
		if (shown <= limit) {
			return false;
		}
		then.run();
		return true;
	}
}
