package ohheck.screens;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.rules.RuleSet;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.Palette;

/* How to play: a step per scene, each with a caption; a tap moves on. */
public class TutorialScreen extends BaseScreen {

	private static final class Step {
		final String heading, caption;
		final Runnable scene;
		final float length;
		final boolean loops;

		Step(String heading, String caption, Runnable scene, float length) {
			this(heading, caption, scene, length, true);
		}

		Step(String heading, String caption, Runnable scene, float length, boolean loops) {
			this.heading = heading;
			this.caption = caption;
			this.scene = scene;
			this.length = length;
			this.loops = loops;
		}
	}

	private static final String HANDS = TutorialStage.HANDS;
	private static final float CAPTION_H = TutorialStage.CAPTION_H;

	private final TutorialScenes scenes = new TutorialScenes(paint, game.settings());

	private final Step[] steps = {
			new Step("Welcome", "Oh Heck is about calling your shots: guess how many tricks you'll win, then win exactly that many.",
					scenes::welcome, 4f, false),
			new Step("The cards", "Cards rank from Ace, the highest, down to 2, the lowest.", scenes::ranks, 4.5f),
			new Step("A trick", "Everyone plays one card in turn. That's a trick. The highest card wins it.", scenes::aTrick, 5.5f),
			new Step("Follow suit", "You must play the suit that was led if you can. Here, a heart.", scenes::followSuit, 4.5f),
			new Step("Can't follow? Play anything", "No cards of the suit led? Play any card, but only the suit led can win, unless…",
					scenes::cantFollow, 5.5f),
			new Step("Trump", "…you play a trump. The card turned up after the deal sets the trump suit, and any trump beats every other suit.",
					scenes::trump, 6f),
			new Step("The deal", "There are " + HANDS + " hands. The first deals 1 card each, the next 2, and so on up to " + HANDS
					+ ". (You can reverse this in Options.)", scenes::theDeal, 6.5f),
			new Step("Bidding", "Before playing, everyone bids how many tricks they'll win. Look at your cards and count your winners.",
					scenes::bidding, 9.5f),
			new Step("Bidding zero", "Zero is a real bid. A weak hand can score well by winning nothing at all.", scenes::biddingZero, 7f),
			new Step("The dealer's hook", "The dealer bids last and can't make the bids add up to the number of tricks, so someone always misses.",
					scenes::dealersHook, 5.5f),
			new Step("The counters", "Top left of the table: tally marks count the " + HANDS
					+ " hands, and under them one card per trick shows how many the table has bid.", scenes::counters, 9f),
			new Step("Scoring", "Make your bid exactly: " + RuleSet.STANDARD.made() + " points, plus " + RuleSet.STANDARD.perTrickBid()
					+ " for each trick you bid. Miss it, high or low: lose " + RuleSet.STANDARD.perTrickOff() + " for each trick off.",
					scenes::scoring, 9f),
			new Step("Ready", "Highest total after " + HANDS + " hands wins. Good luck!", scenes::ready, 5f, false) };

	private static final float HOLD = 1.6f;

	private final Rectangle cornerButton = new Rectangle(), doneButton = new Rectangle();
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
			game.startCampaign();
		} else {
			game.back();
		}
	}

	@Override
	protected void tap(float x, float y) {
		if (cornerButton.contains(x, y)) {
			if (firstRun) {
				finish();
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

	private void header() {
		BitmapFont f = paint.serif(1.3f, Palette.GOLD);
		f.draw(batch, "How to play", 22, h - 18);
		paint.button(cornerButton, firstRun ? "Skip tutorial" : "Back", w - 110, h - 52, 190, 40, assets.buttonAlt);
	}

	private void caption(Step s) {
		float cw = Math.min(w - 60, 860), x = (w - cw) / 2, y = 12;
		paint.frame(assets.panel, x, y, cw, CAPTION_H);
		BitmapFont hd = paint.serif(1.35f, Palette.GOLD);
		hd.draw(batch, s.heading, x + 22, y + CAPTION_H - 12);
		BitmapFont c = paint.ui(1.1f, Palette.CREAM);
		c.draw(batch, s.caption, x + 22, y + CAPTION_H - 44, cw - 44, Align.left, true);
		BitmapFont m = paint.ui(0.9f, Palette.MUTED);
		sb.setLength(0);
		sb.append(step + 1).append(" / ").append(steps.length);
		paint.text(m, sb.toString(), x + cw - 20, y + CAPTION_H - 14, false);
		if (step == steps.length - 1) {
			paint.button(doneButton, "Start", w / 2, y + CAPTION_H + 14, 220, 44, assets.button);
		} else {
			paint.text(m, "Tap to continue", x + cw - 20, y + 22, false);
		}
	}
}
