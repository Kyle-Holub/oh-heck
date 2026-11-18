package ohheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Card;
import rules.Difficulty;
import rules.Match;
import rules.Player;
import rules.Rules;
import rules.SavedGame;

/**
 * The card table: you (bottom) against three computer players (left, top,
 * right). The rules live in {@link Match}; this screen runs the table's
 * {@link TableState} machine, runs the computer players and draws it all,
 * with help from:
 * <ul>
 * <li>{@link TableLayout}: where things go
 * <li>{@link DealAnimation} and {@link TrickView}: cards moving
 * <li>{@link TableOverlays}: panels, scores and menus over the table
 * <li>{@link TableInput}: your taps and drags, and the pause menu
 * <li>{@link TableDrawing}, {@link TableToss} and {@link GameEnd}: drawing the table, and the game's end
 * <li>{@link Autoplay}: the scripted screenshot run (not normal play)
 * </ul>
 * Playing a card: tap it, or drag it up onto the table. Bidding: tap a number.
 */
public class PlayScreen extends BaseScreen {


	private static final int HUMAN = TableLayout.HUMAN;

	/** Your "Bid tricks!"/"Set!" takes this long to glide beside your panel. */
	private static final float LABEL_DROP = 0.6f;

	private final GameOptions options;
	/**
	 * Separate random streams for who sits down, the bots' own randomness and the deck, so a
	 * change that draws on one (a new player, a new habit) doesn't reshuffle the others: the
	 * screenshot run's seeded game stays the same game.
	 */
	private final Random seating, bots, deck;

	final TableLayout table = new TableLayout();
	final TableOverlays overlays;
	private final TableFlow flow;
	private final CardArt art;
	/** Draws the table (counters, trump, panels, your hand); the trick, overlays and effects are separate. */
	private final TableDrawing drawing;
	private final TrickView trick = new TrickView();
	private final Sparks sparks = new Sparks();
	private final Fireworks fireworks = new Fireworks();
	/** At the game's end: the cards thrown off the table, and the order of what follows. */
	private final TableToss toss;
	private final GameEnd end = new GameEnd();
	/** Drives your seat in scripted screenshot runs; null in normal play. */
	private final Autoplay autoplay;

	/** Game time, for animations; stops while paused. */
	private float time;
	/** Where your just-finished game landed in the high score table, or -1. */
	private int newHighScore = -1;
	/** The difficulty this game was started at: its high score badge and statistics level. */
	private Difficulty level;
	/** Who sits in seats 1-3 ({@link Player} ids). */
	private int[] players;

	/** Your taps and drags, and the pause menu. */
	private final TableInput input = new TableInput(this);

	/** Your hand in display order, and each card's tap area; rebuilt when drawn. */
	final List<Card> handOrder = new ArrayList<Card>();
	final List<Rectangle> handRects = new ArrayList<Rectangle>();

	/**
	 * A new game (restored null), or the saved game restored, started at the given difficulty
	 * with the given players seated (drawn afresh if null, as for saves from before the pool).
	 */
	public PlayScreen(OhHeckGame game, Match restored, Difficulty savedLevel, int[] savedPlayers) {
		super(game);
		this.options = game.options;
		seating = stream(options.seed, 0x5EA7L);
		bots = stream(options.seed, 0xB075L);
		deck = stream(options.seed, 0xDEC4L);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(this, table, art);
		this.autoplay = options.autoplayHuman ? new Autoplay(this, options) : null;
		this.drawing = new TableDrawing(this, art);
		this.toss = new TableToss(assets, table);
		this.flow = new TableFlow(new TableFlow.Listener() {
			@Override
			public void beforeDeal(Match m) {
				if (autoplay != null) {
					autoplay.skipHands(m);
				}
				trick.clear();
				input.clearTouch();
			}

			@Override
			public void save(Match m) {
				PlayScreen.this.save();
			}

			@Override
			public void shuffleSound() {
				PlayScreen.this.game.audio.play(Audio.Effect.SHUFFLE);
			}

			@Override
			public void played(int seat, Card c) {
				if (seat != HUMAN) {
					// computer players' cards fly in from their fan; yours already started from your hand
					Vector2 from = table.seatAnchor(seat);
					trick.fly(c, from.x - Assets.CARD_W / 2f, from.y - Assets.CARD_H / 2f, time);
				}
				Audio audio = PlayScreen.this.game.audio;
				audio.play(Audio.Effect.PLAY_CARD);
				if (flow.match().phase() == Match.Phase.HAND_OVER) {
					audio.play(flow.match().madeBid(HUMAN) ? Audio.Effect.WIN : Audio.Effect.LOSE);
				}
			}

			@Override
			public void handEnded() {
				Match m = flow.match();
				if (PlayScreen.this.game.stats().countHand(m.handIndex(), level, m.bid(HUMAN), m.tricksWon(HUMAN))) {
					PlayScreen.this.game.saveStats();
				}
				if (m.madeBid(HUMAN)) {
					sparks.burst(table.centreX(), table.centreY(), 110);
				}
			}

			@Override
			public void gameEnded() {
				Match m = flow.match();
				newHighScore = PlayScreen.this.game.recordHighScore(m.score(HUMAN), overlays.place(HUMAN), level);
				int[] finals = new int[m.players()];
				for (int s = 0; s < finals.length; s++) {
					finals[s] = m.score(s);
				}
				if (PlayScreen.this.game.stats().countGame(level, finals, Rules.numberOfHands(), players)) {
					PlayScreen.this.game.saveStats();
				}
				overlays.playAgainButton.set(0, 0, 0, 0); // not tappable until drawn
				overlays.highScoresButton.set(0, 0, 0, 0);
				// in turn: the table's cards fall away, then (if you won) fireworks, then the scores
				toss.toss(m);
				end.start(m.leaders().contains(HUMAN)); // you won (or tied for the win)
			}
		});
		if (restored == null) {
			newGame();
		} else {
			level = savedLevel;
			seatPlayers(savedPlayers != null ? savedPlayers : Player.draw(level, seating));
			overlays.setMatch(restored);
			flow.resume(restored);
		}
	}

	// ------------------------------------------------------------ the game

	Match match() {
		return flow.match();
	}

	TableState state() {
		return flow.state();
	}

	float stateTime() {
		return flow.stateTime();
	}

	boolean isMenuOpen() {
		return input.menuOpen();
	}

	boolean isDragging() {
		return input.dragging();
	}

	public boolean isGameOver() {
		return flow.match().phase() == Match.Phase.GAME_OVER;
	}

	/** Game over and the scores are up (after the cards have fallen, and any fireworks have begun). */
	boolean resultsShown() {
		return flow.state() == TableState.GAME_OVER && end.resultsShown(flow.stateTime());
	}

	// for TableDrawing: what it draws from

	TableFlow flow() {
		return flow;
	}

	float time() {
		return time;
	}

	Card pressedCard() {
		return input.pressed();
	}

	/** A new game: a table drawn for the difficulty now chosen in Options. */
	private void newGame() {
		level = game.settings.difficulty();
		seatPlayers(Player.draw(level, seating));
		toss.clear();
		end.reset();
		game.stats().startGame();
		game.saveStats();
		Match m = Match.random(TableLayout.SEATS, deck, game.settings.handOrder());
		overlays.setMatch(m);
		newHighScore = -1;
		flow.start(m);
	}

	boolean humanCanPlay() {
		return !input.menuOpen() && flow.yourTurnToPlay();
	}

	// ----------------------------------------------------------- update

	@Override
	public void render(float delta) {
		update(Math.min(delta, 0.1f) * options.speed * game.settings.speedFactor());
		draw();
	}

	private void update(float dt) {
		if (input.menuOpen()) {
			if (autoplay != null) {
				autoplay.whilePaused(dt);
			}
			return;
		}
		time += dt;
		TableState before = flow.state();
		flow.update(dt);
		if (before == TableState.TRICK && flow.state() != TableState.TRICK) {
			trick.clear(); // the winner has gathered the trick in
		}
		overlays.update(dt);
		sparks.step(dt);
		fireworks.update(dt);
		toss.update(dt);
		if (end.update(flow.stateTime(), toss.allGone())) { // the last card has fallen, and you won
			sparks.burst(table.w / 2, table.h / 2 + 100, 160);
			fireworks.show(table.w, table.h);
		}
		drawing.updateMotes(dt, flow.trumpFaceUp() && !toss.trumpGone());
		if (autoplay != null) {
			if (flow.yourTurnToBid() || flow.yourTurnToPlay()) {
				autoplay.yourTurn(dt);
			} else if (flow.state() == TableState.RESULTS) {
				autoplay.onResults(flow.stateTime());
			}
		}
	}

	@Override
	boolean animating() {
		if (input.menuOpen()) {
			return false;
		}
		switch (flow.state()) {
		case SHUFFLE:
		case DEAL:
		case TRICK:
		case HAND_END:
			return true;
		case RESULTS:
			if (flow.stateTime() < LABEL_DROP) {
				return true;
			}
			break;
		case TURN:
			if (flow.trumpTime() < TableFlow.TRUMP_FLIP_TIME) {
				return true;
			}
			break;
		default:
			break;
		}
		return input.dragging() || sparks.active() || fireworks.active() || trick.anyInFlight(time) || toss.active();
	}

	// ------------------------------------------------------------ input

	@Override
	public void show() {
		super.show();
		game.audio.startMusic();
	}

	/** A random stream: unseeded normally; from the run's seed and a salt for the screenshot run. */
	static Random stream(Long seed, long salt) {
		return seed == null ? new Random() : new Random(seed * 0x9E3779B97F4A7C15L ^ salt);
	}

	/** The name shown for a seat: "You", or the computer player sitting there. */
	String name(int seat) {
		return seat == HUMAN ? "You" : Player.byId(players[seat - 1]).name;
	}

	private void seatPlayers(int[] ids) {
		players = ids;
		flow.seat(ids, bots);
	}

	/** Saves the game so it survives Android closing the app; a finished game is forgotten. */
	private void save() {
		if (options.persist) {
			if (isGameOver()) {
				game.settings.clearSavedGame();
			} else {
				game.settings.saveGame(new SavedGame(match(), level, players));
			}
		}
	}

	@Override
	public void pause() {
		save();
		if (autoplay == null) {
			input.openMenu(); // leaving the app (home button, a call): come back to the menu
		}
	}

	// touches go to TableInput

	@Override
	protected void touchDown(float x, float y) {
		super.touchDown(x, y);
		input.touchDown(x, y);
	}

	@Override
	protected void touchDragged(float x, float y) {
		input.touchDragged(x, y, movedFrom(x, y));
	}

	@Override
	protected void touchUp(float x, float y) {
		if (!input.touchUp(x, y)) {
			super.touchUp(x, y); // a tap, unless it was a swipe
		}
	}

	/** A tap at world position (x, y). Autoplay taps come through here too. */
	@Override
	protected void tap(float x, float y) {
		input.tap(x, y);
	}

	@Override
	protected void back() {
		input.back();
	}

	/** Plays your card, flying in from (fromX, fromY). */
	void humanPlay(Card c, float fromX, float fromY) {
		input.clearTouch();
		if (flow.play(c)) {
			trick.fly(c, fromX, fromY, time);
		}
	}

	int newHighScore() {
		return newHighScore;
	}

	// ------------------------------------------------------------- draw

	private void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		table.resize(w, h);
		batch.draw(assets.table, 0, 0, w, h);

		drawing.tally();
		drawing.bidTracker();
		drawing.trump(toss.trumpGone());
		for (int seat = 0; seat < match().players(); seat++) {
			drawing.seat(seat);
		}
		drawing.hand();
		trick.draw(batch, assets, art, table, match(), flow.state() == TableState.TRICK, flow.stateTime(), time, fade);
		if (flow.isDealing()) {
			flow.deal.draw(batch, assets, art, table);
		}
		fireworks.draw(batch, assets.glow); // behind the panels, so the scores stay readable
		if (!input.menuOpen()) {
			overlays.drawMenuButton();
			switch (flow.state()) {
			case TURN:
				if (match().phase() == Match.Phase.BIDDING && match().currentSeat() == HUMAN) {
					overlays.drawBidPanel();
				}
				break;
			case HAND_END:
				overlays.drawHandEnd(flow.stateTime());
				break;
			case RESULTS:
				overlays.drawHandResults();
				overlays.drawHandLabels(MathUtils.clamp(flow.stateTime() / LABEL_DROP, 0, 1), 1f);
				break;
			case GAME_OVER:
				if (resultsShown()) {
					overlays.drawGameOver(newHighScore);
				}
				break;
			default:
				break;
			}
		}
		if (input.dragging()) {
			drawing.dragged(input.pressed(), input.dragX(), input.dragY());
		}
		toss.draw(batch); // over everything else as the cards fall away (the scores wait until they've gone)
		sparks.draw(batch, assets.glow);
		if (input.menuOpen()) {
			overlays.drawMenu();
		} else {
			overlays.drawHint();
		}
		batch.end();
	}

	// ----------------------------------------------------------- helpers

	@Override
	public String stateTag() {
		return Autoplay.describe(this);
	}
}
