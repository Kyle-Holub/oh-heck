package ohheck.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/* A Match as "key=value" lines, for saving. */
final class MatchCodec {

	private MatchCodec() {
	}

	private static final String FORMAT = "oh-heck-match-3", NO_TRUMP = "-";

	static String encode(Match m) {
		StringBuilder sb = new StringBuilder(FORMAT).append('\n');
		line(sb, "rules", m.rules().code());
		line(sb, "players", String.valueOf(m.players));
		line(sb, "phase", m.phase.name());
		line(sb, "hand", String.valueOf(m.handIndex));
		line(sb, "dealer", String.valueOf(m.dealer));
		line(sb, "current", String.valueOf(m.current));
		line(sb, "trump", m.trumpCard == null ? NO_TRUMP : code(m.trumpCard));
		for (int s = 0; s < m.players; s++) {
			line(sb, "hand" + s, cards(m.hands.get(s)));
		}
		StringBuilder b = new StringBuilder();
		for (int s = 0; s < m.players; s++) {
			b.append(s > 0 ? "," : "").append(m.bids[s] == Match.NO_BID ? "-" : String.valueOf(m.bids[s]));
		}
		line(sb, "bids", b.toString());
		line(sb, "tricks", ints(m.tricks));
		line(sb, "scores", ints(m.scores));
		line(sb, "points", ints(m.lastHandPoints));
		line(sb, "trick", cards(m.trick.cards));
		line(sb, "trickSeats", seats(m.trick.seats));
		line(sb, "last", cards(m.lastTrick));
		line(sb, "lastSeats", seats(m.lastTrickSeats));
		line(sb, "lastWinner", String.valueOf(m.lastTrickWinner));
		line(sb, "history", cards(m.history));
		line(sb, "historySeats", seats(m.historySeats));
		return sb.toString();
	}

	static Match decode(String saved, Random rng) {
		try {
			String[] lines = saved.split("\n");
			String format = lines[0].trim();
			if (!format.equals(FORMAT)) {
				throw new IllegalArgumentException("Not a saved game");
			}
			Map<String, String> v = keyValues(lines);
			Match m = new Match(Integer.parseInt(req(v, "players")), rng, RuleSet.parse(req(v, "rules")));
			readHand(m, v);
			readSeats(m, v);
			readTricks(m, v);
			m.check();
			return m;
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Corrupt saved game: " + e.getMessage(), e);
		}
	}

	private static Map<String, String> keyValues(String[] lines) {
		Map<String, String> v = new HashMap<String, String>();
		for (int i = 1; i < lines.length; i++) {
			int eq = lines[i].indexOf('=');
			if (eq > 0) {
				v.put(lines[i].substring(0, eq), lines[i].substring(eq + 1).trim());
			}
		}
		return v;
	}

	private static void readHand(Match m, Map<String, String> v) {
		m.phase = Match.Phase.valueOf(req(v, "phase"));
		m.handIndex = Integer.parseInt(req(v, "hand"));
		m.dealer = Integer.parseInt(req(v, "dealer"));
		m.current = Integer.parseInt(req(v, "current"));
		String trump = req(v, "trump");
		m.trumpCard = trump.equals(NO_TRUMP) ? null : card(trump);
	}

	private static void readSeats(Match m, Map<String, String> v) {
		for (int s = 0; s < m.players; s++) {
			m.hands.set(s, parseCards(req(v, "hand" + s)));
		}
		String[] b = req(v, "bids").split(",");
		int[] t = parseInts(req(v, "tricks"), m.players), sc = parseInts(req(v, "scores"), m.players),
				pt = parseInts(req(v, "points"), m.players);
		for (int s = 0; s < m.players; s++) {
			m.bids[s] = b[s].equals("-") ? Match.NO_BID : Integer.parseInt(b[s]);
			m.tricks[s] = t[s];
			m.scores[s] = sc[s];
			m.lastHandPoints[s] = pt[s];
		}
	}

	private static void readTricks(Match m, Map<String, String> v) {
		m.trick.cards.addAll(parseCards(req(v, "trick")));
		m.trick.seats.addAll(parseSeats(req(v, "trickSeats")));
		m.lastTrick = Collections.unmodifiableList(parseCards(req(v, "last")));
		m.lastTrickSeats = Collections.unmodifiableList(parseSeats(req(v, "lastSeats")));
		m.lastTrickWinner = Integer.parseInt(req(v, "lastWinner"));
		m.history.addAll(parseCards(req(v, "history")));
		m.historySeats.addAll(parseSeats(req(v, "historySeats")));
	}

	private static void line(StringBuilder sb, String key, String value) {
		sb.append(key).append('=').append(value).append('\n');
	}

	private static String req(Map<String, String> v, String key) {
		String s = v.get(key);
		if (s == null) {
			throw new IllegalArgumentException("Missing " + key);
		}
		return s;
	}

	private static final String[] RANK_CODES = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
	private static final String SUIT_CODES = "CDHS";
	private static final Suit[] SUITS_BY_CODE = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };

	private static String code(Card c) {
		return RANK_CODES[c.rank().strength()] + SUIT_CODES.charAt(Arrays.asList(SUITS_BY_CODE).indexOf(c.suit()));
	}

	private static Card card(String code) {
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

	private static List<Card> parseCards(String s) {
		List<Card> list = new ArrayList<Card>();
		for (String p : s.isEmpty() ? new String[0] : s.split(",")) {
			list.add(card(p.trim()));
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
