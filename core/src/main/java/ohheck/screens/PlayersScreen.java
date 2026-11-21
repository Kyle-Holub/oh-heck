package ohheck.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import ohheck.players.Player;
import ohheck.ui.BaseScreen;
import ohheck.ui.GameHost;
import ohheck.ui.MenuChrome;
import ohheck.ui.Palette;
import ohheck.ui.Scroller;

/* The roster of computer players. */
public class PlayersScreen extends BaseScreen {

	private final MenuChrome chrome = new MenuChrome(paint);

	private static final float ROW_H = 62, ROW_GAP = 6, COLUMNS_H = 26, BADGE = 26;

	private final Rectangle backButton = new Rectangle();
	private final Scroller scroller = new Scroller();

	public void scrollToEnd() {
		scroller.scrollTo(Float.MAX_VALUE);
	}

	public PlayersScreen(GameHost game) {
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
	protected void update(float step) {
		shownFor += step * game.options().speed;
	}

	@Override
	protected void draw() {
		ScreenUtils.clear(Palette.BG);
		begin();
		batch.draw(assets.table, 0, 0, w, h);

		float listW = Math.min(w - 60, 980), x = (w - listW) / 2;
		float listTop = h - MenuChrome.HEADER_H - COLUMNS_H - 8;
		float content = Player.ALL.size() * (ROW_H + ROW_GAP);
		scroller.update(frameDelta(), Gdx.input.isTouched(), content - listTop + 14);
		layoutColumns(x, listW);
		float y = listTop + scroller.offset();
		for (Player p : Player.ALL) {
			if (y - ROW_H < h && y > 0) {
				drawRow(p, x, y, listW);
			}
			y -= ROW_H + ROW_GAP;
		}
		drawColumnHeads();
		chrome.header("Roster", backButton, w, h);
		batch.end();
	}
	private float colName, colLevel, colStyle, colTrait, traitW;

	private void layoutColumns(float x, float listW) {
		colName = x + 20;
		colLevel = x + 190;
		colStyle = x + 320;
		colTrait = x + 470;
		traitW = x + listW - 20 - colTrait;
	}

	private void drawRow(Player p, float x, float y, float listW) {
		paint.frame(assets.panel, x, y - ROW_H, listW, ROW_H);
		float mid = y - ROW_H / 2;
		BitmapFont n = paint.serif(1.45f, Palette.CREAM);
		glyphs.setText(n, p.name);
		n.draw(batch, p.name, colName, mid + glyphs.height / 2);
		batch.draw(assets.levelBadge[p.level.ordinal()], colLevel, mid - BADGE / 2, BADGE, BADGE);
		BitmapFont f = paint.ui(1.1f, Palette.CREAM);
		glyphs.setText(f, p.level.label);
		f.draw(batch, p.level.label, colLevel + BADGE + 8, mid + glyphs.height / 2);
		f.setColor(Palette.GOLD_LIGHT);
		glyphs.setText(f, p.style.label);
		f.draw(batch, p.style.label, colStyle, mid + glyphs.height / 2);
		BitmapFont t = paint.ui(0.95f, Palette.MUTED);
		glyphs.setText(t, p.trait, Palette.MUTED, traitW, Align.left, true);
		t.draw(batch, p.trait, colTrait, mid + glyphs.height / 2, traitW, Align.left, true);
	}

	private void drawColumnHeads() {
		paint.rect(0, h - MenuChrome.HEADER_H - COLUMNS_H - 8, w, COLUMNS_H + 8, MenuChrome.HEADER);
		BitmapFont c = paint.ui(0.95f, Palette.MUTED);
		float cy = h - MenuChrome.HEADER_H - 10;
		c.draw(batch, "Player", colName, cy);
		c.draw(batch, "Level", colLevel, cy);
		c.draw(batch, "Style", colStyle, cy);
		c.draw(batch, "Habit", colTrait, cy);
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
