package ohheck.table;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StateViewTest {

	@Test
	public void everyMomentHasItsView() {
		for (TableState s : TableState.values()) {
			assertEquals(s.name(), StateView.of(s).name());
		}
		assertEquals(TableState.values().length, StateView.values().length);
	}
}
