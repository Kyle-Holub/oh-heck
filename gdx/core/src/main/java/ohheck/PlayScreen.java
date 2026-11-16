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
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

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
 * Playing a card: tap it to lift it, then tap it again (or drag it up onto
 * the table). Bidding: tap a number, then tap it again to confirm.
 */
public class PlayScreen extends ScreenAdapter {

	private static final int HUMAN = 0;
	private static final String[] NAMES = { "You", "Tom", "Max", "Dan" };

	private static final float WORLD_W = 800, WORLD_H = 480;
	private static final float HAND_SCALE = 1.2f, BACK_SCALE = 0.55f, TRUMP_SCALE = 0.8f;
	private static final float LINE = 20f;

	// timings, in seconds at normal speed
	private static final float AI_DELAY = 0.8f;
	private static final float TRICK_PAUSE = 1.7f, COLLECT_TIME = 0.45f;
	private static final float PLAY_FLIGHT = 0.25f;
	private static final float DEAL_INTERVAL = 0.06f, DEAL_FLIGHT = 0.3f;
	private static final float HINT_TIME = 2.2f;
	private static final float AUTOPLAY_DELAY = 1.2f, AUTOPLAY_CONFIRM = 0.7f, HAND_OVER_AUTO = 3f;

	// touch
	private static final float DRAG_START = 12f, LIFT = 22f, SELECTED_LIFT = 44f;

	private static final Color PANEL = new Color(0f, 0f, 0f, 0.72f);
	private static final Color BUTTON = new Color(0.16f, 0.42f, 0.24f, 1f);
	private static final Color BUTTON_OFF = new Color(0.3f, 0.3f, 0.3f, 1f);
	private static final Color GOLD = new Color(1f, 0.82f, 0.25f, 1f);
	private static final Color DIM = new Color(0.55f, 0.55f, 0.55f, 1f);
	private static final Color MENU_BACKDROP = new Color(0f, 0f, 0f, 0.78f);
	private static final Color MENU_BUTTON = new Color(0.2f, 0.2f, 0.2f, 1f);
	private static final Color SET_RED = new Color(1f, 0.55f, 0.5f, 1f);

	private final Assets assets;
	private final GameOptions options;
	private final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	private final SpriteBatch batch = new SpriteBatch();
	private final GlyphLayout glyphs = new GlyphLayout();
	private final Vector2 tmp = new Vector2();
	private final Bot bot = new Bot();
	private final Random rng;

	private Match match;
	private float time; // game time, for animations
	private float timer; // computer turn / autoplay timer
	private float trickPause;
	private String lastTrickText = "";
	private String hint = "";
	private float hintTime;
	private boolean menuOpen;

	// dealing animation
	private int dealtHand = -1;
	private float dealT, dealDuration;

	// card flights into the trick: start position and start time per card
	private final Map<Card, float[]> flights = new HashMap<Card, float[]>();

	// human selection and dragging
	private Card selected;
	private int pendingBid = -1;
	private Card pressed;
	private float downX, downY, dragX, dragY;
	private boolean dragging;

	// autoplay (screenshots): the move chosen and whether it's been selected yet
	private boolean autoSelected;
	private boolean menuDemoDone;

	// layout, rebuilt every frame while drawing
	private float w = WORLD_W, h = WORLD_H;
	private final List<Rectangle> bidButtons = new ArrayList<Rectangle>();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>();
	private final List<Card> handOrder = new ArrayList<Card>();
	private final Rectangle menuButton = new Rectangle();
	private final List<Rectangle> menuItems = new ArrayList<Rectangle>();
	private static final String[] MENU = { "Resume", "New game", "Quit" };

	public PlayScreen(Assets assets, GameOptions options) {
		this.assets = assets;
		this.options = options;
		this.rng = options.seed == null ? new Random() : new Random(options.seed);
		newGame();
	}

	private void newGame() {
		match = new Match(4, rng);
		timer = 0;
		trickPause = 0;
		lastTrickText = "";
		dealtHand = -1;
		flights.clear();
		clearSelection();
	}

	@Override
	public void show() {
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
					menuOpen = !menuOpen;
					clearSelection();
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
	private void tap(float x, float y) {
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
			dealT = dealDuration; // tap skips the deal
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
			match.nextHand();
			timer = 0;
			break;
		case GAME_OVER:
			newGame();
			break;
		}
	}

	private void tapBid(float x, float y) {
		for (int b = 0; b < bidButtons.size(); b++) {
			if (bidButtons.get(b).contains(x, y)) {
				if (!match.isLegalBid(b)) {
					showHint("As dealer you can't bid " + b + ": the bids would add up to " + match.handSize());
				} else if (pendingBid == b) {
					match.placeBid(HUMAN, b);
					clearSelection();
					timer = 0;
				} else {
					pendingBid = b;
				}
				return;
			}
		}
		pendingBid = -1;
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
		if (c == selected) {
			Rectangle r = handRects.get(i);
			humanPlay(c, r.x, r.y);
		} else {
			selected = c;
		}
	}

	private void menuChoice(int i) {
		menuOpen = false;
		if (i == 1) {
			newGame();
		} else if (i == 2) {
			Gdx.app.exit();
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
		pendingBid = -1;
		pressed = null;
		dragging = false;
		autoSelected = false;
	}

	private void showHint(String s) {
		hint = s;
		hintTime = HINT_TIME;
	}

	// ---------------------------------------------------------------- update

	@Override
	public void render(float delta) {
		update(Math.min(delta, 0.1f) * options.speed);
		draw();
		if (options.frameListener != null) {
			options.frameListener.onFrame(stateTag());
		}
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

		// a new hand starts with the deal
		if (match.phase() == Match.Phase.BIDDING && match.handIndex() != dealtHand) {
			dealtHand = match.handIndex();
			dealT = 0;
			dealDuration = 4 * match.handSize() * DEAL_INTERVAL + DEAL_FLIGHT + 0.15f;
			flights.clear();
		}
		if (isDealing()) {
			dealT += dt;
			return;
		}

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
			if (options.autoplayHuman) {
				timer += dt;
				if (timer >= HAND_OVER_AUTO) {
					timer = 0;
					tap(w / 2, h / 2);
				}
			}
			break;
		default:
			break;
		}
	}

	private void botMove(int seat) {
		if (match.phase() == Match.Phase.BIDDING) {
			match.placeBid(seat, bot.chooseBid(match.hand(seat), match.trump(), match.bidsSoFar()));
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
		timer = 0;
		if (winner >= 0) {
			List<Card> trick = match.lastTrick();
			Card best = trick.get(match.lastTrickSeats().indexOf(winner));
			boolean trumped = best.suit() == match.trump() && trick.get(0).suit() != match.trump();
			lastTrickText = NAMES[winner] + (trumped ? " trumped" : " won") + " with the " + pretty(best);
			trickPause = TRICK_PAUSE;
		}
	}

	/**
	 * Plays the human seat for automated screenshots, through the same taps a
	 * person would make: select the bot's choice, wait, then tap it again.
	 */
	private void autoplayHuman(float dt) {
		timer += dt;
		// once per game, open and close the menu to capture it
		if (!menuDemoDone && match.handIndex() == 5 && match.phase() == Match.Phase.BIDDING) {
			menuDemoDone = true;
			timer = 0;
			tapCentre(menuButton);
			return;
		}
		if (!autoSelected && timer >= AUTOPLAY_DELAY) {
			autoSelected = true;
			timer = 0;
			autoTap();
		} else if (autoSelected && timer >= AUTOPLAY_CONFIRM) {
			timer = 0;
			autoTap();
			autoSelected = false;
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

	private boolean isDealing() {
		return dealtHand == match.handIndex() && match.phase() == Match.Phase.BIDDING && dealT < dealDuration;
	}

	// ------------------------------------------------------------------ draw

	private void draw() {
		ScreenUtils.clear(Color.BLACK);
		viewport.apply();
		batch.setProjectionMatrix(viewport.getCamera().combined);
		w = viewport.getWorldWidth();
		h = viewport.getWorldHeight();

		batch.begin();
		batch.setColor(Color.WHITE);
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
				drawHandResults();
			} else if (match.phase() == Match.Phase.GAME_OVER) {
				drawGameOver();
			}
		}
		if (dragging && selected != null) {
			float cw = Assets.CARD_W * HAND_SCALE, ch = Assets.CARD_H * HAND_SCALE;
			batch.draw(assets.face(selected), dragX - cw / 2, dragY - ch / 2, cw, ch);
		}
		if (hintTime > 0 && !menuOpen) {
			drawHint();
		}
		if (menuOpen) {
			drawMenu();
		}
		batch.end();
	}

	private void drawInfo() {
		BitmapFont f = font(1.1f);
		f.setColor(Color.WHITE);
		float y = h - 10;
		if (match.phase() != Match.Phase.GAME_OVER) {
			f.draw(batch, "Hand " + (match.handIndex() + 1) + " of 8  -  " + match.handSize()
					+ (match.handSize() == 1 ? " card" : " cards"), 12, y);
		}
		f.setColor(GOLD);
		f.draw(batch, statusText(), 12, y - LINE);
	}

	private String statusText() {
		if (isDealing()) {
			return NAMES[match.dealer()] + (match.dealer() == HUMAN ? " deal" : " deals") + "  (tap to skip)";
		}
		if (trickPause > 0) {
			return lastTrickText;
		}
		int s = match.currentSeat();
		switch (match.phase()) {
		case BIDDING:
			return s == HUMAN ? "Your bid" : NAMES[s] + " is bidding...";
		case PLAYING:
			if (s != HUMAN) {
				return NAMES[s] + " is thinking...";
			}
			if (selected != null) {
				return "Tap the card again to play it";
			}
			return match.currentTrick().isEmpty() ? "Your lead: tap a card" : "Your turn: tap a card";
		case HAND_OVER:
			return "Hand over";
		default:
			return "Game over";
		}
	}

	private void drawTrump() {
		float cw = Assets.CARD_W * TRUMP_SCALE, ch = Assets.CARD_H * TRUMP_SCALE;
		BitmapFont f = font(1f);
		f.setColor(Color.WHITE);
		text(f, "Trump", w - 12 - cw / 2, h - 10, true);
		if (isDealing()) {
			batch.draw(assets.cardBack, w - 12 - cw, h - 30 - ch, cw, ch);
		} else {
			batch.draw(assets.face(match.trumpCard()), w - 12 - cw, h - 30 - ch, cw, ch);
		}
	}

	/** Top-left corner of an opponent's name tag. */
	private float tagX(int seat) {
		return seat == 1 ? 12 : seat == 2 ? w / 2 - 70 : w - 152;
	}

	private float tagTop(int seat) {
		return seat == 2 ? h - 8 : h / 2 + 70;
	}

	private void drawOpponent(int seat) {
		float x = tagX(seat), top = tagTop(seat);
		BitmapFont f = font(1.1f);
		f.setColor(isTurn(seat) ? GOLD : Color.WHITE);
		f.draw(batch, NAMES[seat] + (match.dealer() == seat ? "  (dealer)" : ""), x, top);
		f.setColor(Color.WHITE);
		f.draw(batch, bidLine(seat), x, top - LINE);
		f.draw(batch, "Score " + match.score(seat), x, top - 2 * LINE);

		int n = isDealing() ? dealtTo(seat) : match.hand(seat).size();
		float bw = Assets.CARD_W * BACK_SCALE, bh = Assets.CARD_H * BACK_SCALE;
		float y = top - 3 * LINE - 8 - bh;
		for (int i = 0; i < n; i++) {
			float cx = seat == 3 ? x + 140 - bw - (n - 1 - i) * 12 : x + i * 12;
			batch.draw(assets.cardBack, cx, y, bw, bh);
		}
	}

	/** Centre of where a seat's cards are, for cards flying to and from it. */
	private Vector2 seatAnchor(int seat) {
		if (seat == HUMAN) {
			return new Vector2(w / 2, 70);
		}
		float bh = Assets.CARD_H * BACK_SCALE;
		float y = tagTop(seat) - 3 * LINE - 8 - bh / 2;
		float x = seat == 1 ? 12 + 40 : seat == 2 ? w / 2 : w - 52;
		return new Vector2(x, y);
	}

	private boolean isTurn(int seat) {
		return match.currentSeat() == seat && trickPause <= 0 && !isDealing()
				&& (match.phase() == Match.Phase.BIDDING || match.phase() == Match.Phase.PLAYING);
	}

	private void drawHumanTag() {
		BitmapFont f = font(1.1f);
		float x = 12, y = 12 + 3 * LINE;
		f.setColor(isTurn(HUMAN) ? GOLD : Color.WHITE);
		f.draw(batch, "You" + (match.dealer() == HUMAN ? "  (dealer)" : ""), x, y);
		f.setColor(Color.WHITE);
		f.draw(batch, bidLine(HUMAN), x, y - LINE);
		f.draw(batch, "Score " + match.score(HUMAN), x, y - 2 * LINE);
	}

	private String bidLine(int seat) {
		Integer b = match.bid(seat);
		return "Bid " + (b == null ? "-" : b.toString()) + "   Won " + match.tricksWon(seat);
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
		float maxWidth = w - 300;
		float gap = n == 1 ? 0 : Math.min(cw + 6, (maxWidth - cw) / (n - 1));
		float start = (w - (gap * (n - 1) + cw)) / 2;

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
			if (c == selected) {
				rect(x - 3, y - 3, cw + 6, ch + 6, GOLD);
			}
			batch.setColor(myTurn && !canPlay ? DIM : Color.WHITE);
			batch.draw(assets.face(c), x, y, cw, ch);
		}
		batch.setColor(Color.WHITE);
	}

	/** Where a seat's card sits in the middle of the table. */
	private Vector2 trickSlot(int seat) {
		float cx = w / 2, cy = h / 2 + 5;
		float x = cx - Assets.CARD_W / 2f + (seat == 1 ? -95 : seat == 3 ? 95 : 0);
		float y = cy - Assets.CARD_H / 2f + (seat == HUMAN ? -50 : seat == 2 ? 50 : 0);
		return new Vector2(x, y);
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
			collect = MathUtils.clamp(1 - trickPause / COLLECT_TIME, 0, 1);
		} else {
			cards = match.currentTrick();
			seats = match.currentTrickSeats();
		}
		float cw = Assets.CARD_W, ch = Assets.CARD_H;
		Vector2 sink = winner >= 0 ? seatAnchor(winner) : null;
		for (int i = 0; i < cards.size(); i++) {
			Card c = cards.get(i);
			Vector2 slot = trickSlot(seats.get(i));
			float x = slot.x, y = slot.y, scale = 1f;
			float[] f = flights.get(c);
			if (f != null) {
				float p = Interpolation.pow2Out.apply(MathUtils.clamp((time - f[2]) / PLAY_FLIGHT, 0, 1));
				x = MathUtils.lerp(f[0], slot.x, p);
				y = MathUtils.lerp(f[1], slot.y, p);
			}
			if (collect > 0) {
				float p = Interpolation.pow2In.apply(collect);
				x = MathUtils.lerp(x, sink.x - cw / 4, p);
				y = MathUtils.lerp(y, sink.y - ch / 4, p);
				scale = MathUtils.lerp(1f, 0.5f, p);
			} else if (seats.get(i) == winner) {
				rect(x - 4, y - 4, cw + 8, ch + 8, GOLD);
			}
			batch.draw(assets.face(c), x, y, cw * scale, ch * scale);
		}
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
		int n = match.handSize();
		Vector2 from = seatAnchor(match.dealer());
		float bw = Assets.CARD_W * 0.7f, bh = Assets.CARD_H * 0.7f;
		for (int k = 0; k < 4 * n; k++) {
			float start = k * DEAL_INTERVAL;
			if (dealT < start || dealT >= start + DEAL_FLIGHT) {
				continue;
			}
			int seat = (match.dealer() + 1 + k) % 4;
			Vector2 to = seatAnchor(seat);
			float p = Interpolation.pow2Out.apply((dealT - start) / DEAL_FLIGHT);
			batch.draw(assets.cardBack, MathUtils.lerp(from.x, to.x, p) - bw / 2, MathUtils.lerp(from.y, to.y, p) - bh / 2,
					bw, bh);
		}
	}

	private void drawBidPanel() {
		int n = match.handSize();
		float bs = 58, gap = 8;
		float rowW = (n + 1) * bs + n * gap;
		float pw = Math.max(rowW + 40, 440), ph = 160;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		rect(px, py, pw, ph, PANEL);

		BitmapFont f = font(1.3f);
		f.setColor(pendingBid >= 0 ? GOLD : Color.WHITE);
		text(f, pendingBid >= 0 ? "Tap " + pendingBid + " again to bid " + pendingBid : "How many tricks will you take?",
				w / 2, py + ph - 14, true);
		BitmapFont s = font(1f);
		s.setColor(Color.LIGHT_GRAY);
		int forbidden = match.forbiddenBid();
		String note = "Bids so far: " + match.bidsSoFar() + " of " + n
				+ (forbidden >= 0 ? "   -   as dealer you can't bid " + forbidden : "");
		text(s, note, w / 2, py + ph - 44, true);

		bidButtons.clear();
		float bx = (w - rowW) / 2, by = py + 20;
		BitmapFont bf = font(1.7f);
		for (int b = 0; b <= n; b++) {
			Rectangle r = new Rectangle(bx + b * (bs + gap), by, bs, bs);
			bidButtons.add(r);
			boolean ok = match.isLegalBid(b);
			if (b == pendingBid) {
				rect(r.x - 3, r.y - 3, r.width + 6, r.height + 6, GOLD);
			}
			rect(r.x, r.y, r.width, r.height, ok ? BUTTON : BUTTON_OFF);
			bf.setColor(ok ? Color.WHITE : DIM);
			text(bf, String.valueOf(b), r.x + bs / 2, r.y + bs / 2 + 12, true);
		}
	}

	private void drawHandResults() {
		float pw = 470, ph = 220;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		rect(px, py, pw, ph, PANEL);
		BitmapFont f = font(1.3f);
		f.setColor(Color.WHITE);
		text(f, "Hand " + (match.handIndex() + 1) + " results", w / 2, py + ph - 14, true);

		BitmapFont t = font(1.1f);
		float[] cols = { px + 24, px + 150, px + 215, px + 290, px + 380 };
		String[] head = { "Player", "Bid", "Won", "Points", "Total" };
		float y = py + ph - 52;
		t.setColor(Color.LIGHT_GRAY);
		for (int c = 0; c < cols.length; c++) {
			t.draw(batch, head[c], cols[c], y);
		}
		for (int seat = 0; seat < 4; seat++) {
			y -= 26;
			t.setColor(match.madeBid(seat) ? Color.WHITE : SET_RED);
			int pts = match.lastHandPoints(seat);
			String[] row = { NAMES[seat] + (match.madeBid(seat) ? "" : "  (set)"), String.valueOf(match.bid(seat)),
					String.valueOf(match.tricksWon(seat)), (pts > 0 ? "+" : "") + pts, String.valueOf(match.score(seat)) };
			for (int c = 0; c < cols.length; c++) {
				t.draw(batch, row[c], cols[c], y);
			}
		}
		BitmapFont s = font(1f);
		s.setColor(GOLD);
		text(s, "Tap to continue", w / 2, py + 26, true);
	}

	private void drawGameOver() {
		float pw = 420, ph = 230;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		rect(px, py, pw, ph, PANEL);

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
		BitmapFont f = font(1.5f);
		f.setColor(GOLD);
		text(f, title.toString(), w / 2, py + ph - 16, true);

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
		BitmapFont t = font(1.2f);
		float y = py + ph - 66;
		for (int s : order) {
			t.setColor(Color.WHITE);
			t.draw(batch, NAMES[s], px + 110, y);
			text(t, String.valueOf(match.score(s)), px + pw - 120, y, false);
			y -= 28;
		}
		BitmapFont s = font(1f);
		s.setColor(GOLD);
		text(s, "Tap to play again", w / 2, py + 26, true);
	}

	private void drawMenuButton() {
		menuButton.set(w - 92, 10, 80, 38);
		rect(menuButton.x, menuButton.y, menuButton.width, menuButton.height, PANEL);
		BitmapFont f = font(1.2f);
		f.setColor(Color.WHITE);
		text(f, "Menu", menuButton.x + menuButton.width / 2, menuButton.y + 27, true);
	}

	private void drawMenu() {
		rect(0, 0, w, h, MENU_BACKDROP);
		float bw = 260, bh = 56, gap = 14;
		float total = MENU.length * bh + (MENU.length - 1) * gap;
		float top = h / 2 + total / 2 + 20;
		BitmapFont title = font(1.5f);
		title.setColor(GOLD);
		text(title, "Paused", w / 2, top + 36, true);
		menuItems.clear();
		BitmapFont f = font(1.4f);
		for (int i = 0; i < MENU.length; i++) {
			Rectangle r = new Rectangle((w - bw) / 2, top - (i + 1) * bh - i * gap, bw, bh);
			menuItems.add(r);
			rect(r.x, r.y, r.width, r.height, i == 0 ? BUTTON : MENU_BUTTON);
			f.setColor(Color.WHITE);
			text(f, MENU[i], w / 2, r.y + bh / 2 + 10, true);
		}
	}

	private void drawHint() {
		BitmapFont f = font(1.15f);
		glyphs.setText(f, hint);
		float pw = glyphs.width + 32, ph = 40;
		float px = (w - pw) / 2, py = 8 + Assets.CARD_H * HAND_SCALE + SELECTED_LIFT + 6;
		rect(px, py, pw, ph, PANEL);
		f.setColor(GOLD);
		f.draw(batch, hint, px + 16, py + ph / 2 + 8);
	}

	// --------------------------------------------------------------- helpers

	private BitmapFont font(float scale) {
		assets.font.getData().setScale(scale);
		return assets.font;
	}

	/** Draws text centred on x, or right-aligned to x when centre is false. */
	private void text(BitmapFont f, String s, float x, float y, boolean centre) {
		glyphs.setText(f, s);
		f.draw(batch, s, centre ? x - glyphs.width / 2 : x - glyphs.width, y);
	}

	private void rect(float x, float y, float rw, float rh, Color c) {
		batch.setColor(c);
		batch.draw(assets.white, x, y, rw, rh);
		batch.setColor(Color.WHITE);
	}

	/** "Ace of Spades" */
	static String pretty(Card c) {
		return cap(c.rank().name()) + " of " + cap(c.suit().name());
	}

	/** "heart", "spade"... */
	private static String singular(Suit s) {
		String n = s.name().toLowerCase();
		return n.substring(0, n.length() - 1);
	}

	private static String cap(String s) {
		return s.charAt(0) + s.substring(1).toLowerCase();
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
	public String stateTag() {
		int hand = match.handIndex() + 1;
		if (menuOpen) {
			return "menu:" + hand;
		}
		if (isDealing()) {
			return "deal:" + hand;
		}
		if (trickPause > 0) {
			return (trickPause < COLLECT_TIME ? "collect:" : "trick:") + hand;
		}
		switch (match.phase()) {
		case BIDDING:
			if (match.currentSeat() == HUMAN) {
				return (pendingBid >= 0 ? "bidconfirm:" : "bid:") + hand;
			}
			return "bidding:" + hand;
		case PLAYING:
			if (match.currentSeat() == HUMAN) {
				return (selected != null ? "selected:" : "play:") + hand + ":" + match.currentTrick().size();
			}
			return "playing:" + hand;
		case HAND_OVER:
			return "handover:" + hand;
		default:
			return "gameover";
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void dispose() {
		batch.dispose();
	}
}
