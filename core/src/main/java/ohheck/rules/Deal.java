package ohheck.rules;

import java.util.ArrayList;
import java.util.List;

/* One hand's cards, a hand per seat, and the card turned up for trump. */
public record Deal(List<List<Card>> hands, Card trump) {

	public static Deal of(String trump, String... hands) {
		List<List<Card>> dealt = new ArrayList<List<Card>>();
		for (String hand : hands) {
			List<Card> cards = new ArrayList<Card>();
			for (String code : hand.trim().split("\\s+")) {
				cards.add(Card.parse(code));
			}
			dealt.add(cards);
		}
		return new Deal(dealt, Card.parse(trump));
	}
}
