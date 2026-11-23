package ohheck.campaign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ohheck.campaign.Study.Goal;

/* Every study, in the order they are met; StudiesTest checks each has a line that works. */
public final class Studies {

	private Studies() {
	}

	public static final List<Study> ALL = Collections.unmodifiableList(Arrays.asList(
			new Study("lone-ace", "The lone ace", Chapter.ONE, Goal.MAKE_IT, -1, "AS", 3, new int[] { 1, 0, 0, 1 },
					"4H 2D AH", "3C 8D 9C", "8H 6C 3D", "5S 4C 7C"),
			new Study("two-of-three", "Two from three", Chapter.ONE, Goal.MAKE_IT, -1, "4C", 1, new int[] { 2, 0, 0, 0 },
					"7C KH 4H", "6S JD 3H", "9S AD 7S", "3D KS 9H"),
			new Study("stop-horatio", "Stop Horatio", Chapter.ONE, Goal.SET_THEM, 3, "6H", 2, new int[] { 1, 0, 0, 1 },
					"9H 2H 2C", "JC 6C 5S", "3S 5D 4H", "QS 3H KD"),
			new Study("only-bid", "The only bid", Chapter.ONE, Goal.CHOOSE_THE_BID, -1, "KS", 0, new int[] { 0, 0, 0, 0 },
					"8D 3H 4S", "9C AH 4D", "6D 7D JC", "3C 10H 10C"),
			new Study("jacks-job", "The jack's job", Chapter.ONE, Goal.MAKE_IT, -1, "10S", 1, new int[] { 1, 0, 0, 1 },
					"JS KD 10D AH", "9C 2C QS 3H", "7H 3D JH 10H", "9D 8H 8S KH")));

	public static List<Study> of(Chapter c) {
		List<Study> mine = new ArrayList<Study>();
		for (Study s : ALL) {
			if (s.chapter == c) {
				mine.add(s);
			}
		}
		return mine;
	}
}
