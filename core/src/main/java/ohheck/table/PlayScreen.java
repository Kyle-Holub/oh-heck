package ohheck.table;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Kick;
import ohheck.anim.Tween;
import ohheck.campaign.Chapter;
import ohheck.campaign.Cosmetic;
import ohheck.campaign.Stage;
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
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.CardWipe;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;

/* The card table screen: a custom game, saved as it goes, or a campaign chapter's table. */
public class PlayScreen extends BaseScreen implements TableScene, TableActions {

	private static final float SIGN_BESIDE = 14, SIGN_TOP = 10, SIGN_H = 44, SIGN_PAD = 18, SIGN_DELAY = 0.3f, SIGN_LENGTH = 2.8f, VERSUS_SHAKE = 9, SET_SHAKE = 7, SET_EDGES = 0.7f, HIT_SHAKE = 5, HIT_FLASH = 0.35f;
	private static final Tween SIGN_IN = Tween.over(0.35f, Ease.OUT), SIGN_OUT = Tween.over(0.35f, Ease.IN);

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
	private final DeckShower shower = new DeckShower();
	private final GameEnd end = new GameEnd();
	private final YourSeat you;

	private float time;
	private final Kick kick = new Kick();
	private boolean slammed;
	private TableState lastState;
	private float setAt = -99, hitAt = -99;
	private boolean hitShown;
	private final String sign;
	private final GameRecorder recorder;
	private final BadgeKeeper badges;
	private final Chapter chapter;
	private boolean prizesWaiting;
	private final Stage stage;
	private Difficulty level;
	private Seating players;

	private final TableInput input = new TableInput(this, this, targets);

	private final List<Card> handOrder = new ArrayList<Card>();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>();

	public static PlayScreen fresh(GameHost game) {
		return new PlayScreen(game, null, null, null);
	}

	public static PlayScreen resume(GameHost game, SavedGame saved) {
		return new PlayScreen(game, saved, null, null);
	}

	public static PlayScreen campaign(GameHost game, Chapter chapter, Stage stage) {
		return new PlayScreen(game, null, chapter, stage);
	}

	private PlayScreen(GameHost game, SavedGame saved, Chapter chapter, Stage stage) {
		super(game);
		this.chapter = chapter;
		this.stage = stage;
		this.sign = chapter == null ? "Custom game \u2013 " + game.settings().mode().label
				: "Chapter " + chapter.number() + " \u2013 " + stage.title;
		this.options = game.options();
		this.streams = TableStreams.of(options.seed);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(paint, this, layout, art, targets);
		this.you = options.autoplayHuman ? new Autoplay(this, input, targets, options) : YourSeat.PERSON;
		this.drawing = new TableDrawing(this, paint, art, game.post(), game.settings()::handSort);
		this.recorder = new GameRecorder(game.records());
		this.badges = new BadgeKeeper(game.records());
		TableFlow.Listener keeper = chapter == null ? recorder : new CampaignKeeper();
		this.flow = new TableFlow(you, new Effects(), new TableSounds(game.audio()), keeper, badges);
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
			boolean won = m.leaders().contains(Seating.YOU);
			if (!won) {
				shower.start(finalScoresSeed(m));
			}
			end.start(won, m.players(), stage != Stage.DUEL, !options.autoplayHuman);
		}
	}

	private final class CampaignKeeper implements TableFlow.Listener {
		@Override
		public void gameEnded(Match m) {
			if (!m.leaders().equals(List.of(Seating.YOU))) {
				return;
			}
			win();
		}
	}

	private void win() {
		prizesWaiting = stage == Stage.DUEL && !game.records().campaign().cleared(chapter);
		game.records().campaign().win(chapter, stage);
		game.records().saveCampaign();
	}

	@Override
	public void skipGameEnd() {
		end.tap(flow.stateTime());
		shower.clear();
	}

	@Override
	public void refuseBid(int b) {
		overlays.refuseBid(b);
	}

	@Override
	public void answerTutorialOffer(boolean take) {
		flow.enter(TableState.PICK_DEALER);
		if (take) {
			game.showTutorial(true);
		} else {
			game.settings().setTutorialSeen(true);
		}
	}

	@Override
	public void forceWin() {
		win();
		showHighScores();
	}

	@Override
	public boolean inCampaign() {
		return chapter != null;
	}

	@Override
	public boolean prizesWaiting() {
		return prizesWaiting;
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
	public float podiumTime() {
		return flow.state() == TableState.GAME_OVER ? end.podiumTime(flow.stateTime()) : -1;
	}

	@Override
	public float sinceResults() {
		return end.sinceResults(flow.stateTime());
	}

	@Override
	public boolean podiumDone() {
		return end.podiumDone(flow.stateTime());
	}

	@Override
	public boolean resultsShown() {
		return flow.state() == TableState.GAME_OVER && end.resultsShown(flow.stateTime());
	}

	@Override
	public TableFlow flow() {
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
		seatPlayers(chapter == null ? Player.draw(level, streams.seating) : chapter.seating(stage));
		shower.clear();
		end.reset();
		RuleSet rules = chapter == null ? game.settings().rules() : chapter.rules(stage);
		Match m = Match.random(players.players(), streams.deck, rules);
		overlays.setMatch(m);
		if (chapter == null) {
			recorder.table(level, players, true);
		}
		badges.fresh();
		flow.start(m);
		slammed = false;
		if (!game.settings().tutorialSeen() && !game.options().autoplayHuman) {
			flow.enter(TableState.OFFER_TUTORIAL);
		} else if (stage == Stage.DUEL) {
			flow.enter(TableState.VERSUS);
		}
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
		if (flow.inHitStop() && !hitShown) {
			kick.shake(time, HIT_SHAKE);
			hitAt = time;
		}
		hitShown = flow.inHitStop();
		if (flow.state() != lastState) {
			lastState = flow.state();
			if (lastState == TableState.HAND_END) {
				if (!match().madeBid(Seating.YOU)) {
					kick.shake(time, SET_SHAKE);
					setAt = time;
				}
			}
		}
		if (flow.state() == TableState.VERSUS && !slammed && flow.stateTime() >= TableOverlays.VERSUS_SLAM) {
			slammed = true;
			kick.shake(time, VERSUS_SHAKE);
		}
		overlays.update(dt);
		sparks.step(dt);
		fireworks.update(dt);
		shower.update(dt, layout.w, layout.h);
		if (end.update(flow.stateTime(), !shower.active())) {
			sparks.burst(layout.w / 2, layout.h / 2 + 100, 160);
			fireworks.show(layout.w, layout.h);
		}
		drawing.updateMotes(dt, flow.trumpFaceUp() && !gameOver());
		you.update(dt);
	}

	@Override
	public boolean animating() {
		if (input.menuOpen()) {
			return false;
		}
		if (flow.state().animating(this)) {
			return true;
		}
		return input.dragging() || sparks.active() || fireworks.active() || cards.moving(time) || shower.active() || kick.active(time) || time - setAt < SET_EDGES || overlays.refusing()
				|| time < signFrom() + SIGN_LENGTH;
	}

	@Override
	public void show() {
		super.show();
		Cosmetic look = duelLook();
		if (look != null) {
			assets.dress(look, look);
		}
		game.audio().startMusic();
	}

	@Override
	public void hide() {
		super.hide();
		if (duelLook() != null) {
			assets.dress(game.settings().cardBack(), game.settings().felt());
		}
	}

	private Cosmetic duelLook() {
		if (stage != Stage.DUEL) {
			return null;
		}
		for (Cosmetic c : Cosmetic.values()) {
			if (c.wonBy == chapter) {
				return c;
			}
		}
		return null;
	}

	@Override
	public String habit(int seat) {
		return seat == Seating.YOU ? "" : players.at(seat).trait;
	}

	@Override
	public String name(int seat) {
		return seat == Seating.YOU ? "You" : players.at(seat).name;
	}

	private void seatPlayers(Seating ids) {
		players = ids;
		layout.seat(ids.players());
		flow.seat(ids, streams.bots);
	}

	private void save() {
		if (chapter != null) {
			return;
		}
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
		if (prizesWaiting) {
			game.showChapterCleared(chapter);
		} else if (chapter != null) {
			game.showCampaign();
		} else {
			game.showHighScores(recorder.newHighScore());
		}
	}

	@Override
	public void newGame() {
		if (chapter != null) {
			game.playCampaign(chapter, stage);
		} else {
			game.newGame();
		}
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
		kickX = kick.dx(time);
		kickY = kick.dy(time);
		kickZoom = kick.zoom(time);
		begin();
		layout.resize(w, h);
		batch.draw(assets.table, 0, 0, w, h);
		drawTable();
		fireworks.draw(batch, assets.glow);
		if (!input.menuOpen()) {
			overlays.drawMenuButton();
			drawOverlay();
		}
		shower.draw(batch, assets, art, layout.w, layout.h);
		sparks.draw(batch, assets.glow);
		if (input.menuOpen()) {
			overlays.drawMenu();
		} else {
			overlays.drawHint();
		}
		drawSetEdges();
		drawSign();
		if (chapter != null) {
			CardWipe.draw(batch, assets.back(), w, h, time);
		}
		batch.end();
	}

	private void drawHitFlash() {
		float t = time - hitAt;
		if (t >= HIT_FLASH || match().lastTrickWinner() < 0) {
			return;
		}
		Vector2 at = layout.trickSlot(match().lastTrickWinner());
		float gw = Assets.CARD_W * 2.4f, gh = Assets.CARD_H * 2f;
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(1f, 0.92f, 0.7f, 0.9f * (1 - t / HIT_FLASH));
		batch.draw(assets.glow, at.x + Assets.CARD_W / 2 - gw / 2, at.y + Assets.CARD_H / 2 - gh / 2, gw, gh);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
	}

	private void drawSetEdges() {
		float t = time - setAt;
		if (t >= SET_EDGES) {
			return;
		}
		paint.edgeGlow(Palette.RED, 0.55f * (1 - t / SET_EDGES), w, h);
	}

	private void drawSign() {
		float t = time - signFrom();
		if (t < 0 || t > SIGN_LENGTH) {
			return;
		}
		float drop = Math.min(SIGN_IN.progress(t), 1 - SIGN_OUT.progress(t - SIGN_LENGTH + SIGN_OUT.end()));
		BitmapFont f = paint.serif(1.3f, Palette.GOLD_LIGHT);
		paint.glyphs.setText(f, sign);
		float sw = paint.glyphs.width + 2 * SIGN_PAD, y = h - drop * (SIGN_H + SIGN_TOP), x = layout.trumpX() - SIGN_BESIDE - sw;
		paint.frame(assets.panelTurn, x, y, sw, SIGN_H);
		f.draw(batch, sign, x + SIGN_PAD, y + SIGN_H / 2 + paint.glyphs.height / 2);
	}

	private float signFrom() {
		return chapter == null ? SIGN_DELAY : stage == Stage.DUEL ? TableOverlays.VERSUS_LENGTH : CardWipe.LENGTH;
	}

	private void drawTable() {
		drawing.tally();
		drawing.bidTracker();
		drawing.trump(gameOver());
		for (int seat = 0; seat < match().players(); seat++) {
			drawing.seat(seat);
		}
		drawing.momentum();
		drawHitFlash();
		drawing.layoutHand();
		if (flow.state() == TableState.TRICK) {
			TrickView.drawTrickWon(art, assets, layout, match(), flow.stateTime());
		}
		cards.update(this, time);
		if (flow.state() == TableState.OFFER_TUTORIAL || flow.state() == TableState.VERSUS
				|| flow.state() == TableState.PICK_DEALER && !DealerPick.landed(flow.stateTime())) {
			return;
		}
		cards.draw(batch, assets, art, match(), flow.trumpFaceUp(), time, paint.fade());
		if (flow.isDealing()) {
			flow.deal().drawDeck(batch, assets, art, layout);
		}
	}

	private void drawOverlay() {
		flow.state().drawOverlay(this, overlays, recorder.newHighScore());
	}

	@Override
	public String stateTag() {
		return TableState.describe(this);
	}

	private boolean gameOver() {
		return flow.state() == TableState.GAME_OVER;
	}

	private static long finalScoresSeed(Match m) {
		long seed = 0;
		for (int s = 0; s < m.players(); s++) {
			seed = seed * 31 + m.score(s);
		}
		return seed;
	}
}
