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
			new TrickShot("block-right", "Block the right", Chapter.ONE, Goal.SET_THEM, 3, "3H", 3, new int[] { 1, 0, 0, 1 },
					"AC 8H 2S", "7D JC AS", "3C AD 5H", "3S 7C 7H"),
			new TrickShot("only-bid", "The only bid", Chapter.ONE, Goal.CHOOSE_THE_BID, -1, "KS", 0, new int[] { 0, 0, 0, 0 },
					"8D 3H 4S", "9C AH 4D", "6D 7D JC", "3C 10H 10C"),
			new TrickShot("three-of-four", "Three of four", Chapter.TWO, Goal.MAKE_IT, -1, "5D", 1, new int[] { 3, 1, 1, 0 },
					"7D 10S AS 6H", "6S 2H 10D 10C", "9H 2C QC JS", "4S 2S AC 7S"),
			new TrickShot("squeeze-right", "Squeeze the right", Chapter.TWO, Goal.SET_THEM, 3, "7C", 0, new int[] { 3, 0, 1, 1 },
					"6S AD 8C 6D", "JD 9H 2D 10S", "QS KS 7D JH", "10D 3H JS 6H"),
			new TrickShot("big-call", "The big call", Chapter.TWO, Goal.CHOOSE_THE_BID, -1, "9S", 1, new int[] { 0, 0, 1, 1 },
					"4C 7H KH 6S", "3S 3C 7C JD", "JC 6H AC 4S", "5C 9D 3H 9H")));

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
