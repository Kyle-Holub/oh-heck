package ohheck.screens;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.rules.RuleSet;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;

/*
 * How to play: a step per scene, each with a caption; a tap moves on. A caption word written
 * between asterisks, such as *trick*, is drawn in italics.
 */
public class TutorialScreen extends BaseScreen {

	private static final class Step {
		final String heading;
		final String[] words;
		final boolean[] italic;
		final Runnable scene;
		final float length;
		final boolean loops;

		Step(String heading, String caption, Runnable scene, float length) {
			this(heading, caption, scene, length, true);
		}

		Step(String heading, String caption, Runnable scene, float length, boolean loops) {
			this.heading = heading;
			this.words = caption.split(" ");
			this.italic = new boolean[words.length];
			for (int i = 0; i < words.length; i++) {
				italic[i] = words[i].startsWith("*");
				words[i] = words[i].replace("*", "");
			}
			this.scene = scene;
			this.length = length;
			this.loops = loops;
		}
	}

	private static final String HANDS = TutorialStage.HANDS;
	private static final float CAPTION_H = TutorialStage.CAPTION_H;

	private final TutorialScenes scenes = new TutorialScenes(paint, game.settings());

	private final Step[] steps = {
			new Step("Welcome", "Oh Heck is a trick-taking game using a standard deck of 52 cards. Predict how many tricks you will win, and attempt to"
					+ " win exactly that amount.",
					scenes::welcome, 4f, false),
			new Step("The cards", "Cards rank from Ace, the highest, down to 2, the lowest.", scenes::ranks, 4.5f),
			new Step("A trick", "Everyone plays one card in turn. That's a *trick*. The highest card wins it.", scenes::aTrick, 5.5f),
			new Step("Follow suit", "You must play the suit that was led if you can. Here, a heart.", scenes::followSuit, 4.5f),
			new Step("Can't follow? Play anything", "No cards of the suit led? Play any card, but only the suit led can win, unless…",
					scenes::cantFollow, 5.5f),
			new Step("Trump", "…you play a *trump*. The card turned up after the deal sets the trump suit, and any trump beats every other suit. Here a 2 beats an ace!",
					scenes::trump, 6f),
			new Step("Bidding", "Before playing, everyone bids how many tricks they'll win. Look at your cards and count your winners.",
					scenes::bidding, 6.5f, false),
			new Step("Bidding zero", "Zero is a real bid. A weak hand can score by winning nothing at all. Here you still scored with 0 tricks.", scenes::biddingZero, 9.4f),
			new Step("Getting set", "Miss your bid, high or low, and you're *set*. Here you bid 1, but both tricks go elsewhere.",
					scenes::gettingSet, 7.2f),
			new Step("The classic deal", "In a classic game, there are " + HANDS + " hands. The first deals 1 card each, the next 2,"
					+ " and so on up to " + HANDS + ".", scenes::theDeal, 6.5f),
			new Step("The dealer", "The first dealer is picked at random, then the deal passes clockwise each hand. The dealer bids"
					+ " last and has an extra rule\u2026", scenes::theDealer, 8.5f),
			new Step("The dealer's hook", "The dealer cannot bid a number which makes the total bids equal the cards in hand. Someone"
					+ " always gets an unexpected trick, thus how the game got its name: Oh Heck!", scenes::dealersHook, 5.5f, false),
			new Step("Scoring", "Make your bid exactly: " + RuleSet.STANDARD.made() + " points, plus " + RuleSet.STANDARD.perTrickBid()
					+ " for each trick you bid. Miss it, high or low: lose " + RuleSet.STANDARD.perTrickOff() + " for each trick off.",
					scenes::scoring, 10f),
			new Step("The counters", "During a game, tally marks count the hands and card icons count the bids.",
					scenes::counters, 9f),
			new Step("Ready", "Highest total points at the end wins. Good luck!", scenes::ready, 5f, false) };

	private static final float HOLD = 1.6f;

	private final Rectangle doneButton = new Rectangle(), backButton = new Rectangle();
	private boolean firstRun;
	private int step;
	private final StringBuilder sb = new StringBuilder();

	public TutorialScreen(GameHost game) {
		super(game);
	}

	public void open(boolean firstRun) {
		this.firstRun = firstRun;
		step = 0;
		scenes.restart();
	}

	@Override
	protected void update(float step) {
		float dt = step * game.options().speed;
		shownFor += dt;
		Step s = steps[this.step];
		scenes.advance(dt, s.length, s.loops, HOLD);
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);
		scenes.size(w, h);
		steps[step].scene.run();
		scenes.sparks.draw(batch, assets.glow);
		caption(steps[step]);
		header();
		batch.end();
	}

	public void next() {
		if (step < steps.length - 1) {
			step++;
			scenes.restart();
		} else {
			finish();
		}
	}

	private void finish() {
		if (firstRun) {
			game.settings().setTutorialSeen(true);
		}
		game.back();
	}

	private void previous() {
		step--;
		scenes.restart();
	}

	@Override
	protected void tap(float x, float y) {
		if (step > 0 && backButton.contains(x, y)) {
			previous();
			return;
		}
		next();
	}

	@Override
	protected void back() {
		if (step > 0) {
			previous();
		} else {
			finish();
		}
	}

	@Override
	public String stateTag() {
		return "tutorial:" + (step + 1);
	}

	private void header() {
		paint.centred(paint.serif(1.3f, Palette.GOLD), "How to play", w / 2, h - 18);
		if (step > 0) {
			paint.button(backButton, "Back", 80, h - 52, 130, 40, assets.buttonAlt);
		} else {
			backButton.set(0, 0, 0, 0);
		}
	}

	private void drawWords(Step s, float left, float top, float width) {
		BitmapFont plain = paint.ui(1.1f, Palette.CREAM), lean = paint.uiItalic(1.1f, Palette.CREAM);
		float space = plain.getSpaceXadvance(), line = plain.getLineHeight(), x = left, y = top;
		for (int i = 0; i < s.words.length; i++) {
			BitmapFont f = s.italic[i] ? lean : plain;
			paint.glyphs.setText(f, s.words[i]);
			if (x > left && x + paint.glyphs.width > left + width) {
				x = left;
				y -= line;
			}
			f.draw(batch, s.words[i], x, y);
			x += paint.glyphs.width + space;
		}
	}

	private void caption(Step s) {
		float cw = Math.min(w - 60, 860), x = (w - cw) / 2, y = 12;
		paint.frame(assets.panel, x, y, cw, CAPTION_H);
		BitmapFont hd = paint.serif(1.35f, Palette.GOLD);
		hd.draw(batch, s.heading, x + 22, y + CAPTION_H - 12);
		drawWords(s, x + 22, y + CAPTION_H - 44, cw - 44);
		BitmapFont m = paint.ui(0.9f, Palette.MUTED);
		sb.setLength(0);
		sb.append(step + 1).append(" / ").append(steps.length);
		paint.text(m, sb.toString(), x + cw - 20, y + CAPTION_H - 14, false);
		if (step == steps.length - 1) {
			paint.button(doneButton, firstRun ? "Start" : "Done", w / 2, y + CAPTION_H + 14, 220, 44, assets.button);
		} else {
			paint.text(m, "Tap to continue", x + cw - 20, y + 22, false);
		}
	}
}
