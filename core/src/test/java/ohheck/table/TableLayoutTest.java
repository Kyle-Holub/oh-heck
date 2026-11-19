package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

/** Where things go on the table, at the two shapes it's seen in: 800 wide, and a stretched phone. */
public class TableLayoutTest {

	private static final float[] WIDTHS = { 800, 1040 };

	@Test
	public void panelsAreOnScreenAndApart() {
		for (float w : WIDTHS) {
			TableLayout t = new TableLayout();
			t.resize(w, 480);
			Rectangle[] panels = new Rectangle[TableLayout.SEATS];
			for (int s = 0; s < TableLayout.SEATS; s++) {
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
	public void eachSeatPlaysIntoItsOwnSlot() {
		TableLayout t = new TableLayout();
		t.resize(800, 480);
		for (int a = 0; a < TableLayout.SEATS; a++) {
			for (int b = a + 1; b < TableLayout.SEATS; b++) {
				Vector2 pa = new Vector2(t.trickSlot(a)), pb = t.trickSlot(b);
				assertTrue(a + " and " + b, pa.dst(pb) > 20);
			}
		}
	}
}
