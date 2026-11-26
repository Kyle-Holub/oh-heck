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
					"4C 7H KH 6S", "3S 3C 7C JD", "JC 6H AC 4S", "5C 9D 3H 9H"),
			new TrickShot("nothing-to-see", "Nothing to see", Chapter.THREE, Goal.MAKE_IT, -1, "KD", 1, new int[] { 0, 0, 0, 0 },
					"QD KS 5D AS", "5H 8H 4C 9H", "AD AH 10D 9C", "9S 2C JS 10C"),
			new TrickShot("spoil-right", "Spoil the right", Chapter.THREE, Goal.SET_THEM, 3, "2S", 3, new int[] { 1, 0, 1, 1 },
					"9H 3S 7S 7C", "10S 9S KD QH", "6H 10D 10H 7H", "JC 7D 5S JD"),
			new TrickShot("blind-zero", "A blind zero", Chapter.THREE, Goal.CHOOSE_THE_BID, -1, "AS", 1, new int[] { 0, 0, 1, 1 },
					"KC 3C AC", "AH KH 2D", "10S 3D 4D", "6H QC 10C"),
			new TrickShot("spare-trump", "A trump to spare", Chapter.FOUR, Goal.MAKE_IT, -1, "8D", 3, new int[] { 2, 1, 0, 0 },
					"QD 6H 7D KD", "8C KH JS JC", "QH JH 9D 2H", "6C 8S 5D 7S"),
			new TrickShot("long-way-round", "The long way round", Chapter.FOUR, Goal.SET_THEM, 3, "AC", 3, new int[] { 1, 0, 0, 1 },
					"AH 2D 4D 10C", "4C JH 8C 6H", "8S 9D 3S 7S", "QC JS 7H KS"),
			new TrickShot("low-trumps", "Low trumps", Chapter.FOUR, Goal.CHOOSE_THE_BID, -1, "2S", 1, new int[] { 0, 0, 1, 1 },
					"9C 4D 5S 6S", "9D 3H 8C 7S", "4H 5C 8S QC", "10C 6D 10D 2H")));

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
