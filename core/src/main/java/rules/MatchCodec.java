package rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A {@link Match} as text, for saving: "key=value" lines under a format line. Reads its
 * older format too (cards as numbers); the tests in SaveTest cover both.
 */
final class MatchCodec {

	private MatchCodec() {
	}

	/** Cards written as text ("AS", "10H"). */
	private static final String FORMAT = "oh-heck-match-2";
	/** The first format, which wrote cards as numbers from the enums' declaration order; still read. */
	private static final String FORMAT_1 = "oh-heck-match-1";

	static String encode(Match m) {
		StringBuilder sb = new StringBuilder(FORMAT).append('\n');
		line(sb, "rules", m.rules().name);
		line(sb, "players", String.valueOf(m.players));
		line(sb, "phase", m.phase.name());
		line(sb, "hand", String.valueOf(m.handIndex));
		line(sb, "order", m.order.name());
		line(sb, "dealer", String.valueOf(m.dealer));
		line(sb, "current", String.valueOf(m.current));
		line(sb, "trump", code(m.trumpCard));
		for (int s = 0; s < m.players; s++) {
			line(sb, "hand" + s, cards(m.hands.get(s)));
		}
		StringBuilder b = new StringBuilder();
		for (int s = 0; s < m.players; s++) {
			b.append(s > 0 ? "," : "").append(m.bids[s] == null ? "-" : m.bids[s].toString());
		}
		line(sb, "bids", b.toString());
		line(sb, "tricks", ints(m.tricks));
		line(sb, "scores", ints(m.scores));
		line(sb, "points", ints(m.lastHandPoints));
		line(sb, "trick", cards(m.trick));
		line(sb, "trickSeats", seats(m.trickSeats));
		line(sb, "last", cards(m.lastTrick));
		line(sb, "lastSeats", seats(m.lastTrickSeats));
		line(sb, "lastWinner", String.valueOf(m.lastTrickWinner));
		line(sb, "history", cards(m.history));
		line(sb, "historySeats", seats(m.historySeats));
		return sb.toString();
	}

	/** @throws IllegalArgumentException if the text is not a valid saved game */
	static Match decode(String saved, Random rng) {
		try {
			java.util.Map<String, String> v = new java.util.HashMap<String, String>();
			String[] lines = saved.split("\n");
			String format = lines[0].trim();
			if (!format.equals(FORMAT) && !format.equals(FORMAT_1)) {
				throw new IllegalArgumentException("Not a saved game");
			}
			boolean legacy = format.equals(FORMAT_1);
			for (int i = 1; i < lines.length; i++) {
				int eq = lines[i].indexOf('=');
				if (eq > 0) {
					v.put(lines[i].substring(0, eq), lines[i].substring(eq + 1).trim());
				}
			}
			// (games saved before there were rule sets were all standard)
			RuleSet rules = v.containsKey("rules") ? RuleSet.named(v.get("rules")) : RuleSet.STANDARD;
			Match m = new Match(Integer.parseInt(req(v, "players")), rng, rules);
			m.phase = Match.Phase.valueOf(req(v, "phase"));
			m.handIndex = Integer.parseInt(req(v, "hand"));
			// (games saved before the hands could run downwards always ran up)
			m.order = v.containsKey("order") ? HandOrder.valueOf(v.get("order")) : HandOrder.UP;
			m.dealer = Integer.parseInt(req(v, "dealer"));
			m.current = Integer.parseInt(req(v, "current"));
			m.trumpCard = card(req(v, "trump"), legacy);
			for (int s = 0; s < m.players; s++) {
				m.hands.set(s, parseCards(req(v, "hand" + s), legacy));
			}
			String[] b = req(v, "bids").split(",");
			int[] t = parseInts(req(v, "tricks"), m.players), sc = parseInts(req(v, "scores"), m.players),
					pt = parseInts(req(v, "points"), m.players);
			for (int s = 0; s < m.players; s++) {
				m.bids[s] = b[s].equals("-") ? null : Integer.valueOf(b[s]);
				m.tricks[s] = t[s];
				m.scores[s] = sc[s];
				m.lastHandPoints[s] = pt[s];
			}
			m.trick.addAll(parseCards(req(v, "trick"), legacy));
			m.trickSeats.addAll(parseSeats(req(v, "trickSeats")));
			m.lastTrick = Collections.unmodifiableList(parseCards(req(v, "last"), legacy));
			m.lastTrickSeats = Collections.unmodifiableList(parseSeats(req(v, "lastSeats")));
			m.lastTrickWinner = Integer.parseInt(req(v, "lastWinner"));
			// (games saved before alpha-1.0.8's smarter players have no history: they just start forgetful)
			if (v.containsKey("history") && v.containsKey("historySeats")) {
				m.history.addAll(parseCards(v.get("history"), legacy));
				m.historySeats.addAll(parseSeats(v.get("historySeats")));
			}
			m.check();
			return m;
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Corrupt saved game: " + e.getMessage(), e);
		}
	}

	private static void line(StringBuilder sb, String key, String value) {
		sb.append(key).append('=').append(value).append('\n');
	}

	private static String req(java.util.Map<String, String> v, String key) {
		String s = v.get(key);
		if (s == null) {
			throw new IllegalArgumentException("Missing " + key);
		}
		return s;
	}

	// Card codes: rank then suit letter, e.g. "AS", "10H", "QD". Independent of the enums' order.
	private static final String[] RANK_CODES = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
	private static final String SUIT_CODES = "CDHS";
	private static final Suit[] SUITS_BY_CODE = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };
	// The first format's numbering: suit * 13 + rank, in these orders
	private static final Rank[] LEGACY_RANKS = { Rank.ACE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX,
			Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING };
	private static final Suit[] LEGACY_SUITS = { Suit.CLUBS, Suit.SPADES, Suit.HEARTS, Suit.DIAMONDS };

	private static String code(Card c) {
		return RANK_CODES[c.rank().strength()] + SUIT_CODES.charAt(java.util.Arrays.asList(SUITS_BY_CODE).indexOf(c.suit()));
	}

	private static Card card(String code, boolean legacy) {
		if (legacy) {
			int n = Integer.parseInt(code);
			return Card.of(LEGACY_RANKS[n % 13], LEGACY_SUITS[n / 13]);
		}
		int suit = SUIT_CODES.indexOf(code.charAt(code.length() - 1));
		String rank = code.substring(0, code.length() - 1);
		for (Rank r : Rank.values()) {
			if (RANK_CODES[r.strength()].equals(rank) && suit >= 0) {
				return Card.of(r, SUITS_BY_CODE[suit]);
			}
		}
		throw new IllegalArgumentException("Not a card: " + code);
	}

	private static String cards(List<Card> cs) {
		StringBuilder sb = new StringBuilder();
		for (Card c : cs) {
			sb.append(sb.length() > 0 ? "," : "").append(code(c));
		}
		return sb.toString();
	}

	private static List<Card> parseCards(String s, boolean legacy) {
		List<Card> list = new ArrayList<Card>();
		for (String p : s.isEmpty() ? new String[0] : s.split(",")) {
			list.add(card(p.trim(), legacy));
		}
		return list;
	}

	private static String seats(List<Integer> seats) {
		StringBuilder sb = new StringBuilder();
		for (Integer i : seats) {
			sb.append(sb.length() > 0 ? "," : "").append(i);
		}
		return sb.toString();
	}

	private static List<Integer> parseSeats(String s) {
		List<Integer> list = new ArrayList<Integer>();
		for (String p : s.isEmpty() ? new String[0] : s.split(",")) {
			list.add(Integer.valueOf(p));
		}
		return list;
	}

	private static String ints(int[] a) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			sb.append(i > 0 ? "," : "").append(a[i]);
		}
		return sb.toString();
	}

	private static int[] parseInts(String s, int n) {
		String[] p = s.split(",");
		if (p.length != n) {
			throw new IllegalArgumentException("Expected " + n + " values");
		}
		int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = Integer.parseInt(p[i]);
		}
		return a;
	}
}
