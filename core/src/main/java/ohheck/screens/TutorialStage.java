package ohheck.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Kick;
import ohheck.anim.Timeline;
import ohheck.fx.Sparks;
import ohheck.platform.Settings;
import ohheck.players.Player;
import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;
import ohheck.table.Counters;
import ohheck.table.PlayerPanel;
import ohheck.table.SeatState;
import ohheck.ui.Assets;
import ohheck.ui.BaseScreen;
import ohheck.ui.CardArt;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/* The tutorial's drawing kit: its little table, the scene clock, cards, glows and labels. */
abstract class TutorialStage {

	static final float CAPTION_H = 92, CW = Assets.CARD_W, CH = Assets.CARD_H;
	static final String HANDS = String.valueOf(RuleSet.STANDARD.numberOfHands());

	final Painter paint;
	final SpriteBatch batch;
	final Assets assets;
	final Settings settings;
	final CardArt art;
	final PlayerPanel panel;
	final Counters counterArt;
	final Sparks sparks = new Sparks();
	final Timeline clock = new Timeline();
	final Kick kick = new Kick();
	float t;
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

	void restart() {
		clock.restart();
		t = 0;
	}

	void advance(float dt, float length, boolean loops, float hold) {
		clock.advance(dt, length, loops, hold);
		t = clock.t();
		sparks.step(dt);
	}

	void size(float w, float h) {
		this.w = w;
		this.h = h;
	}

	float cx() {
		return w / 2;
	}

	float cy() {
		return (h - 50 + CAPTION_H + 12) / 2 + 10;
	}

	static Card card(Rank r, Suit s) {
		return Card.of(r, s);
	}

	static final String LEFT = Player.byId(4).name, RIGHT = Player.byId(7).name;
	static final String TOP = Player.byId(6).name;
	static final String[] NAMES = { "You", LEFT, TOP, RIGHT };

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

	void seat(int s, SeatState state) {
		scoredSeat(s, state.unscored());
	}

	void scoredSeat(int s, SeatState state) {
		panel.draw(state.named(NAMES[s]), seatX(s), seatTop(s), clock.shown());
	}

	SeatState plain() {
		return nextState().plain(true);
	}

	SeatState waiting() {
		return nextState();
	}

	SeatState bidOf(int bid) {
		return nextState().bid(bid);
	}

	final SeatState[] states = { new SeatState(), new SeatState(), new SeatState(), new SeatState(),
			new SeatState(), new SeatState(), new SeatState(), new SeatState() };
	int nextState;

	SeatState nextState() {
		nextState = (nextState + 1) % states.length;
		return states[nextState].reset();
	}

	float rowX(int i, int n) {
		float gap = CW + 16;
		return cx() - (n * gap - 16) / 2 + i * gap;
	}

	float rowY(boolean withHand) {
		return withHand ? handY() + CH + 56 : cy() - CH / 2 - 10;
	}

	float fromX(int seat) {
		return seatX(seat) + PlayerPanel.W / 2 - CW / 2;
	}

	float fromY(int seat) {
		return seatTop(seat) - PlayerPanel.H / 2 - CH / 2;
	}

	float handX(int i, int n) {
		float gap = CW + 14;
		return cx() - (n * gap - 14) / 2 + i * gap;
	}

	float handY() {
		return CAPTION_H + 12 + 12;
	}

	float at(float a, float b) {
		return clock.at(a, b);
	}

	boolean passed(float a) {
		return clock.passed(a);
	}

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

	void flying(Card c, float x0, float y0, float x1, float y1, float a, float b, float bright, float alpha) {
		if (t < a) {
			return;
		}
		float k = at(a, b);
		card(c, MathUtils.lerp(x0, x1, k), MathUtils.lerp(y0, y1, k), 6 * (1 - k) + 2, bright, alpha);
	}

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

	void floating(String s, float x, float y, float a, Color c) {
		if (t < a || t > a + 1.6f) {
			return;
		}
		float k = (t - a) / 1.6f;
		BitmapFont f = paint.serif(1.8f, c, 1 - k * k);
		paint.centred(f, s, x, y + 40 * k);
	}

	void label(String s, float x, float y, float alpha) {
		if (alpha <= 0) {
			return;
		}
		BitmapFont f = paint.ui(1f, Palette.GOLD_LIGHT, alpha);
		paint.centred(f, s, x, y);
	}
}
