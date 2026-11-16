package rules;

import java.util.ArrayList;
import java.util.List;

/** Short card names for tests: "AS" = ace of spades, "10H" = ten of hearts. */
final class TestCards {

	private TestCards() {
	}

	static Card c(String name) {
		String r = name.substring(0, name.length() - 1);
		char s = name.charAt(name.length() - 1);
		return Card.of(rank(r), suit(s));
	}

	static List<Card> cards(String names) {
		List<Card> list = new ArrayList<Card>();
		for (String n : names.trim().split("\\s+")) {
			list.add(c(n));
		}
		return list;
	}

	private static Rank rank(String r) {
		switch (r) {
		case "A": return Rank.ACE;
		case "K": return Rank.KING;
		case "Q": return Rank.QUEEN;
		case "J": return Rank.JACK;
		case "10": return Rank.TEN;
		case "9": return Rank.NINE;
		case "8": return Rank.EIGHT;
		case "7": return Rank.SEVEN;
		case "6": return Rank.SIX;
		case "5": return Rank.FIVE;
		case "4": return Rank.FOUR;
		case "3": return Rank.THREE;
		case "2": return Rank.TWO;
		default: throw new IllegalArgumentException(r);
		}
	}

	private static Suit suit(char s) {
		switch (s) {
		case 'C': return Suit.CLUBS;
		case 'S': return Suit.SPADES;
		case 'H': return Suit.HEARTS;
		case 'D': return Suit.DIAMONDS;
		default: throw new IllegalArgumentException(String.valueOf(s));
		}
	}
}
