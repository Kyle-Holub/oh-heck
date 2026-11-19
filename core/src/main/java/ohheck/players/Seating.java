package ohheck.players;

import java.util.Arrays;

/**
 * Who sits at the table: you in seat {@link #YOU}, and a computer player (by its permanent
 * {@link Player} id) in each of the other seats. A value: it never changes once made.
 */
public final class Seating {

	/** Your seat, and how many seats the table has. */
	public static final int YOU = 0, SEATS = 4;
	/** The computer players' seats. */
	public static final int OTHERS = SEATS - 1;

	/** The other seats' players, in seat order, yours left out. */
	private final int[] ids;

	private Seating(int[] ids) {
		this.ids = ids;
	}

	/** The players with these ids in the other seats, in order (seats 1, 2, 3). */
	public static Seating of(int... ids) {
		if (ids.length != OTHERS) {
			throw new IllegalArgumentException("A table seats " + OTHERS + " computer players, not " + ids.length);
		}
		return new Seating(ids.clone());
	}

	/** The id of the computer player in this seat (any seat but {@link #YOU}). */
	public int idAt(int seat) {
		if (seat == YOU || seat < 0 || seat >= SEATS) {
			throw new IllegalArgumentException("No computer player in seat " + seat);
		}
		return ids[seat < YOU ? seat : seat - 1]; // (the others' seats, in order, skipping yours)
	}

	/** The computer player in this seat (any seat but {@link #YOU}). */
	public Player at(int seat) {
		return Player.byId(idAt(seat));
	}

	/** The ids, seat 1 first (a copy). */
	public int[] ids() {
		return ids.clone();
	}

	/** Whether every player here is still on the roster (a removed player's id means a new table). */
	public boolean onRoster() {
		for (int id : ids) {
			if (Player.byId(id) == null) {
				return false;
			}
		}
		return true;
	}

	/** "4,10,1": the ids, seat 1 first. */
	public String encode() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length; i++) {
			sb.append(i > 0 ? "," : "").append(ids[i]);
		}
		return sb.toString();
	}

	/** Reads {@link #encode()}'s form, or null if it isn't one id per seat. */
	public static Seating parse(String s) {
		String[] f = s.split(",");
		if (f.length != OTHERS) {
			return null;
		}
		try {
			int[] ids = new int[OTHERS];
			for (int i = 0; i < OTHERS; i++) {
				ids[i] = Integer.parseInt(f[i].trim());
			}
			return new Seating(ids);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Seating && Arrays.equals(ids, ((Seating) o).ids);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(ids);
	}

	@Override
	public String toString() {
		return encode();
	}
}
