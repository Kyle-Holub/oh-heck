package ohheck.anim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.Test;

public class TweenTest {

	@Test
	public void theCurvesMatchLibGdxsExactly() {
		for (int i = 0; i <= 1000; i++) {
			float p = i / 1000f;
			assertEquals(Interpolation.pow2Out.apply(p), Ease.OUT.apply(p), 0f);
			assertEquals(Interpolation.pow2In.apply(p), Ease.IN.apply(p), 0f);
		}
	}

	@Test
	public void aTweenRunsOverItsWindow() {
		Tween t = Tween.over(2f, Ease.LINEAR).after(1f);
		assertEquals(0, t.progress(0.5f), 0);
		assertEquals(0.5f, t.progress(2f), 1e-6);
		assertEquals(1, t.progress(5f), 0);
		assertEquals(15f, t.value(2f, 10f, 20f), 1e-5);
		assertFalse(t.active(0.9f));
		assertTrue(t.active(1f));
		assertTrue(t.done(3f));
		assertEquals(3f, t.end(), 0);
	}

	@Test
	public void phasesFollowOneAnother() {
		Phases p = new Phases(0.4f, 1.2f, 0.4f);
		assertEquals(0, p.at(0.1f));
		assertEquals(1, p.at(0.5f));
		assertEquals(0.1f, p.into(0.5f), 1e-6);
		assertEquals(2, p.at(1.7f));
		assertEquals(3, p.at(5f));
		assertEquals(2f, p.length(), 1e-6);
	}
}
