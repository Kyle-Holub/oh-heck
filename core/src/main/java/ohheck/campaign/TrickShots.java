package ohheck.campaign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ohheck.campaign.TrickShot.Goal;

/* Every trick shot, in the order they are met; TrickShotsTest checks each has a line that works. */
public final class TrickShots {

	private TrickShots() {
	}

	public static final List<TrickShot> ALL = Collections.unmodifiableList(Arrays.asList(
			new TrickShot("lone-ace", "The lone ace", Chapter.ONE, Goal.MAKE_IT, -1, "AS", 3, new int[] { 1, 0, 0, 1 },
					"4H 2D AH", "3C 8D 9C", "8H 6C 3D", "5S 4C 7C"),
			new TrickShot("stop-horatio", "Stop Horatio", Chapter.ONE, Goal.SET_THEM, 3, "6H", 2, new int[] { 1, 0, 0, 1 },
					"9H 2H 2C", "JC 6C 5S", "3S 5D 4H", "QS 3H KD"),
			new TrickShot("only-bid", "The only bid", Chapter.ONE, Goal.CHOOSE_THE_BID, -1, "KS", 0, new int[] { 0, 0, 0, 0 },
					"8D 3H 4S", "9C AH 4D", "6D 7D JC", "3C 10H 10C")));

	public static List<TrickShot> of(Chapter c) {
		List<TrickShot> mine = new ArrayList<TrickShot>();
		for (TrickShot s : ALL) {
			if (s.chapter == c) {
				mine.add(s);
			}
		}
		return mine;
	}
}
