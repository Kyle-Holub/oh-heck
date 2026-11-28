package ohheck.table;

/*
 * The draw for the first dealer: the dealer's coin gathers out of sparks in the middle of the table, then spins while
 * a light runs round the table, slowing, and stops on the seat that deals, then holds there a moment.
 */
public final class DealerPick {

	public static final float APPEAR = 0.6f;
	private static final float[] TICKS = { 0, 0.1f, 0.2f, 0.3f, 0.42f, 0.56f, 0.72f, 0.92f, 1.16f, 1.46f };
	public static final float LANDED = APPEAR + TICKS[TICKS.length - 1], FLIGHT = 0.4f, SETTLED = LANDED + FLIGHT, LENGTH = LANDED + 1.1f;

	private DealerPick() {
	}

	public static int litSeat(float t, int dealer, int players) {
		if (t < APPEAR) {
			return -1;
		}
		int ticks = 0;
		while (ticks < TICKS.length && t >= APPEAR + TICKS[ticks]) {
			ticks++;
		}
		return Math.floorMod(dealer + ticks - TICKS.length, players);
	}

	public static boolean landed(float t) {
		return t >= LANDED;
	}

	public static boolean settled(float t) {
		return t >= SETTLED;
	}
}
