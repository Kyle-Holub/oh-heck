package ohheck.table;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.fx.Fireworks;
import ohheck.fx.Sparks;
import ohheck.platform.GameOptions;
import ohheck.players.Difficulty;
import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.records.SavedGame;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;

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
 * <li>{@link YourSeat}: you, or {@link Autoplay} in the scripted screenshot run
 * </ul>
 * Playing a card: tap it, or drag it up onto the table. Bidding: tap a number.
 */
public class PlayScreen extends BaseScreen implements TableScene, TableActions {

	private static final int HUMAN = Seating.YOU;


	private final GameOptions options;
	/** The random streams for seating, the bots and the deck (kept apart: see TableStreams). */
	private final TableStreams streams;

	private final TableLayout table = new TableLayout();
	private final TableOverlays overlays;
	/** Where the overlays' buttons are, for your taps (and Autoplay's). */
	private final OverlayTargets targets = new OverlayTargets();
	private final TableFlow flow;
	private final CardArt art;
	/** Draws the table (counters, trump, panels, your hand); the trick, overlays and effects are separate. */
	private final TableDrawing drawing;
	/** Every card on the table, each one sprite for the whole game. */
	private final CardSprites cards = new CardSprites();
	private final Sparks sparks = new Sparks();
	private final Fireworks fireworks = new Fireworks();
	/** At the game's end: the cards thrown off the table, and the order of what follows. */
	private final TableToss toss;
	private final GameEnd end = new GameEnd();
	/** Who plays your seat: you, or Autoplay in the scripted screenshot run. */
	private final YourSeat you;

	/** Game time, for animations; stops while paused. */
	private float time;
	/** Keeps your statistics and high scores as the game goes. */
	private final GameRecorder recorder;
	/** The difficulty this game was started at: its high score badge and statistics level. */
	private Difficulty level;
	/** Who sits in seats 1-3 ({@link Player} ids). */
	private Seating players;

	/** Your taps and drags, and the pause menu. */
	private final TableInput input = new TableInput(this, this, targets);

	/** Your hand in display order, and each card's tap area; rebuilt when drawn. */
	private final List<Card> handOrder = new ArrayList<Card>();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>();

	/** A new game, at the difficulty now chosen in Options. */
	public static PlayScreen fresh(GameHost game) {
		return new PlayScreen(game, null);
	}

	/** The saved game, carried on where it was left. */
	public static PlayScreen resume(GameHost game, SavedGame saved) {
		return new PlayScreen(game, saved);
	}

	/** A new game (saved null), or the saved one. */
	private PlayScreen(GameHost game, SavedGame saved) {
		super(game);
		this.options = game.options();
		this.streams = TableStreams.of(options.seed);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(paint, this, table, art, targets);
		this.you = options.autoplayHuman ? new Autoplay(this, input, targets, options) : YourSeat.PERSON;
		this.drawing = new TableDrawing(this, paint, art, game.post());
		this.toss = new TableToss(assets, table);
		this.recorder = new GameRecorder(game.records());
		// your seat hears first (the screenshot run plays out the hands it skips before the rest react)
		this.flow = new TableFlow(you, new Effects(), new TableSounds(game.audio()), recorder);
		if (!you.thinkAtOnce() && options.fixedStep <= 0) {
			flow.thinkWith(Thinker.background()); // so a slow Hard player never stalls a frame
		}
		if (saved == null) {
			startGame();
		} else {
			resumeGame(saved);
		}
	}

	/** This screen's own reactions at the table: the trick, sparks, and the ending. */
	private final class Effects implements TableFlow.Listener {
		@Override
		public void beforeDeal(Match m) {
			input.clearTouch();
		}

		@Override
		public void save(Match m) {
			PlayScreen.this.save();
		}

		@Override
		public void handEnded(Match m) {
			if (m.madeBid(HUMAN)) {
				sparks.burst(table.centreX(), table.centreY(), 110);
			}
		}

		@Override
		public void gameEnded(Match m) {
			targets.clearGameOverButtons(); // not tappable until drawn
			// in turn: the table's cards fall away, then (if you won) fireworks, then the scores
			toss.toss(m);
			end.start(m.leaders().contains(HUMAN)); // you won (or tied for the win)
		}
	}

	// ------------------------------------------------- the table, as TableScene

	@Override
	public boolean isMenuOpen() {
		return input.menuOpen();
	}

	@Override
	public boolean isDragging() {
		return input.dragging();
	}

	public boolean isGameOver() {
		return flow.match().phase() == Match.Phase.GAME_OVER;
	}

	/** Game over and the scores are up (after the cards have fallen, and any fireworks have begun). */
	@Override
	public boolean resultsShown() {
		return flow.state() == TableState.GAME_OVER && end.resultsShown(flow.stateTime());
	}

	@Override
	public FlowView flow() {
		return flow;
	}

	@Override
	public float time() {
		return time;
	}

	@Override
	public Card pressedCard() {
		return input.pressed();
	}

	@Override
	public float dragX() {
		return input.dragX();
	}

	@Override
	public float dragY() {
		return input.dragY();
	}

	@Override
	public TableLayout layout() {
		return table;
	}

	@Override
	public List<Card> handOrder() {
		return handOrder;
	}

	@Override
	public List<Rectangle> handRects() {
		return handRects;
	}

	// ------------------------------------------------------------ the game


	/** A new game: a table drawn for the difficulty now chosen in Options. */
	private void startGame() {
		level = game.settings().difficulty();
		seatPlayers(Player.draw(level, streams.seating));
		toss.clear();
		end.reset();
		Match m = Match.random(Seating.SEATS, streams.deck, game.settings().handOrder(), RuleSet.STANDARD);
		overlays.setMatch(m);
		recorder.table(level, players, true);
		flow.start(m);
	}

	/** The saved game, at its own difficulty, with its players (drawn afresh for saves from before the table travelled with it). */
	private void resumeGame(SavedGame saved) {
		level = saved.level();
		seatPlayers(saved.players() != null ? saved.players() : Player.draw(level, streams.seating));
		recorder.table(level, players, false);
		overlays.setMatch(saved.match());
		flow.resume(saved.match());
	}

	@Override
	public boolean humanCanPlay() {
		return !input.menuOpen() && flow.yourTurnToPlay();
	}

	// ----------------------------------------------------------- update

	@Override
	protected void update(float step) {
		advance(step * options.speed * game.settings().speedFactor());
	}

	/** Moves the table on by dt seconds of game time. */
	private void advance(float dt) {
		if (input.menuOpen()) {
			you.whilePaused(dt);
			return;
		}
		time += dt;
		flow.update(dt);
		overlays.update(dt);
		sparks.step(dt);
		fireworks.update(dt);
		toss.update(dt);
		if (end.update(flow.stateTime(), toss.allGone())) { // the last card has fallen, and you won
			sparks.burst(table.w / 2, table.h / 2 + 100, 160);
			fireworks.show(table.w, table.h);
		}
		drawing.updateMotes(dt, flow.trumpFaceUp() && !toss.trumpGone());
		you.update(dt);
	}

	@Override
	public boolean animating() {
		if (input.menuOpen()) {
			return false;
		}
		if (StateView.of(flow.state()).animating(this)) {
			return true;
		}
		return input.dragging() || sparks.active() || fireworks.active() || cards.moving(time) || toss.active();
	}

	// ------------------------------------------------------------ input

	@Override
	public void show() {
		super.show();
		game.audio().startMusic();
	}

	/** The name shown for a seat: "You", or the computer player sitting there. */
	@Override
	public String name(int seat) {
		return seat == HUMAN ? "You" : players.at(seat).name;
	}

	private void seatPlayers(Seating ids) {
		players = ids;
		flow.seat(ids, streams.bots);
	}

	/** Saves the game so it survives Android closing the app; a finished game is forgotten. */
	private void save() {
		if (isGameOver()) {
			game.records().clearSavedGame();
		} else {
			game.records().saveGame(new SavedGame(match(), level, players));
		}
	}

	@Override
	public void pause() {
		save();
		if (you.menuOnPause()) {
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

	// ------------------------------------------- what your taps do, as TableActions

	@Override
	public boolean bid(int b) {
		return flow.bid(b);
	}

	@Override
	public void play(Card c) {
		input.clearTouch();
		flow.play(c); // (it flies to the trick from wherever its sprite is)
	}

	@Override
	public boolean skip() {
		return flow.skip();
	}

	@Override
	public boolean nextHand() {
		return flow.nextHand();
	}

	@Override
	public void hint(String s) {
		overlays.hint(s);
	}

	@Override
	public void showHighScores() {
		game.showHighScores(recorder.newHighScore());
	}

	@Override
	public void newGame() {
		game.newGame();
	}

	@Override
	public void showOptions() {
		game.showOptions();
	}

	@Override
	public void showTitle() {
		game.showTitle();
	}

	// ------------------------------------------------------------- draw

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		table.resize(w, h);
		batch.draw(assets.table, 0, 0, w, h);
		drawTable();
		fireworks.draw(batch, assets.glow); // behind the panels, so the scores stay readable
		if (!input.menuOpen()) {
			overlays.drawMenuButton();
			drawOverlay();
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

	/** The counters, trump, players' panels, then every card, and the deck while it's shuffled. */
	private void drawTable() {
		drawing.tally();
		drawing.bidTracker();
		drawing.trump(toss.trumpGone());
		for (int seat = 0; seat < match().players(); seat++) {
			drawing.seat(seat);
		}
		drawing.layoutHand();
		if (flow.state() == TableState.TRICK) {
			TrickView.drawTrickWon(art, assets, table, match(), flow.stateTime());
		}
		cards.update(this, time);
		cards.draw(batch, assets, art, match(), flow.trumpFaceUp(), time, paint.fade());
		if (flow.isDealing()) {
			flow.deal().drawShuffle(batch, assets, art, table);
		}
	}

	/** Whatever the moment shows over the table: your bid panel, the hand's end, the scores. */
	private void drawOverlay() {
		StateView.of(flow.state()).drawOverlay(this, overlays, recorder.newHighScore());
	}



	// ----------------------------------------------------------- helpers

	@Override
	public String stateTag() {
		return StateView.describe(this);
	}
}
