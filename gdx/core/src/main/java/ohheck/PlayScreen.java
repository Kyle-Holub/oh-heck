package ohheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
 * top, right). All rules come from {@link Match}; this class only draws the
 * state, runs the computer players on timers and turns taps into moves.
 */
public class PlayScreen extends ScreenAdapter {

	private static final int HUMAN = 0;
	private static final String[] NAMES = { "You", "Tom", "Max", "Dan" };

	private static final float WORLD_W = 800, WORLD_H = 480;
	private static final float HAND_SCALE = 1.2f, BACK_SCALE = 0.55f, TRUMP_SCALE = 0.8f;
	private static final float AI_DELAY = 0.8f, AUTOPLAY_DELAY = 1.6f, TRICK_PAUSE = 1.6f, HAND_OVER_AUTO = 3f;
	private static final float LINE = 20f;

	private static final Color PANEL = new Color(0f, 0f, 0f, 0.72f);
	private static final Color BUTTON = new Color(0.16f, 0.42f, 0.24f, 1f);
	private static final Color BUTTON_OFF = new Color(0.3f, 0.3f, 0.3f, 1f);
	private static final Color GOLD = new Color(1f, 0.82f, 0.25f, 1f);
	private static final Color DIM = new Color(0.55f, 0.55f, 0.55f, 1f);

	private final Assets assets;
	private final GameOptions options;
	private final ExtendViewport viewport = new ExtendViewport(WORLD_W, WORLD_H);
	private final SpriteBatch batch = new SpriteBatch();
	private final GlyphLayout glyphs = new GlyphLayout();
	private final Vector2 touch = new Vector2();
	private final Bot bot = new Bot();
	private final Random rng;

	private Match match;
	private float timer;
	private float trickPause;
	private String lastTrickText = "";

	// hit areas, rebuilt every frame while drawing
	private final List<Rectangle> bidButtons = new ArrayList<Rectangle>();
	private final List<Rectangle> handRects = new ArrayList<Rectangle>();
	private final List<Card> handOrder = new ArrayList<Card>();

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
		boolean tapped = Gdx.input.justTouched();
		if (tapped) {
			touch.set(Gdx.input.getX(), Gdx.input.getY());
			viewport.unproject(touch);
		}

		if (trickPause > 0) {
			trickPause -= dt;
			return;
		}

		switch (match.phase()) {
		case BIDDING:
		case PLAYING:
			if (match.currentSeat() == HUMAN && !options.autoplayHuman) {
				if (tapped) {
					handleHumanTap(touch.x, touch.y);
				}
			} else {
				timer += dt;
				float wait = match.currentSeat() == HUMAN ? AUTOPLAY_DELAY : AI_DELAY;
				if (timer >= wait) {
					timer = 0;
					botMove(match.currentSeat());
				}
			}
			break;
		case HAND_OVER:
			timer += dt;
			if (tapped || (options.autoplayHuman && timer >= HAND_OVER_AUTO)) {
				timer = 0;
				match.nextHand();
			}
			break;
		case GAME_OVER:
			if (tapped && !options.autoplayHuman) {
				newGame();
			}
			break;
		}
	}

	private void botMove(int seat) {
		if (match.phase() == Match.Phase.BIDDING) {
			match.placeBid(seat, bot.chooseBid(match.hand(seat), match.trump(), match.bidsSoFar()));
		} else {
			Card c = bot.choosePlay(match.hand(seat), match.currentTrick(), match.trump(), match.bid(seat),
					match.tricksWon(seat));
			play(seat, c);
		}
	}

	private void play(int seat, Card c) {
		int winner = match.playCard(seat, c);
		if (winner >= 0) {
			List<Card> trick = match.lastTrick();
			Card best = trick.get(match.lastTrickSeats().indexOf(winner));
			boolean trumped = best.suit() == match.trump() && trick.get(0).suit() != match.trump();
			lastTrickText = NAMES[winner] + (trumped ? " trumped" : " won") + " with the " + pretty(best);
			trickPause = TRICK_PAUSE;
			timer = 0;
		}
	}

	private void handleHumanTap(float x, float y) {
		if (match.phase() == Match.Phase.BIDDING) {
			for (int b = 0; b < bidButtons.size(); b++) {
				if (bidButtons.get(b).contains(x, y) && match.isLegalBid(b)) {
					match.placeBid(HUMAN, b);
					timer = 0;
					return;
				}
			}
		} else {
			List<Card> legal = match.legalPlays(HUMAN);
			// topmost card first: later cards are drawn over earlier ones
			for (int i = handRects.size() - 1; i >= 0; i--) {
				if (handRects.get(i).contains(x, y)) {
					Card c = handOrder.get(i);
					if (legal.contains(c)) {
						play(HUMAN, c);
					}
					return;
				}
			}
		}
	}

	// ------------------------------------------------------------------ draw

	private void draw() {
		ScreenUtils.clear(Color.BLACK);
		viewport.apply();
		batch.setProjectionMatrix(viewport.getCamera().combined);
		float w = viewport.getWorldWidth(), h = viewport.getWorldHeight();

		batch.begin();
		batch.setColor(Color.WHITE);
		batch.draw(assets.table, 0, 0, w, h);

		drawInfo(w, h);
		drawTrump(w, h);
		drawOpponent(1, 12, h / 2 + 70, false);
		drawOpponent(2, w / 2 - 70, h - 8, false);
		drawOpponent(3, w - 152, h / 2 + 70, true);
		drawHumanTag();
		drawHand(w);
		drawTrick(w, h);

		if (trickPause <= 0) {
			if (match.phase() == Match.Phase.BIDDING && match.currentSeat() == HUMAN) {
				drawBidPanel(w, h);
			} else if (match.phase() == Match.Phase.HAND_OVER) {
				drawHandResults(w, h);
			} else if (match.phase() == Match.Phase.GAME_OVER) {
				drawGameOver(w, h);
			}
		}
		batch.end();
	}

	private void drawInfo(float w, float h) {
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
		if (trickPause > 0) {
			return lastTrickText;
		}
		int s = match.currentSeat();
		switch (match.phase()) {
		case BIDDING:
			return s == HUMAN ? "Your bid" : NAMES[s] + " is bidding...";
		case PLAYING:
			return s == HUMAN ? (match.currentTrick().isEmpty() ? "Your lead: tap a card" : "Your turn: tap a card")
					: NAMES[s] + " is thinking...";
		case HAND_OVER:
			return "Hand over";
		default:
			return "Game over";
		}
	}

	private void drawTrump(float w, float h) {
		float cw = Assets.CARD_W * TRUMP_SCALE, ch = Assets.CARD_H * TRUMP_SCALE;
		BitmapFont f = font(1f);
		f.setColor(Color.WHITE);
		text(f, "Trump", w - 12 - cw / 2, h - 10, true);
		batch.draw(assets.face(match.trumpCard()), w - 12 - cw, h - 30 - ch, cw, ch);
	}

	private void drawOpponent(int seat, float x, float top, boolean alignRight) {
		BitmapFont f = font(1.1f);
		f.setColor(match.currentSeat() == seat && trickPause <= 0 && isTurnPhase() ? GOLD : Color.WHITE);
		String name = NAMES[seat] + (match.dealer() == seat ? "  (dealer)" : "");
		f.draw(batch, name, x, top);
		f.setColor(Color.WHITE);
		f.draw(batch, bidLine(seat), x, top - LINE);
		f.draw(batch, "Score " + match.score(seat), x, top - 2 * LINE);

		// face-down cards
		int n = match.hand(seat).size();
		float bw = Assets.CARD_W * BACK_SCALE, bh = Assets.CARD_H * BACK_SCALE;
		float y = top - 3 * LINE - 8 - bh;
		for (int i = 0; i < n; i++) {
			float cx = alignRight ? x + 140 - bw - (n - 1 - i) * 12 : x + i * 12;
			batch.draw(assets.cardBack, cx, y, bw, bh);
		}
	}

	private void drawHumanTag() {
		BitmapFont f = font(1.1f);
		float x = 12, y = 12 + 3 * LINE;
		f.setColor(match.currentSeat() == HUMAN && trickPause <= 0 && isTurnPhase() ? GOLD : Color.WHITE);
		f.draw(batch, "You" + (match.dealer() == HUMAN ? "  (dealer)" : ""), x, y);
		f.setColor(Color.WHITE);
		f.draw(batch, bidLine(HUMAN), x, y - LINE);
		f.draw(batch, "Score " + match.score(HUMAN), x, y - 2 * LINE);
	}

	private String bidLine(int seat) {
		Integer b = match.bid(seat);
		return "Bid " + (b == null ? "-" : b.toString()) + "   Won " + match.tricksWon(seat);
	}

	private void drawHand(float w) {
		handOrder.clear();
		handOrder.addAll(match.hand(HUMAN));
		Collections.sort(handOrder, DISPLAY_ORDER);
		handRects.clear();
		int n = handOrder.size();
		if (n == 0) {
			return;
		}
		float cw = Assets.CARD_W * HAND_SCALE, ch = Assets.CARD_H * HAND_SCALE;
		float maxWidth = w - 300;
		float gap = n == 1 ? 0 : Math.min(cw + 6, (maxWidth - cw) / (n - 1));
		float start = (w - (gap * (n - 1) + cw)) / 2;

		boolean myTurn = match.phase() == Match.Phase.PLAYING && match.currentSeat() == HUMAN && trickPause <= 0;
		List<Card> legal = myTurn ? match.legalPlays(HUMAN) : Collections.<Card>emptyList();
		for (int i = 0; i < n; i++) {
			Card c = handOrder.get(i);
			boolean canPlay = legal.contains(c);
			float x = start + i * gap, y = canPlay ? 26 : 8;
			batch.setColor(myTurn && !canPlay ? DIM : Color.WHITE);
			batch.draw(assets.face(c), x, y, cw, ch);
			handRects.add(new Rectangle(x, y, i == n - 1 ? cw : gap, ch));
		}
		batch.setColor(Color.WHITE);
	}

	private void drawTrick(float w, float h) {
		List<Card> cards;
		List<Integer> seats;
		int winner = -1;
		if (trickPause > 0) {
			cards = match.lastTrick();
			seats = match.lastTrickSeats();
			winner = match.lastTrickWinner();
		} else {
			cards = match.currentTrick();
			seats = match.currentTrickSeats();
		}
		float cx = w / 2, cy = h / 2 + 5;
		float cw = Assets.CARD_W, ch = Assets.CARD_H;
		for (int i = 0; i < cards.size(); i++) {
			int seat = seats.get(i);
			float x = cx - cw / 2 + (seat == 1 ? -95 : seat == 3 ? 95 : 0);
			float y = cy - ch / 2 + (seat == 0 ? -50 : seat == 2 ? 50 : 0);
			if (seat == winner) {
				rect(x - 4, y - 4, cw + 8, ch + 8, GOLD);
			}
			batch.draw(assets.face(cards.get(i)), x, y, cw, ch);
		}
	}

	private void drawBidPanel(float w, float h) {
		int n = match.handSize();
		float bs = 52, gap = 8;
		float rowW = (n + 1) * bs + n * gap;
		float pw = Math.max(rowW + 40, 420), ph = 150;
		float px = (w - pw) / 2, py = h / 2 + 5 - ph / 2;
		rect(px, py, pw, ph, PANEL);

		BitmapFont f = font(1.3f);
		f.setColor(Color.WHITE);
		text(f, "How many tricks will you take?", w / 2, py + ph - 14, true);
		BitmapFont s = font(1f);
		s.setColor(Color.LIGHT_GRAY);
		int forbidden = match.forbiddenBid();
		String note = "Bids so far: " + match.bidsSoFar() + " of " + n
				+ (forbidden >= 0 ? "   -   as dealer you can't bid " + forbidden : "");
		text(s, note, w / 2, py + ph - 44, true);

		bidButtons.clear();
		float bx = (w - rowW) / 2, by = py + 18;
		BitmapFont bf = font(1.6f);
		for (int b = 0; b <= n; b++) {
			Rectangle r = new Rectangle(bx + b * (bs + gap), by, bs, bs);
			bidButtons.add(r);
			boolean ok = match.isLegalBid(b);
			rect(r.x, r.y, r.width, r.height, ok ? BUTTON : BUTTON_OFF);
			bf.setColor(ok ? Color.WHITE : DIM);
			text(bf, String.valueOf(b), r.x + bs / 2, r.y + bs / 2 + 11, true);
		}
	}

	private void drawHandResults(float w, float h) {
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
			t.setColor(match.madeBid(seat) ? Color.WHITE : new Color(1f, 0.55f, 0.5f, 1f));
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

	private void drawGameOver(float w, float h) {
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

	// --------------------------------------------------------------- helpers

	private boolean isTurnPhase() {
		return match.phase() == Match.Phase.BIDDING || match.phase() == Match.Phase.PLAYING;
	}

	private BitmapFont font(float scale) {
		assets.font.getData().setScale(scale);
		return assets.font;
	}

	/** Draws text centred on x, or right-aligned to x when centre is false. */
	private void text(BitmapFont f, String s, float x, float y, boolean centre) {
		glyphs.setText(f, s);
		f.draw(batch, s, centre ? x - glyphs.width / 2 : x - glyphs.width, y);
	}

	private void rect(float x, float y, float w, float h, Color c) {
		batch.setColor(c);
		batch.draw(assets.white, x, y, w, h);
		batch.setColor(Color.WHITE);
	}

	/** "Ace of Spades" */
	static String pretty(Card c) {
		return cap(c.rank().name()) + " of " + cap(c.suit().name());
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
		if (trickPause > 0) {
			return "trick:" + hand;
		}
		switch (match.phase()) {
		case BIDDING:
			return match.currentSeat() == HUMAN ? "bid:" + hand : "bidding:" + hand;
		case PLAYING:
			return match.currentSeat() == HUMAN ? "play:" + hand + ":" + match.currentTrick().size() : "playing:" + hand;
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
