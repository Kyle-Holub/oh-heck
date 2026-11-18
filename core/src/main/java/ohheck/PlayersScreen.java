package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import rules.Player;

/** The twelve computer players you can meet: name, level and habit, in a list that scrolls. */
public class PlayersScreen extends BaseScreen {

	private static final float ROW_H = 62, ROW_GAP = 6, COLUMNS_H = 26, BADGE = 26;

	private final Rectangle backButton = new Rectangle();
	private final Scroller scroller = new Scroller();

	public PlayersScreen(OhHeckGame game) {
		super(game);
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
			scroller.offset = shownFor > 1.0f ? 9999 : 0; // the screenshot run sees the end of the list
			if (shownFor > 2.2f) {
				game.showTutorial(); // (the screenshot run goes on through the tutorial to a game)
				return;
			}
		}
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		float listW = Math.min(w - 60, 980), x = (w - listW) / 2;
		float listTop = h - HEADER_H - COLUMNS_H - 8;
		float content = Player.ALL.size() * (ROW_H + ROW_GAP);
		scroller.update(dt, Gdx.input.isTouched(), content - listTop + 14);

		// the columns: name, level, style, then the habit taking the rest of the row
		float cName = x + 20, cLevel = x + 190, cStyle = x + 320, cTrait = x + 470, traitW = x + listW - 20 - cTrait;
		float y = listTop + scroller.offset;
		for (Player p : Player.ALL) {
			if (y - ROW_H < h && y > 0) { // only what's on screen
				frame(assets.panel, x, y - ROW_H, listW, ROW_H);
				float mid = y - ROW_H / 2;
				BitmapFont n = serif(1.45f);
				n.setColor(Palette.CREAM);
				glyphs.setText(n, p.name);
				n.draw(batch, p.name, cName, mid + glyphs.height / 2);
				batch.draw(assets.levelBadge[p.level.ordinal()], cLevel, mid - BADGE / 2, BADGE, BADGE);
				BitmapFont f = ui(1.1f);
				f.setColor(Palette.CREAM);
				glyphs.setText(f, p.level.label);
				f.draw(batch, p.level.label, cLevel + BADGE + 8, mid + glyphs.height / 2);
				f.setColor(Palette.GOLD_LIGHT);
				glyphs.setText(f, p.style.label);
				f.draw(batch, p.style.label, cStyle, mid + glyphs.height / 2);
				BitmapFont t = ui(0.95f);
				t.setColor(Palette.MUTED);
				glyphs.setText(t, p.trait, Palette.MUTED, traitW, Align.left, true);
				t.draw(batch, p.trait, cTrait, mid + glyphs.height / 2, traitW, Align.left, true);
			}
			y -= ROW_H + ROW_GAP;
		}

		// the header and column names drawn last, so the list scrolls under them
		rect(0, h - HEADER_H - COLUMNS_H - 8, w, COLUMNS_H + 8, HEADER);
		BitmapFont c = ui(0.95f);
		c.setColor(Palette.MUTED);
		float cy = h - HEADER_H - 10;
		c.draw(batch, "Player", cName, cy);
		c.draw(batch, "Level", cLevel, cy);
		c.draw(batch, "Style", cStyle, cy);
		c.draw(batch, "Habit", cTrait, cy);
		header("Roster", backButton);
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
		return "players";
	}
}
