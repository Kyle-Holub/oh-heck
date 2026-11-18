package ohheck;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

/** The rules, from assets/files/rules.txt. */
public class RulesScreen extends BaseScreen {

	private static final Color HEADER = new Color(0.06f, 0.04f, 0.03f, 0.97f);
	private static final float HEADER_H = 56;

	private final List<String> titles, bodies;
	private final Scroller scroller = new Scroller();
	private float contentHeight;
	private final Rectangle backButton = new Rectangle();

	public RulesScreen(OhHeckGame game) {
		super(game);
		RulesText text = RulesText.parse(Gdx.files.internal("files/rules.txt").readString("UTF-8"));
		titles = text.headings;
		bodies = text.bodies;
	}

	@Override
	public void show() {
		super.show();
		scroller.reset();
	}

	@Override
	protected void drag(float dy) {
		scroller.drag(dy);
	}

	@Override
	public void render(float delta) {
		float dt = Math.min(delta, 0.1f);
		shownFor += dt * game.options.speed;
		if (game.options.autoplayHuman) {
			scroller.offset = shownFor > 1.0f ? 230 : 0;
			if (shownFor > 2.2f) {
				game.newGame();
				return;
			}
		}

		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		float colW = Math.min(w - 100, 720), x = (w - colW) / 2;
		scroller.update(dt, Gdx.input.isTouched(), contentHeight - (h - HEADER_H) + 30);

		float y = h - HEADER_H - 20 + scroller.offset;
		float top = y;
		for (int i = 0; i < titles.size(); i++) {
			BitmapFont t = serif(1.45f);
			t.setColor(GOLD);
			glyphs.setText(t, titles.get(i));
			t.draw(batch, titles.get(i), x, y);
			y -= glyphs.height + 14;
			BitmapFont b = ui(1.12f);
			b.setColor(Palette.CREAM);
			glyphs.setText(b, bodies.get(i), Palette.CREAM, colW, Align.left, true);
			b.draw(batch, bodies.get(i), x, y, colW, Align.left, true);
			y -= glyphs.height + 30;
		}
		contentHeight = top - y;

		// header bar drawn last so text scrolls under it
		rect(0, h - HEADER_H, w, HEADER_H, HEADER);
		rect(0, h - HEADER_H - 2, w, 2, Palette.GOLD);
		BitmapFont hf = serif(1.6f);
		hf.setColor(Palette.GOLD);
		centred(hf, "How to Play", w / 2, h - 16);
		button(backButton, "Back", 70, h - HEADER_H + 8, 110, HEADER_H - 16, assets.buttonAlt);
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
		game.back();
	}

	@Override
	public String stateTag() {
		return "rules";
	}
}
