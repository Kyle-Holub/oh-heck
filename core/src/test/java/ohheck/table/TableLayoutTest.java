package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

import ohheck.players.Seating;

public class TableLayoutTest {

	private static final float[] WIDTHS = { 800, 1040 };

	@Test
	public void panelsAreOnScreenAndApart() {
		for (float w : WIDTHS) {
			TableLayout t = new TableLayout();
			t.resize(w, 480);
			Rectangle[] panels = new Rectangle[Seating.SEATS];
			for (int s = 0; s < Seating.SEATS; s++) {
				panels[s] = new Rectangle(t.panelX(s), t.panelTop(s) - TableLayout.PANEL_H, TableLayout.PANEL_W,
						TableLayout.PANEL_H);
				assertTrue(w + ": seat " + s + " on screen", panels[s].x >= 0 && panels[s].x + panels[s].width <= w
						&& panels[s].y >= 0 && panels[s].y + panels[s].height <= 480);
			}
			for (int a = 0; a < panels.length; a++) {
				for (int b = a + 1; b < panels.length; b++) {
					assertTrue(w + ": seats " + a + " and " + b + " apart", !panels[a].overlaps(panels[b]));
				}
			}
		}
	}

	@Test
	public void aFullHandFitsAndIsCentred() {
		for (float w : WIDTHS) {
			TableLayout t = new TableLayout();
			t.resize(w, 480);
			t.layoutHand(8);
			float right = t.handStart + 7 * t.handGap + TableLayout.handCardW();
			assertTrue(w + ": on screen", t.handStart >= 0 && right <= w);
			assertEquals(w + ": centred", w / 2, (t.handStart + right) / 2, 0.01f);
			assertTrue(w + ": cards overlap no more than they must", t.handGap > 0);
		}
	}

	@Test
	public void yourDeckIsClearOfYourHandYourPanelAndTheLeftFan() {
		for (float w : WIDTHS) {
			TableLayout t = new TableLayout();
			t.resize(w, 480);
			Vector2 c = t.deckSpot(Seating.YOU);
			Rectangle deck = new Rectangle(c.x - TableLayout.backW() / 2, c.y - TableLayout.backH() / 2,
					TableLayout.backW(), TableLayout.backH());
			for (int n = 1; n <= 13; n++) {
				t.layoutHand(n);
				Rectangle hand = new Rectangle(t.handStart, TableLayout.HAND_Y, (n - 1) * t.handGap
						+ TableLayout.handCardW(), TableLayout.handCardH() + TableLayout.LIFT);
				assertTrue(w + ": clear of a hand of " + n, !deck.overlaps(hand));
			}
			Rectangle panel = new Rectangle(t.panelX(0), t.panelTop(0) - TableLayout.PANEL_H, TableLayout.PANEL_W,
					TableLayout.PANEL_H);
			Vector2 f = t.fanOrigin(1);
			Rectangle fan = new Rectangle(f.x, f.y, 13 * TableLayout.FAN_STEP + TableLayout.backW(), TableLayout.backH());
			assertTrue(w + ": clear of your panel", !deck.overlaps(panel));
			assertTrue(w + ": clear of the left player's cards", !deck.overlaps(fan));
		}
	}

	@Test
	public void eachSeatPlaysIntoItsOwnSlot() {
		TableLayout t = new TableLayout();
		t.resize(800, 480);
		for (int a = 0; a < Seating.SEATS; a++) {
			for (int b = a + 1; b < Seating.SEATS; b++) {
				Vector2 pa = new Vector2(t.trickSlot(a)), pb = t.trickSlot(b);
				assertTrue(a + " and " + b, pa.dst(pb) > 20);
			}
		}
	}
}
