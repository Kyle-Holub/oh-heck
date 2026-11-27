package ohheck.table;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.anim.Ease;
import ohheck.anim.Kick;
import ohheck.anim.Tween;
import ohheck.campaign.Chapter;
import ohheck.campaign.Cosmetic;
import ohheck.campaign.PracticeCoach;
import ohheck.campaign.PracticeRound;
import ohheck.campaign.Stage;
import ohheck.fx.Fireworks;
import ohheck.fx.Sparks;
import ohheck.platform.Audio;
import ohheck.platform.GameOptions;
import ohheck.players.Difficulty;
import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.records.SavedGame;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.CardWipe;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;
import ohheck.ui.Wording;

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
	private final TableSounds sounds;
	private static final float TENSION_DIM = 0.3f, TENSION_EASE = 0.4f;
	private final Color tensionShade = new Color();
	private float tension, bestFrom = -1;
	private static final int BEST_SPARKS = 70;
	private Card refused;
	private float refusedAt = -99;
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
	private final PracticeGuide guide;
	private boolean firstPractice;
	private static final String PRACTICE_SIGN = "Practice round";
	private static final String[] PRACTICE_NAMES = { "You", "Left", "Across", "Right" };
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
		return new PlayScreen(game, null, null, null, false);
	}

	public static PlayScreen practice(GameHost game, boolean firstTime) {
		PlayScreen p = new PlayScreen(game, null, null, null, true);
		p.firstPractice = firstTime;
		return p;
	}

	public static PlayScreen resume(GameHost game, SavedGame saved) {
		return new PlayScreen(game, saved, null, null, false);
	}

	public static PlayScreen campaign(GameHost game, Chapter chapter, Stage stage) {
		return new PlayScreen(game, null, chapter, stage, false);
	}

	private PlayScreen(GameHost game, SavedGame saved, Chapter chapter, Stage stage, boolean practice) {
		super(game);
		this.chapter = chapter;
		this.stage = stage;
		this.guide = practice ? new PracticeGuide() : null;
		this.sign = practice ? PRACTICE_SIGN
				: chapter == null ? "Custom game \u2013 " + game.settings().mode().label : "Chapter " + chapter.number() + " \u2013 " + stage.title;
		this.options = game.options();
		this.streams = TableStreams.of(options.seed);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(paint, this, layout, art, targets);
		this.you = options.autoplayHuman ? new Autoplay(this, input, targets, options) : YourSeat.PERSON;
		this.drawing = new TableDrawing(this, paint, art, game.post(), game.settings()::handSort);
		this.recorder = new GameRecorder(game.records());
		this.badges = new BadgeKeeper(game.records());
		TableFlow.Listener keeper = practice ? new TableFlow.Listener() {
		} : chapter == null ? recorder : new CampaignKeeper();
		this.sounds = new TableSounds(game.audio());
		this.flow = practice ? new TableFlow(you, new Effects(), sounds) : new TableFlow(you, new Effects(), sounds, keeper, badges);
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
			if (m.suddenDeath()) {
				hint(Wording.SUDDEN_DEATH);
			}
		}

		@Override
		public void changed(Match m) {
			PlayScreen.this.save();
			List<Move> moves = m.moves();
			Move last = moves.isEmpty() ? null : moves.get(moves.size() - 1);
			if (last instanceof Move.PickTrump pick) {
				hint(Wording.pickedTrump(pick.seat() == Seating.YOU ? null : name(pick.seat()), pick.suit()));
			} else if (last instanceof Move.SwapWidow swap && swap.seat() != Seating.YOU) {
				hint(Wording.widowSwap(name(swap.seat()), swap.give() != null));
			}
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
			end.start(won, m.players(), stage != Stage.DUEL && guide == null, !options.autoplayHuman);
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
		game.audio().play(Audio.Effect.TANKARD);
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
		seatPlayers(guide != null ? PracticeRound.RIVALS : chapter == null ? Player.draw(level, streams.seating) : chapter.seating(stage));
		shower.clear();
		end.reset();
		RuleSet rules = chapter == null ? game.settings().rules() : chapter.rules(stage);
		Match m = guide != null ? PracticeRound.match() : Match.random(players.players(), streams.deck, rules);
		overlays.setMatch(m);
		if (guide != null) {
			flow.useBots(PracticeRound.bots());
		} else if (chapter == null) {
			recorder.table(level, players, true);
		}
		badges.fresh();
		flow.start(m);
		slammed = false;
		if (guide != null) {
			return;
		}
		if (stage == Stage.DUEL) {
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
		if (finishing()) {
			if (guide.finaleFlips(time - dt, time)) {
				sparks.burst(w / 2, h / 2, 120);
			}
			if (guide.finaleEnds(time)) {
				game.finishPractice();
			}
		} else if (guide == null || !guide.update(this)) {
			flow.update(dt);
		}
		sounds.update(flow, time);
		if (flow.state() == TableState.GAME_OVER && recorder.newHighScore() == 0) {
			float since = sinceResults(), locks = overlays.bestLocksAt();
			if (since >= locks && bestFrom < locks) {
				sparks.burst(overlays.bestSpot().x, overlays.bestSpot().y, BEST_SPARKS);
				game.audio().play(Audio.Effect.COIN);
			}
			bestFrom = since;
		}
		tension = MathUtils.clamp(tension + (flow.bidHangsOnLastTrick() ? dt : -dt) / TENSION_EASE, 0, 1);
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
		if (input.menuOpen() || tip() != null) {
			return false;
		}
		if (flow.state().animating(this)) {
			return true;
		}
		return input.dragging() || sparks.active() || fireworks.active() || cards.moving(time) || shower.active() || kick.active(time) || time - setAt < SET_EDGES || overlays.refusing() || finishing()
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
		sounds.stop();
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
		if (guide != null) {
			return PRACTICE_NAMES[seat];
		}
		return seat == Seating.YOU ? "You" : players.at(seat).name;
	}

	private void seatPlayers(Seating ids) {
		players = ids;
		layout.seat(ids.players());
		flow.seat(ids, streams.bots);
	}

	private void save() {
		if (chapter != null || guide != null) {
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
		input.touchDragged(x, y);
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
		if (guide != null && (!flow.trumpFaceUp() || guide.update(this) || !guide.allows(match(), new Move.Bid(Seating.YOU, b)))) {
			return true;
		}
		return flow.bid(b);
	}

	@Override
	public boolean swapWidow(Card give, Card take) {
		input.clearTouch();
		return flow.swapWidow(give, take);
	}

	@Override
	public void refuseCard(Card c) {
		refused = c;
		refusedAt = time;
		game.audio().play(Audio.Effect.THUP);
		game.audio().buzz(Audio.Buzz.DOUBLE);
	}

	@Override
	public void liftWidow() {
		game.audio().play(Audio.Effect.PAPER);
	}

	@Override
	public void touchedCard() {
		game.audio().buzz(Audio.Buzz.TICK);
	}

	@Override
	public Card refusedCard() {
		return refused;
	}

	@Override
	public float refusedFor() {
		return time - refusedAt;
	}

	@Override
	public int reviewing() {
		return targets.review();
	}

	public void reviewTricks(int which) {
		targets.review(which);
	}

	@Override
	public Card hoveredCard() {
		return input.hovered();
	}

	@Override
	public boolean pickTrump(Suit s) {
		return flow.pickTrump(s);
	}

	@Override
	public boolean finishing() {
		return guide != null && guide.finishing();
	}

	@Override
	public boolean inPractice() {
		return guide != null;
	}

	@Override
	public String placardName(int seat) {
		return guide != null && seat != Seating.YOU ? "" : name(seat);
	}

	@Override
	public boolean playable(Card c) {
		return match().legalPlays(Seating.YOU).contains(c) && (guide == null || guide.allows(match(), new Move.Play(Seating.YOU, c)));
	}

	@Override
	public boolean bidAllowed(int b) {
		return guide == null || guide.allows(match(), new Move.Bid(Seating.YOU, b));
	}

	@Override
	public PracticeCoach.Tip tip() {
		return guide == null ? null : guide.tip();
	}

	@Override
	public void dismissTip() {
		if (guide != null) {
			guide.dismiss();
		}
	}

	@Override
	public void play(Card c) {
		input.clearTouch();
		if (guide != null && (guide.update(this) || !playable(c))) {
			return;
		}
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
		if (guide != null && firstPractice) {
			guide.startFinale(time);
		} else if (guide != null) {
			game.back();
		} else if (prizesWaiting) {
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
		if (tension > 0) {
			paint.rect(0, 0, w, h, tensionShade.set(0, 0, 0, TENSION_DIM * tension));
		}
		drawTable();
		fireworks.draw(batch, assets.glow);
		if (!input.menuOpen()) {
			overlays.drawMenuButton();
			drawOverlay();
			if (guide != null && guide.tip() != null) {
				overlays.drawTip(guide.tip());
			}
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
		if (finishing()) {
			guide.drawFinale(paint, layout, match(), time, w, h);
		}
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
		paint.glow(at.x + Assets.CARD_W / 2 - gw / 2, at.y + Assets.CARD_H / 2 - gh / 2, gw, gh, Palette.LAMP, 0.9f * (1 - t / HIT_FLASH));
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
		drawing.widow();
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
		if (flow.state() == TableState.VERSUS
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
		PracticeCoach.Tip t = tip();
		return t != null ? "tip:" + t.id() : guide != null ? "practice:" + TableState.describe(this) : TableState.describe(this);
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
