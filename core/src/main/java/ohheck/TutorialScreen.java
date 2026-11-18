package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Card;
import rules.Player;
import rules.Rank;
import rules.Rules;
import rules.Suit;

/**
 * How to play, as a tutorial: thirteen short steps, each a little scene at the table that
 * plays out and then loops, with a caption beneath. A tap moves on. It opens the first
 * time you start a game (with Skip), and from Options (with Back).
 */
public class TutorialScreen extends BaseScreen {

	/** A step: its heading, caption, how long its scene runs, and whether it then plays again. */
	private static final class Step {
		final String heading, caption;
		final float length;
		final boolean loops;

		Step(String heading, String caption, float length) {
			this(heading, caption, length, true);
		}

		Step(String heading, String caption, float length, boolean loops) {
			this.heading = heading;
			this.caption = caption;
			this.length = length;
			this.loops = loops;
		}
	}

	private static final Step[] STEPS = {
			new Step("Welcome", "Oh Heck is about calling your shots: guess how many tricks you'll win, then win exactly that many.", 4f, false),
			new Step("The cards", "Cards rank from Ace, the highest, down to 2, the lowest.", 4.5f),
			new Step("A trick", "Everyone plays one card in turn. That's a trick. The highest card wins it.", 5.5f),
			new Step("Follow suit", "You must play the suit that was led if you can. Here, a heart.", 4.5f),
			new Step("Can't follow? Play anything", "No cards of the suit led? Play any card, but only the suit led can win, unless…", 5.5f),
			new Step("Trump", "…you play a trump. The card turned up after the deal sets the trump suit, and any trump beats every other suit.", 6f),
			new Step("The deal", "There are 8 hands. The first deals 1 card each, the next 2, and so on up to 8. (You can reverse this in Options.)", 6.5f),
			new Step("Bidding", "Before playing, everyone bids how many tricks they'll win. Look at your cards and count your winners.", 9.5f),
			new Step("Bidding zero", "Zero is a real bid. A weak hand can score well by winning nothing at all.", 5.5f),
			new Step("The dealer's hook", "The dealer bids last and can't make the bids add up to the number of tricks, so someone always misses.", 5.5f),
			new Step("The counters", "Top left of the table: tally marks count the 8 hands, and under them one card per trick shows how many the table has bid.", 9f),
			new Step("Scoring", "Make your bid exactly: " + Rules.MADE + " points, plus " + Rules.PER_TRICK_BID
					+ " for each trick you bid. Miss it, high or low: lose " + Rules.PER_TRICK_OFF + " for each trick off.", 9f),
			new Step("Ready", "Highest total after 8 hands wins. Good luck!", 5f, false) };

	/** After a step's scene has run, it holds this long before playing again. */
	private static final float HOLD = 1.6f;
	private static final float CAPTION_H = 92, CW = Assets.CARD_W, CH = Assets.CARD_H;

	private final CardArt art;
	private final PlayerPanel panel;
	private final Counters counterArt;
	private final Sparks sparks = new Sparks();
	private final Fireworks fireworks = new Fireworks();
	private final Rectangle cornerButton = new Rectangle(), doneButton = new Rectangle();
	/** Opened before your first game (Skip, then the game) or from Options (Back). */
	private boolean firstRun;
	private int step;
	/** Seconds into this step, and into its scene (which loops). */
	/** The scene's clock; t is its time this frame, which every scene draws from. */
	private final Timeline clock = new Timeline();
	private float t;
	private final StringBuilder sb = new StringBuilder();
	private final Color tint = new Color();

	public TutorialScreen(OhHeckGame game) {
		super(game);
		art = new CardArt(batch, assets);
		panel = new PlayerPanel(this);
		counterArt = new Counters(this);
	}

	/** Starts from the first step; firstRun if it's before your first game. */
	void open(boolean firstRun) {
		this.firstRun = firstRun;
		step = 0;
		clock.restart();
		t = 0;
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f) * game.options.speed;
		shownFor += dt;
		Step s = STEPS[step];
		clock.advance(dt, s.length, s.loops, HOLD); // (the first and last play once)
		t = clock.t();
		sparks.step(dt);
		fireworks.update(dt);
		if (game.tour != null && game.tour.step(this, clock.shown())) { // (the tour times each step)
			return;
		}

		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		fireworks.draw(batch, assets.glow);
		scene(step);
		sparks.draw(batch, assets.glow);
		caption(s);
		header();
		batch.end();
	}

	boolean lastStep() {
		return step == STEPS.length - 1;
	}

	void next() {
		if (step < STEPS.length - 1) {
			step++;
			clock.restart();
			t = 0;
		} else {
			finish();
		}
	}

	private void finish() {
		if (firstRun) {
			game.settings.setTutorialSeen(true);
			game.newGame();
		} else {
			game.back();
		}
	}

	@Override
	protected void tap(float x, float y) {
		if (cornerButton.contains(x, y)) {
			if (firstRun) {
				finish(); // skip: straight to the game
			} else {
				game.back();
			}
			return;
		}
		next();
	}

	@Override
	protected void back() {
		if (firstRun) {
			finish();
		} else {
			game.back();
		}
	}

	@Override
	public String stateTag() {
		return "tutorial:" + (step + 1);
	}

	// ------------------------------------------------------------ frame

	private void header() {
		BitmapFont f = serif(1.3f);
		f.setColor(GOLD);
		f.draw(batch, "How to play", 22, h - 18);
		button(cornerButton, firstRun ? "Skip tutorial" : "Back", w - 110, h - 52, 190, 40, assets.buttonAlt);
	}

	private void caption(Step s) {
		float cw = Math.min(w - 60, 860), x = (w - cw) / 2, y = 12;
		frame(assets.panel, x, y, cw, CAPTION_H);
		BitmapFont hd = serif(1.35f);
		hd.setColor(GOLD);
		hd.draw(batch, s.heading, x + 22, y + CAPTION_H - 12);
		BitmapFont c = ui(1.1f);
		c.setColor(Palette.CREAM);
		c.draw(batch, s.caption, x + 22, y + CAPTION_H - 44, cw - 44, Align.left, true);
		BitmapFont m = ui(0.9f);
		m.setColor(Palette.MUTED);
		sb.setLength(0);
		sb.append(step + 1).append(" / ").append(STEPS.length);
		text(m, sb.toString(), x + cw - 20, y + CAPTION_H - 14, false);
		if (step == STEPS.length - 1) {
			button(doneButton, "Ready to play", w / 2, y + CAPTION_H + 14, 220, 44, assets.button);
		} else {
			text(m, "Tap to continue", x + cw - 20, y + 22, false);
		}
	}

	// ------------------------------------------------------------ the scenes

	/** The scene's centre: the middle of the table above the caption. */
	private float cx() {
		return w / 2;
	}

	private float cy() {
		return (h - 50 + CAPTION_H + 12) / 2 + 10;
	}

	private static Card card(Rank r, Suit s) {
		return Card.of(r, s);
	}

	private void scene(int i) {
		switch (i) {
		case 0:
			welcome();
			break;
		case 1:
			ranks();
			break;
		case 2:
			aTrick();
			break;
		case 3:
			followSuit();
			break;
		case 4:
			cantFollow();
			break;
		case 5:
			trump();
			break;
		case 6:
			theDeal();
			break;
		case 7:
			bidding();
			break;
		case 8:
			biddingZero();
			break;
		case 9:
			dealersHook();
			break;
		case 10:
			counters();
			break;
		case 11:
			scoring();
			break;
		default:
			ready();
			break;
		}
	}

	// ---- the seats: three players, for room: you bottom left, Rupert left, Oswald right

	/** The two players at the tutorial's table, by roster id (so a renamed player is renamed here too). */
	private static final String LEFT = Player.byId(4).name, RIGHT = Player.byId(7).name;
	private static final String[] NAMES = { "You", LEFT, "", RIGHT };

	private float seatX(int seat) {
		switch (seat) {
		case 1:
			return 20;
		case 2:
			return cx() - PlayerPanel.W / 2;
		case 3:
			return w - 20 - PlayerPanel.W;
		default:
			return 20;
		}
	}

	private float seatTop(int seat) {
		switch (seat) {
		case 1:
		case 3:
			return cy() + 70;
		case 2:
			return h - 58;
		default:
			return CAPTION_H + 12 + 8 + PlayerPanel.H;
		}
	}

	/** Draws a seat's panel: bid null and dealing for a plain one; slide moves it in from off-screen. */
	private void seat(int s, Integer bid, int won, boolean turn, boolean dealer, int score, int handSize, boolean plain,
			float slide) {
		float dx = s == 3 ? 1 : s == 2 ? 0 : -1, dy = s == 2 ? 1 : 0;
		float off = (1 - slide) * 260;
		panel.draw(NAMES[s], seatX(s) + dx * off, seatTop(s) + dy * off, turn, dealer, score, bid, won, handSize, plain,
				clock.shown());
	}


	/**
	 * The trick, laid out as a row (clearer than the table's cross): card i of n. With your
	 * hand on show it sits above the hand (and the top seat is left out, for room).
	 */
	private float rowX(int i, int n) {
		float gap = CW + 16;
		return cx() - (n * gap - 16) / 2 + i * gap;
	}

	private float rowY(boolean withHand) {
		return withHand ? handY() + CH + 56 : cy() - CH / 2 - 10; // (a clear gap: the hand isn't part of the trick)
	}

	/** Where a seat's card comes from (its panel). */
	private float fromX(int seat) {
		return seatX(seat) + PlayerPanel.W / 2 - CW / 2;
	}

	private float fromY(int seat) {
		return seatTop(seat) - PlayerPanel.H / 2 - CH / 2;
	}

	// ---- timing

	private float at(float a, float b) {
		return clock.at(a, b);
	}

	private boolean passed(float a) {
		return clock.passed(a);
	}

	// ---- drawing

	/** A face-up card at (x, y), its shadow lifted by lift; brightness 1 normal, less to dim it. */
	private void card(Card c, float x, float y, float lift, float bright, float alpha) {
		if (alpha <= 0) {
			return;
		}
		batch.setColor(1, 1, 1, alpha);
		art.shadow(x, y, CW, CH, lift);
		batch.setColor(bright, bright, bright, alpha);
		batch.draw(assets.face(c), x, y + lift * 0.4f, CW, CH);
		batch.setColor(Color.WHITE);
	}

	/** A card moving from (x0, y0) to (x1, y1) over the scene's a..b. */
	private void flying(Card c, float x0, float y0, float x1, float y1, float a, float b, float bright, float alpha) {
		if (t < a) {
			return;
		}
		float k = at(a, b);
		card(c, MathUtils.lerp(x0, x1, k), MathUtils.lerp(y0, y1, k), 6 * (1 - k) + 2, bright, alpha);
	}

	/** A soft gold glow behind a card (strength 0-1). */
	private void glow(float x, float y, float strength) {
		if (strength <= 0) {
			return;
		}
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		batch.setColor(1f, 0.82f, 0.45f, 0.55f * strength);
		float g = 26;
		batch.draw(assets.glow, x - g, y - g, CW + 2 * g, CH + 2 * g);
		batch.setColor(Color.WHITE);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	/** Text rising and fading from (x, y) over a..a+1.4, in colour c. */
	private void floating(String s, float x, float y, float a, Color c) {
		if (t < a || t > a + 1.6f) {
			return;
		}
		float k = (t - a) / 1.6f;
		BitmapFont f = serif(1.8f);
		f.setColor(c.r, c.g, c.b, 1 - k * k);
		centred(f, s, x, y + 40 * k);
	}

	/** A label in small gold capitals, centred on x. */
	private void label(String s, float x, float y, float alpha) {
		if (alpha <= 0) {
			return;
		}
		BitmapFont f = ui(1f);
		f.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, alpha);
		centred(f, s, x, y);
	}

	// ---- 1. welcome: the title, and the four aces fanned out beneath it, sparkling

	private static final Suit[] ACE_SUITS = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };

	private void welcome() {
		art.art(assets.titleText, cx(), cy() + 70, 0.62f, at(0, 0.8f));
		float open = at(0.9f, 1.9f), sway = MathUtils.sin(clock.shown() * 1.1f) * 2f; // a slow breath once open
		float pivotX = cx(), pivotY = cy() - CH - 30;
		for (int i = 0; i < 4; i++) {
			float k = at(0.7f + i * 0.08f, 1.2f + i * 0.08f);
			if (k <= 0) {
				continue;
			}
			float rot = (i - 1.5f) * -21f * open + sway * open;
			// each card turned about a point below the fan, so they spread like a hand of cards
			float rx = MathUtils.sinDeg(-rot) * (CH + 10), ry = MathUtils.cosDeg(rot) * (CH + 10);
			float x = pivotX + rx - CW / 2, y = pivotY + ry - CH / 2 + (1 - k) * -40;
			Card c = card(Rank.ACE, ACE_SUITS[i]);
			batch.setColor(1, 1, 1, k);
			art.shadow(x, y, CW, CH, 6);
			batch.draw(assets.face(c), x, y, CW / 2, CH / 2, CW, CH, 1, 1, rot);
			batch.setColor(Color.WHITE);
			if (open >= 1) {
				Glints.draw(batch, assets.glow, c, x, y, CW, CH, CW, rot, clock.shown() + i * 0.7f, 0.8f);
			}
		}
		if (passed(2.0f) && game.settings.effects()) {
			sparks.burst(cx(), cy() - 20, 60);
		}
	}

	// ---- 2. the ranks: a suit fanned out, Ace to 2, a shimmer running down it

	private static final Rank[] HIGH_TO_LOW = { Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN, Rank.NINE,
			Rank.EIGHT, Rank.SEVEN, Rank.SIX, Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO };

	private void ranks() {
		float step = Math.min(64, (w - 120 - CW) / 12), x0 = cx() - (12 * step + CW) / 2, y = cy() - CH / 2;
		for (int i = 0; i < 13; i++) {
			float a = 0.1f + i * 0.07f;
			Card c = card(HIGH_TO_LOW[i], Suit.SPADES);
			float x = x0 + i * step;
			float shimmer = 1 - Math.min(1, Math.abs(t - (1.9f + i * 0.12f)) / 0.18f);
			glow(x, y, shimmer);
			flying(c, cx() - CW / 2, y - 60, x, y + shimmer * 8, a, a + 0.4f, 1, at(a, a + 0.1f));
		}
		label("HIGHEST", x0 + CW / 2, y + CH + 26, at(1.4f, 1.8f));
		label("LOWEST", x0 + 12 * step + CW / 2, y + CH + 26, at(1.4f, 1.8f));
	}

	// ---- 3. a trick: three hearts in, one by one into a row; the King wins; the trick goes to Oswald

	private void aTrick() {
		int[] order = { 1, 3, 0 }; // who plays, in turn
		Card[] cards = { card(Rank.SEVEN, Suit.HEARTS), card(Rank.KING, Suit.HEARTS), card(Rank.NINE, Suit.HEARTS) };
		boolean taken = t >= 3.4f;
		seat(0, null, 0, false, false, 0, 1, true, 1);
		seat(1, null, 0, false, false, 0, 1, true, 1);
		seat(3, taken ? Integer.valueOf(1) : null, taken ? 1 : 0, false, false, 0, 1, !taken, 1);
		float gone = at(2.8f, 3.4f), y = rowY(false);
		for (int i = 0; i < 3; i++) {
			float a = 0.3f + i * 0.5f;
			boolean king = i == 1;
			float lift = king ? at(2.0f, 2.4f) * 10 : 0;
			float x = MathUtils.lerp(rowX(i, 3), fromX(3), gone), cy = MathUtils.lerp(y, fromY(3), gone) + lift;
			if (t >= a + 0.45f) {
				if (king) {
					glow(x, cy, at(2.0f, 2.4f) * (1 - gone));
				}
				card(cards[i], x, cy, 2 + lift, 1, 1 - gone);
			} else {
				flying(cards[i], fromX(order[i]), fromY(order[i]), rowX(i, 3), y, a, a + 0.45f, 1, 1);
			}
		}
	}

	// ---- your hand along the bottom of the scene

	private float handX(int i, int n) {
		float gap = CW + 14;
		return cx() - (n * gap - 14) / 2 + i * gap;
	}

	private float handY() {
		return CAPTION_H + 12 + 12;
	}

	// ---- 4. follow suit: a heart is led; your heart is the only card you may play

	private void followSuit() {
		Card led = card(Rank.TEN, Suit.HEARTS);
		Card[] hand = { card(Rank.FOUR, Suit.HEARTS), card(Rank.QUEEN, Suit.CLUBS), card(Rank.EIGHT, Suit.SPADES) };
		float y = rowY(true);
		flying(led, fromX(3), fromY(3), rowX(0, 2), y, 0.2f, 0.6f, 1, 1);
		seat(3, null, 0, false, false, 0, 1, true, 1);
		float dim = at(0.9f, 1.3f);
		for (int i = 0; i < hand.length; i++) {
			float x = handX(i, 3), hy = handY();
			if (i == 0) {
				float lift = at(1.0f, 1.3f) * 12;
				if (t < 1.6f) {
					glow(x, hy + lift, at(1.0f, 1.3f));
					card(hand[i], x, hy + lift, 2 + lift, 1, 1);
				} else {
					flying(hand[i], x, hy + lift, rowX(1, 2), y, 1.6f, 2.2f, 1, 1);
				}
			} else {
				card(hand[i], x, hy, 2, 1 - 0.55f * dim, 1);
			}
		}
	}

	// ---- 5. can't follow: Oswald leads a diamond, you have none, your King of clubs can't win

	private void cantFollow() {
		Card led = card(Rank.JACK, Suit.DIAMONDS);
		Card[] hand = { card(Rank.SIX, Suit.HEARTS), card(Rank.KING, Suit.CLUBS), card(Rank.FIVE, Suit.SPADES) };
		float y = rowY(true);
		seat(1, null, 0, false, false, 0, 1, true, 1);
		seat(3, null, 0, false, false, 0, 1, true, 1);
		flying(led, fromX(3), fromY(3), rowX(0, 3), y, 0.2f, 0.6f, 1, 1);
		float play = 1.6f;
		for (int i = 0; i < hand.length; i++) {
			float x = handX(i, 3), hy = handY();
			if (i == 1 && t >= play) {
				flying(hand[i], x, hy, rowX(1, 3), y, play, play + 0.5f, 1 - 0.4f * at(3.0f, 3.3f), 1);
			} else {
				glow(x, hy, at(0.8f, 1.2f) * (1 - at(1.4f, 1.6f)) * 0.7f);
				card(hand[i], x, hy, 2, 1, 1);
			}
		}
		flying(card(Rank.NINE, Suit.DIAMONDS), fromX(1), fromY(1), rowX(2, 3), y, 2.3f, 2.7f, 1, 1);
		if (t >= 3.0f) { // the Jack, the highest diamond, wins
			glow(rowX(0, 3), y, at(3.0f, 3.3f));
			card(led, rowX(0, 3), y, 2, 1, 1);
		}
	}

	// ---- 6. trump: the turned-up card says spades; a lowly 2 of spades beats the Ace of diamonds

	private void trump() {
		Card trumpCard = card(Rank.FIVE, Suit.SPADES);
		float tx = cx() + CW * 2.2f, ty = rowY(false) + CH + 30; // up and to the right, as at the table
		float p = at(0.3f, 0.9f);
		float lit = at(0.9f, 1.5f);
		glow(tx, ty, lit * 0.8f);
		art.shadow(tx, ty, CW, CH, 4);
		art.flip(trumpCard, tx, ty, CW, CH, p);
		if (p >= 1) {
			Glints.draw(batch, assets.glow, trumpCard, tx, ty, CW, CH, CW, 0, clock.shown(), 1f);
		}
		Card ace = card(Rank.ACE, Suit.DIAMONDS), two = card(Rank.TWO, Suit.SPADES);
		boolean won = t >= 4.2f;
		seat(0, won ? Integer.valueOf(1) : null, won ? 1 : 0, false, false, 0, 1, !won, 1);
		seat(1, null, 0, false, false, 0, 1, true, 1);
		float gone = at(3.6f, 4.2f), y = rowY(false);
		float ax = MathUtils.lerp(rowX(0, 2), fromX(0), gone), ay = MathUtils.lerp(y, fromY(0), gone);
		float bx = MathUtils.lerp(rowX(1, 2), fromX(0), gone), by = MathUtils.lerp(y, fromY(0), gone);
		if (t >= 1.8f) {
			card(ace, ax, ay, 2, 1, 1 - gone);
		} else {
			flying(ace, fromX(1), fromY(1), rowX(0, 2), y, 1.4f, 1.8f, 1, 1);
		}
		if (t >= 2.4f) {
			float lift = at(2.6f, 3.0f) * 10;
			glow(bx, by + lift, at(2.6f, 3.0f) * (1 - gone));
			card(two, bx, by + lift, 2 + lift, 1, 1 - gone);
			if (gone <= 0) {
				Glints.draw(batch, assets.glow, two, bx, by + lift, CW, CH, CW, 0, clock.shown(), 1f);
			}
		} else {
			flying(two, fromX(0), fromY(0), rowX(1, 2), y, 2.0f, 2.4f, 1, 1);
		}
	}

	// ---- 7. the deal: 1 card each, then 2, then 3 ... up to 8 (two players, for room)

	private static final float DEAL_SCALE = 0.8f;

	private void theDeal() {
		float[] starts = { 0.2f, 1.6f, 3.3f };
		float dw = CW * DEAL_SCALE, dh = CH * DEAL_SCALE, gap = dw + 12;
		float deckX = w - 60 - CW, deckY = cy() - CH / 2 - 20;
		art.stack(deckX + CW / 2, deckY + CH / 2, 12, CW, CH, 0);
		panel.draw(RIGHT, seatX(1), seatTop(1), false, false, 0, null, 0, 1, true, clock.shown());
		seat(0, null, 0, false, true, 0, 1, true, 1);
		// each player's cards in a row beside their panel
		float[] rowYs = { seatTop(0) - PlayerPanel.H / 2 - dh / 2, seatTop(1) - PlayerPanel.H / 2 - dh / 2 };
		for (int round = 0; round < 3; round++) {
			int n = round + 1;
			float a = starts[round], end = round < 2 ? starts[round + 1] - 0.15f : 99f;
			if (t < a || t >= end) {
				continue;
			}
			BitmapFont f = serif(1.6f);
			f.setColor(GOLD.r, GOLD.g, GOLD.b, at(a, a + 0.3f) * (1 - at(5.0f, 5.3f)));
			centred(f, "Hand " + n + ": " + n + (n == 1 ? " card each" : " cards each"), cx(), h - 70);
			for (int k = 0; k < n * 2; k++) {
				int who = k % 2, j = k / 2; // you first (left of the dealer), then Oswald
				float ka = a + k * 0.12f;
				if (t < ka) {
					break;
				}
				float kk = at(ka, ka + 0.35f);
				float x0 = cx() - (n * gap - 12) / 2;
				float x = MathUtils.lerp(deckX, x0 + j * gap, kk), y = MathUtils.lerp(deckY, rowYs[who], kk);
				art.shadow(x, y, dw, dh, 4);
				batch.draw(assets.back(), x, y, dw, dh);
			}
		}
		BitmapFont f = serif(1.6f);
		f.setColor(GOLD.r, GOLD.g, GOLD.b, at(5.2f, 5.6f));
		centred(f, "\u2026 and so on, up to 8", cx(), h - 70);
	}

	// ---- the bid panel, as at the table: 0..n, one picked, some barred

	private void bidPanel(float x, float y, int n, int picked, int barred, float alpha) {
		if (alpha <= 0) {
			return;
		}
		float bs = 46, gap = 8, pw = (n + 1) * (bs + gap) - gap + 36, ph = bs + 30;
		float px = x - pw / 2;
		batch.setColor(1, 1, 1, alpha);
		frame(assets.panel, px, y, pw, ph);
		for (int b = 0; b <= n; b++) {
			float bx = px + 18 + b * (bs + gap), by = y + 15;
			frame(b == barred ? assets.buttonOff : b == picked ? assets.button : assets.buttonAlt, bx, by, bs, bs);
			numeral(b, bx + bs / 2, by + bs / 2, bs * 0.62f, b == barred ? NUMERAL_OFF : Color.WHITE);
			if (b == barred) {
				float k = at(2.2f, 2.5f);
				batch.setColor(Palette.RED.r, Palette.RED.g, Palette.RED.b, alpha);
				batch.draw(assets.stroke, bx + 4, by + bs / 2 - 2, (bs - 8) / 2, 2, (bs - 8) * k, 4, 1, 1, -35, 0, 0,
						assets.stroke.getWidth(), assets.stroke.getHeight(), false, false);
			}
		}
		batch.setColor(Color.WHITE);
	}

	private static final Color NUMERAL_OFF = new Color(0.55f, 0.52f, 0.5f, 0.6f);

	// ---- 8. bidding: count your winners (the trump Ace, the King), bid 2; then the others bid

	private void bidding() {
		Card[] hand = { card(Rank.ACE, Suit.SPADES), card(Rank.KING, Suit.HEARTS), card(Rank.FOUR, Suit.CLUBS) };
		int[] seats = { 0, 1, 3 }, bids = { 2, 1, 0 };
		float[] when = { 6.4f, 7.2f, 7.9f };
		for (int i = 0; i < 3; i++) {
			boolean bid = t >= when[i];
			seat(seats[i], bid ? Integer.valueOf(bids[i]) : null, 0, i == 0 && !bid, false, 0, 3, false, 1);
		}
		// counting: the Ace (1), then the King (2), with a note to read; then the bid panel; then the bids
		float counting = 1 - at(4.0f, 4.3f);
		for (int i = 0; i < 3; i++) {
			float x = handX(i, 3), y = handY();
			float g = i < 2 ? at(0.5f + i * 1.0f, 0.9f + i * 1.0f) * counting : 0;
			glow(x, y, g);
			card(hand[i], x, y, 2, i == 2 ? 1 - 0.4f * at(0.5f, 0.9f) * counting : 1, 1);
			if (i < 2 && g > 0) { // counting them: 1, 2
				numeral(i + 1, x + CW / 2, y + CH + 22, 30, tint.set(1, 1, 1, g));
			}
		}
		BitmapFont f = ui(1.05f);
		f.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, at(1.8f, 2.2f)); // stays up
		f.draw(batch, "The Ace of trumps and a King:\ntwo likely winners, so bid 2", handX(2, 3) + CW + 24, handY() + CH * 0.65f);
		bidPanel(cx(), handY() + CH + 12, 3, t >= 5.2f ? 2 : -1, -1, at(4.4f, 4.8f) * (1 - at(6.2f, 6.4f)));
	}

	// ---- 9. bidding zero: a low hand bids nothing, and lets the trick go by (Oswald takes it)

	private void biddingZero() {
		Card[] hand = { card(Rank.TWO, Suit.DIAMONDS), card(Rank.THREE, Suit.CLUBS), card(Rank.FOUR, Suit.HEARTS) };
		boolean taken = t >= 3.0f;
		seat(0, t >= 0.4f ? Integer.valueOf(0) : null, 0, t >= 3.4f, false, 0, 3, false, 1);
		seat(1, null, 0, false, false, 0, 3, true, 1);
		seat(3, taken ? Integer.valueOf(1) : null, taken ? 1 : 0, false, false, 0, 3, !taken, 1);
		// the trick, in a row: Rupert leads, Oswald, then you
		Card[] played = { card(Rank.NINE, Suit.CLUBS), card(Rank.QUEEN, Suit.CLUBS), card(Rank.THREE, Suit.CLUBS) };
		float[] when = { 1.0f, 1.4f, 1.8f };
		float y = rowY(true), gone = at(2.4f, 3.0f);
		for (int i = 0; i < 3; i++) {
			if (i == 1 && t >= when[2]) {
				continue; // your 3 of clubs, played
			}
			card(hand[i], handX(i, 3), handY(), 2, 1, 1);
		}
		float[] fromXs = { fromX(1), fromX(3), handX(1, 3) }, fromYs = { fromY(1), fromY(3), handY() };
		for (int i = 0; i < 3; i++) {
			float x = MathUtils.lerp(rowX(i, 3), fromX(3), gone), cy = MathUtils.lerp(y, fromY(3), gone);
			float a = when[i];
			if (t >= a + 0.4f) {
				card(played[i], x, cy, 2, 1, 1 - gone);
			} else {
				flying(played[i], fromXs[i], fromYs[i], rowX(i, 3), y, a, a + 0.4f, 1, 1);
			}
		}
		label("NO TRICKS TAKEN: BID MADE", seatX(0) + PlayerPanel.W / 2, seatTop(0) + 22, at(3.4f, 3.8f));
	}

	// ---- 10. the dealer's hook: 3 tricks, 2 bid so far; you, the dealer, can't bid 1

	private void dealersHook() {
		seat(1, t >= 0.3f ? Integer.valueOf(1) : null, 0, false, false, 0, 3, false, 1);
		seat(3, t >= 0.8f ? Integer.valueOf(1) : null, 0, false, false, 0, 3, false, 1);
		seat(0, null, 0, true, true, 0, 3, false, 1);
		bidPanel(cx(), cy() - 30, 3, -1, 1, at(1.6f, 1.9f));
		BitmapFont f = ui(1.15f);
		f.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, at(2.4f, 2.8f));
		centred(f, LEFT + " bid 1.  " + RIGHT + " bid 1.", cx(), cy() - 46);
		f.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, at(3.2f, 3.6f));
		centred(f, "The total of the bids can't be 3, so you can't bid 1.", cx(), cy() - 74);
	}

	// ---- 11. the counters: the tally fills to the 4th hand; the tracker fills as bids come in, then a red one

	private void counters() {
		float scale = 1.6f, left = cx() - 250, top = h - 80;
		int done = clock.count(0.5f, 1.0f, 1.5f);
		counterArt.tally(left, top, done, Counters.pulse(clock.shown()), scale);
		BitmapFont f = ui(1.05f);
		f.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, at(1.6f, 2.0f));
		f.draw(batch, "Hands: three played, the 4th glowing (of 8)", left + 150 * scale, top - 12);
		// a hand of 4 tricks: the bids come in, 1 + 2 + 1, then 1 more: one too many
		int[] totals = { 0, 1, 3, 4, 5 };
		float[] when = { 0, 3.4f, 4.2f, 5.0f, 5.8f };
		int total = totals[clock.count(when) - 1];
		float ty = top - Counters.TALLY_H * scale - Counters.TRACKER_BELOW_TALLY * scale;
		float trackerAlpha = at(2.6f, 3.0f);
		if (trackerAlpha > 0) {
			batch.setColor(1, 1, 1, trackerAlpha);
			float th = counterArt.tracker(left - Counters.TALLY_W * scale / 2, ty, 400, 4, total, scale);
			batch.setColor(Color.WHITE);
			f.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, at(3.0f, 3.4f));
			f.draw(batch, "Bids: one card per trick, filled as the bids come in", left + 150 * scale, ty - 8);
			f.setColor(Palette.RED.r, Palette.RED.g, Palette.RED.b, at(6.0f, 6.4f));
			f.draw(batch, "Red: the table has bid more tricks than there are", left + 150 * scale, ty - th - 6);
		}
	}

	// ---- 12. scoring: your bid of 2 made (+14); Oswald one over his bid of 1, set (-5)

	private void scoring() {
		int mine = clock.count(0.8f, 1.6f), his = clock.count(4.8f, 5.6f);
		seat(0, 2, mine, false, false, t >= 2.2f ? Rules.score(2, 2) : 0, 3, false, 1);
		seat(3, 1, his, false, false, t >= 6.4f ? Rules.score(1, 2) : 0, 3, false, 1);
		seat(1, null, 0, false, false, 0, 3, true, 1);
		float yx = seatX(0) + PlayerPanel.W / 2, yy = seatTop(0) + 10;
		if (passed(2.2f)) {
			sparks.burst(yx, yy - PlayerPanel.H / 2, 70);
		}
		floating("+" + Rules.score(2, 2), yx, yy, 2.2f, Palette.GOLD_LIGHT);
		float hx = seatX(3) + PlayerPanel.W / 2;
		if (t >= 6.2f) {
			art.art(assets.setText, hx, seatTop(3) - PlayerPanel.H - 34, 0.5f, at(6.2f, 6.5f));
		}
		floating(String.valueOf(Rules.score(1, 2)), hx, seatTop(3) + 10, 6.4f, Palette.RED);
		BitmapFont f = ui(1.1f);
		f.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, at(2.4f, 2.8f));
		centred(f, "You bid 2 and won 2: made it. " + Rules.MADE + " + " + Rules.PER_TRICK_BID + " + " + Rules.PER_TRICK_BID
				+ " = " + Rules.score(2, 2) + " points", cx(), cy() + 36);
		f.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, at(6.6f, 7.0f));
		centred(f, RIGHT + " bid 1 and won 2: one trick over, so " + Rules.score(1, 2), cx(), cy() - 4);
	}

	// ---- 13. ready: the title (eased in once and kept), a few fireworks

	private void ready() {
		if (passed(0.3f) && game.settings.effects()) {
			fireworks.show(w, h);
		}
		art.art(assets.titleText, cx(), cy() + 20, 0.62f, 0.25f + 0.75f * at(0, 1.2f));
	}
}
