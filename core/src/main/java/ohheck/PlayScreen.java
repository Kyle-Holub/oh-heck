package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Bot;
import rules.Card;
import rules.Match;
import rules.Suit;

/**
 * The card table: you (bottom) against three computer players (left, top,
 * right). The rules live in {@link Match}; this screen runs the table's
 * {@link TableState} machine, turns touches into moves, runs the computer
 * players and draws it all, with help from:
 * <ul>
 * <li>{@link TableLayout}: where things go
 * <li>{@link DealAnimation} and {@link TrickView}: cards moving
 * <li>{@link TableOverlays}: panels, scores and menus over the table
 * <li>{@link Autoplay}: the scripted screenshot run (not normal play)
 * </ul>
 * Playing a card: tap it, or drag it up onto the table. Bidding: tap a number.
 */
public class PlayScreen extends BaseScreen {

	static final String[] NAMES = { "You", "Tom", "Max", "Dan" };
	private static final int HUMAN = TableLayout.HUMAN;

	// timings, in seconds at normal speed
	private static final float AI_DELAY = 0.8f;
	private static final float HAND_END_SHOW = 2.4f, LABEL_DROP = 0.6f;
	private static final float TRUMP_FLIP_TIME = 0.45f;
	private static final float DRAG_START = 12f;

	private final GameOptions options;
	private final Bot bot = new Bot();
	private final Random rng;

	final TableLayout table = new TableLayout();
	final TableOverlays overlays;
	private final CardArt art;
	private final PlayerPanel panel = new PlayerPanel(this);
	private final DealAnimation deal = new DealAnimation();
	private final TrickView trick = new TrickView();
	private final Sparks sparks = new Sparks();
	private final Motes motes = new Motes();
	/** Drives your seat in scripted screenshot runs; null in normal play. */
	private final Autoplay autoplay;
	private final TextureRegion strokeRegion;

	private Match match;
	private TableState state = TableState.TURN;
	/** Seconds in the current state. */
	private float stateT;
	/** Game time, for animations; stops while paused. */
	private float time;
	/** Seconds since the deal finished: the trump card turns over. */
	private float trumpT;
	private boolean menuOpen;
	/** Where your just-finished game landed in the high score table, or -1. */
	private int newHighScore = -1;

	// touch: the card under your finger, and whether it's being dragged
	private final Vector2 tmp = new Vector2();
	private Card pressed;
	private boolean dragging;
	private float downX, downY, dragX, dragY;

	/** Your hand in display order, and each card's tap area; rebuilt when drawn. */
	final List<Card> handOrder = new ArrayList<Card>();
	final List<Rectangle> handRects = new ArrayList<Rectangle>();

	/** A new game, or the saved game {@code restored} if not null. */
	public PlayScreen(OhHeckGame game, Match restored) {
		super(game);
		this.options = game.options;
		this.rng = options.seed == null ? new Random() : new Random(options.seed);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(this, table, art);
		this.autoplay = options.autoplayHuman ? new Autoplay(this, options) : null;
		this.strokeRegion = new TextureRegion(assets.stroke);
		if (restored == null) {
			newGame();
		} else {
			// carry on where it was left, without replaying the deal
			setMatch(restored);
			trumpT = TRUMP_FLIP_TIME;
			enter(restored.phase() == Match.Phase.HAND_OVER ? TableState.HAND_END : TableState.TURN);
		}
	}

	// ------------------------------------------------------------ the game

	Match match() {
		return match;
	}

	TableState state() {
		return state;
	}

	float stateTime() {
		return stateT;
	}

	boolean isMenuOpen() {
		return menuOpen;
	}

	boolean isDragging() {
		return dragging;
	}

	public boolean isGameOver() {
		return match.phase() == Match.Phase.GAME_OVER;
	}

	private void setMatch(Match m) {
		match = m;
		overlays.setMatch(m);
	}

	private void newGame() {
		setMatch(Match.random(4, rng));
		save();
		newHighScore = -1;
		startHand();
	}

	/** A new hand has been dealt (by the rules): shuffle and deal it on screen. */
	private void startHand() {
		if (autoplay != null) {
			autoplay.skipHands(match);
			if (match.phase() == Match.Phase.GAME_OVER) {
				enter(TableState.GAME_OVER);
				return;
			}
		}
		trick.clear();
		clearTouch();
		trumpT = 0;
		deal.start(match.handSize(), match.dealer(), match.handIndex() == 0);
		enter(TableState.SHUFFLE);
	}

	private void enter(TableState s) {
		state = s;
		stateT = 0;
		switch (s) {
		case HAND_END:
			if (match.madeBid(HUMAN)) {
				sparks.burst(table.centreX(), table.centreY(), 110);
			}
			break;
		case GAME_OVER:
			newHighScore = game.recordHighScore(match.score(HUMAN), overlays.place(HUMAN));
			overlays.playAgainButton.set(0, 0, 0, 0); // not tappable until drawn
			overlays.highScoresButton.set(0, 0, 0, 0);
			if (match.leaders().contains(HUMAN)) {
				sparks.burst(table.w / 2, table.h / 2 + 100, 160);
			}
			break;
		default:
			break;
		}
	}

	private void nextHand() {
		match.nextHand();
		save();
		if (match.phase() == Match.Phase.GAME_OVER) {
			enter(TableState.GAME_OVER);
		} else {
			startHand();
		}
	}

	private void placeBid(int seat, int bid) {
		match.placeBid(seat, bid);
		save();
		enter(TableState.TURN);
	}

	/** Plays a card that starts its flight to the middle from (fromX, fromY). */
	private void playCard(int seat, Card c, float fromX, float fromY) {
		trick.fly(c, fromX, fromY, time);
		int winner = match.playCard(seat, c);
		save();
		game.audio.play(Audio.Effect.PLAY_CARD);
		if (match.phase() == Match.Phase.HAND_OVER) {
			game.audio.play(match.madeBid(HUMAN) ? Audio.Effect.WIN : Audio.Effect.LOSE);
		}
		enter(winner >= 0 ? TableState.TRICK : TableState.TURN);
	}

	private void botMove(int seat) {
		if (match.phase() == Match.Phase.BIDDING) {
			placeBid(seat, bot.chooseBid(match.hand(seat), match.trump(), match.bidsSoFar()));
		} else {
			Card c = bot.choosePlay(match.hand(seat), match.currentTrick(), match.trump(), match.bid(seat),
					match.tricksWon(seat));
			Vector2 from = table.seatAnchor(seat);
			playCard(seat, c, from.x - Assets.CARD_W / 2f, from.y - Assets.CARD_H / 2f);
		}
	}

	/** Saves the game so it survives Android closing the app; a finished game is forgotten. */
	private void save() {
		if (options.persist) {
			game.settings.saveGame(match.phase() == Match.Phase.GAME_OVER ? null : match.save());
		}
	}

	private boolean isDealing() {
		return state == TableState.SHUFFLE || state == TableState.DEAL;
	}

	private boolean humanCanPlay() {
		return !menuOpen && state == TableState.TURN && match.phase() == Match.Phase.PLAYING
				&& match.currentSeat() == HUMAN;
	}

	private boolean trumpFaceUp() {
		return !isDealing() && trumpT >= TRUMP_FLIP_TIME;
	}

	// ----------------------------------------------------------- update

	@Override
	public void render(float delta) {
		update(Math.min(delta, 0.1f) * options.speed * game.settings.speedFactor());
		draw();
	}

	private void update(float dt) {
		if (menuOpen) {
			if (autoplay != null) {
				autoplay.whilePaused(dt);
			}
			return;
		}
		time += dt;
		stateT += dt;
		if (!isDealing()) {
			trumpT += dt;
		}
		overlays.update(dt);
		sparks.update(dt);
		motes.update(dt, trumpFaceUp(), table.trumpX(), table.trumpY(), table.trumpW(), table.trumpH());

		switch (state) {
		case SHUFFLE:
		case DEAL:
			if (deal.update(dt)) {
				game.audio.play(Audio.Effect.SHUFFLE); // a riffle begins
			}
			if (deal.isDone()) {
				enter(TableState.TURN);
			} else if (state == TableState.SHUFFLE && !deal.isShuffling()) {
				enter(TableState.DEAL);
			}
			break;
		case TURN:
			if (match.currentSeat() != HUMAN) {
				if (stateT >= AI_DELAY) {
					botMove(match.currentSeat());
				}
			} else if (autoplay != null) {
				autoplay.yourTurn(dt);
			}
			break;
		case TRICK:
			if (stateT >= TrickView.showLength(match.lastTrickWinner() == HUMAN)) {
				trick.clear();
				enter(match.phase() == Match.Phase.HAND_OVER ? TableState.HAND_END : TableState.TURN);
			}
			break;
		case HAND_END:
			if (stateT >= HAND_END_SHOW) {
				enter(TableState.RESULTS);
			}
			break;
		case RESULTS:
			if (autoplay != null) {
				autoplay.onResults(stateT);
			}
			break;
		case GAME_OVER:
			break;
		}
	}

	@Override
	boolean animating() {
		if (menuOpen) {
			return false;
		}
		switch (state) {
		case SHUFFLE:
		case DEAL:
		case TRICK:
		case HAND_END:
			return true;
		case RESULTS:
			if (stateT < LABEL_DROP) {
				return true;
			}
			break;
		case TURN:
			if (trumpT < TRUMP_FLIP_TIME) {
				return true;
			}
			break;
		default:
			break;
		}
		return dragging || sparks.active() || trick.anyInFlight(time);
	}

	// ------------------------------------------------------------ input

	@Override
	public void show() {
		game.audio.startMusic();
		Gdx.input.setCatchKey(Input.Keys.BACK, true);
		Gdx.input.setInputProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int x, int y, int pointer, int button) {
				if (pointer > 0) {
					return false;
				}
				toWorld(x, y);
				onDown(tmp.x, tmp.y);
				return true;
			}

			@Override
			public boolean touchDragged(int x, int y, int pointer) {
				if (pointer > 0) {
					return false;
				}
				toWorld(x, y);
				onDrag(tmp.x, tmp.y);
				return true;
			}

			@Override
			public boolean touchUp(int x, int y, int pointer, int button) {
				if (pointer > 0) {
					return false;
				}
				toWorld(x, y);
				onUp(tmp.x, tmp.y);
				return true;
			}

			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
					back();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void hide() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void pause() {
		save();
		// leaving the app (home button, a call): come back to the menu
		if (autoplay == null) {
			menuOpen = true;
			clearTouch();
		}
	}

	private void toWorld(int screenX, int screenY) {
		tmp.set(screenX, screenY);
		viewport.unproject(tmp);
	}

	private void onDown(float x, float y) {
		downX = x;
		downY = y;
		pressed = null;
		dragging = false;
		if (!humanCanPlay()) {
			return;
		}
		int i = cardAt(x, y);
		if (i >= 0) {
			pressed = handOrder.get(i);
			dragX = x;
			dragY = y;
		}
	}

	private void onDrag(float x, float y) {
		if (pressed == null) {
			return;
		}
		if (!dragging && Vector2.dst(x, y, downX, downY) > DRAG_START && match.legalPlays(HUMAN).contains(pressed)) {
			dragging = true;
		}
		dragX = x;
		dragY = y;
	}

	private void onUp(float x, float y) {
		if (dragging) {
			Card c = pressed;
			clearTouch();
			float handTop = handRects.isEmpty() ? 150 : handRects.get(0).y + handRects.get(0).height;
			if (y > handTop + 10 && humanCanPlay()) {
				// dropped on the table: play it from where it was let go
				playCard(HUMAN, c, x - TableLayout.handCardW() / 2, y - TableLayout.handCardH() / 2);
			}
			return; // otherwise it drops back into your hand
		}
		pressed = null;
		if (Vector2.dst(x, y, downX, downY) > DRAG_START * 2) {
			return; // a swipe, not a tap
		}
		tap(x, y);
	}

	/** A tap at world position (x, y). Autoplay taps come through here too. */
	@Override
	protected void tap(float x, float y) {
		if (menuOpen) {
			for (int i = 0; i < overlays.menuItems.size(); i++) {
				if (overlays.menuItems.get(i).contains(x, y)) {
					menuChoice(i);
					return;
				}
			}
			return;
		}
		if (overlays.menuButton.contains(x, y)) {
			openMenu();
			return;
		}
		switch (state) {
		case SHUFFLE:
		case DEAL:
			enter(TableState.TURN); // skip the shuffle and the deal
			break;
		case TURN:
			if (match.currentSeat() == HUMAN) {
				if (match.phase() == Match.Phase.BIDDING) {
					tapBid(x, y);
				} else {
					tapCard(x, y);
				}
			}
			break;
		case TRICK:
			break;
		case HAND_END:
			enter(TableState.RESULTS);
			break;
		case RESULTS:
			nextHand();
			break;
		case GAME_OVER:
			// only the buttons: a stray tap mustn't throw away the final scores
			if (overlays.highScoresButton.contains(x, y)) {
				game.showHighScores(newHighScore);
			} else if (overlays.playAgainButton.contains(x, y)) {
				newGame();
			}
			break;
		}
	}

	private void tapBid(float x, float y) {
		for (int b = 0; b < overlays.bidButtons.size(); b++) {
			if (overlays.bidButtons.get(b).contains(x, y)) {
				if (match.isLegalBid(b)) {
					placeBid(HUMAN, b);
				} else {
					overlays.hint("As dealer you can't bid " + b + ": the bids would add up to " + match.handSize());
				}
				return;
			}
		}
	}

	private void tapCard(float x, float y) {
		int i = cardAt(x, y);
		if (i < 0) {
			return;
		}
		Card c = handOrder.get(i);
		if (!match.legalPlays(HUMAN).contains(c)) {
			overlays.hint("You must follow suit: play a " + singular(match.leadSuit()));
			return;
		}
		Rectangle r = handRects.get(i);
		clearTouch();
		playCard(HUMAN, c, r.x, r.y);
	}

	@Override
	protected void back() {
		if (menuOpen) {
			menuOpen = false;
		} else {
			openMenu();
		}
	}

	private void openMenu() {
		menuOpen = true;
		clearTouch();
	}

	private void menuChoice(int i) {
		if (i == 1) {
			game.showOptions(true); // the menu is still open when you come back
			return;
		}
		menuOpen = false;
		if (i == 2) {
			newGame();
		} else if (i == 3) {
			game.showTitle();
		}
	}

	private int cardAt(float x, float y) {
		// topmost first: later cards are drawn over earlier ones
		for (int i = handRects.size() - 1; i >= 0; i--) {
			if (handRects.get(i).contains(x, y)) {
				return i;
			}
		}
		return -1;
	}

	private void clearTouch() {
		pressed = null;
		dragging = false;
	}

	// ------------------------------------------------------------- draw

	private void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		table.resize(w, h);
		batch.draw(assets.table, 0, 0, w, h);

		drawTally();
		drawTrump();
		for (int seat = 0; seat < 4; seat++) {
			drawSeat(seat);
		}
		drawHand();
		trick.draw(batch, assets, art, table, match, state == TableState.TRICK, stateT, time, fade);
		if (isDealing()) {
			deal.draw(batch, assets, art, table);
		}
		if (!menuOpen) {
			overlays.drawMenuButton();
			switch (state) {
			case TURN:
				if (match.phase() == Match.Phase.BIDDING && match.currentSeat() == HUMAN) {
					overlays.drawBidPanel();
				}
				break;
			case HAND_END:
				overlays.drawHandEnd(stateT);
				break;
			case RESULTS:
				overlays.drawHandResults();
				overlays.drawHandLabels(MathUtils.clamp(stateT / LABEL_DROP, 0, 1), 1f);
				break;
			case GAME_OVER:
				overlays.drawGameOver(newHighScore);
				break;
			default:
				break;
			}
		}
		if (dragging) {
			batch.draw(assets.face(pressed), dragX - TableLayout.handCardW() / 2, dragY - TableLayout.handCardH() / 2,
					TableLayout.handCardW(), TableLayout.handCardH());
		}
		sparks.draw(batch, assets.glow);
		if (menuOpen) {
			overlays.drawMenu();
		} else {
			overlays.drawHint();
		}
		batch.end();
	}

	// ---- the hand counter: tally marks, a gate of five then three

	private static final float TALLY_H = 36f, TALLY_W = 6f, TALLY_STEP = 12f;
	/** Small fixed leans so the strokes look hand drawn. */
	private static final float[] TALLY_LEAN = { 3f, -2f, 4f, -1f, 0f, 2f, -3f, 1f };

	private void drawTally() {
		int done = match.phase() == Match.Phase.GAME_OVER ? 8 : match.handIndex();
		float left = 22, top = h - 12, pulse = 0.5f + 0.5f * MathUtils.sin(time * 3f);
		for (int i = 0; i < 8; i++) {
			float cx, cy = top - TALLY_H / 2, len = TALLY_H, rot;
			if (i == 4) { // the bar across the first four
				cx = left + 1.5f * TALLY_STEP;
				len = 4 * TALLY_STEP + 8;
				rot = -52; // leaning up to the right
			} else {
				int col = i < 4 ? i : i - 1;
				cx = left + col * TALLY_STEP + (i > 4 ? 16 : 0);
				rot = TALLY_LEAN[i];
			}
			if (i == done) { // the hand being played glows
				stroke(cx, cy, TALLY_W + 6, len + 6, rot, Palette.GOLD_LIGHT, 0.18f + 0.22f * pulse);
				stroke(cx, cy, TALLY_W, len, rot, Palette.GOLD_LIGHT, 1f);
			} else if (i < done) {
				stroke(cx, cy, TALLY_W, len, rot, GOLD, 1f);
			} else {
				stroke(cx, cy, TALLY_W, len, rot, Palette.MUTED, 0.28f);
			}
		}
	}

	/** A soft-edged bar centred on (cx, cy), rotated by rot degrees. */
	private void stroke(float cx, float cy, float sw, float sh, float rot, Color c, float alpha) {
		batch.setColor(c.r, c.g, c.b, alpha);
		batch.draw(strokeRegion, cx - sw / 2, cy - sh / 2, sw / 2, sh / 2, sw, sh, 1f, 1f, rot);
		batch.setColor(Color.WHITE);
	}

	private void drawTrump() {
		if (state == TableState.SHUFFLE) {
			return; // the trump card is still in the deck
		}
		float x = table.trumpX(), y = table.trumpY(), cw = table.trumpW(), ch = table.trumpH();
		art.shadow(x, y, cw, ch, 4);
		float p = isDealing() ? 0 : MathUtils.clamp(trumpT / TRUMP_FLIP_TIME, 0, 1);
		art.flip(match.trumpCard(), x, y, cw, ch, p);
		motes.draw(batch, assets.glow);
	}

	/** A seat's panel and, for opponents, their face-down cards (while dealing, only those that have landed). */
	private void drawSeat(int seat) {
		boolean turn = state == TableState.TURN && match.currentSeat() == seat;
		panel.draw(NAMES[seat], table.panelX(seat), table.panelTop(seat), turn, match.dealer() == seat,
				match.score(seat), match.bid(seat), match.tricksWon(seat), isDealing());
		if (seat == HUMAN) {
			return;
		}
		int total = match.hand(seat).size();
		int n = isDealing() ? deal.dealtTo(seat) : total;
		float y = table.fanOrigin(seat).y;
		for (int i = 0; i < n; i++) {
			batch.draw(assets.back(), table.fanCardX(seat, i, total), y, TableLayout.backW(), TableLayout.backH());
		}
	}

	private void drawHand() {
		handOrder.clear();
		handOrder.addAll(match.hand(HUMAN));
		Collections.sort(handOrder, DISPLAY_ORDER);
		int n = handOrder.size();
		sizeTo(handRects, n);
		if (n == 0) {
			return;
		}
		int shown = isDealing() ? deal.dealtTo(HUMAN) : n;
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		table.layoutHand(n);
		boolean myTurn = humanCanPlay();
		List<Card> legal = myTurn ? match.legalPlays(HUMAN) : Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			Card c = handOrder.get(i);
			boolean canPlay = legal.contains(c);
			float x = table.handStart + i * table.handGap;
			float y = TableLayout.HAND_Y + (canPlay ? TableLayout.LIFT : 0);
			handRects.get(i).set(x, y, i == n - 1 ? cw : table.handGap, ch);
			if (i >= shown || (dragging && c == pressed)) {
				continue;
			}
			art.shadow(x, y, cw, ch, y - TableLayout.HAND_Y + 5);
			batch.setColor(myTurn && !canPlay ? Palette.CARD_DIM : Color.WHITE);
			art.flip(c, x, y, cw, ch, isDealing() ? deal.flipOfYourCard(i) : 1f);
		}
		batch.setColor(Color.WHITE);
	}

	// ----------------------------------------------------------- helpers

	/** "heart", "spade"... */
	private static String singular(Suit s) {
		String n = s.name().toLowerCase();
		return n.substring(0, n.length() - 1);
	}

	/** Your hand sorted by suit (alternating colours), then rank. */
	private static final Comparator<Card> DISPLAY_ORDER = new Comparator<Card>() {
		private final Suit[] order = { Suit.CLUBS, Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS };

		private int suitIndex(Suit s) {
			for (int i = 0; i < order.length; i++) {
				if (order[i] == s) {
					return i;
				}
			}
			return 0;
		}

		@Override
		public int compare(Card a, Card b) {
			int d = suitIndex(a.suit()) - suitIndex(b.suit());
			return d != 0 ? d : a.rank().strength() - b.rank().strength();
		}
	};

	@Override
	public String stateTag() {
		return Autoplay.describe(this);
	}
}
