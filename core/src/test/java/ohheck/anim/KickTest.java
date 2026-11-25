package ohheck.anim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KickTest {

	@Test
	public void aKickSettlesBackToStill() {
		Kick k = new Kick();
		assertEquals(0, k.dx(5), 0);
		assertEquals(1, k.zoom(5), 0);
		k.shake(5, 6);
		k.zoom(5, 0.05f);
		assertTrue(k.active(5.1f));
		assertTrue(k.zoom(5.2f) < 1);
		assertEquals(0, k.dx(6), 0);
		assertEquals(1, k.zoom(6), 0);
		assertFalse(k.active(6));
	}
}
