package ohheck.table;

import java.util.List;
import java.util.function.IntPredicate;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import ohheck.ui.CardArt;
import ohheck.ui.Painter;
import ohheck.ui.TapAreas;

/*
 * The "Make Your Bid" panel: a button per bid from zero to the hand size, the ones you may not bid greyed. A greyed
 * button that is tapped shakes its head and shows a padlock.
 */
public final class BidPopup {

	public static final float H = 190;
	private static final float BUTTON = 58, GAP = 8, MIN_W = 440, TITLE_TOP = 12, TITLE_SCALE = 0.92f, ROW_RISE = 20,
			MARGIN = 40, NUMERAL_OF_BUTTON = 0.66f, PICKED_RING = 5;
	private static final Color NUMERAL_OFF = new Color(0.55f, 0.52f, 0.5f, 0.6f);

	private final Painter paint;
	private final CardArt art;
	private final Color numeral = new Color();
	private static final float REFUSE_LEN = 0.6f, REFUSE_SHAKE = 7, LOCK = 30;
	private int refused = -1;
	private float refusedFor = REFUSE_LEN;

	public void refuse(int bid) {
		refused = bid;
		refusedFor = 0;
	}

	public void step(float dt) {
		refusedFor += dt;
	}

	public boolean refusing() {
		return refusedFor < REFUSE_LEN;
	}

	public BidPopup(Painter paint, CardArt art) {
		this.paint = paint;
		this.art = art;
	}

	public void draw(float cx, float cy, int handSize, IntPredicate legal, int picked, float alpha, List<Rectangle> buttons) {
		if (alpha <= 0) {
			return;
		}
		TapAreas.sizeTo(buttons, handSize + 1);
		float rowW = (handSize + 1) * BUTTON + handSize * GAP;
		float pw = Math.max(rowW + MARGIN, MIN_W), px = cx - pw / 2, py = cy - H / 2;
		paint.fade(alpha);
		paint.frame(paint.assets.panel, px, py, pw, H);
		float titleH = paint.assets.makeBidText.getHeight() * TITLE_SCALE;
		art.art(paint.assets.makeBidText, cx, py + H - TITLE_TOP - titleH / 2, TITLE_SCALE, alpha);
		float bx = cx - rowW / 2, by = py + ROW_RISE;
		for (int b = 0; b <= handSize; b++) {
			Rectangle r = buttons.get(b).set(bx + b * (BUTTON + GAP), by, BUTTON, BUTTON);
			float p = refusedFor / REFUSE_LEN, shake = b == refused && p < 1 ? MathUtils.sin(refusedFor * 45) * REFUSE_SHAKE * (1 - p) : 0;
			r.x += shake;
			boolean ok = legal.test(b);
			if (b == picked) {
				paint.frame(paint.assets.panelTurn, r.x - PICKED_RING, r.y - PICKED_RING, r.width + 2 * PICKED_RING,
						r.height + 2 * PICKED_RING);
			}
			paint.frame(ok ? paint.assets.button : paint.assets.buttonOff, r.x, r.y, r.width, r.height);
			Color c = ok ? Color.WHITE : NUMERAL_OFF;
			paint.numeral(b, r.x + r.width / 2, r.y + r.height / 2, BUTTON * NUMERAL_OF_BUTTON, numeral.set(c.r, c.g, c.b, c.a * alpha));
			if (shake != 0) {
				paint.batch.setColor(1, 1, 1, alpha * (1 - p));
				paint.batch.draw(paint.assets.padlock, r.x + r.width - LOCK / 2 - 4, r.y + r.height - LOCK / 2 - 4, LOCK, LOCK);
				paint.batch.setColor(Color.WHITE);
				r.x -= shake;
			}
		}
		paint.fade(1f);
	}
}
