package ohheck.campaign;

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

import ohheck.util.Log;

/*
 * How far the campaign has got: the chapters whose table and duel are won, the trick shots made and
 * the badges earned. Saved as "key=value" lines, current format only.
 */
public final class Campaign {

	private static final String FORMAT = "oh-heck-campaign-2";

	private final Set<Badge> badges = EnumSet.noneOf(Badge.class);

	private final Set<Chapter> tablesWon = EnumSet.noneOf(Chapter.class), duelsWon = EnumSet.noneOf(Chapter.class);
	private final Set<String> shotsMade = new TreeSet<String>();

	public boolean open(Chapter c) {
		return c.ready && (c.ordinal() == 0 || cleared(Chapter.values()[c.ordinal() - 1]));
	}

	public boolean tableWon(Chapter c) {
		return tablesWon.contains(c);
	}

	public boolean shotsOpen(Chapter c) {
		return open(c) && tableWon(c);
	}

	public boolean duelOpen(Chapter c) {
		return open(c) && tableWon(c);
	}

	public boolean cleared(Chapter c) {
		return duelsWon.contains(c);
	}

	public boolean solved(String shot) {
		return shotsMade.contains(shot);
	}

	public int madeIn(Chapter c) {
		int n = 0;
		for (TrickShot s : TrickShots.of(c)) {
			n += solved(s.id) ? 1 : 0;
		}
		return n;
	}

	public boolean allMade(Chapter c) {
		return madeIn(c) == TrickShots.of(c).size();
	}

	public TrickShot nextTrickShot(Chapter c) {
		for (TrickShot s : TrickShots.of(c)) {
			if (!solved(s.id)) {
				return s;
			}
		}
		return TrickShots.of(c).get(0);
	}

	public void winTable(Chapter c) {
		tablesWon.add(c);
	}

	public void winDuel(Chapter c) {
		tablesWon.add(c);
		duelsWon.add(c);
	}

	public void solve(String shot) {
		shotsMade.add(shot);
	}

	public boolean has(Badge b) {
		return badges.contains(b);
	}

	public boolean earn(Badge b) {
		return badges.add(b);
	}

	public String encode() {
		return FORMAT + "\ntables=" + names(tablesWon) + "\nduels=" + names(duelsWon) + "\nshots="
				+ String.join(",", shotsMade) + "\nbadges=" + names(badges) + "\n";
	}

	public static Campaign decode(String saved) {
		Campaign c = new Campaign();
		if (saved == null || saved.isEmpty()) {
			return c;
		}
		try {
			String[] lines = saved.split("\n");
			if (!lines[0].trim().equals(FORMAT)) {
				throw new IllegalArgumentException("Not a campaign");
			}
			for (int i = 1; i < lines.length; i++) {
				int eq = lines[i].indexOf('=');
				if (eq < 0) {
					continue;
				}
				String key = lines[i].substring(0, eq), value = lines[i].substring(eq + 1).trim();
				for (String item : value.isEmpty() ? new String[0] : value.split(",")) {
					if (key.equals("tables")) {
						c.tablesWon.add(Chapter.valueOf(item));
					} else if (key.equals("duels")) {
						c.duelsWon.add(Chapter.valueOf(item));
					} else if (key.equals("shots")) {
						c.shotsMade.add(item);
					} else if (key.equals("badges")) {
						c.badges.add(Badge.valueOf(item));
					}
				}
			}
			return c;
		} catch (RuntimeException e) {
			Log.warn("Campaign", "Unreadable campaign progress; starting afresh", e);
			return new Campaign();
		}
	}

	private static String names(Set<? extends Enum<?>> items) {
		StringBuilder sb = new StringBuilder();
		for (Enum<?> c : items) {
			sb.append(sb.length() > 0 ? "," : "").append(c.name());
		}
		return sb.toString();
	}
}
