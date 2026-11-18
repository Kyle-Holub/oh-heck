package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Card;
import rules.Difficulty;
import rules.Match;
import rules.Player;
import rules.Rules;
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
	/** At the game's end, the final hand's cards and the trump thrown off the table; null until then. */
	private FallingCards tumble;
	/** When the trump goes, then each card of the final hand in the order played. */
	private static final float TOSS_TRUMP = 0.15f, TOSS_FIRST = 0.35f, TOSS_STEP = 0.04f;
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
	/** The difficulty this game was started at: its high score badge and statistics level. */
	private Difficulty level;
	/** Who sits in seats 1-3 ({@link Player} ids). */
	private int[] players;

	// touch: the card under your finger, and whether it's being dragged
	private Card pressed;
	private boolean dragging;
	private float dragX, dragY;

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
				int[] finals = new int[4];
				for (int s = 0; s < 4; s++) {
					finals[s] = m.score(s);
				}
				if (PlayScreen.this.game.stats().countGame(level, finals, Rules.numberOfHands(), players)) {
					PlayScreen.this.game.saveStats();
				}
				overlays.playAgainButton.set(0, 0, 0, 0); // not tappable until drawn
				overlays.highScoresButton.set(0, 0, 0, 0);
				tossTheTable(m);
				if (m.leaders().contains(HUMAN)) { // you won (or tied for the win)
					sparks.burst(table.w / 2, table.h / 2 + 100, 160);
					fireworks.show(table.w, table.h);
				}
			}
		});
		if (restored == null) {
			newGame();
		} else {
			level = savedLevel;
			seatPlayers(savedPlayers != null ? savedPlayers : Player.draw(level, rng));
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

	/** Throws the trump and every card of the final hand (from where each was played) off the table. */
	private void tossTheTable(Match m) {
		// (its own seed, so the throw never changes the game's random numbers)
		tumble = FallingCards.forTable(assets, new Random(m.score(HUMAN) * 31L + m.score(0)));
		float cx = table.w / 2, cy = table.h / 2;
		tumble.toss(m.trumpCard(), table.trumpX() + table.trumpW() / 2 - cx, table.trumpY() + table.trumpH() / 2 - cy,
				table.trumpW(), 0, TOSS_TRUMP);
		List<Card> played = m.playedThisHand();
		List<Integer> seats = m.playedThisHandSeats();
		for (int i = 0; i < played.size(); i++) {
			int seat = seats.get(i);
			Vector2 slot = table.trickSlot(seat);
			tumble.toss(played.get(i), slot.x + Assets.CARD_W / 2f - cx, slot.y + Assets.CARD_H / 2f - cy, Assets.CARD_W,
					TrickView.tilt(played.get(i), seat, m.handIndex()), TOSS_FIRST + i * TOSS_STEP);
		}
	}

	/** Whether the trump card has been thrown off the table (so it's no longer drawn in its place). */
	private boolean trumpGone() {
		return tumble != null && (tumble.anyThrown() || !tumble.active()); // (once all have fallen, none is left)
	}

	/** A new game: a table drawn for the difficulty now chosen in Options. */
	private void newGame() {
		level = Difficulty.values()[game.settings.difficulty()];
		seatPlayers(Player.draw(level, rng));
		tumble = null;
		game.stats().startGame();
		game.saveStats();
		Match m = Match.random(4, rng, game.settings.handOrder());
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
		if (tumble != null) {
			tumble.update(dt, table.w, table.h);
		}
		motes.update(dt, flow.trumpFaceUp() && !trumpGone(), table.trumpX(), table.trumpY(), table.trumpW(), table.trumpH());
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
		return dragging || sparks.active() || fireworks.active() || trick.anyInFlight(time)
				|| (tumble != null && tumble.active());
	}

	// ------------------------------------------------------------ input

	@Override
	public void show() {
		super.show();
		game.audio.startMusic();
	}

	/** The name shown for a seat: "You", or the computer player sitting there. */
	String name(int seat) {
		return seat == HUMAN ? "You" : Player.ALL.get(players[seat - 1]).name;
	}

	private void seatPlayers(int[] ids) {
		players = ids;
		flow.seat(ids, rng);
	}

	/** Saves the game so it survives Android closing the app; a finished game is forgotten. */
	private void save() {
		if (options.persist) {
			if (isGameOver()) {
				game.settings.clearSavedGame();
			} else {
				game.settings.saveGame(match().save(), level.ordinal(), players);
			}
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
			float cw = TableLayout.handCardW(), ch = TableLayout.handCardH();
			batch.draw(assets.face(pressed), dragX - cw / 2, dragY - ch / 2, cw, ch);
			if (pressed.suit() == match().trump()) {
				Glints.draw(batch, assets.glow, pressed, dragX - cw / 2, dragY - ch / 2, cw, ch, cw, 0, time, 1f);
			}
		}
		if (tumble != null) {
			tumble.draw(batch, table.w / 2, table.h / 2); // over the scores, briefly, as they fall away
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

	// ---- the bid tracker, under the tally: one card per trick in the hand

	private static final float TRACKER_GAP = 4, TRACKER_MAX_W = 21, TRACKER_BELOW_TALLY = 10;

	/**
	 * How the table has bid: one card per trick in this hand, filled in as the bids add
	 * up, then a red card for each trick bid beyond the hand (everyone can't make it).
	 */
	private void drawBidTracker() {
		Match m = match();
		int n = m.handSize(), total = m.bidsSoFar();
		int filled = Math.min(total, n), count = Math.max(total, n);
		float x0 = 22 - TALLY_W / 2; // lined up with the tally's first mark
		float avail = table.panelX(2) - 14 - x0; // and clear of the top player's panel
		float iw = Math.min(TRACKER_MAX_W, avail / count - TRACKER_GAP), ih = iw * 18f / 14f;
		float y = h - 12 - TALLY_H - TRACKER_BELOW_TALLY - ih;
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
		if (flow.state() == TableState.SHUFFLE || trumpGone()) {
			if (trumpGone()) {
				motes.draw(batch, assets.glow); // the last few still rising
			}
			return; // the trump card is still in the deck, or thrown off the table
		}
		float x = table.trumpX(), y = table.trumpY(), cw = table.trumpW(), ch = table.trumpH();
		art.shadow(x, y, cw, ch, 4);
		float p = flow.isDealing() ? 0 : MathUtils.clamp(flow.trumpTime() / TableFlow.TRUMP_FLIP_TIME, 0, 1);
		// face up, the trump is lit: a warm glow behind it, and the card itself brightened
		float lit = MathUtils.clamp((flow.trumpTime() - TableFlow.TRUMP_FLIP_TIME) / 0.6f, 0, 1);
		if (p >= 1 && lit > 0) {
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
			float gw = cw * 1.9f, gh = ch * 1.6f;
			batch.setColor(1f, 0.78f, 0.4f, 0.22f * lit);
			batch.draw(assets.glow, x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh);
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			batch.setColor(Color.WHITE);
		}
		art.flip(match().trumpCard(), x, y, cw, ch, p);
		if (p >= 1) {
			Glints.draw(batch, assets.glow, match().trumpCard(), x, y, cw, ch, cw, 0, time, lit);
		}
		if (p >= 1 && lit > 0) {
			// and a light on it in the screen effects, lifting the corner's vignette (after the bloom,
			// so the card brightens without glaring)
			game.post.light((x + cw / 2) / w, (y + ch / 2) / h, 0.3f, lit);
		}
		motes.draw(batch, assets.glow);
	}

	/** A seat's panel and, for opponents, their face-down cards (while dealing, only those that have landed). */
	private void drawSeat(int seat) {
		boolean turn = flow.state() == TableState.TURN && match().currentSeat() == seat;
		panel.draw(name(seat), table.panelX(seat), table.panelTop(seat), turn, match().dealer() == seat,
				match().score(seat), match().bid(seat), match().tricksWon(seat), flow.isDealing(), time);
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
			float flip = flow.isDealing() ? flow.deal.flipOfYourCard(i) : 1f;
			art.flip(c, x, y, cw, ch, flip);
			if (flip >= 1 && flow.trumpFaceUp() && c.suit() == match().trump()) {
				Glints.draw(batch, assets.glow, c, x, y, cw, ch, i == n - 1 ? cw : table.handGap, 0, time,
						myTurn && !canPlay ? 0.5f : 1f);
			}
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
