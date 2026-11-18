package ohheck.screens;

import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;

/**
 * The screenshot run's walk through the menus, all in one place: Title, High scores
 * (both tabs), Options, Roster (top, then the end of the list), each step of How to
 * play, and on into a game, where Autoplay takes your seat. Each menu screen calls
 * {@link #step} once a frame; normal play has no tour.
 * <p>
 * {@link #plan} decides what happens at each stop (plain logic, tested in
 * ScreenshotTourTest); {@link #step} works out which stop a screen is and carries it out.
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

	/** The tour's stops, in order. */
	enum Stop {
		TITLE, HIGH_SCORES, OPTIONS, ROSTER, TUTORIAL, GAME
	}

	/** What to do at a stop this frame. */
	enum Move {
		/** Stay: nothing changes yet. */
		STAY,
		/** Show the second half of this stop: the Statistics tab, the end of the Roster. */
		SECOND_HALF,
		/** The tutorial's next step. */
		NEXT_STEP,
		/** On to the next stop. */
		LEAVE
	}

	private final GameHost game;

	public ScreenshotTour(GameHost game) {
		this.game = game;
	}

	/** What to do at this stop, shown for this long (lastStep: on the tutorial's last step). */
	static Move plan(Stop stop, float shown, boolean lastStep) {
		switch (stop) {
		case TITLE:
			return shown > TITLE ? Move.LEAVE : Move.STAY;
		case HIGH_SCORES:
			return shown > SCORES_LEAVE ? Move.LEAVE : shown > STATISTICS_FROM ? Move.SECOND_HALF : Move.STAY;
		case OPTIONS:
			return shown > OPTIONS ? Move.LEAVE : Move.STAY;
		case ROSTER:
			return shown > ROSTER_LEAVE ? Move.LEAVE : shown > ROSTER_END_FROM ? Move.SECOND_HALF : Move.STAY;
		case TUTORIAL:
			return shown <= TUTORIAL_STEP ? Move.STAY : lastStep ? Move.LEAVE : Move.NEXT_STEP;
		default:
			return Move.STAY;
		}
	}

	/** The stop after this one. */
	static Stop next(Stop stop) {
		return Stop.values()[Math.min(stop.ordinal() + 1, Stop.GAME.ordinal())];
	}

	/** Moves the tour along; true if it has gone on to another screen (so this one stops drawing). */
	@Override
	public boolean step(BaseScreen screen, float shown) {
		Stop stop = stopOf(screen);
		if (stop == null) {
			return false;
		}
		Move move = plan(stop, shown, stop == Stop.TUTORIAL && ((TutorialScreen) screen).lastStep());
		if (stop == Stop.HIGH_SCORES && move != Move.STAY) {
			((HighScoresScreen) screen).showStatistics();
		}
		if (stop == Stop.ROSTER) {
			((PlayersScreen) screen).scrollToEnd(move != Move.STAY);
		}
		if (move == Move.NEXT_STEP) {
			((TutorialScreen) screen).next();
		}
		if (move != Move.LEAVE) {
			return false;
		}
		go(next(stop));
		return true;
	}

	private static Stop stopOf(BaseScreen screen) {
		if (screen instanceof TitleScreen) {
			return Stop.TITLE;
		}
		if (screen instanceof HighScoresScreen) {
			return Stop.HIGH_SCORES;
		}
		if (screen instanceof OptionsScreen) {
			return Stop.OPTIONS;
		}
		if (screen instanceof PlayersScreen) {
			return Stop.ROSTER;
		}
		if (screen instanceof TutorialScreen) {
			return Stop.TUTORIAL;
		}
		return null;
	}

	private void go(Stop to) {
		switch (to) {
		case HIGH_SCORES:
			game.showHighScores(-1);
			break;
		case OPTIONS:
			game.showOptions();
			break;
		case ROSTER:
			game.showPlayers();
			break;
		case TUTORIAL:
			game.showTutorial();
			break;
		case GAME:
			game.newGame();
			break;
		default:
			break;
		}
	}
}
