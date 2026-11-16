package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Bot;
import rules.Card;
import rules.Match;
import rules.Suit;

/**
 * The card table: one human (bottom) against three computer players (left,
 * top, right). All rules come from {@link Match}; this class draws the state,
 * animates it, runs the computer players on timers and turns touches into
 * moves.
 * <p>
 * Playing a card: tap it, or drag it up onto the table. Bidding: tap a number.
 */
public class PlayScreen extends BaseScreen {

	private static final int HUMAN = 0;
	private static final String[] NAMES = { "You", "Tom", "Max", "Dan" };

	private TextureRegion whiteRegion;
	private static final float HAND_SCALE = 1.2f, BACK_SCALE = 0.55f, TRUMP_SCALE = 0.95f;

	// timings, in seconds at normal speed
	private static final float AI_DELAY = 0.8f;
	private static final float TRICK_PAUSE = 1.7f, COLLECT_TIME = 0.45f, TRICK_WON_SHOW = 0.9f;
	private static final float HAND_END_SHOW = 2.4f;
	private static final float PLAY_FLIGHT = 0.25f;
	private static final float DEAL_INTERVAL = 0.09f, DEAL_FLIGHT = 0.4f;
	private static final float HINT_TIME = 2.2f;
	private static final float AUTOPLAY_DELAY = 1.2f, HAND_OVER_AUTO = 2.2f;

	// touch
	private static final float DRAG_START = 12f, LIFT = 22f, SELECTED_LIFT = 44f;


	private final GameOptions options;
	private final PlayerPanel panel = new PlayerPanel(this);
	private final Vector2 tmp = new Vector2();
	private final Bot bot = new Bot();
	private final Random rng;

	private Match match;
	private float time; // game time, for animations
	private float timer; // computer turn / autoplay timer
	private float trickPause, trickTotal;
	private boolean humanWonTrick;
	private float handOverT; // time since the hand ended: first "Bid Tricks!"/"Set!", then the table
	private String hint = "";
	private float hintTime;
	private boolean menuOpen;

	// dealing animation, and the trump card turning over once it's done
	private int dealtHand = -1;
	private float dealT, dealDuration, trumpFlip;
	private static final float FLIP_TIME = 0.32f, TRUMP_FLIP_TIME = 0.45f;

	// shuffling before each deal: the deck comes to the middle, is split and riffled (twice for a
	// new game, once between hands), then goes to the dealer
	private float shuffleT, shuffleDuration;
	private int shufflePasses, shuffleSounds;
	private static final float SHUFFLE_GATHER = 0.4f, SHUFFLE_SPLIT = 0.3f, SHUFFLE_RIFFLE = 0.85f,
			SHUFFLE_SETTLE = 0.3f, SHUFFLE_PASS = SHUFFLE_SPLIT + SHUFFLE_RIFFLE + SHUFFLE_SETTLE, SHUFFLE_HOME = 0.4f;
	private static final int RIFFLE_CARDS = 24;

	// gold sparks when you make your bid or win the game
	private static final int SPARKS = 140;
	private final float[] spX = new float[SPARKS], spY = new float[SPARKS], spVX = new float[SPARKS],
			spVY = new float[SPARKS], spLife = new float[SPARKS], spMax = new float[SPARKS], spSize = new float[SPARKS],
			spHue = new float[SPARKS];
	private int burstHand = -1;
	private boolean burstGameOver;

	// card flights into the trick: start position and start time per card
	private final Map<Card, float[]> flights = new HashMap<Card, float[]>();

	// human selection and dragging
	private Card selected;
	private Card pressed;
	private float downX, downY, dragX, dragY;
	private boolean dragging;

	// autoplay (screenshots)
	private boolean menuDemoDone;

	// layout, rebuilt every frame while drawing
	private final List<Rectangle> bidButtons = new ArrayList<Rectangle>();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>();
	private final List<Card> handOrder = new ArrayList<Card>();
	private final Rectangle menuButton = new Rectangle();
	private Rectangle playAgainButton = new Rectangle(), highScoresButton = new Rectangle();
	/** Where your just-finished game landed in the high score table, or -1. */
	private int newHighScore = -1;
	private final List<Rectangle> menuItems = new ArrayList<Rectangle>();
	private static final String[] MENU = { "Resume", "New game", "Main menu" };

	/** A new game, or the saved game {@code restored} if not null. */
	public PlayScreen(OhHeckGame game, Match restored) {
		super(game);
		this.options = game.options;
		this.whiteRegion = new TextureRegion(assets.white);
		this.rng = options.seed == null ? new Random() : new Random(options.seed);
		if (restored == null) {
			newGame();
		} else {
			match = restored;
			// don't replay the deal for a hand that was already under way
			dealtHand = match.handIndex();
			dealT = dealDuration = 0;
			shuffleT = shuffleDuration = 0;
			trumpFlip = TRUMP_FLIP_TIME;
		}
	}

	private void newGame() {
		match = Match.random(4, rng);
		save();
		burstGameOver = false;
		newHighScore = -1;
		burstHand = -1;
		timer = 0;
		trickPause = 0;
		dealtHand = -1;
		flights.clear();
		clearSelection();
	}

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

	public boolean isGameOver() {
		return match.phase() == Match.Phase.GAME_OVER;
	}

	/** Saves the game so it survives Android closing the app; a finished game is forgotten. */
	private void save() {
		if (options.persist) {
			game.settings.saveGame(match.phase() == Match.Phase.GAME_OVER ? null : match.save());
		}
	}

	@Override
	public void pause() {
		save();
		// leaving the app (home button, a call): come back to the menu
		if (!options.autoplayHuman) {
			menuOpen = true;
			clearSelection();
		}
	}

	private void toWorld(int screenX, int screenY) {
		tmp.set(screenX, screenY);
		viewport.unproject(tmp);
	}

	// ----------------------------------------------------------------- input

	private void onDown(float x, float y) {
		downX = x;
		downY = y;
		pressed = null;
		dragging = false;
		if (menuOpen || !humanCanPlay()) {
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
			selected = pressed;
		}
		dragX = x;
		dragY = y;
	}

	private void onUp(float x, float y) {
		if (dragging) {
			Card c = pressed;
			dragging = false;
			pressed = null;
			selected = null; // played below, or dropped back into the hand
			Rectangle r = handRects.isEmpty() ? null : handRects.get(0);
			float handTop = r == null ? 150 : r.y + r.height;
			if (y > handTop + 10 && humanCanPlay()) {
				// dropped on the table: play it from where it was let go
				float cw = Assets.CARD_W * HAND_SCALE, ch = Assets.CARD_H * HAND_SCALE;
				humanPlay(c, x - cw / 2, y - ch / 2);
			}
			return;
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
			for (int i = 0; i < menuItems.size(); i++) {
				if (menuItems.get(i).contains(x, y)) {
					menuChoice(i);
					return;
				}
			}
			return;
		}
		if (menuButton.contains(x, y)) {
			menuOpen = true;
			clearSelection();
			return;
		}
		if (isDealing()) {
			shuffleT = shuffleDuration; // tap skips the shuffle and the deal
			shuffleSounds = shufflePasses;
			dealT = dealDuration;
			return;
		}
		if (trickPause > 0) {
			return;
		}
		switch (match.phase()) {
		case BIDDING:
			if (match.currentSeat() == HUMAN) {
				tapBid(x, y);
			}
			break;
		case PLAYING:
			if (match.currentSeat() == HUMAN) {
				tapCard(x, y);
			}
			break;
		case HAND_OVER:
			if (handOverT < HAND_END_SHOW) {
				handOverT = HAND_END_SHOW;
			} else {
				match.nextHand();
				if (match.phase() == Match.Phase.GAME_OVER) {
					newHighScore = game.recordHighScore(match.score(HUMAN), place(HUMAN));
				}
				save();
				timer = 0;
			}
			break;
		case GAME_OVER:
			if (highScoresButton.contains(x, y)) {
				game.showHighScores(newHighScore);
			} else {
				newGame(); // the Play again button, or anywhere else
			}
			break;
		}
	}

	private void tapBid(float x, float y) {
		for (int b = 0; b < bidButtons.size(); b++) {
			if (bidButtons.get(b).contains(x, y)) {
				if (!match.isLegalBid(b)) {
					showHint("As dealer you can't bid " + b + ": the bids would add up to " + match.handSize());
				} else {
					match.placeBid(HUMAN, b);
					save();
					clearSelection();
					timer = 0;
				}
				return;
			}
		}
	}

	private void tapCard(float x, float y) {
		int i = cardAt(x, y);
		if (i < 0) {
			selected = null;
			return;
		}
		Card c = handOrder.get(i);
		if (!match.legalPlays(HUMAN).contains(c)) {
			Suit lead = match.leadSuit();
			showHint("You must follow suit: play a " + singular(lead));
			return;
		}
		Rectangle r = handRects.get(i);
		humanPlay(c, r.x, r.y);
	}

	@Override
	protected void back() {
		menuOpen = !menuOpen;
		clearSelection();
	}

	private void menuChoice(int i) {
		menuOpen = false;
		if (i == 1) {
			newGame();
		} else if (i == 2) {
			game.showTitle();
		}
	}

	private boolean humanCanPlay() {
		return !menuOpen && !isDealing() && trickPause <= 0 && match.phase() == Match.Phase.PLAYING
				&& match.currentSeat() == HUMAN;
	}

	private int cardAt(float x, float y) {
		// topmost first: later cards are drawn over earlier ones
		for (int i = handRects.size() - 1; i >= 0; i--) {
			Rectangle r = handRects.get(i);
			if (r.contains(x, y)) {
				return i;
			}
		}
		return -1;
	}

	private void clearSelection() {
		selected = null;
		pressed = null;
		dragging = false;
	}

	private void showHint(String s) {
		hint = s;
		hintTime = HINT_TIME;
	}

	// ---------------------------------------------------------------- update

	@Override
	public void render(float delta) {
		update(Math.min(delta, 0.1f) * options.speed * game.settings.speedFactor());
		draw();
	}

	private void update(float dt) {
		if (menuOpen) {
			if (options.autoplayHuman) {
				timer += dt;
				if (timer > 1.5f) {
					timer = 0;
					tapCentre(menuItems.isEmpty() ? null : menuItems.get(0));
				}
			}
			return;
		}
		time += dt;
		hintTime -= dt;
		updateSparks(dt);

		// scripted runs play some hands instantly, to get to the interesting ones sooner
		while (options.autoplayHuman && match.phase() == Match.Phase.BIDDING && match.handIndex() != dealtHand
				&& options.skipHands.contains(match.handIndex() + 1)) {
			playHandInstantly();
		}

		// a new hand starts with the deal
		if (match.phase() == Match.Phase.BIDDING && match.handIndex() != dealtHand) {
			dealtHand = match.handIndex();
			dealT = 0;
			dealDuration = 4 * match.handSize() * DEAL_INTERVAL + DEAL_FLIGHT + FLIP_TIME + 0.1f;
			shufflePasses = match.handIndex() == 0 ? 2 : 1;
			shuffleDuration = SHUFFLE_GATHER + shufflePasses * SHUFFLE_PASS + SHUFFLE_HOME;
			shuffleT = 0;
			shuffleSounds = 0;
			trumpFlip = 0;
			flights.clear();
		}
		if (isDealing()) {
			if (isShuffling()) {
				shuffleT += dt;
				// the shuffle sound starts with each riffle
				if (shuffleSounds < shufflePasses
						&& shuffleT >= SHUFFLE_GATHER + shuffleSounds * SHUFFLE_PASS + SHUFFLE_SPLIT) {
					shuffleSounds++;
					game.audio.play(Audio.Effect.SHUFFLE);
				}
			} else {
				dealT += dt;
			}
			return;
		}
		trumpFlip += dt;

		if (trickPause > 0) {
			trickPause -= dt;
			if (trickPause <= 0) {
				flights.clear();
			}
			return;
		}

		switch (match.phase()) {
		case BIDDING:
		case PLAYING:
			if (match.currentSeat() == HUMAN) {
				if (options.autoplayHuman) {
					autoplayHuman(dt);
				}
			} else {
				timer += dt;
				if (timer >= AI_DELAY) {
					timer = 0;
					botMove(match.currentSeat());
				}
			}
			break;
		case HAND_OVER:
			if (burstHand != match.handIndex() && match.madeBid(HUMAN)) {
				burstHand = match.handIndex();
				burst(w / 2, h / 2 + 5, 110);
			}
			handOverT += dt;
			if (options.autoplayHuman && handOverT >= HAND_END_SHOW + HAND_OVER_AUTO) {
				tap(w / 2, h / 2);
			}
			break;
		case GAME_OVER:
			if (!burstGameOver && match.leaders().contains(HUMAN)) {
				burstGameOver = true;
				burst(w / 2, h / 2 + 100, 160);
			}
			break;
		default:
			break;
		}
	}

	/** Plays the current hand to the end with the bot in every seat, then deals the next. */
	private void playHandInstantly() {
		while (match.phase() == Match.Phase.BIDDING || match.phase() == Match.Phase.PLAYING) {
			int s = match.currentSeat();
			if (match.phase() == Match.Phase.BIDDING) {
				match.placeBid(s, bot.chooseBid(match.hand(s), match.trump(), match.bidsSoFar()));
			} else {
				match.playCard(s, bot.choosePlay(match.hand(s), match.currentTrick(), match.trump(), match.bid(s),
						match.tricksWon(s)));
			}
		}
		if (match.phase() == Match.Phase.HAND_OVER) {
			match.nextHand();
		}
	}

	private void botMove(int seat) {
		if (match.phase() == Match.Phase.BIDDING) {
			match.placeBid(seat, bot.chooseBid(match.hand(seat), match.trump(), match.bidsSoFar()));
			save();
		} else {
			Card c = bot.choosePlay(match.hand(seat), match.currentTrick(), match.trump(), match.bid(seat),
					match.tricksWon(seat));
			Vector2 from = seatAnchor(seat);
			startFlight(c, from.x - Assets.CARD_W / 2f, from.y - Assets.CARD_H / 2f);
			play(seat, c);
		}
	}

	private void humanPlay(Card c, float fromX, float fromY) {
		startFlight(c, fromX, fromY);
		clearSelection();
		play(HUMAN, c);
	}

	private void startFlight(Card c, float x, float y) {
		flights.put(c, new float[] { x, y, time });
	}

	private void play(int seat, Card c) {
		int winner = match.playCard(seat, c);
		save();
		timer = 0;
		game.audio.play(Audio.Effect.PLAY_CARD);
		if (match.phase() == Match.Phase.HAND_OVER) {
			game.audio.play(match.madeBid(HUMAN) ? Audio.Effect.WIN : Audio.Effect.LOSE);
		}
		if (winner >= 0) {
			humanWonTrick = winner == HUMAN;
			trickTotal = TRICK_PAUSE + (humanWonTrick ? TRICK_WON_SHOW : 0);
			trickPause = trickTotal;
			handOverT = 0;
		}
	}

	/**
	 * Plays the human seat for automated screenshots, through the same taps a
	 * person would make: wait a moment, then tap the bot's choice.
	 */
	private void autoplayHuman(float dt) {
		timer += dt;
		// once per game, open and close the menu to capture it
		if (!menuDemoDone && match.handIndex() == 6 && match.phase() == Match.Phase.BIDDING) {
			menuDemoDone = true;
			timer = 0;
			tapCentre(menuButton);
			return;
		}
		if (timer >= AUTOPLAY_DELAY) {
			timer = 0;
			autoTap();
		}
	}

	private void autoTap() {
		if (match.phase() == Match.Phase.BIDDING) {
			int b = bot.chooseBid(match.hand(HUMAN), match.trump(), match.bidsSoFar());
			tapCentre(b < bidButtons.size() ? bidButtons.get(b) : null);
		} else {
			Card c = bot.choosePlay(match.hand(HUMAN), match.currentTrick(), match.trump(), match.bid(HUMAN),
					match.tricksWon(HUMAN));
			int i = handOrder.indexOf(c);
			tapCentre(i >= 0 ? handRects.get(i) : null);
		}
	}

	private void tapCentre(Rectangle r) {
		if (r != null) {
			tap(r.x + Math.min(r.width, 30) / 2, r.y + r.height / 2);
		}
	}

	/** Shuffling comes first, as part of dealing. */
	private boolean isShuffling() {
		return isDealing() && shuffleT < shuffleDuration;
	}

	private boolean isDealing() {
		return dealtHand == match.handIndex() && match.phase() == Match.Phase.BIDDING && dealT < dealDuration;
	}

	// ------------------------------------------------------------------ draw

	private void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		drawInfo();
		drawTrump();
		for (int seat = 1; seat <= 3; seat++) {
			drawOpponent(seat);
		}
		drawHumanTag();
		drawHand();
		drawTrick();
		if (isDealing()) {
			drawDealing();
		}
		if (!menuOpen) {
			drawMenuButton();
		}

		if (trickPause <= 0 && !isDealing() && !menuOpen) {
			if (match.phase() == Match.Phase.BIDDING && match.currentSeat() == HUMAN) {
				drawBidPanel();
			} else if (match.phase() == Match.Phase.HAND_OVER) {
				if (handOverT < HAND_END_SHOW) {
					drawHandEnd();
				} else {
					drawHandResults();
					drawHandLabels(MathUtils.clamp((handOverT - HAND_END_SHOW) / LABEL_DROP, 0, 1), 1f);
				}
			} else if (match.phase() == Match.Phase.GAME_OVER) {
				drawGameOver();
			}
		}
		if (dragging && selected != null) {
			float cw = Assets.CARD_W * HAND_SCALE, ch = Assets.CARD_H * HAND_SCALE;
			batch.draw(assets.face(selected), dragX - cw / 2, dragY - ch / 2, cw, ch);
		}
		drawSparks();
		if (hintTime > 0 && !menuOpen) {
			drawHint();
		}
		if (menuOpen) {
			drawMenu();
		}
		batch.end();
	}

	// ---- the hand counter: tally marks, a gate of five then three

	private static final float TALLY_H = 36f, TALLY_W = 5f, TALLY_STEP = 12f;
	/** Small fixed leans so the strokes look hand drawn. */
	private static final float[] TALLY_LEAN = { 3f, -2f, 4f, -1f, 0f, 2f, -3f, 1f };

	private void drawInfo() {
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

	/** A bar centred on (cx, cy), rotated by rot degrees. */
	private void stroke(float cx, float cy, float sw, float sh, float rot, Color c, float alpha) {
		batch.setColor(c.r, c.g, c.b, alpha);
		batch.draw(whiteRegion, cx - sw / 2, cy - sh / 2, sw / 2, sh / 2, sw, sh, 1f, 1f, rot);
		batch.setColor(Color.WHITE);
	}

	private void drawTrump() {
		if (isShuffling()) {
			return; // the trump card is still in the deck
		}
		float cw = Assets.CARD_W * TRUMP_SCALE, ch = Assets.CARD_H * TRUMP_SCALE;
		float x = w - 12 - cw, y = h - 12 - ch;
		shadow(x, y, cw, ch, 4);
		float p = isDealing() ? 0 : MathUtils.clamp(trumpFlip / TRUMP_FLIP_TIME, 0, 1);
		flipCard(match.trumpCard(), x, y, cw, ch, p);
		drawMotes(x, y, cw, ch, p >= 1f);
	}

	// ---- faint gold motes drifting up off the trump card

	private static final int MOTES = 28;
	private final float[] moX = new float[MOTES], moY = new float[MOTES], moVX = new float[MOTES],
			moVY = new float[MOTES], moLife = new float[MOTES], moMax = new float[MOTES], moSize = new float[MOTES];
	private float moteClock = -1, moteSpawn;

	/** Rises from the card's edges; new motes only once the card is face up. */
	private void drawMotes(float x, float y, float cw, float ch, boolean spawning) {
		float dt = moteClock < 0 ? 0 : MathUtils.clamp(time - moteClock, 0, 0.1f);
		moteClock = time;
		if (spawning) {
			moteSpawn += dt * 7f; // about seven a second
		}
		for (int i = 0; i < MOTES && moteSpawn >= 1; i++) {
			if (moLife[i] > 0) {
				continue;
			}
			moteSpawn -= 1;
			// somewhere along the card's outline, mostly the top and sides
			if (MathUtils.random() < 0.4f) {
				moX[i] = x + MathUtils.random(cw);
				moY[i] = y + ch - MathUtils.random(4f);
			} else {
				moX[i] = MathUtils.randomBoolean() ? x + MathUtils.random(3f) : x + cw - MathUtils.random(3f);
				moY[i] = y + MathUtils.random(ch * 0.2f, ch);
			}
			moVX[i] = MathUtils.random(-5f, 5f);
			moVY[i] = MathUtils.random(7f, 18f);
			moMax[i] = moLife[i] = MathUtils.random(1.6f, 3.2f);
			moSize[i] = MathUtils.random(3f, 7f);
		}
		moteSpawn = Math.min(moteSpawn, 1);
		for (int i = 0; i < MOTES; i++) {
			if (moLife[i] <= 0) {
				continue;
			}
			moLife[i] -= dt;
			moX[i] += (moVX[i] + MathUtils.sin(time * 1.7f + i) * 4f) * dt;
			moY[i] += moVY[i] * dt;
			float age = 1 - moLife[i] / moMax[i];
			// fade in quickly, out slowly, and twinkle a little
			float a = Math.min(age * 6f, 1f) * (1 - age) * (0.75f + 0.25f * MathUtils.sin(time * 6f + i * 2.3f));
			float sz = moSize[i] * (1 - age * 0.4f);
			batch.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, 0.45f * a);
			batch.draw(assets.glow, moX[i] - sz / 2, moY[i] - sz / 2, sz, sz);
		}
		batch.setColor(Color.WHITE);
	}


	// ---- player panels: name, dealer icon, score, and a row of bid/trick icons as in the original game

	private static final float PANEL_W = PlayerPanel.W, PANEL_H = PlayerPanel.H;

	/** Top-left corner of a seat's panel. */
	private float panelX(int seat) {
		return seat == HUMAN || seat == 1 ? 12 : seat == 2 ? w / 2 - PANEL_W / 2 : w - 12 - PANEL_W;
	}

	private float panelTop(int seat) {
		return seat == HUMAN ? 12 + PANEL_H : seat == 2 ? h - 6 : h / 2 + 78;
	}

	private void drawOpponent(int seat) {
		drawPanel(seat);
		// while dealing, only the cards that have landed, each already in its final place
		int total = match.hand(seat).size();
		int n = isDealing() ? dealtTo(seat) : total;
		float bw = Assets.CARD_W * BACK_SCALE, bh = Assets.CARD_H * BACK_SCALE;
		float y = fanOrigin(seat).y;
		for (int i = 0; i < n; i++) {
			batch.draw(assets.back(), fanCardX(seat, i, total), y, bw, bh);
		}
	}

	/** Left edge of the i-th of n face-down cards in an opponent's fan. */
	private float fanCardX(int seat, int i, int n) {
		float bw = Assets.CARD_W * BACK_SCALE;
		Vector2 fan = fanOrigin(seat);
		return seat == 3 ? fan.x - bw - (n - 1 - i) * 12 : fan.x + i * 12;
	}

	/** Your hand's layout for n cards: left edge of the first card and the step between cards. */
	private float[] handLayout(int n) {
		float cw = Assets.CARD_W * HAND_SCALE;
		// leave room for your panel on the left and the menu button on the right
		float maxWidth = Math.max(330, w - 2 * (PANEL_W + 26));
		float gap = n <= 1 ? 0 : Math.min(cw + 6, (maxWidth - cw) / (n - 1));
		return new float[] { (w - (gap * (n - 1) + cw)) / 2, gap };
	}

	/** Where an opponent's face-down cards start: beside the top panel, under the side panels. */
	private Vector2 fanOrigin(int seat) {
		float bh = Assets.CARD_H * BACK_SCALE;
		float x = panelX(seat), top = panelTop(seat);
		if (seat == 2) {
			return new Vector2(x + PANEL_W + 10, top - PANEL_H / 2 - bh / 2);
		}
		return new Vector2(seat == 3 ? x + PANEL_W : x, top - PANEL_H - 6 - bh);
	}

	/** Centre of where a seat's cards are, for cards flying to and from it. */
	private Vector2 seatAnchor(int seat) {
		if (seat == HUMAN) {
			return new Vector2(w / 2, 70);
		}
		Vector2 f = fanOrigin(seat);
		float bw = Assets.CARD_W * BACK_SCALE, bh = Assets.CARD_H * BACK_SCALE;
		return new Vector2(seat == 3 ? f.x - bw : f.x + bw, f.y + bh / 2);
	}

	private boolean isTurn(int seat) {
		return match.currentSeat() == seat && trickPause <= 0 && !isDealing()
				&& (match.phase() == Match.Phase.BIDDING || match.phase() == Match.Phase.PLAYING);
	}

	private void drawHumanTag() {
		drawPanel(HUMAN);
	}

	private void drawPanel(int seat) {
		panel.draw(NAMES[seat], panelX(seat), panelTop(seat), isTurn(seat), match.dealer() == seat, match.score(seat),
				match.bid(seat), match.tricksWon(seat), isDealing());
	}

	private void drawHand() {
		handOrder.clear();
		handOrder.addAll(match.hand(HUMAN));
		Collections.sort(handOrder, DISPLAY_ORDER);
		handRects.clear();
		int n = handOrder.size();
		if (n == 0) {
			return;
		}
		int shown = isDealing() ? dealtTo(HUMAN) : n;
		float cw = Assets.CARD_W * HAND_SCALE, ch = Assets.CARD_H * HAND_SCALE;
		float[] layout = handLayout(n);
		float start = layout[0], gap = layout[1];

		boolean myTurn = humanCanPlay();
		List<Card> legal = myTurn ? match.legalPlays(HUMAN) : Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			Card c = handOrder.get(i);
			boolean canPlay = legal.contains(c);
			float x = start + i * gap;
			float y = 8 + (c == selected ? SELECTED_LIFT : canPlay ? LIFT : 0);
			handRects.add(new Rectangle(x, y, i == n - 1 ? cw : gap, ch));
			if (i >= shown || (dragging && c == selected)) {
				continue;
			}
			shadow(x, y, cw, ch, y - 8 + 5);
			if (c == selected) {
				frame(assets.panelTurn, x - 7, y - 7, cw + 14, ch + 14);
			}
			batch.setColor(myTurn && !canPlay ? Palette.CARD_DIM : Color.WHITE);
			if (isDealing()) {
				flipCard(c, x, y, cw, ch, MathUtils.clamp((dealT - arrivalTime(HUMAN, i)) / FLIP_TIME, 0, 1));
			} else {
				batch.draw(assets.face(c), x, y, cw, ch);
			}
		}
		batch.setColor(Color.WHITE);
	}

	/** Where a seat's card sits in the middle of the table. */
	private Vector2 trickSlot(int seat) {
		float cx = w / 2, cy = h / 2 + 15;
		float x = cx - Assets.CARD_W / 2f + (seat == 1 ? -108 : seat == 3 ? 108 : 0);
		float y = cy - Assets.CARD_H / 2f + (seat == HUMAN ? -60 : seat == 2 ? 60 : 0);
		return new Vector2(x, y);
	}

	private static final float MAX_TILT = 9f;

	/** A card's resting tilt in the middle: random-looking, but steady for that card in that hand. */
	private float tilt(Card c, int seat) {
		long k = ((c.suit().ordinal() * 13L + c.rank().ordinal()) * 31 + match.handIndex()) * 8 + seat;
		k = (k + 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
		k ^= k >>> 31;
		float r = (k & 0xFFFF) / 65535f;
		return (r * 2 - 1) * MAX_TILT;
	}

	private void drawTrick() {
		List<Card> cards;
		List<Integer> seats;
		int winner = -1;
		float collect = 0; // 0 = on the table, 1 = gathered in by the winner
		if (trickPause > 0) {
			cards = match.lastTrick();
			seats = match.lastTrickSeats();
			winner = match.lastTrickWinner();
			float elapsed = trickTotal - trickPause;
			collect = MathUtils.clamp((elapsed - (TRICK_PAUSE - COLLECT_TIME)) / COLLECT_TIME, 0, 1);
			if (humanWonTrick && elapsed > TRICK_PAUSE - COLLECT_TIME / 2) {
				float a = MathUtils.clamp((elapsed - (TRICK_PAUSE - COLLECT_TIME / 2)) / 0.3f, 0, 1);
				drawArt(assets.trickWonText, w / 2, h / 2 + 5, 0.8f, a);
			}
			if (elapsed >= TRICK_PAUSE) {
				return; // the cards have gone to the winner
			}
		} else {
			cards = match.currentTrick();
			seats = match.currentTrickSeats();
		}
		float cw = Assets.CARD_W, ch = Assets.CARD_H;
		Vector2 sink = winner >= 0 ? seatAnchor(winner) : null;
		for (int i = 0; i < cards.size(); i++) {
			Card c = cards.get(i);
			Vector2 slot = trickSlot(seats.get(i));
			float x = slot.x, y = slot.y, scale = 1f, lift = 5f, turn = 1f;
			float rot = tilt(c, seats.get(i));
			float[] f = flights.get(c);
			if (f != null) {
				float raw = MathUtils.clamp((time - f[2]) / PLAY_FLIGHT, 0, 1);
				float p = Interpolation.pow2Out.apply(raw);
				x = MathUtils.lerp(f[0], slot.x, p);
				y = MathUtils.lerp(f[1], slot.y, p);
				lift += (1 - raw) * 30;
				rot *= p; // settles into its tilt as it lands
				if (seats.get(i) != HUMAN) {
					turn = MathUtils.clamp(raw * 1.4f, 0, 1); // opponents' cards turn over on the way
				}
			}
			if (collect > 0) {
				float p = Interpolation.pow2In.apply(collect);
				x = MathUtils.lerp(x, sink.x - cw / 4, p);
				y = MathUtils.lerp(y, sink.y - ch / 4, p);
				scale = MathUtils.lerp(1f, 0.5f, p);
				lift += p * 18;
				rot *= 1 - p;
			}
			shadow(x, y, cw * scale, ch * scale, lift);
			if (collect == 0 && seats.get(i) == winner) {
				batch.setColor(1f, 1f, 1f, fade);
				assets.panelTurn.draw(batch, x - 7, y - 7, cw / 2 + 7, ch / 2 + 7, cw + 14, ch + 14, 1f, 1f, rot);
				batch.setColor(Color.WHITE);
			}
			flipCard(c, x, y, cw * scale, ch * scale, turn, rot);
		}
	}

	// ---- the shuffle

	private static final float DECK_SCALE = 0.85f, HALF_GAP = 52f;

	/** A deck seen from above at an angle: count cards stacked with a slight offset, centred on (cx, cy). */
	private void stack(float cx, float cy, int count, float cw, float ch, float rotation) {
		int shown = Math.min(count, 14);
		for (int i = 0; i < shown; i++) {
			float x = cx - cw / 2 - i * 0.45f, y = cy - ch / 2 + i * 0.7f;
			if (i == 0) {
				shadow(x, y, cw, ch, 4 + shown * 0.4f);
			}
			batch.draw(assets.back(), x, y, cw / 2, ch / 2, cw, ch, 1f, 1f, rotation);
		}
	}

	private void drawShuffle() {
		float cw = Assets.CARD_W * DECK_SCALE, ch = Assets.CARD_H * DECK_SCALE;
		float cx = w / 2, cy = h / 2 + 5;
		Vector2 dealer = seatAnchor(match.dealer());
		float t = shuffleT;

		// gather: the deck comes from the dealer to the middle of the table
		if (t < SHUFFLE_GATHER) {
			float p = Interpolation.pow2Out.apply(t / SHUFFLE_GATHER);
			stack(MathUtils.lerp(dealer.x, cx, p), MathUtils.lerp(dealer.y, cy, p), 52, cw, ch, 0);
			return;
		}
		t -= SHUFFLE_GATHER;

		// riffle passes: split into two halves, then cards fall alternately from each onto one pile
		if (t < shufflePasses * SHUFFLE_PASS) {
			t %= SHUFFLE_PASS;
			if (t < SHUFFLE_SPLIT) {
				float p = Interpolation.pow2Out.apply(t / SHUFFLE_SPLIT);
				stack(cx - HALF_GAP * p, cy, 26, cw, ch, 8 * p);
				stack(cx + HALF_GAP * p, cy, 26, cw, ch, -8 * p);
				return;
			}
			t -= SHUFFLE_SPLIT;
			if (t < SHUFFLE_RIFFLE) {
				float step = (SHUFFLE_RIFFLE - 0.15f) / RIFFLE_CARDS;
				int landed = 0, leftGone = 0, rightGone = 0;
				for (int i = 0; i < RIFFLE_CARDS; i++) {
					if (t >= i * step) {
						if (i % 2 == 0) {
							leftGone++;
						} else {
							rightGone++;
						}
					}
					if (t >= i * step + 0.15f) {
						landed++;
					}
				}
				int perHalf = RIFFLE_CARDS / 2;
				stack(cx - HALF_GAP, cy, 26 * (perHalf - leftGone) / perHalf + 1, cw, ch, 8);
				stack(cx + HALF_GAP, cy, 26 * (perHalf - rightGone) / perHalf + 1, cw, ch, -8);
				stack(cx, cy, landed * 52 / RIFFLE_CARDS, cw, ch, 0);
				// cards in the air, arcing from their half to the pile
				for (int i = 0; i < RIFFLE_CARDS; i++) {
					float p = (t - i * step) / 0.15f;
					if (p < 0 || p >= 1) {
						continue;
					}
					float from = i % 2 == 0 ? cx - HALF_GAP : cx + HALF_GAP;
					float x = MathUtils.lerp(from, cx, p), y = cy + 8 + MathUtils.sin(MathUtils.PI * p) * 16;
					float rot = MathUtils.lerp(i % 2 == 0 ? 8 : -8, 0, p);
					batch.draw(assets.back(), x - cw / 2, y - ch / 2, cw / 2, ch / 2, cw, ch, 1f, 1f, rot);
				}
				return;
			}
			// settle: the deck is squared up
			stack(cx, cy, 52, cw, ch, 0);
			return;
		}
		t -= shufflePasses * SHUFFLE_PASS;

		// home: the deck goes back to the dealer, ready to deal
		float p = Interpolation.pow2In.apply(MathUtils.clamp(t / SHUFFLE_HOME, 0, 1));
		float s = MathUtils.lerp(1f, BACK_SCALE / DECK_SCALE, p);
		stack(MathUtils.lerp(cx, dealer.x, p), MathUtils.lerp(cy, dealer.y, p), 52, cw * s, ch * s, 0);
	}

	/** When the n-th card dealt to a seat lands, in seconds from the start of the deal. */
	private float arrivalTime(int seat, int n) {
		int count = 0;
		for (int k = 0; k < 4 * match.handSize(); k++) {
			if ((match.dealer() + 1 + k) % 4 == seat && count++ == n) {
				return k * DEAL_INTERVAL + DEAL_FLIGHT;
			}
		}
		return 0;
	}

	/** A soft shadow under a card; the higher the card is lifted, the further and fainter it falls. */
	private void shadow(float x, float y, float cw, float ch, float lift) {
		float sw = cw * 160f / 120f, sh = ch * 200f / 160f;
		batch.setColor(1f, 1f, 1f, MathUtils.clamp(0.6f - lift * 0.008f, 0.28f, 0.6f));
		batch.draw(assets.shadow, x + cw / 2 - sw / 2 + lift * 0.2f, y + ch / 2 - sh / 2 - 2 - lift * 0.4f, sw, sh);
		batch.setColor(Color.WHITE);
	}

	/**
	 * Draws a card turning over about its upright centre line: p = 0 face down,
	 * 0.5 edge on, 1 face up. Uses the batch's current tint.
	 */
	private void flipCard(Card c, float x, float y, float cw, float ch, float p) {
		flipCard(c, x, y, cw, ch, p, 0f);
	}

	/** As above, turned by rot degrees about the card's centre. */
	private void flipCard(Card c, float x, float y, float cw, float ch, float p, float rot) {
		float sx = p >= 1f ? 1f : Math.abs(MathUtils.cos(MathUtils.PI * p));
		TextureRegion r = p < 0.5f ? assets.back() : assets.face(c);
		if (rot == 0f) {
			batch.draw(r, x + cw * (1 - sx) / 2, y, cw * sx, ch);
		} else {
			batch.draw(r, x + cw * (1 - sx) / 2, y, cw * sx / 2, ch / 2, cw * sx, ch, 1f, 1f, rot);
		}
	}

	// ---- celebration sparks

	private void burst(float x, float y, int count) {
		for (int n = 0, i = 0; i < SPARKS && n < count; i++) {
			if (spLife[i] > 0) {
				continue;
			}
			float angle = MathUtils.random(0.15f, MathUtils.PI - 0.15f);
			float speed = MathUtils.random(140f, 420f);
			spX[i] = x + MathUtils.random(-60f, 60f);
			spY[i] = y + MathUtils.random(-20f, 20f);
			spVX[i] = MathUtils.cos(angle) * speed;
			spVY[i] = MathUtils.sin(angle) * speed;
			spMax[i] = spLife[i] = MathUtils.random(0.9f, 1.8f);
			spSize[i] = MathUtils.random(5f, 14f);
			spHue[i] = MathUtils.random();
			n++;
		}
	}

	private void updateSparks(float dt) {
		for (int i = 0; i < SPARKS; i++) {
			if (spLife[i] <= 0) {
				continue;
			}
			spLife[i] -= dt;
			spVY[i] -= 380f * dt;
			spVX[i] *= 1f - 0.8f * dt;
			spX[i] += spVX[i] * dt;
			spY[i] += spVY[i] * dt;
		}
	}

	private void drawSparks() {
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		for (int i = 0; i < SPARKS; i++) {
			if (spLife[i] <= 0) {
				continue;
			}
			float a = spLife[i] / spMax[i];
			float twinkle = 0.7f + 0.3f * MathUtils.sin(time * 30f + i);
			float g = MathUtils.lerp(0.65f, 0.9f, spHue[i]);
			batch.setColor(1f, g, 0.35f, a * 0.55f);
			float big = spSize[i] * 2.4f;
			batch.draw(assets.glow, spX[i] - big / 2, spY[i] - big / 2, big, big);
			batch.setColor(1f, 0.95f, 0.8f, a * twinkle);
			float core = spSize[i] * 0.7f;
			batch.draw(assets.glow, spX[i] - core / 2, spY[i] - core / 2, core, core);
		}
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	/** Cards already arrived at a seat during the deal animation. */
	private int dealtTo(int seat) {
		int n = match.handSize();
		int arrived = 0;
		for (int k = 0; k < 4 * n; k++) {
			int s = (match.dealer() + 1 + k) % 4;
			if (s == seat && dealT >= k * DEAL_INTERVAL + DEAL_FLIGHT) {
				arrived++;
			}
		}
		return arrived;
	}

	private void drawDealing() {
		if (isShuffling()) {
			drawShuffle();
			return;
		}
		int n = match.handSize();
		Vector2 from = seatAnchor(match.dealer());
		float fromW = Assets.CARD_W * BACK_SCALE, fromH = Assets.CARD_H * BACK_SCALE;
		float[] hand = handLayout(n);
		int[] dealtSoFar = new int[4];
		for (int k = 0; k < 4 * n; k++) {
			int seat = (match.dealer() + 1 + k) % 4;
			int j = dealtSoFar[seat]++; // this is the j-th card to reach that seat
			float start = k * DEAL_INTERVAL;
			if (dealT < start || dealT >= start + DEAL_FLIGHT) {
				continue;
			}
			// fly straight to the place the card will stay, face down; yours flip once they land
			float tx, ty, tw, th;
			if (seat == HUMAN) {
				tw = Assets.CARD_W * HAND_SCALE;
				th = Assets.CARD_H * HAND_SCALE;
				tx = hand[0] + j * hand[1];
				ty = 8;
			} else {
				tw = Assets.CARD_W * BACK_SCALE;
				th = Assets.CARD_H * BACK_SCALE;
				tx = fanCardX(seat, j, n);
				ty = fanOrigin(seat).y;
			}
			float p = Interpolation.pow2Out.apply((dealT - start) / DEAL_FLIGHT);
			float cw = MathUtils.lerp(fromW, tw, p), ch = MathUtils.lerp(fromH, th, p);
			float x = MathUtils.lerp(from.x - fromW / 2, tx, p), y = MathUtils.lerp(from.y - fromH / 2, ty, p);
			shadow(x, y, cw, ch, 26 * (1 - p) + 4);
			batch.draw(assets.back(), x, y, cw, ch);
		}
	}

	private void drawBidPanel() {
		int n = match.handSize();
		float bs = 58, gap = 8;
		float rowW = (n + 1) * bs + n * gap;
		float pw = Math.max(rowW + 40, 440), ph = 176;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		frame(assets.panel, px, py, pw, ph);

		drawArt(assets.makeBidText, w / 2, py + ph - 36, 0.52f, 1f);
		BitmapFont s = ui(1f);
		s.setColor(Palette.MUTED);
		int forbidden = match.forbiddenBid();
		String note = "Bids so far: " + match.bidsSoFar() + " of " + n
				+ (forbidden >= 0 ? "   -   as dealer you can't bid " + forbidden : "");
		text(s, note, w / 2, py + ph - 60, true);

		bidButtons.clear();
		float bx = (w - rowW) / 2, by = py + 20;
		BitmapFont bf = ui(1.7f);
		for (int b = 0; b <= n; b++) {
			Rectangle r = new Rectangle(bx + b * (bs + gap), by, bs, bs);
			bidButtons.add(r);
			boolean ok = match.isLegalBid(b);
			frame(ok ? assets.button : assets.buttonOff, r.x, r.y, r.width, r.height);
			bf.setColor(ok ? Palette.CREAM : Palette.MUTED);
			text(bf, String.valueOf(b), r.x + bs / 2, r.y + bs / 2 + 12, true);
		}
	}

	/** Draws one of the original title-art images centred on (cx, cy). */
	private void drawArt(Texture t, float cx, float cy, float scale, float alpha) {
		float aw = t.getWidth() * scale, ah = t.getHeight() * scale;
		batch.setColor(1f, 1f, 1f, alpha);
		batch.draw(t, cx - aw / 2, cy - ah / 2, aw, ah);
		batch.setColor(Color.WHITE);
	}

	/** The original end-of-hand moment: "Bid Tricks!" or "You got set!", and how each opponent did. */
	private void drawHandEnd() {
		float a = MathUtils.clamp(handOverT / 0.3f, 0, 1);
		drawArt(match.madeBid(HUMAN) ? assets.bidTricksText : assets.setText, w / 2, h / 2 + 5, 0.95f, a);
		drawHandLabels(0f, a);
		BitmapFont t = ui(1f);
		t.setColor(Palette.MUTED);
		text(t, "Tap for the scores", w / 2, h / 2 - 40, true);
	}

	private static final float LABEL_DROP = 0.6f;

	/**
	 * "Bid tricks!" / "Set!" for each player. The opponents' sit by their cards;
	 * yours starts as the big art in the middle and, as drop goes 0 to 1, glides
	 * to the right of your panel, where it stays while the scores are up.
	 */
	private void drawHandLabels(float drop, float alpha) {
		BitmapFont f = assets.serif;
		for (int seat = 0; seat < 4; seat++) {
			if (seat == HUMAN && drop <= 0) {
				continue;
			}
			boolean made = match.madeBid(seat);
			String s = made ? "Bid tricks!" : "Set!";
			float scale, x, y, a = alpha;
			if (seat == HUMAN) {
				float p = Interpolation.pow2Out.apply(drop);
				f.getData().setScale(2.6f * Assets.SERIF_BASE);
				glyphs.setText(f, s);
				float sx = w / 2 - glyphs.width / 2, sy = h / 2 + 5 + glyphs.height / 2;
				f.getData().setScale(1.6f * Assets.SERIF_BASE);
				glyphs.setText(f, s);
				float ex = panelX(HUMAN) + PANEL_W + 16;
				float ey = panelTop(HUMAN) - PANEL_H / 2 + glyphs.height / 2;
				scale = MathUtils.lerp(2.6f, 1.6f, p);
				x = MathUtils.lerp(sx, ex, p);
				y = MathUtils.lerp(sy, ey, p);
				a *= MathUtils.clamp(drop * 3f, 0, 1);
			} else {
				scale = 1.9f;
				f.getData().setScale(scale * Assets.SERIF_BASE);
				glyphs.setText(f, s);
				Vector2 o = fanOrigin(seat);
				float bh = Assets.CARD_H * BACK_SCALE;
				x = seat == 3 ? o.x - glyphs.width : o.x;
				y = o.y + bh / 2 + glyphs.height / 2;
			}
			f.getData().setScale(scale * Assets.SERIF_BASE);
			f.setColor(0f, 0f, 0f, 0.8f * a);
			f.draw(batch, s, x + 2, y - 2);
			Color c = made ? GOLD : Palette.RED;
			f.setColor(c.r, c.g, c.b, a);
			f.draw(batch, s, x, y);
		}
	}

	/** Seats from highest score to lowest; ties keep seat order. */
	private List<Integer> ranked() {
		List<Integer> order = new ArrayList<Integer>();
		for (int s = 0; s < 4; s++) {
			order.add(s);
		}
		Collections.sort(order, new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return match.score(b) - match.score(a);
			}
		});
		return order;
	}

	/** 1 for the lead; players on the same score share a place. */
	private int place(int seat) {
		int p = 1;
		for (int s = 0; s < 4; s++) {
			if (match.score(s) > match.score(seat)) {
				p++;
			}
		}
		return p;
	}

	private void drawHandResults() {
		float pw = 470, ph = 220;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		frame(assets.panel, px, py, pw, ph);
		BitmapFont f = ui(1.3f);
		f.setColor(Palette.CREAM);
		text(f, "Hand " + (match.handIndex() + 1) + " results", w / 2, py + ph - 14, true);

		BitmapFont t = ui(1.1f);
		float[] cols = { px + 22, px + 62, px + 185, px + 245, px + 310, px + 395 };
		String[] head = { "", "Player", "Bid", "Won", "Points", "Total" };
		float y = py + ph - 52;
		t.setColor(Palette.MUTED);
		for (int c = 0; c < cols.length; c++) {
			t.draw(batch, head[c], cols[c], y);
		}
		for (int seat : ranked()) {
			y -= 26;
			int pts = match.lastHandPoints(seat);
			t.setColor(Palette.MUTED);
			t.draw(batch, HighScores.ordinal(place(seat)), cols[0], y);
			t.setColor(match.madeBid(seat) ? Palette.CREAM : Palette.RED);
			String[] row = { NAMES[seat] + (match.madeBid(seat) ? "" : "  (set)"), String.valueOf(match.bid(seat)),
					String.valueOf(match.tricksWon(seat)), (pts > 0 ? "+" : "") + pts };
			for (int c = 0; c < row.length; c++) {
				t.draw(batch, row[c], cols[c + 1], y);
			}
			t.setColor(place(seat) == 1 ? GOLD : Palette.CREAM);
			t.draw(batch, String.valueOf(match.score(seat)), cols[5], y);
		}
		BitmapFont s = ui(1f);
		s.setColor(GOLD);
		text(s, "Tap to continue", w / 2, py + 26, true);
	}

	private void drawGameOver() {
		float pw = 420, ph = 306;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		frame(assets.panel, px, py, pw, ph);
		drawArt(assets.gameOverText, w / 2, py + ph - 50, 0.66f, 1f);

		List<Integer> leaders = match.leaders();
		StringBuilder title = new StringBuilder();
		for (int i = 0; i < leaders.size(); i++) {
			if (i > 0) {
				title.append(i == leaders.size() - 1 ? " and " : ", ");
			}
			title.append(NAMES[leaders.get(i)]);
		}
		if (leaders.size() > 1) {
			title.append(" tie!");
		} else {
			title.append(leaders.get(0) == HUMAN ? " win the game!" : " wins the game!");
		}
		BitmapFont f = ui(1.5f);
		f.setColor(GOLD);
		text(f, title.toString(), w / 2, py + ph - 94, true);

		BitmapFont t = ui(1.2f);
		float y = py + ph - 130;
		for (int s : ranked()) {
			Color c = place(s) == 1 ? GOLD : Palette.CREAM;
			t.setColor(Palette.MUTED);
			t.draw(batch, HighScores.ordinal(place(s)), px + 60, y);
			t.setColor(c);
			t.draw(batch, NAMES[s], px + 120, y);
			text(t, String.valueOf(match.score(s)), px + pw - 140, y, false);
			if (s == HUMAN && newHighScore >= 0) {
				BitmapFont n = ui(0.95f);
				n.setColor(Palette.GOLD_LIGHT);
				n.draw(batch, newHighScore == 0 ? "New best!" : "High score!", px + pw - 128, y - 2);
			}
			y -= 27;
		}
		float bw = 170, bh = 42, by = py + 16;
		playAgainButton = button("Play again", w / 2 - bw / 2 - 8, by, bw, bh, assets.button);
		highScoresButton = button("High scores", w / 2 + bw / 2 + 8, by, bw, bh, assets.buttonAlt);
	}

	private void drawMenuButton() {
		menuButton.set(w - 92, 10, 80, 38);
		frame(assets.buttonAlt, menuButton.x, menuButton.y, menuButton.width, menuButton.height);
		BitmapFont f = ui(1.2f);
		f.setColor(Palette.CREAM);
		text(f, "Menu", menuButton.x + menuButton.width / 2, menuButton.y + 27, true);
	}

	private void drawMenu() {
		rect(0, 0, w, h, Palette.BACKDROP);
		float bw = 260, bh = 56, gap = 14;
		float total = MENU.length * bh + (MENU.length - 1) * gap;
		float top = h / 2 + total / 2 + 20;
		BitmapFont title = ui(1.5f);
		title.setColor(GOLD);
		text(title, "Paused", w / 2, top + 36, true);
		menuItems.clear();
		BitmapFont f = ui(1.4f);
		for (int i = 0; i < MENU.length; i++) {
			Rectangle r = new Rectangle((w - bw) / 2, top - (i + 1) * bh - i * gap, bw, bh);
			menuItems.add(r);
			frame(i == 0 ? assets.button : assets.buttonAlt, r.x, r.y, r.width, r.height);
			f.setColor(Palette.CREAM);
			text(f, MENU[i], w / 2, r.y + bh / 2 + 10, true);
		}
	}

	private void drawHint() {
		BitmapFont f = ui(1.15f);
		glyphs.setText(f, hint);
		float pw = glyphs.width + 32, ph = 40;
		float px = (w - pw) / 2, py = 8 + Assets.CARD_H * HAND_SCALE + SELECTED_LIFT + 6;
		frame(assets.panel, px, py, pw, ph);
		f.setColor(GOLD);
		f.draw(batch, hint, px + 16, py + ph / 2 + 8);
	}

	// --------------------------------------------------------------- helpers

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

	/** A short description of what is on screen, for automated screenshots. */
	@Override
	public String stateTag() {
		int hand = match.handIndex() + 1;
		if (menuOpen) {
			return "menu:" + hand;
		}
		if (isShuffling()) {
			return "shuffle:" + hand;
		}
		if (isDealing()) {
			return "deal:" + hand;
		}
		if (trickPause > 0) {
			float elapsed = trickTotal - trickPause;
			return (elapsed >= TRICK_PAUSE ? "trickwon:" : elapsed > TRICK_PAUSE - COLLECT_TIME ? "collect:" : "trick:") + hand;
		}
		switch (match.phase()) {
		case BIDDING:
			if (match.currentSeat() == HUMAN) {
				return "bid:" + hand;
			}
			return "bidding:" + hand;
		case PLAYING:
			if (match.currentSeat() == HUMAN) {
				return (dragging ? "drag:" : "play:") + hand + ":" + match.currentTrick().size();
			}
			return "playing:" + hand;
		case HAND_OVER:
			return (handOverT < HAND_END_SHOW ? "handend:" : "handover:") + hand;
		default:
			return "gameover";
		}
	}
}
