package ohheck.players;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Rank;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;
import ohheck.rules.TableView;
import ohheck.rules.Trick;

/*
 * Monte Carlo search: each option is played out on many imagined deals of the unseen cards,
 * weighted by how well they explain the bids, with everyone playing as NormalBot. It scores
 * its own points less half a rival's, and in the last hand plays to win. The tuning constants
 * were measured in the arena.
 */
final class HardBot implements Bot {

	static final double WIN = 100, WIN_TIEBREAK = 0.1;
	static final double SPOIL = 0.5;
	private final double spoil;
	static final int CANDIDATES = 4;
	static final double BID_SHARPNESS = 1.5;

	private final Random rng;
	private final NormalBot policy = new NormalBot();

	private final double effort;
	private final boolean playToWin;

	HardBot(Random rng) {
		this(rng, SPOIL);
	}

	HardBot(Random rng, double spoil) {
		this(rng, spoil, 1, true);
	}

	HardBot(Random rng, double spoil, double effort, boolean playToWin) {
		this.rng = rng;
		this.spoil = spoil;
		this.effort = effort;
		this.playToWin = playToWin;
	}

	@Override
	public int chooseBid(TableView v) {
		final List<Integer> bids = new ArrayList<Integer>();
		for (int b = 0; b <= v.handSize(); b++) {
			if (v.isLegalBid(b)) {
				bids.add(b);
			}
		}
		if (bids.size() == 1) {
			return bids.get(0);
		}
		final TableView view = v;
		final int rival = rival(v);
		int pick = pick(v, bids.size(), budget(BID_DEALS_BASE + BID_DEALS_PER_CARD * v.handSize(), bids.size()), new Outcome() {
			@Override
			public double of(int option, List<Card>[] deal) {
				Sim s = new Sim(view, deal);
				s.bid(view.seat(), bids.get(option));
				s.finishBidding(policy);
				s.playOut(policy);
				return s.value(view.seat(), rival, spoil, playToWin);
			}
		});
		return Bot.neverZeroWithTheTrumpAce(v, bids.get(pick));
	}

	@Override
	public Card choosePlay(TableView v) {
		final List<Card> legal = v.legalPlays();
		if (legal.size() == 1) {
			return legal.get(0);
		}
		final TableView view = v;
		final int rival = rival(v);
		int deals = legal.size() <= 2 ? FEW_DEALS : v.hand().size() >= 5 ? MANY_DEALS : SOME_DEALS;
		int pick = pick(v, legal.size(), budget(deals, legal.size()), new Outcome() {
			@Override
			public double of(int option, List<Card>[] deal) {
				Sim s = new Sim(view, deal);
				s.play(view.seat(), legal.get(option));
				s.playOut(policy);
				return s.value(view.seat(), rival, spoil, playToWin);
			}
		});
		return legal.get(pick);
	}

	static final int BID_DEALS_BASE = 24, BID_DEALS_PER_CARD = 8, MANY_DEALS = 80, SOME_DEALS = 48, FEW_DEALS = 24;
	static final int BATCH = 8, MIN_DEALS = 16;
	static final double CLEAR = 2.5;
	static final int MAX_PLAYOUTS = 480;

	private int budget(int deals, int options) {
		int capped = Math.min(deals, MAX_PLAYOUTS / options);
		return Math.max(MIN_DEALS, (int) Math.round(capped * effort));
	}

	private interface Outcome {
		double of(int option, List<Card>[] deal);
	}

	private int pick(TableView v, int options, int budget, Outcome outcome) {
		List<List<Card>[]> deals = deals(v, budget);
		double[][] value = new double[options][budget];
		double[] total = new double[options];
		int used = 0;
		while (used < budget) {
			int end = Math.min(budget, used + BATCH);
			for (int o = 0; o < options; o++) {
				for (int d = used; d < end; d++) {
					value[o][d] = outcome.of(o, deals.get(d));
					total[o] += value[o][d];
				}
			}
			used = end;
			if (used >= MIN_DEALS && clearlyBest(value, total, used)) {
				break;
			}
		}
		int best = 0;
		for (int o = 1; o < options; o++) {
			if (total[o] > total[best]) {
				best = o;
			}
		}
		return best;
	}

	private static boolean clearlyBest(double[][] value, double[] total, int n) {
		int best = 0, second = -1;
		for (int o = 1; o < total.length; o++) {
			if (total[o] > total[best]) {
				second = best;
				best = o;
			} else if (second < 0 || total[o] > total[second]) {
				second = o;
			}
		}
		double sum = 0, sumSq = 0;
		for (int d = 0; d < n; d++) {
			double diff = value[best][d] - value[second][d];
			sum += diff;
			sumSq += diff * diff;
		}
		double mean = sum / n, variance = Math.max(0, sumSq / n - mean * mean);
		return mean > CLEAR * Math.sqrt(variance / n) + 1e-9;
	}

	static int rival(TableView v) {
		int rival = -1;
		for (int s = 0; s < v.players(); s++) {
			if (s != v.seat() && (rival < 0 || v.score(s) > v.score(rival))) {
				rival = s;
			}
		}
		return rival;
	}

	private List<List<Card>[]> deals(TableView v, int samples) {
		List<Card> unseen = unseen(v);
		List<List<Card>[]> deals = new ArrayList<List<Card>[]>();
		if (!anyoneElseBid(v)) {
			for (int i = 0; i < samples; i++) {
				deals.add(deal(v, unseen));
			}
			return deals;
		}
		int n = samples * CANDIDATES;
		List<List<Card>[]> candidates = new ArrayList<List<Card>[]>(n);
		double[] weight = new double[n];
		for (int i = 0; i < n; i++) {
			List<Card>[] d = deal(v, unseen);
			candidates.add(d);
			weight[i] = likelihood(v, d);
		}
		return keepByWeight(candidates, weight, samples);
	}

	private static List<Card> unseen(TableView v) {
		List<Card> unseen = new ArrayList<Card>();
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				Card c = Card.of(r, s);
				if (v.rules().inDeck(c) && !v.hand().contains(c) && !v.played(c) && c != v.trumpCard()) {
					unseen.add(c);
				}
			}
		}
		return unseen;
	}

	private List<List<Card>[]> keepByWeight(List<List<Card>[]> candidates, double[] weight, int samples) {
		int n = candidates.size();
		double total = 0;
		for (double w : weight) {
			total += w;
		}
		List<List<Card>[]> deals = new ArrayList<List<Card>[]>();
		double step = total / samples, at = rng.nextDouble() * step, run = 0;
		for (int i = 0; i < n && deals.size() < samples; i++) {
			run += weight[i];
			while (run > at && deals.size() < samples) {
				deals.add(candidates.get(i));
				at += step;
			}
		}
		while (deals.size() < samples) {
			deals.add(candidates.get(rng.nextInt(n)));
		}
		return deals;
	}

	private static boolean anyoneElseBid(TableView v) {
		for (int s = 0; s < v.players(); s++) {
			if (s != v.seat() && v.hasBid(s)) {
				return true;
			}
		}
		return false;
	}

	private double likelihood(TableView v, List<Card>[] deal) {
		double w = 1;
		for (int s = 0; s < v.players(); s++) {
			if (s == v.seat() || !v.hasBid(s)) {
				continue;
			}
			TableView then = bidTimeView(v, deal, s);
			int expected = policy.chooseBid(then);
			w *= Math.exp(-BID_SHARPNESS * Math.abs(expected - v.bid(s)));
		}
		return w;
	}

	private static TableView bidTimeView(TableView v, List<Card>[] deal, int s) {
		TableView.Builder t = new TableView.Builder(v.players()).seat(s)
				.hand(v.handIndex(), v.numberOfHands(), v.handSize(), v.dealer(), v.trumpCard(), v.rules())
				.pickedTrump(v.pickedTrump(), false).cards(deal[s]);
		for (int i = 0; i < v.history().size(); i++) {
			if (v.historySeats().get(i) == s) {
				t.addCard(v.history().get(i));
			}
		}
		for (int k = 0; k < v.players(); k++) {
			t.seatState(k, Match.NO_BID, 0, v.score(k), v.handSize());
		}
		RuleSet rules = v.rules();
		for (int k = rules.nextSeat(v.dealer(), v.players()); k != s; k = rules.nextSeat(k, v.players())) {
			t.seatState(k, v.bid(k), 0, v.score(k), v.handSize());
		}
		return t.view();
	}

	@SuppressWarnings("unchecked")
	private List<Card>[] deal(TableView v, List<Card> unseen) {
		for (int attempt = 0; attempt < 30; attempt++) {
			boolean ignoreVoids = attempt == 29;
			List<Card> pool = new ArrayList<Card>(unseen);
			Collections.shuffle(pool, rng);
			List<Card>[] hands = new List[v.players()];
			boolean ok = true;
			for (int s = 0; s < v.players() && ok; s++) {
				hands[s] = new ArrayList<Card>();
				if (s == v.seat()) {
					hands[s].addAll(v.hand());
					continue;
				}
				for (int i = 0; i < pool.size() && hands[s].size() < v.cardsLeft(s);) {
					Card c = pool.get(i);
					if (ignoreVoids || !v.isVoid(s, c.suit())) {
						hands[s].add(c);
						pool.remove(i);
					} else {
						i++;
					}
				}
				ok = hands[s].size() == v.cardsLeft(s);
			}
			if (ok) {
				return hands;
			}
		}
		throw new IllegalStateException("Couldn't deal the unseen cards");
	}

	private static final class Sim {
		private final int players, dealer, handSize, numberOfHands, handIndex;
		private final Card trumpCard;
		private final Suit pickedTrump;
		private final List<Card>[] hands;
		private final int[] bids, tricks, scores;
		private final Trick trick;
		private final boolean[] played = new boolean[Card.COUNT];
		private final boolean[][] voids;
		private int current;
		private final TableView.Builder view;
		private final RuleSet rules;

		Sim(TableView v, List<Card>[] deal) {
			rules = v.rules();
			players = v.players();
			dealer = v.dealer();
			handSize = v.handSize();
			numberOfHands = v.numberOfHands();
			handIndex = v.handIndex();
			trumpCard = v.trumpCard();
			pickedTrump = v.pickedTrump();
			hands = copy(deal);
			bids = new int[players];
			tricks = new int[players];
			scores = new int[players];
			voids = new boolean[players][4];
			Suit[] suits = Suit.values();
			for (int s = 0; s < players; s++) {
				bids[s] = v.bid(s);
				tricks[s] = v.tricksWon(s);
				scores[s] = v.score(s);
				for (Suit suit : suits) {
					voids[s][suit.ordinal()] = v.isVoid(s, suit);
				}
			}
			trick = new Trick(players, rules);
			trick.set(v.trick(), v.trickSeats());
			for (int i = 0; i < Card.COUNT; i++) {
				played[i] = v.played(i);
			}
			current = v.seat();
			view = new TableView.Builder(players);
		}

		@SuppressWarnings("unchecked")
		private static List<Card>[] copy(List<Card>[] deal) {
			List<Card>[] c = new List[deal.length];
			for (int i = 0; i < deal.length; i++) {
				c[i] = new ArrayList<Card>(deal[i]);
			}
			return c;
		}

		void bid(int seat, int b) {
			bids[seat] = b;
			current = rules.nextSeat(seat, players);
		}

		void finishBidding(Bot policy) {
			int seat = rules.nextSeat(dealer, players);
			for (int n = 0; n < players; n++, seat = rules.nextSeat(seat, players)) {
				if (bids[seat] < 0) {
					bids[seat] = policy.chooseBid(viewFor(seat));
				}
			}
			current = rules.nextSeat(dealer, players);
		}

		void play(int seat, Card c) {
			if (trick.revealsVoid(c)) {
				voids[seat][trick.leadSuit().ordinal()] = true;
			}
			hands[seat].remove(c);
			played[c.index()] = true;
			int winner = trick.add(seat, c, pickedTrump != null ? pickedTrump : trumpCard == null ? null : trumpCard.suit());
			if (winner < 0) {
				current = rules.nextSeat(seat, players);
				return;
			}
			tricks[winner]++;
			trick.clear();
			current = winner;
		}

		void playOut(Bot policy) {
			while (!hands[current].isEmpty()) {
				play(current, policy.choosePlay(viewFor(current)));
			}
		}

		double value(int me, int rival, double spoil, boolean playToWin) {
			double mine = rules.score(bids[me], tricks[me]);
			if (playToWin && handIndex == numberOfHands - 1) {
				double total = scores[me] + mine;
				int tied = 1;
				for (int s = 0; s < players; s++) {
					if (s == me) {
						continue;
					}
					double theirs = scores[s] + rules.score(bids[s], tricks[s]);
					if (theirs > total) {
						return mine * WIN_TIEBREAK;
					}
					if (theirs == total) {
						tied++;
					}
				}
				return WIN / tied + mine * WIN_TIEBREAK;
			}
			return rival < 0 ? mine : mine - spoil * rules.score(bids[rival], tricks[rival]);
		}

		private TableView viewFor(int seat) {
			TableView.Builder v = view;
			v.seat(seat).hand(handIndex, numberOfHands, handSize, dealer, trumpCard, rules).pickedTrump(pickedTrump, false).cards(hands[seat]);
			for (int s = 0; s < players; s++) {
				v.seatState(s, bids[s], tricks[s], scores[s], hands[s].size()).voids(s, voids[s]);
			}
			return v.trick(trick.cards(), trick.seats()).played(played).view();
		}
	}
}
