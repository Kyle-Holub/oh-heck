package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Timeline;
import ohheck.fx.Fireworks;
import ohheck.fx.Sparks;
import ohheck.platform.Settings;
import ohheck.players.Player;
import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.Rules;
import ohheck.rules.Suit;
import ohheck.table.Counters;
import ohheck.table.PlayerPanel;
import ohheck.table.SeatState;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/**
 * What the tutorial's scenes are drawn with: the little table (three seats, a row for the
 * trick, your hand), the scene's clock, cards still or flying, glows, rising numbers and
 * labels, and the sparks and fireworks. {@link TutorialScenes} draws the scenes with these;
 * {@link TutorialScreen} runs the steps around them.
 */
abstract class TutorialStage {

	static final float CAPTION_H = 92, CW = Assets.CARD_W, CH = Assets.CARD_H;
	/** "8", the number of hands in a game, for the captions. */
	static final String HANDS = String.valueOf(Rules.numberOfHands());

	final Painter paint;
	final SpriteBatch batch;
	final Assets assets;
	final Settings settings;
	final CardArt art;
	final PlayerPanel panel;
	final Counters counterArt;
	final Sparks sparks = new Sparks();
	final Fireworks fireworks = new Fireworks();
	/** The scene's clock; t is its time this frame, which every scene draws from. */
	final Timeline clock = new Timeline();
	float t;
	/** The screen's size this frame. */
	float w = BaseScreen.WORLD_W, h = BaseScreen.WORLD_H;
	final Color tint = new Color();

	TutorialStage(Painter paint, Settings settings) {
		this.paint = paint;
		this.batch = paint.batch;
		this.assets = paint.assets;
		this.settings = settings;
		this.art = new CardArt(batch, assets);
		this.panel = new PlayerPanel(paint);
		this.counterArt = new Counters(paint);
	}

	/** Starts the scene's clock again. */
	void restart() {
		clock.restart();
		t = 0;
	}

	/** Moves the scene on by dt: a scene of this length, looping (after a hold) or not. */
	void advance(float dt, float length, boolean loops, float hold) {
		clock.advance(dt, length, loops, hold);
		t = clock.t();
		sparks.step(dt);
		fireworks.update(dt);
	}

	/** The screen's size this frame. */
	void size(float w, float h) {
		this.w = w;
		this.h = h;
	}

	
	/** The scene's centre: the middle of the table above the caption. */
	float cx() {
		return w / 2;
	}

	float cy() {
		return (h - 50 + CAPTION_H + 12) / 2 + 10;
	}

	static Card card(Rank r, Suit s) {
		return Card.of(r, s);
	}

	// ---- the seats: three players, for room: you bottom left, Rupert left, Oswald right

	/** The two players at the tutorial's table, by roster id (so a renamed player is renamed here too). */
	static final String LEFT = Player.byId(4).name, RIGHT = Player.byId(7).name;
	static final String[] NAMES = { "You", LEFT, "", RIGHT };

	float seatX(int seat) {
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

	float seatTop(int seat) {
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

	/** Draws seat s's panel showing state (its name is filled in). */
	void seat(int s, SeatState state) {
		panel.draw(state.named(NAMES[s]), seatX(s), seatTop(s), clock.shown());
	}

	/** A placard with nothing on it yet. */
	SeatState plain() {
		return nextState().plain(true);
	}

	/** Yet to bid: the hourglass. */
	SeatState waiting() {
		return nextState();
	}

	SeatState bidOf(int bid) {
		return nextState().bid(bid);
	}

	/** Placard states, reused round in turn (a scene shows at most a few a frame). */
	final SeatState[] states = { new SeatState(), new SeatState(), new SeatState(), new SeatState(),
			new SeatState(), new SeatState(), new SeatState(), new SeatState() };
	int nextState;

	SeatState nextState() {
		nextState = (nextState + 1) % states.length;
		return states[nextState].reset();
	}


	/**
	 * The trick, laid out as a row (clearer than the table's cross): card i of n. With your
	 * hand on show it sits above the hand (and the top seat is left out, for room).
	 */
	float rowX(int i, int n) {
		float gap = CW + 16;
		return cx() - (n * gap - 16) / 2 + i * gap;
	}

	float rowY(boolean withHand) {
		return withHand ? handY() + CH + 56 : cy() - CH / 2 - 10; // (a clear gap: the hand isn't part of the trick)
	}

	/** Where a seat's card comes from (its panel). */
	float fromX(int seat) {
		return seatX(seat) + PlayerPanel.W / 2 - CW / 2;
	}

	float fromY(int seat) {
		return seatTop(seat) - PlayerPanel.H / 2 - CH / 2;
	}

	// ---- your hand along the bottom of the scene

	float handX(int i, int n) {
		float gap = CW + 14;
		return cx() - (n * gap - 14) / 2 + i * gap;
	}

	float handY() {
		return CAPTION_H + 12 + 12;
	}

	// ---- timing

	float at(float a, float b) {
		return clock.at(a, b);
	}

	boolean passed(float a) {
		return clock.passed(a);
	}

	// ---- drawing

	/** A face-up card at (x, y), its shadow lifted by lift; brightness 1 normal, less to dim it. */
	void card(Card c, float x, float y, float lift, float bright, float alpha) {
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
	void flying(Card c, float x0, float y0, float x1, float y1, float a, float b, float bright, float alpha) {
		if (t < a) {
			return;
		}
		float k = at(a, b);
		card(c, MathUtils.lerp(x0, x1, k), MathUtils.lerp(y0, y1, k), 6 * (1 - k) + 2, bright, alpha);
	}

	/** A soft gold glow behind a card (strength 0-1). */
	void glow(float x, float y, float strength) {
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
	void floating(String s, float x, float y, float a, Color c) {
		if (t < a || t > a + 1.6f) {
			return;
		}
		float k = (t - a) / 1.6f;
		BitmapFont f = paint.serif(1.8f);
		f.setColor(c.r, c.g, c.b, 1 - k * k);
		paint.centred(f, s, x, y + 40 * k);
	}

	/** A label in small gold capitals, centred on x. */
	void label(String s, float x, float y, float alpha) {
		if (alpha <= 0) {
			return;
		}
		BitmapFont f = paint.ui(1f);
		f.setColor(Palette.GOLD_LIGHT.r, Palette.GOLD_LIGHT.g, Palette.GOLD_LIGHT.b, alpha);
		paint.centred(f, s, x, y);
	}
}
