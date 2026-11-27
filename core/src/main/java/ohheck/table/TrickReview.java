package ohheck.table;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.Numbers;
import ohheck.ui.Painter;
import ohheck.ui.Palette;
import ohheck.ui.TapAreas;

/*
 * The hand's tricks, opened from the score card's Tricks button: each trick's winning card in order, up to seven a row so a
 * thirteen-card hand fits in two, with who won it; tap one and the whole trick lays out, every card under the
 * name of who played it, the lead marked and the winner lit.
 */
final class TrickReview {

	private static final float MARGIN_X = 70, MARGIN_Y = 40, PER_ROW = 7, CELL_GAP = 34, CARD = 0.85f, OPEN_CARD = 0.95f,
			OPEN_GAP = 48, BUTTON_W = 150, BUTTON_H = 40;
	private static final String TITLE = "Tricks - hand ", PICK = "Tap a trick to see every card", LED = "LED", BACK = "Back",
			CLOSE = "Close";

	private final Painter paint;
	private final CardArt art;
	private final TableScene scene;
	private final TableLayout layout;
	private final OverlayTargets targets;
	private String title, trickLine;
	private int titleFor = -1, trickFor = -1;

	TrickReview(Painter paint, CardArt art, TableScene scene, TableLayout layout, OverlayTargets targets) {
		this.paint = paint;
		this.art = art;
		this.scene = scene;
		this.layout = layout;
		this.targets = targets;
	}

	void draw() {
		Match m = scene.match();
		int players = m.players(), count = m.playedThisHand().size() / players;
		float pw = layout.w - 2 * MARGIN_X, ph = layout.h - 2 * MARGIN_Y, px = MARGIN_X, py = MARGIN_Y;
		paint.rect(0, 0, layout.w, layout.h, Palette.BACKDROP);
		paint.frame(paint.assets.panel, px, py, pw, ph);
		if (titleFor != m.handIndex()) {
			titleFor = m.handIndex();
			title = TITLE + (titleFor + 1);
			trickFor = -1;
		}
		paint.text(paint.serif(1.5f, Palette.GOLD_LIGHT), title, layout.w / 2, py + ph - 26, true);
		paint.button(targets.button(OverlayTargets.Button.REVIEW_CLOSE), CLOSE, px + pw - 20 - BUTTON_W / 2, py + 18, BUTTON_W, BUTTON_H,
				paint.assets.button);
		int open = targets.review();
		if (open >= 0 && open < count) {
			drawTrick(m, open, py + ph / 2);
			paint.button(targets.button(OverlayTargets.Button.REVIEW_BACK), BACK, px + 20 + BUTTON_W / 2, py + 18, BUTTON_W, BUTTON_H,
					paint.assets.buttonAlt);
		} else {
			drawWinners(m, count, py + ph - 80);
		}
	}

	private void drawWinners(Match m, int count, float top) {
		List<Rectangle> cells = targets.tricks();
		TapAreas.sizeTo(cells, count);
		float cw = Assets.CARD_W * CARD, ch = Assets.CARD_H * CARD, rowH = ch + 64;
		for (int k = 0; k < count; k++) {
			int row = (int) (k / PER_ROW), inRow = (int) Math.min(PER_ROW, count - row * PER_ROW), col = (int) (k - row * PER_ROW);
			float span = inRow * cw + (inRow - 1) * CELL_GAP, x = (layout.w - span) / 2 + col * (cw + CELL_GAP);
			float y = top - 26 - ch - row * rowH;
			int winner = winnerOf(m, k);
			cells.get(k).set(x, y, cw, ch);
			paint.pressable(cells.get(k));
			paint.text(paint.ui(0.9f, Palette.MUTED), Numbers.text(k + 1), x + cw / 2, y + ch + 22, true);
			art.shadow(x, y, cw, ch, 3);
			paint.batch.draw(paint.assets.face(card(m, k, winner)), x, y, cw, ch);
			paint.text(paint.ui(0.9f, Palette.CREAM), scene.name(winner), x + cw / 2, y - 10, true);
		}
		paint.text(paint.ui(0.95f, Palette.MUTED), PICK, layout.w / 2, top - 26 - (count > PER_ROW ? 2 : 1) * rowH - 6, true);
	}

	private void drawTrick(Match m, int k, float cy) {
		int players = m.players(), winner = winnerOf(m, k);
		List<Card> played = m.playedThisHand();
		List<Integer> seats = m.playedThisHandSeats();
		float cw = Assets.CARD_W * OPEN_CARD, ch = Assets.CARD_H * OPEN_CARD, span = players * cw + (players - 1) * OPEN_GAP;
		if (trickFor != k) {
			trickFor = k;
			trickLine = "Trick " + (k + 1) + ", won by " + scene.name(winner);
		}
		paint.text(paint.ui(1.1f, Palette.GOLD), trickLine, layout.w / 2, cy + ch / 2 + 60, true);
		for (int i = 0; i < players; i++) {
			int seat = seats.get(k * players + i);
			float x = (layout.w - span) / 2 + i * (cw + OPEN_GAP), y = cy - ch / 2;
			if (seat == winner) {
				paint.glow(x - cw * 0.4f, y - ch * 0.3f, cw * 1.8f, ch * 1.6f, Palette.EMBER, 0.55f);
			}
			if (i == 0) {
				paint.text(paint.ui(0.85f, Palette.MUTED), LED, x + cw / 2, y + ch + 20, true);
			}
			art.shadow(x, y, cw, ch, 4);
			paint.batch.draw(paint.assets.face(played.get(k * players + i)), x, y, cw, ch);
			paint.text(paint.ui(1.05f, seat == winner ? Palette.GOLD_LIGHT : Palette.CREAM), scene.name(seat), x + cw / 2, y - 14, true);
		}
	}

	private static int winnerOf(Match m, int k) {
		int players = m.players();
		List<Card> trick = m.playedThisHand().subList(k * players, (k + 1) * players);
		return m.playedThisHandSeats().get(k * players + m.rules().trickWinner(trick, m.trump()));
	}

	private static Card card(Match m, int k, int seat) {
		int players = m.players();
		for (int i = 0; i < players; i++) {
			if (m.playedThisHandSeats().get(k * players + i) == seat) {
				return m.playedThisHand().get(k * players + i);
			}
		}
		return null;
	}
}
