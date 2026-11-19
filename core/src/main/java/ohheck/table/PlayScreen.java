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

/* The card table screen. */
public class PlayScreen extends BaseScreen implements TableScene, TableActions {

	private final GameOptions options;
	private final TableStreams streams;

	private final TableLayout layout = new TableLayout();
	private final TableOverlays overlays;
	private final OverlayTargets targets = new OverlayTargets();
	private final TableFlow flow;
	private final CardArt art;
	private final TableDrawing drawing;
	private final CardSprites cards = new CardSprites();
	private final Sparks sparks = new Sparks();
	private final Fireworks fireworks = new Fireworks();
	private final TableToss toss;
	private final GameEnd end = new GameEnd();
	private final YourSeat you;

	private float time;
	private final GameRecorder recorder;
	private Difficulty level;
	private Seating players;

	private final TableInput input = new TableInput(this, this, targets);

	private final List<Card> handOrder = new ArrayList<Card>();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>();

	public static PlayScreen fresh(GameHost game) {
		return new PlayScreen(game, null);
	}

	public static PlayScreen resume(GameHost game, SavedGame saved) {
		return new PlayScreen(game, saved);
	}

	private PlayScreen(GameHost game, SavedGame saved) {
		super(game);
		this.options = game.options();
		this.streams = TableStreams.of(options.seed);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(paint, this, layout, art, targets);
		this.you = options.autoplayHuman ? new Autoplay(this, input, targets, options) : YourSeat.PERSON;
		this.drawing = new TableDrawing(this, paint, art, game.post());
		this.toss = new TableToss(assets, layout);
		this.recorder = new GameRecorder(game.records());
		this.flow = new TableFlow(you, new Effects(), new TableSounds(game.audio()), recorder);
		if (!you.thinkAtOnce() && options.fixedStep <= 0) {
			flow.thinkWith(Thinker.background());
		}
		if (saved == null) {
			startGame();
		} else {
			resumeGame(saved);
		}
	}

	private final class Effects implements TableFlow.Listener {
		@Override
		public void beforeDeal(Match m) {
			input.clearTouch();
		}

		@Override
		public void changed(Match m) {
			PlayScreen.this.save();
		}

		@Override
		public void handEnded(Match m) {
			if (m.madeBid(Seating.YOU)) {
				sparks.burst(layout.centreX(), layout.centreY(), 110);
			}
		}

		@Override
		public void gameEnded(Match m) {
			targets.clearGameOverButtons();
			toss.toss(m);
			end.start(m.leaders().contains(Seating.YOU));
		}
	}

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
		return layout;
	}

	@Override
	public List<Card> handOrder() {
		return handOrder;
	}

	@Override
	public List<Rectangle> handRects() {
		return handRects;
	}

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

	@Override
	protected void update(float step) {
		advance(step * options.speed * game.settings().speedFactor());
	}

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
		if (end.update(flow.stateTime(), toss.allGone())) {
			sparks.burst(layout.w / 2, layout.h / 2 + 100, 160);
			fireworks.show(layout.w, layout.h);
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

	@Override
	public void show() {
		super.show();
		game.audio().startMusic();
	}

	@Override
	public String name(int seat) {
		return seat == Seating.YOU ? "You" : players.at(seat).name;
	}

	private void seatPlayers(Seating ids) {
		players = ids;
		flow.seat(ids, streams.bots);
	}

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
			input.openMenu();
		}
	}

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
			super.touchUp(x, y);
		}
	}

	@Override
	protected void tap(float x, float y) {
		input.tap(x, y);
	}

	@Override
	protected void back() {
		input.back();
	}

	@Override
	public boolean bid(int b) {
		return flow.bid(b);
	}

	@Override
	public void play(Card c) {
		input.clearTouch();
		flow.play(c);
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

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		layout.resize(w, h);
		batch.draw(assets.table, 0, 0, w, h);
		drawTable();
		fireworks.draw(batch, assets.glow);
		if (!input.menuOpen()) {
			overlays.drawMenuButton();
			drawOverlay();
		}
		toss.draw(batch);
		sparks.draw(batch, assets.glow);
		if (input.menuOpen()) {
			overlays.drawMenu();
		} else {
			overlays.drawHint();
		}
		batch.end();
	}

	private void drawTable() {
		drawing.tally();
		drawing.bidTracker();
		drawing.trump(toss.trumpGone());
		for (int seat = 0; seat < match().players(); seat++) {
			drawing.seat(seat);
		}
		drawing.layoutHand();
		if (flow.state() == TableState.TRICK) {
			TrickView.drawTrickWon(art, assets, layout, match(), flow.stateTime());
		}
		cards.update(this, time);
		cards.draw(batch, assets, art, match(), flow.trumpFaceUp(), time, paint.fade());
		if (flow.isDealing()) {
			flow.deal().drawShuffle(batch, assets, art, layout);
		}
	}

	private void drawOverlay() {
		StateView.of(flow.state()).drawOverlay(this, overlays, recorder.newHighScore());
	}

	@Override
	public String stateTag() {
		return StateView.describe(this);
	}
}
