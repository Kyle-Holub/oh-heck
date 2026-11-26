package ohheck.campaign;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ohheck.util.Log;

/*
 * How far the campaign has got: the stages won in each chapter, the trick shots made and the badges
 * earned. Saved as "key=value" lines, a line of chapters per stage, current format only.
 */
public final class Campaign {

	private static final String FORMAT = "oh-heck-campaign-3";

	private final Set<Badge> badges = EnumSet.noneOf(Badge.class);

	private final Map<Stage, Set<Chapter>> won = new EnumMap<Stage, Set<Chapter>>(Stage.class);
	private final Set<String> shotsMade = new TreeSet<String>();

	public boolean open(Chapter c) {
		return c.ready && unlocked(c);
	}

	public boolean unlocked(Chapter c) {
		return c.ordinal() == 0 || cleared(Chapter.values()[c.ordinal() - 1]);
	}

	public Campaign() {
		for (Stage s : Stage.values()) {
			won.put(s, EnumSet.noneOf(Chapter.class));
		}
	}

	public boolean won(Chapter c, Stage s) {
		return won.get(s).contains(c);
	}

	public boolean open(Chapter c, Stage s) {
		return open(c) && (s.ordinal() == 0 || won(c, Stage.values()[s.ordinal() - 1]));
	}

	public boolean shotsOpen(Chapter c) {
		return open(c) && won(c, Stage.TABLE);
	}

	public boolean cleared(Chapter c) {
		return won(c, Stage.DUEL);
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

	public void win(Chapter c, Stage s) {
		for (Stage before : Stage.values()) {
			if (before.ordinal() <= s.ordinal()) {
				won.get(before).add(c);
			}
		}
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
		StringBuilder sb = new StringBuilder(FORMAT).append('\n');
		for (Stage s : Stage.values()) {
			sb.append(s.key).append('=').append(names(won.get(s))).append('\n');
		}
		return sb.append("shots=").append(String.join(",", shotsMade)).append("\nbadges=").append(names(badges)).append('\n')
				.toString();
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
				Stage stage = stageKeyed(key);
				for (String item : value.isEmpty() ? new String[0] : value.split(",")) {
					if (stage != null) {
						c.won.get(stage).add(Chapter.valueOf(item));
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

	private static Stage stageKeyed(String key) {
		for (Stage s : Stage.values()) {
			if (s.key.equals(key)) {
				return s;
			}
		}
		return null;
	}

	private static String names(Set<? extends Enum<?>> items) {
		StringBuilder sb = new StringBuilder();
		for (Enum<?> c : items) {
			sb.append(sb.length() > 0 ? "," : "").append(c.name());
		}
		return sb.toString();
	}
}
