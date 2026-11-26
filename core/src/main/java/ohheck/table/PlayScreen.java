package ohheck.table;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
import ohheck.platform.GameOptions;
import ohheck.players.Difficulty;
import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.records.SavedGame;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;
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
	private final PracticeGuide guide;
	private boolean firstPractice, finaleDone;
	private float finaleAt = -1;
	private static final float FINALE_GATHER = 0.5f, FINALE_SHAKE = 1.3f, FINALE_FLASH = 1.45f, FINALE_FLIP = 0.4f, FINALE_LENGTH = 2.9f,
			FINALE_W = 150, FINALE_H = FINALE_W * 98 / 73;
	private static final String CHAPTER = "CHAPTER";
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
		this.flow = practice ? new TableFlow(you, new Effects(), new TableSounds(game.audio()))
				: new TableFlow(you, new Effects(), new TableSounds(game.audio()), keeper, badges);
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
		if (finaleAt >= 0) {
			float before = time - dt - finaleAt;
			if (before < FINALE_FLASH + FINALE_FLIP / 2 && time - finaleAt >= FINALE_FLASH + FINALE_FLIP / 2) {
				sparks.burst(w / 2, h / 2, 120);
			}
			if (time - finaleAt >= FINALE_LENGTH && !finaleDone) {
				finaleDone = true;
				game.finishPractice();
			}
		} else if (guide == null || !guide.update(this)) {
			flow.update(dt);
		}
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
		return input.dragging() || sparks.active() || fireworks.active() || cards.moving(time) || shower.active() || kick.active(time) || time - setAt < SET_EDGES || overlays.refusing() || finaleAt >= 0
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
		if (guide != null && (!flow.trumpFaceUp() || guide.update(this) || !guide.allows(match(), new Move.Bid(Seating.YOU, b)))) {
			return true;
		}
		return flow.bid(b);
	}

	@Override
	public boolean finishing() {
		return finaleAt >= 0;
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
			if (finaleAt < 0) {
				finaleAt = time;
			}
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
		if (finaleAt >= 0) {
			drawFinale(time - finaleAt);
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
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(1f, 0.92f, 0.7f, 0.9f * (1 - t / HIT_FLASH));
		batch.draw(assets.glow, at.x + Assets.CARD_W / 2 - gw / 2, at.y + Assets.CARD_H / 2 - gh / 2, gw, gh);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
	}

	private void drawFinale(float t) {
		paint.fade(Math.min(1, t * 3));
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		paint.fade(1f);
		float cx = w / 2, cy = h / 2;
		if (t < FINALE_GATHER) {
			float k = Ease.IN.apply(t / FINALE_GATHER);
			for (int seat = 1; seat < match().players(); seat++) {
				float sx = layout.panelX(seat) + PlayerPanel.W / 2, sy = layout.panelTop(seat) - PlayerPanel.H / 2;
				float x = MathUtils.lerp(sx, cx, k), y = MathUtils.lerp(sy, cy, k), cw = MathUtils.lerp(FINALE_W * 0.6f, FINALE_W, k);
				float ch = cw * 98 / 73;
				batch.draw(assets.back(), x - cw / 2, y - ch / 2, cw / 2, ch / 2, cw, ch, 1, 1, (1 - k) * (seat * 20 - 40));
			}
			return;
		}
		float shake = t < FINALE_SHAKE ? (t - FINALE_GATHER) / (FINALE_SHAKE - FINALE_GATHER) : 0;
		float flip = MathUtils.clamp((t - FINALE_FLASH) / FINALE_FLIP, 0, 1), sx = Math.abs(MathUtils.cos(flip * MathUtils.PI));
		float glowA = t < FINALE_FLASH ? 0.3f + 0.6f * shake : 0.6f + 0.2f * MathUtils.sin(t * 3);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(1f, 0.82f, 0.45f, glowA);
		batch.draw(assets.glow, cx - FINALE_W * 1.3f, cy - FINALE_H, FINALE_W * 2.6f, FINALE_H * 2);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.setColor(Color.WHITE);
		float wobble = MathUtils.sin(t * 40) * 6 * shake, cw = FINALE_W * Math.max(0.03f, sx);
		if (flip < 0.5f) {
			batch.draw(assets.back(), cx - cw / 2, cy - FINALE_H / 2, cw / 2, FINALE_H / 2, cw, FINALE_H, 1, 1, wobble);
		} else {
			batch.draw(assets.campaignFace, cx - cw / 2, cy - FINALE_H / 2, cw, FINALE_H);
			if (flip >= 1) {
				paint.text(paint.ui(0.9f, Palette.GOLD), CHAPTER, cx, cy + FINALE_H / 2 - 18, true);
				paint.numeral(1, cx, cy + FINALE_H * 0.2f, 48, Palette.GOLD_LIGHT);
				float e = 50;
				batch.draw(assets.emblem(Chapter.ONE.emblem), cx - e / 2, cy - e / 2 - 14, e, e);
				paint.text(paint.serif(1.4f, Palette.GOLD_LIGHT), Chapter.ONE.mode.label, cx, cy - FINALE_H / 2 + 46, true);
			}
		}
		float flash = t - FINALE_FLASH;
		if (flash >= 0 && flash < 0.2f) {
			paint.fade(0.35f * (1 - flash / 0.2f));
			paint.rect(0, 0, w, h, Palette.CREAM);
			paint.fade(1f);
		}
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
		return t != null ? "tip:" + t.id() : TableState.describe(this);
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
