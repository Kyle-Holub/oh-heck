package rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Looks ahead. For each bid or card it could make, it deals out the cards it
 * can't see many times over (only in ways that fit what it has seen, such as
 * who is out of which suit), plays each deal out with everyone playing like
 * {@link NormalBot}, and picks the choice that does best on average.
 * <p>
 * "Best" is its own points minus half the points of whoever it is racing: the
 * leader if it is behind, or the player in second if it is ahead, so it
 * spoils that player's bid when it can do so cheaply. It never sees anyone's
 * cards.
 */
final class HardBot implements Bot {

	/** Imagined deals per decision (the arena varies it to measure the trade-off). */
	static int samples = 40;
	/** How much spoiling the rival's hand is worth, against scoring its own. */
	static final double SPOIL = 0.5;

	private final Random rng;
	private final NormalBot policy = new NormalBot();

	HardBot(Random rng) {
		this.rng = rng;
	}

	@Override
	public int chooseBid(TableView v) {
		List<Integer> bids = new ArrayList<Integer>();
		for (int b = 0; b <= v.handSize; b++) {
			if (v.isLegalBid(b)) {
				bids.add(b);
			}
		}
		if (bids.size() == 1) {
			return bids.get(0);
		}
		List<List<Card>[]> deals = deals(v);
		int rival = rival(v);
		int best = bids.get(0);
		double bestValue = Double.NEGATIVE_INFINITY;
		for (int b : bids) {
			double total = 0;
			for (List<Card>[] deal : deals) {
				Sim s = new Sim(v, deal);
				s.bid(v.seat, b);
				s.finishBidding(policy);
				s.playOut(policy);
				total += s.value(v.seat, rival);
			}
			if (total > bestValue) {
				bestValue = total;
				best = b;
			}
		}
		return best;
	}

	@Override
	public Card choosePlay(TableView v) {
		List<Card> legal = v.legalPlays();
		if (legal.size() == 1) {
			return legal.get(0);
		}
		List<List<Card>[]> deals = deals(v);
		int rival = rival(v);
		Card best = legal.get(0);
		double bestValue = Double.NEGATIVE_INFINITY;
		for (Card c : legal) {
			double total = 0;
			for (List<Card>[] deal : deals) {
				Sim s = new Sim(v, deal);
				s.play(v.seat, c);
				s.playOut(policy);
				total += s.value(v.seat, rival);
			}
			if (total > bestValue) {
				bestValue = total;
				best = c;
			}
		}
		return best;
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

	/** {@link #samples} deals of the unseen cards to the other players. The same deals are used for every choice. */
	private List<List<Card>[]> deals(TableView v) {
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
		for (int i = 0; i < samples; i++) {
			deals.add(deal(v, unseen));
		}
		return deals;
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

		/** Our points for the hand, less SPOIL times the rival's. */
		double value(int me, int rival) {
			double mine = Rules.score(bids[me], tricks[me]);
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
