package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Card;
import rules.Difficulty;
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

	/** Your "Bid tricks!"/"Set!" takes this long to glide beside your panel. */
	private static final float LABEL_DROP = 0.6f;
	/** How far a finger moves on a card before it counts as dragging it. */
	private static final float DRAG_START = 12f;

	private final GameOptions options;
	private final Random rng;

	final TableLayout table = new TableLayout();
	final TableOverlays overlays;
	private final TableFlow flow;
	private final CardArt art;
	private final PlayerPanel panel = new PlayerPanel(this);
	private final TrickView trick = new TrickView();
	private final Sparks sparks = new Sparks();
	private final Fireworks fireworks = new Fireworks();
	private final Motes motes = new Motes();
	/** Drives your seat in scripted screenshot runs; null in normal play. */
	private final Autoplay autoplay;
	private final TextureRegion strokeRegion;

	/** Game time, for animations; stops while paused. */
	private float time;
	private boolean menuOpen;
	/** Where your just-finished game landed in the high score table, or -1. */
	private int newHighScore = -1;
	/** The easiest level this game has been played at: its high score badge. */
	private Difficulty lowest;

	// touch: the card under your finger, and whether it's being dragged
	private Card pressed;
	private boolean dragging;
	private float dragX, dragY;

	/** Your hand in display order, and each card's tap area; rebuilt when drawn. */
	final List<Card> handOrder = new ArrayList<Card>();
	final List<Rectangle> handRects = new ArrayList<Rectangle>();

	/** A new game, or the saved game {@code restored} if not null, against computer players of the given difficulty. */
	public PlayScreen(OhHeckGame game, Match restored, Difficulty difficulty) {
		super(game);
		this.options = game.options;
		this.rng = options.seed == null ? new Random() : new Random(options.seed);
		this.art = new CardArt(batch, assets);
		this.overlays = new TableOverlays(this, table, art);
		this.autoplay = options.autoplayHuman ? new Autoplay(this, options) : null;
		this.strokeRegion = new TextureRegion(assets.stroke);
		this.flow = new TableFlow(new TableFlow.Listener() {
			@Override
			public void beforeDeal(Match m) {
				if (autoplay != null) {
					autoplay.skipHands(m);
				}
				trick.clear();
				clearTouch();
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
				if (flow.match().madeBid(HUMAN)) {
					sparks.burst(table.centreX(), table.centreY(), 110);
				}
			}

			@Override
			public void gameEnded() {
				Match m = flow.match();
				newHighScore = PlayScreen.this.game.recordHighScore(m.score(HUMAN), overlays.place(HUMAN), lowest);
				overlays.playAgainButton.set(0, 0, 0, 0); // not tappable until drawn
				overlays.highScoresButton.set(0, 0, 0, 0);
				if (m.leaders().contains(HUMAN)) { // you won (or tied for the win)
					sparks.burst(table.w / 2, table.h / 2 + 100, 160);
					fireworks.show(table.w, table.h);
				}
			}
		}, difficulty, rng);
		lowest = difficulty;
		if (restored == null) {
			newGame();
		} else {
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
		return menuOpen;
	}

	boolean isDragging() {
		return dragging;
	}

	public boolean isGameOver() {
		return flow.match().phase() == Match.Phase.GAME_OVER;
	}

	private void newGame() {
		Match m = Match.random(4, rng);
		overlays.setMatch(m);
		newHighScore = -1;
		flow.start(m);
	}

	private boolean humanCanPlay() {
		return !menuOpen && flow.yourTurnToPlay();
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
		TableState before = flow.state();
		flow.update(dt);
		if (before == TableState.TRICK && flow.state() != TableState.TRICK) {
			trick.clear(); // the winner has gathered the trick in
		}
		overlays.update(dt);
		sparks.step(dt);
		fireworks.update(dt);
		motes.update(dt, flow.trumpFaceUp(), table.trumpX(), table.trumpY(), table.trumpW(), table.trumpH());
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
		if (menuOpen) {
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
		return dragging || sparks.active() || fireworks.active() || trick.anyInFlight(time);
	}

	// ------------------------------------------------------------ input

	/** Notes that this game has also been played at level d (only an easier level changes its badge). */
	void playedAt(Difficulty d) {
		if (d.ordinal() < lowest.ordinal()) {
			lowest = d;
		}
	}

	@Override
	public void show() {
		super.show();
		// a difficulty changed in Options applies straight away
		Difficulty chosen = Difficulty.values()[game.settings.difficulty()];
		if (chosen != flow.difficulty()) {
			flow.setDifficulty(chosen, rng);
			playedAt(chosen);
			save();
		}
		game.audio.startMusic();
	}

	/** Saves the game so it survives Android closing the app; a finished game is forgotten. */
	private void save() {
		if (options.persist) {
			game.settings.saveGame(isGameOver() ? null : match().save(), lowest.ordinal());
		}
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

	@Override
	protected void touchDown(float x, float y) {
		super.touchDown(x, y);
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

	@Override
	protected void touchDragged(float x, float y) {
		if (pressed == null) {
			return;
		}
		if (!dragging && movedFrom(x, y) > DRAG_START && match().legalPlays(HUMAN).contains(pressed)) {
			dragging = true;
		}
		dragX = x;
		dragY = y;
	}

	@Override
	protected void touchUp(float x, float y) {
		if (dragging) {
			Card c = pressed;
			clearTouch();
			float handTop = handRects.isEmpty() ? 150 : handRects.get(0).y + handRects.get(0).height;
			if (y > handTop + 10 && humanCanPlay()) {
				// dropped on the table: play it from where it was let go
				humanPlay(c, x - TableLayout.handCardW() / 2, y - TableLayout.handCardH() / 2);
			}
			return; // otherwise it drops back into your hand
		}
		pressed = null;
		super.touchUp(x, y); // a tap, unless it was a swipe
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
		switch (flow.state()) {
		case SHUFFLE:
		case DEAL:
		case HAND_END:
			flow.skip(); // the shuffle and deal, or the hand-end message
			break;
		case TURN:
			if (match().currentSeat() == HUMAN) {
				if (match().phase() == Match.Phase.BIDDING) {
					tapBid(x, y);
				} else {
					tapCard(x, y);
				}
			}
			break;
		case TRICK:
			break;
		case RESULTS:
			flow.nextHand();
			break;
		case GAME_OVER:
			// only the buttons: a stray tap mustn't throw away the final scores
			if (overlays.highScoresButton.contains(x, y)) {
				game.showHighScores(newHighScore);
			} else if (overlays.playAgainButton.contains(x, y)) {
				game.newGame(); // a fresh table, at the difficulty now chosen in Options
			}
			break;
		}
	}

	private void tapBid(float x, float y) {
		for (int b = 0; b < overlays.bidButtons.size(); b++) {
			if (overlays.bidButtons.get(b).contains(x, y)) {
				if (!flow.bid(b)) {
					overlays.hint("As dealer you can't bid " + b + ": the bids would add up to " + match().handSize());
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
		if (!match().legalPlays(HUMAN).contains(c)) {
			overlays.hint("You must follow suit: play a " + singular(match().leadSuit()));
			return;
		}
		Rectangle r = handRects.get(i);
		humanPlay(c, r.x, r.y);
	}

	/** Plays your card, flying in from (fromX, fromY). */
	private void humanPlay(Card c, float fromX, float fromY) {
		clearTouch();
		if (flow.play(c)) {
			trick.fly(c, fromX, fromY, time);
		}
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
			game.showOptions(); // the menu is still open when you come back
			return;
		}
		menuOpen = false;
		if (i == 2) {
			game.newGame(); // a fresh table, at the difficulty now chosen in Options
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
		drawBidTracker();
		drawTrump();
		for (int seat = 0; seat < 4; seat++) {
			drawSeat(seat);
		}
		drawHand();
		trick.draw(batch, assets, art, table, match(), flow.state() == TableState.TRICK, flow.stateTime(), time, fade);
		if (flow.isDealing()) {
			flow.deal.draw(batch, assets, art, table);
		}
		fireworks.draw(batch, assets.glow); // behind the panels, so the scores stay readable
		if (!menuOpen) {
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
		int done = match().phase() == Match.Phase.GAME_OVER ? 8 : match().handIndex();
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

	// ---- the bid tracker, right of the tally: one card per trick in the hand

	private static final float TRACKER_GAP = 3, TRACKER_MAX_W = 16;

	/**
	 * How the table has bid: one card per trick in this hand, filled in as the bids add
	 * up, then a red card for each trick bid beyond the hand (everyone can't make it).
	 */
	private void drawBidTracker() {
		Match m = match();
		int n = m.handSize(), total = m.bidsSoFar();
		int filled = Math.min(total, n), count = Math.max(total, n);
		float x0 = 22 + 6 * TALLY_STEP + 16 + 22; // just past the tally's last mark
		float avail = table.panelX(2) - 14 - x0; // and clear of the top player's panel
		float iw = Math.min(TRACKER_MAX_W, avail / count - TRACKER_GAP), ih = iw * 18f / 14f;
		float y = h - 12 - TALLY_H / 2 - ih / 2; // level with the tally marks
		for (int i = 0; i < count; i++) {
			Texture t = i < filled ? assets.bidTaken : i < n ? assets.bidOpen : assets.bidOver;
			batch.draw(t, x0 + i * (iw + TRACKER_GAP), y, iw, ih);
		}
	}

	/** A soft-edged bar centred on (cx, cy), rotated by rot degrees. */
	private void stroke(float cx, float cy, float sw, float sh, float rot, Color c, float alpha) {
		batch.setColor(c.r, c.g, c.b, alpha);
		batch.draw(strokeRegion, cx - sw / 2, cy - sh / 2, sw / 2, sh / 2, sw, sh, 1f, 1f, rot);
		batch.setColor(Color.WHITE);
	}

	private void drawTrump() {
		if (flow.state() == TableState.SHUFFLE) {
			return; // the trump card is still in the deck
		}
		float x = table.trumpX(), y = table.trumpY(), cw = table.trumpW(), ch = table.trumpH();
		art.shadow(x, y, cw, ch, 4);
		float p = flow.isDealing() ? 0 : MathUtils.clamp(flow.trumpTime() / TableFlow.TRUMP_FLIP_TIME, 0, 1);
		art.flip(match().trumpCard(), x, y, cw, ch, p);
		motes.draw(batch, assets.glow);
	}

	/** A seat's panel and, for opponents, their face-down cards (while dealing, only those that have landed). */
	private void drawSeat(int seat) {
		boolean turn = flow.state() == TableState.TURN && match().currentSeat() == seat;
		panel.draw(NAMES[seat], table.panelX(seat), table.panelTop(seat), turn, match().dealer() == seat,
				match().score(seat), match().bid(seat), match().tricksWon(seat), flow.isDealing());
		if (seat == HUMAN) {
			return;
		}
		int total = match().hand(seat).size();
		int n = flow.isDealing() ? flow.deal.dealtTo(seat) : total;
		float y = table.fanOrigin(seat).y;
		for (int i = 0; i < n; i++) {
			batch.draw(assets.back(), table.fanCardX(seat, i, total), y, TableLayout.backW(), TableLayout.backH());
		}
	}

	private void drawHand() {
		handOrder.clear();
		handOrder.addAll(match().hand(HUMAN));
		Collections.sort(handOrder, DISPLAY_ORDER);
		int n = handOrder.size();
		sizeTo(handRects, n);
		if (n == 0) {
			return;
		}
		int shown = flow.isDealing() ? flow.deal.dealtTo(HUMAN) : n;
		float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
		table.layoutHand(n);
		boolean myTurn = humanCanPlay();
		List<Card> legal = myTurn ? match().legalPlays(HUMAN) : Collections.<Card>emptyList();
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
			art.flip(c, x, y, cw, ch, flow.isDealing() ? flow.deal.flipOfYourCard(i) : 1f);
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
