package ohheck.screens;

import ohheck.ui.Navigator;
import ohheck.ui.Tagged;
import ohheck.ui.Tour;

/** The screenshot run's route through the menus and into a game. */
public final class ScreenshotTour implements Tour {

	static final float TITLE = 3.5f, OPTIONS = 1.5f;
	static final float STATISTICS_FROM = 1.5f, SCORES_LEAVE = 3f;
	static final float ROSTER_END_FROM = 1.0f, ROSTER_LEAVE = 2.2f;
	static final float TUTORIAL_STEP = 4.0f;

	enum Stop {
		TITLE, HIGH_SCORES, OPTIONS, ROSTER, TUTORIAL, GAME
	}

	enum Move {
		STAY,
		SECOND_HALF,
		NEXT_STEP,
		LEAVE
	}

	private final Navigator game;

	public ScreenshotTour(Navigator game) {
		this.game = game;
	}

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

	static Stop next(Stop stop) {
		return Stop.values()[Math.min(stop.ordinal() + 1, Stop.GAME.ordinal())];
	}

	@Override
	public boolean step(Tagged screen, float shown) {
		if (!(screen instanceof TourStop)) {
			return false;
		}
		TourStop at = (TourStop) screen;
		Stop stop = at.tourStop();
		Move move = plan(stop, shown, at.lastStep());
		at.secondHalf(move == Move.SECOND_HALF || move == Move.LEAVE);
		if (move == Move.NEXT_STEP) {
			at.nextStep();
		}
		if (move != Move.LEAVE) {
			return false;
		}
		go(next(stop));
		return true;
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
