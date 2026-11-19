package ohheck.players;

import java.util.Arrays;

/* Who sits in each computer seat, by player id. */
public final class Seating {

	public static final int YOU = 0, SEATS = 4;
	public static final int OTHERS = SEATS - 1;

	private final int[] ids;

	private Seating(int[] ids) {
		this.ids = ids;
	}

	public static Seating of(int... ids) {
		if (ids.length != OTHERS) {
			throw new IllegalArgumentException("A table seats " + OTHERS + " computer players, not " + ids.length);
		}
		return new Seating(ids.clone());
	}

	public int idAt(int seat) {
		if (seat == YOU || seat < 0 || seat >= SEATS) {
			throw new IllegalArgumentException("No computer player in seat " + seat);
		}
		return ids[seat < YOU ? seat : seat - 1];
	}

	public Player at(int seat) {
		return Player.byId(idAt(seat));
	}

	public int[] ids() {
		return ids.clone();
	}

	public boolean onRoster() {
		for (int id : ids) {
			if (Player.byId(id) == null) {
				return false;
			}
		}
		return true;
	}

	public String encode() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length; i++) {
			sb.append(i > 0 ? "," : "").append(ids[i]);
		}
		return sb.toString();
	}

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
