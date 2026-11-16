package ohheck;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

/** The rules, from the desktop game's rules.txt, plus the phone controls. */
public class RulesScreen extends BaseScreen {

	/** Headings for the first nine paragraphs of rules.txt (the tenth describes the desktop screen). */
	private static final String[] HEADINGS = { "How to Play Oh Heck", "Cards", "Hands", "Objective", "Dealing",
			"Trump Suit", "Bidding", "Play", "Scoring" };

	private static final String CONTROLS = "Bidding: tap a number to choose your bid, then tap it again to confirm. "
			+ "As dealer you can't make the bid that would make everyone's bids add up to the number of tricks; "
			+ "that number is greyed out.\n\n"
			+ "Playing: the cards you may play are raised and the rest are dimmed. Tap a card to lift it, then tap it "
			+ "again to play it, or drag it up onto the table.\n\n"
			+ "Tap anywhere to skip the deal or to move on after a hand. The Menu button or your phone's back button "
			+ "pauses the game.";

	private static final Color HEADER = new Color(0.05f, 0.05f, 0.05f, 0.95f);
	private static final Color DIMMED = new Color(0.35f, 0.35f, 0.35f, 1f);
	private static final float HEADER_H = 56;

	private final List<String> titles = new ArrayList<String>();
	private final List<String> bodies = new ArrayList<String>();
	private float scroll, velocity, contentHeight;
	private Rectangle backButton = new Rectangle();

	public RulesScreen(OhHeckGame game) {
		super(game);
		String[] lines = Gdx.files.internal("files/rules.txt").readString("UTF-8").split("\\r?\\n");
		for (int i = 0; i < HEADINGS.length && i < lines.length; i++) {
			titles.add(HEADINGS[i]);
			bodies.add(lines[i].trim());
		}
		titles.add("Controls");
		bodies.add(CONTROLS);
	}

	@Override
	public void show() {
		super.show();
		scroll = 0;
		velocity = 0;
	}

	@Override
	protected void drag(float dy) {
		scroll += dy;
		velocity = dy * 60f;
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f);
		shownFor += dt * game.options.speed;
		if (!Gdx.input.isTouched()) {
			scroll += velocity * dt;
			velocity *= Math.pow(0.04, dt);
		}
		if (game.options.autoplayHuman) {
			scroll = shownFor > 1.0f ? 230 : 0;
			if (shownFor > 2.2f) {
				game.showOptions();
				return;
			}
		}

		ScreenUtils.clear(Color.BLACK);
		begin();
		batch.setColor(DIMMED);
		batch.draw(assets.table, 0, 0, w, h);
		batch.setColor(Color.WHITE);

		float colW = Math.min(w - 100, 720), x = (w - colW) / 2;
		float maxScroll = Math.max(0, contentHeight - (h - HEADER_H) + 30);
		scroll = MathUtils.clamp(scroll, 0, maxScroll);

		float y = h - HEADER_H - 20 + scroll;
		float top = y;
		for (int i = 0; i < titles.size(); i++) {
			BitmapFont t = serif(1.45f);
			t.setColor(GOLD);
			glyphs.setText(t, titles.get(i));
			t.draw(batch, titles.get(i), x, y);
			y -= glyphs.height + 14;
			BitmapFont b = ui(1.12f);
			b.setColor(Color.WHITE);
			glyphs.setText(b, bodies.get(i), Color.WHITE, colW, Align.left, true);
			b.draw(batch, bodies.get(i), x, y, colW, Align.left, true);
			y -= glyphs.height + 30;
		}
		contentHeight = top - y;

		// header bar drawn last so text scrolls under it
		rect(0, h - HEADER_H, w, HEADER_H, HEADER);
		BitmapFont hf = serif(1.6f);
		hf.setColor(Color.WHITE);
		centred(hf, "How to Play", w / 2, h - 16);
		backButton = button("Back", 70, h - HEADER_H + 8, 110, HEADER_H - 16, BUTTON);
		batch.end();
	}

	@Override
	protected void tap(float x, float y) {
		if (backButton.contains(x, y)) {
			back();
		}
	}

	@Override
	protected void back() {
		game.showTitle();
	}

	@Override
	public String stateTag() {
		return "rules";
	}
}
