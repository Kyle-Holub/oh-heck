package ohheck.players;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.junit.Test;

import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Rank;
import ohheck.rules.TableView;
import ohheck.rules.TestCards;

/*
 * Positions where a sensible player has an obvious move, asked of every computer player with many
 * random seeds: a habit may lean a choice, never into a careless one. Then each habit is checked to
 * show where it should.
 */
public class BotScenarioTest {

	private static final int TRIES = 40, HARD_TRIES = 12;
	private static final Match.Phase BIDDING = Match.Phase.BIDDING;

	private static final class Contender {
		final String name;
		final Function<Random, Bot> make;
		final boolean hard;

		Contender(String name, Function<Random, Bot> make, boolean hard) {
			this.name = name;
			this.make = make;
			this.hard = hard;
		}
	}

	private static List<Contender> everyone() {
		List<Contender> all = new ArrayList<Contender>();
		for (Difficulty d : Difficulty.values()) {
			all.add(new Contender("plain " + d.label, d::bot, d == Difficulty.HARD));
		}
		for (Player p : Player.ALL) {
			all.add(new Contender(p.name + " (" + p.style.label + ")", p::bot, p.level == Difficulty.HARD));
		}
		return all;
	}

	private static void everyoneAlways(Position p, String what, java.util.function.Predicate<Card> sensible) {
		TableView v = p.view();
		for (Contender c : everyone()) {
			for (long seed = 0; seed < (c.hard ? HARD_TRIES : TRIES); seed++) {
				Card card = c.make.apply(new Random(seed)).choosePlay(v);
				assertTrue(c.name + " " + what + " but played " + card + " (" + p + ")", sensible.test(card));
			}
		}
	}

	private static void everyoneBids(Position p, String what, java.util.function.IntPredicate sensible) {
		TableView v = p.view();
		for (Contender c : everyone()) {
			for (long seed = 0; seed < (c.hard ? HARD_TRIES : TRIES); seed++) {
				int bid = c.make.apply(new Random(seed)).chooseBid(v);
				assertTrue(c.name + " " + what + " but bid " + bid + " (" + p + ")", sensible.test(bid));
			}
		}
	}

	private static Card card(String name) {
		return TestCards.c(name);
	}

	@Test
	public void aZeroBidNeverLeadsAnAce() {
		Position p = Position.deal(4, "2S").me(0, "AH 4C 6D 9H").bids(0, 1, 1, 1);
		everyoneAlways(p, "bid 0 and leads", c -> c.rank() != Rank.ACE);
		Position trumps = Position.deal(4, "2D").me(0, "AS KH 3C 5S").bids(0, 1, 0, 2);
		everyoneAlways(trumps, "bid 0 and leads", c -> c.rank() != Rank.ACE);
	}

	@Test
	public void aZeroBidDucksUnderWhenItCan() {
		Position p = Position.deal(3, "2S").me(1, "AH 4H 6C").bids(1, 0, 1, 2).trick(0, "10H");
		everyoneAlways(p, "bid 0 with others still to play", c -> c.equals(card("4H")));
	}

	@Test
	public void aMadeBidDucksWhenLast() {
		Position p = Position.deal(3, "2S").me(3, "AH 3H 5C").bids(1, 0, 2, 0).trick(0, "9H KH 4H");
		everyoneAlways(p, "had nothing to gain from the trick", c -> c.equals(card("3H")));
	}

	@Test
	public void aZeroBidDoesntTrumpIn() {
		Position p = Position.deal(3, "2S").me(2, "5S 7C 8D").bids(1, 1, 0, 0).trick(0, "9H 4H");
		everyoneAlways(p, "bid 0 and could discard", c -> c.suit() != p.view().trump());
	}

	@Test
	public void needingEveryTrickWinsWhenItCan() {
		Position p = Position.deal(2, "2S").me(3, "JH 3H").bids(0, 1, 0, 2).trick(0, "9H 10H 4H");
		everyoneAlways(p, "needed every trick", c -> c.equals(card("JH")));
	}

	@Test
	public void needingEveryTrickTrumpsWhenItMust() {
		Position p = Position.deal(2, "2S").me(3, "6S 3D").bids(0, 1, 0, 2).trick(0, "9H 10H 4H");
		everyoneAlways(p, "needed every trick and was void", c -> c.equals(card("6S")));
	}

	@Test
	public void aWeakHandBidsLittle() {
		Position p = Position.deal(5, "AS").me(0, "2C 3D 4C 2H 3H");
		everyoneBids(p, "held five low side cards", b -> b <= 1);
	}

	@Test
	public void aStrongHandBidsMost() {
		Position p = Position.deal(3, "2S").me(0, "AS KS QS");
		everyoneBids(p, "held the top three trumps", b -> b >= 2);
	}

	private static double rate(Function<Random, Bot> make, int tries, java.util.function.Function<Bot, Boolean> shows) {
		int n = 0;
		for (long seed = 0; seed < tries; seed++) {
			n += shows.apply(make.apply(new Random(seed))) ? 1 : 0;
		}
		return (double) n / tries;
	}

	private static Function<Random, Bot> player(int id) {
		return Player.byId(id)::bot;
	}

	@Test
	public void theTrumpCounterBidsHisTrumps() {
		TableView v = Position.deal(5, "9S").me(0, "2S 3S 4S 5D 6C").view();
		double counter = rate(player(2), 200, b -> b.chooseBid(v) >= 3);
		double plain = rate(Difficulty.EASY::bot, 200, b -> b.chooseBid(v) >= 3);
		assertTrue("Nathaniel bid his three trumps " + counter + " of the time, plain Easy " + plain,
				counter > 0.3 && counter > plain + 0.2);
	}

	@Test
	public void theSpendthriftWinsWithHisBiggestCard() {
		TableView v = Position.deal(3, "2S").me(3, "KH 9H 4C").bids(0, 1, 0, 1).trick(0, "8H 5H 2H").view();
		double spendthrift = rate(player(3), 200, b -> b.choosePlay(v).equals(card("KH")));
		double plain = rate(Difficulty.EASY::bot, 200, b -> b.choosePlay(v).equals(card("KH")));
		assertTrue("Wilfred won with the king " + spendthrift + " of the time, plain Easy " + plain,
				spendthrift > plain + 0.2);
	}

	@Test
	public void theGreedyPlayerNeverBidsLowerAndOftenHigher() {
		Bot greedy = Player.byId(5).bot(new Random(1)), steady = Player.byId(4).bot(new Random(1));
		int higher = 0, asked = 0;
		for (long seed = 0; seed < 300; seed++) {
			Match m = Match.random(4, new Random(seed));
			for (int hand = 0; hand < 6 && m.phase() == BIDDING; hand++) {
				TableView v = m.view(m.currentSeat());
				int g = greedy.chooseBid(v), s = steady.chooseBid(v);
				assertTrue("Phillip bid " + g + " below Rupert's " + s, g >= s);
				higher += g > s ? 1 : 0;
				asked++;
				m.placeBid(m.currentSeat(), s);
			}
		}
		assertTrue("Phillip bid higher " + higher + " times in " + asked, higher > asked / 12);
	}

	@Test
	public void theVoidMakerDiscardsHisShortSuit() {
		TableView v = Position.deal(4, "2S").me(1, "5C 2D 3D 7D").bids(1, 0, 1, 1).trick(0, "9H").view();
		double maker = rate(player(10), 40, b -> b.choosePlay(v).equals(card("5C")));
		double plain = rate(Difficulty.NORMAL::bot, 40, b -> b.choosePlay(v).equals(card("5C")));
		assertTrue("Mortimer threw the singleton " + maker + " of the time, plain Normal " + plain, maker > plain + 0.5);
	}

	@Test
	public void easyPlayersRememberOnlyTheTrickInProgress() {
		TableView v = Position.deal(4, "2S").me(1, "KH 4C").bids(3, 0, 0, 0).won(2, 0, 0, 0)
				.played(0, "AH 5H 6H 7H").played(0, "JD 8D 9D 10D").trick(0, "3C").view();
		TableView remembered = EasyBot.forgetful(v);
		assertTrue(v.played(card("AH")));
		assertFalse("forgot the ace from two tricks ago", remembered.played(card("AH")));
		assertFalse("forgot the last trick too", remembered.played(card("10D")));
		assertTrue("sees the trick in progress", remembered.played(card("3C")));
		assertEquals(v.hand(), remembered.hand());
	}

	@Test
	public void forgettingChangesWhatTheyLead() {
		TableView v = Position.deal(4, "2S").me(0, "KH 4C").bids(3, 0, 0, 0).won(2, 0, 0, 0)
				.played(0, "AH 5H 6H 7H").played(0, "JD 8D 9D 10D").view();
		Card steady = new NormalBot().choosePlay(v), easy = new NormalBot().choosePlay(EasyBot.forgetful(v));
		assertEquals("a Normal player cashes the king it knows is boss", card("KH"), steady);
		assertNotEquals("an Easy player has forgotten the ace is gone", card("KH"), easy);
	}
}
