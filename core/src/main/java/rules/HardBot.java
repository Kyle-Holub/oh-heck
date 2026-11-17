package rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Looks ahead. For each bid or card it could make, it deals out the cards it
 * can't see many times over, plays each deal out with everyone playing like
 * {@link NormalBot}, and picks the choice that does best on average.
 * <p>
 * The imagined deals fit what it has seen: who is out of which suit, and the
 * bids. A deal where a player who bid 3 holds nothing worth a trick is
 * unlikely, so deals are weighted by how well they explain each bid.
 * <p>
 * "Best" is its own points minus half the points of whoever it is racing: the
 * leader if it is behind, or the player in second if it is ahead, so it
 * spoils that player's bid when it can do so cheaply. It never sees anyone's
 * cards.
 */
final class HardBot implements Bot {

	/** In the last hand: the value of winning the game, and of each of our own points besides. */
	static final double WIN = 100, WIN_TIEBREAK = 0.1;
	/** Switch for the arena, to measure playing the last hand to win. */
	static boolean playToWin = true;
	/** How much spoiling the rival's hand is worth, against scoring its own. */
	static final double SPOIL = 0.5;
	/** Candidate deals drawn per deal kept, when weighting deals by the bids. */
	static final int CANDIDATES = 4;
	/**
	 * How sharply a deal is discounted per trick of difference between a player's bid and the
	 * bid that deal suggests (tuned in the arena: 0.7 and 3 both did worse).
	 */
	static final double BID_SHARPNESS = 1.5;

	private final Random rng;
	private final NormalBot policy = new NormalBot();

	HardBot(Random rng) {
		this.rng = rng;
	}

	@Override
	public int chooseBid(TableView v) {
		final List<Integer> bids = new ArrayList<Integer>();
		for (int b = 0; b <= v.handSize; b++) {
			if (v.isLegalBid(b)) {
				bids.add(b);
			}
		}
		if (bids.size() == 1) {
			return bids.get(0);
		}
		final TableView view = v;
		final int rival = rival(v);
		int pick = pick(v, bids.size(), budget(BID_DEALS_BASE + BID_DEALS_PER_CARD * v.handSize, bids.size()), new Outcome() {
			@Override
			public double of(int option, List<Card>[] deal) {
				Sim s = new Sim(view, deal);
				s.bid(view.seat, bids.get(option));
				s.finishBidding(policy);
				s.playOut(policy);
				return s.value(view.seat, rival);
			}
		});
		return bids.get(pick);
	}

	@Override
	public Card choosePlay(TableView v) {
		final List<Card> legal = v.legalPlays();
		if (legal.size() == 1) {
			return legal.get(0);
		}
		final TableView view = v;
		final int rival = rival(v);
		int deals = legal.size() <= 2 ? FEW_DEALS : v.hand.size() >= 5 ? MANY_DEALS : SOME_DEALS;
		int pick = pick(v, legal.size(), budget(deals, legal.size()), new Outcome() {
			@Override
			public double of(int option, List<Card>[] deal) {
				Sim s = new Sim(view, deal);
				s.play(view.seat, legal.get(option));
				s.playOut(policy);
				return s.value(view.seat, rival);
			}
		});
		return legal.get(pick);
	}

	// ---- how hard to think

	/**
	 * Imagined deals per decision: more for bids in big hands and for plays with many cards
	 * left, where an extra deal changes the answer most; fewer for a choice of two. The
	 * arena scales them all with {@link #effort} to measure the trade-off.
	 */
	static final int BID_DEALS_BASE = 24, BID_DEALS_PER_CARD = 8, MANY_DEALS = 80, SOME_DEALS = 48, FEW_DEALS = 24;
	static double effort = 1;
	/** Deals are played in batches; after MIN_DEALS, stop as soon as one option is clearly best. */
	static final int BATCH = 8, MIN_DEALS = 16;
	/** How clearly: the best option's lead over the next, in standard errors of that lead. */
	static final double CLEAR = 2.5;
	/** At most this many play-outs per decision (deals times options), so no decision stutters on a phone. */
	static final int MAX_PLAYOUTS = 480;

	private static int budget(int deals, int options) {
		int capped = Math.min(deals, MAX_PLAYOUTS / options);
		return Math.max(MIN_DEALS, (int) Math.round(capped * effort));
	}

	/** The value of one option played out on one imagined deal. */
	private interface Outcome {
		double of(int option, List<Card>[] deal);
	}

	/**
	 * Tries every option on the same imagined deals, a batch at a time, and returns the one
	 * that does best on average, stopping early once one option is clearly ahead.
	 */
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

	/** Is the leading option ahead of the next by more than CLEAR standard errors (comparing deal by deal)? */
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

	/** The player to race: the highest-scoring other player (the leader, or second place if we lead). */
	static int rival(TableView v) {
		int rival = -1;
		for (int s = 0; s < v.players; s++) {
			if (s != v.seat && (rival < 0 || v.scores[s] > v.scores[rival])) {
				rival = s;
			}
		}
		return rival;
	}

	// ---- imagining the unseen cards

	/**
	 * {@code samples} deals of the unseen cards to the other players, the same deals used
	 * for every choice. When reading the bids, draws several times as many and keeps them in
	 * proportion to how well each explains the bids made.
	 */
	private List<List<Card>[]> deals(TableView v, int samples) {
		List<Card> unseen = new ArrayList<Card>();
		for (Suit s : Suit.values()) {
			for (Rank r : Rank.values()) {
				Card c = Card.of(r, s);
				if (!v.hand.contains(c) && !v.played[TableView.index(c)] && c != v.trumpCard) {
					unseen.add(c);
				}
			}
		}
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
		double total = 0;
		for (int i = 0; i < n; i++) {
			List<Card>[] d = deal(v, unseen);
			candidates.add(d);
			weight[i] = likelihood(v, d);
			total += weight[i];
		}
		// keep samples of them, in proportion to their weights (evenly spaced picks)
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
		for (int s = 0; s < v.players; s++) {
			if (s != v.seat && v.bids[s] != null) {
				return true;
			}
		}
		return false;
	}

	/** How well this deal explains everyone's bids: Normal's bid with that hand against the bid made. */
	private double likelihood(TableView v, List<Card>[] deal) {
		double w = 1;
		for (int s = 0; s < v.players; s++) {
			if (s == v.seat || v.bids[s] == null) {
				continue;
			}
			TableView then = bidTimeView(v, deal, s);
			int expected = policy.chooseBid(then);
			w *= Math.exp(-BID_SHARPNESS * Math.abs(expected - v.bids[s]));
		}
		return w;
	}

	/** What player s saw when bidding, if this deal is the truth: their cards then, and the bids before theirs. */
	private static TableView bidTimeView(TableView v, List<Card>[] deal, int s) {
		TableView t = new TableView(v.players);
		t.seat = s;
		t.dealer = v.dealer;
		t.handSize = v.handSize;
		t.handIndex = v.handIndex;
		t.numberOfHands = v.numberOfHands;
		t.trumpCard = v.trumpCard;
		t.hand.addAll(deal[s]);
		for (int i = 0; i < v.history.size(); i++) {
			if (v.historySeats.get(i) == s) {
				t.hand.add(v.history.get(i));
			}
		}
		for (int k = (v.dealer + 1) % v.players; k != s; k = (k + 1) % v.players) {
			t.bids[k] = v.bids[k];
		}
		for (int k = 0; k < v.players; k++) {
			t.cardsLeft[k] = v.handSize;
			t.scores[k] = v.scores[k];
		}
		return t;
	}

	/** One random deal that fits the known voids (or ignores them if none can be found). */
	@SuppressWarnings("unchecked")
	private List<Card>[] deal(TableView v, List<Card> unseen) {
		for (int attempt = 0; attempt < 30; attempt++) {
			boolean ignoreVoids = attempt == 29;
			List<Card> pool = new ArrayList<Card>(unseen);
			Collections.shuffle(pool, rng);
			List<Card>[] hands = new List[v.players];
			boolean ok = true;
			for (int s = 0; s < v.players && ok; s++) {
				hands[s] = new ArrayList<Card>();
				if (s == v.seat) {
					hands[s].addAll(v.hand);
					continue;
				}
				for (int i = 0; i < pool.size() && hands[s].size() < v.cardsLeft[s];) {
					Card c = pool.get(i);
					if (ignoreVoids || !v.voids[s][c.suit().ordinal()]) {
						hands[s].add(c);
						pool.remove(i);
					} else {
						i++;
					}
				}
				ok = hands[s].size() == v.cardsLeft[s];
			}
			if (ok) {
				return hands;
			}
		}
		throw new IllegalStateException("Couldn't deal the unseen cards");
	}

	// ---- playing an imagined deal out

	/** The rest of the hand, played out quickly on an imagined deal. */
	private static final class Sim {
		private final int players, dealer, handSize, numberOfHands, handIndex;
		private final Card trumpCard;
		private final List<Card>[] hands;
		private final int[] bids, tricks, scores;
		private final List<Card> trick = new ArrayList<Card>();
		private final List<Integer> trickSeats = new ArrayList<Integer>();
		private final boolean[] played = new boolean[52];
		private final boolean[][] voids;
		private int current;
		private final TableView view;

		Sim(TableView v, List<Card>[] deal) {
			players = v.players;
			dealer = v.dealer;
			handSize = v.handSize;
			numberOfHands = v.numberOfHands;
			handIndex = v.handIndex;
			trumpCard = v.trumpCard;
			hands = copy(deal); // each simulation plays out its own copy of the imagined deal
			bids = new int[players];
			tricks = v.tricksWon.clone();
			scores = v.scores.clone();
			voids = new boolean[players][];
			for (int s = 0; s < players; s++) {
				bids[s] = v.bids[s] == null ? -1 : v.bids[s];
				voids[s] = v.voids[s].clone();
			}
			trick.addAll(v.trick);
			trickSeats.addAll(v.trickSeats);
			System.arraycopy(v.played, 0, played, 0, 52);
			current = v.seat;
			view = new TableView(players);
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
			current = (seat + 1) % players;
		}

		/** The players after us bid as Normal players would. */
		void finishBidding(Bot policy) {
			while (bids[dealer] < 0) {
				bid(current, policy.chooseBid(viewFor(current)));
			}
			current = (dealer + 1) % players;
		}

		void play(int seat, Card c) {
			if (!trick.isEmpty() && c.suit() != trick.get(0).suit()) {
				voids[seat][trick.get(0).suit().ordinal()] = true;
			}
			hands[seat].remove(c);
			played[TableView.index(c)] = true;
			trick.add(c);
			trickSeats.add(seat);
			if (trick.size() < players) {
				current = (seat + 1) % players;
				return;
			}
			int winner = trickSeats.get(Rules.trickWinner(trick, trumpCard.suit()));
			tricks[winner]++;
			trick.clear();
			trickSeats.clear();
			current = winner;
		}

		void playOut(Bot policy) {
			while (!hands[current].isEmpty()) {
				play(current, policy.choosePlay(viewFor(current)));
			}
		}

		/**
		 * How good this outcome is for us: our points for the hand, less SPOIL times the
		 * rival's. In the last hand, what counts is winning the game: WIN for a win (shared
		 * for a tie), plus a little for our own points so a lost cause still scores.
		 */
		double value(int me, int rival) {
			double mine = Rules.score(bids[me], tricks[me]);
			if (playToWin && handIndex == numberOfHands - 1) {
				double total = scores[me] + mine;
				int tied = 1;
				for (int s = 0; s < players; s++) {
					if (s == me) {
						continue;
					}
					double theirs = scores[s] + Rules.score(bids[s], tricks[s]);
					if (theirs > total) {
						return mine * WIN_TIEBREAK;
					}
					if (theirs == total) {
						tied++;
					}
				}
				return WIN / tied + mine * WIN_TIEBREAK;
			}
			return rival < 0 ? mine : mine - SPOIL * Rules.score(bids[rival], tricks[rival]);
		}

		/** What the given player would see now (reusing one view object). */
		private TableView viewFor(int seat) {
			TableView v = view;
			v.seat = seat;
			v.players = players;
			v.dealer = dealer;
			v.handSize = handSize;
			v.handIndex = handIndex;
			v.numberOfHands = numberOfHands;
			v.trumpCard = trumpCard;
			v.hand.clear();
			v.hand.addAll(hands[seat]);
			for (int s = 0; s < players; s++) {
				v.bids[s] = bids[s] < 0 ? null : bids[s];
				v.tricksWon[s] = tricks[s];
				v.scores[s] = scores[s];
				v.cardsLeft[s] = hands[s].size();
				System.arraycopy(voids[s], 0, v.voids[s], 0, 4);
			}
			v.trick.clear();
			v.trick.addAll(trick);
			v.trickSeats.clear();
			v.trickSeats.addAll(trickSeats);
			System.arraycopy(played, 0, v.played, 0, 52);
			return v;
		}
	}
}
