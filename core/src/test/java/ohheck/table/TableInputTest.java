package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;

import ohheck.players.Bot;
import ohheck.players.Difficulty;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;
import ohheck.ui.TapAreas;
import ohheck.ui.Wording;

public class TableInputTest {

	static final class FakeTable implements TableScene, TableActions {
		final TableFlow flow = new TableFlow();
		final TableLayout layout = new TableLayout();
		final OverlayTargets targets = new OverlayTargets();
		final TableInput input = new TableInput(this, this, targets);
		final List<Card> handOrder = new ArrayList<Card>();
		final List<Rectangle> handRects = new ArrayList<Rectangle>();
		final List<String> did = new ArrayList<String>();
		TableState pretend;
		boolean results;

		FakeTable() {
			flow.seat(Seating.of(4, 5, 6), new Random(1));
		}

		@Override
		public TableFlow flow() {
			return flow;
		}

		@Override
		public TableState state() {
			return pretend != null ? pretend : flow.state();
		}

		@Override
		public float time() {
			return 0;
		}

		@Override
		public String name(int seat) {
			return "Seat " + seat;
		}

		@Override
		public TableLayout layout() {
			return layout;
		}

		@Override
		public List<Card> handOrder() {
			return handOrder;
		}

		@Override
		public List<Rectangle> handRects() {
			return handRects;
		}

		@Override
		public boolean humanCanPlay() {
			return !input.menuOpen() && flow.yourTurnToPlay();
		}

		@Override
		public boolean isMenuOpen() {
			return input.menuOpen();
		}

		@Override
		public boolean isDragging() {
			return input.dragging();
		}

		@Override
		public Card pressedCard() {
			return input.pressed();
		}

		@Override
		public float dragX() {
			return input.dragX();
		}

		@Override
		public float dragY() {
			return input.dragY();
		}

		@Override
		public boolean resultsShown() {
			return results;
		}

		@Override
		public void skipGameEnd() {
			did.add("skip end");
		}

		@Override
		public boolean bid(int b) {
			return flow.bid(b);
		}

		@Override
		public void play(Card c) {
			did.add("play " + c);
			flow.play(c);
		}

		@Override
		public boolean swapWidow(Card give, Card take) {
			did.add("swap " + give);
			return flow.swapWidow(give, take);
		}

		@Override
		public boolean pickTrump(Suit suit) {
			did.add("pick " + suit);
			return flow.pickTrump(suit);
		}

		@Override
		public boolean skip() {
			return flow.skip();
		}

		@Override
		public boolean nextHand() {
			return flow.nextHand();
		}

		@Override
		public void hint(String s) {
			did.add("hint " + s);
		}

		@Override
		public void showHighScores() {
			did.add("high scores");
		}

		@Override
		public void newGame() {
			did.add("new game");
		}

		@Override
		public void showOptions() {
			did.add("options");
		}

		@Override
		public void showTitle() {
			did.add("title");
		}
	}

	private final FakeTable t = new FakeTable();

	private void skipToTurn() {
		for (int i = 0; i < 3 && t.flow.state() != TableState.TURN; i++) {
			t.flow.skip();
		}
	}

	@Test
	public void theMenuOpensAndGoesWhereItSays() {
		t.flow.start(Match.random(4, new Random(2)));
		t.targets.button(OverlayTargets.Button.MENU).set(700, 0, 80, 40);
		t.input.tap(740, 20);
		assertTrue(t.input.menuOpen());
		for (MenuItem item : MenuItem.values()) {
			t.targets.menuItem(item).set(300, 300 - item.ordinal() * 60, 200, 50);
		}
		t.input.tap(400, 260);
		assertEquals("options", t.did.get(0));
		assertTrue("the menu waits for your return", t.input.menuOpen());
		t.input.tap(400, 320);
		assertFalse(t.input.menuOpen());
		t.input.back();
		assertTrue(t.input.menuOpen());
		t.input.back();
		assertFalse(t.input.menuOpen());
	}

	@Test
	public void atGameOverOnlyTheButtonsCount() {
		t.flow.start(Match.random(4, new Random(3)));
		t.pretend = TableState.GAME_OVER;
		t.results = true;
		t.targets.button(OverlayTargets.Button.CONTINUE).set(200, 50, 170, 42);
		t.targets.button(OverlayTargets.Button.MAIN_MENU).set(400, 50, 170, 42);
		t.input.tap(100, 300);
		assertTrue(t.did.isEmpty());
		t.input.tap(250, 70);
		t.input.tap(450, 70);
		assertEquals("[high scores, title]", t.did.toString());
	}

	@Test
	public void aTapOnANumberBids() {
		Match m = new Match(4, new Random(4), 3, RuleSet.STANDARD);
		t.flow.start(m);
		skipToTurn();
		assertEquals(TableState.TURN, t.flow.state());
		TapAreas.sizeTo(t.targets.bids(), m.handSize() + 1);
		for (int b = 0; b <= m.handSize(); b++) {
			t.targets.bid(b).set(100 + b * 70, 100, 58, 58);
		}
		t.input.tap(105, 105);
		assertTrue(m.hasBid(Seating.YOU));
		assertEquals(0, m.bid(Seating.YOU));
	}

	@Test
	public void aCardDraggedOntoTheTableIsPlayed() {
		Match m = new Match(4, new Random(5), 3, RuleSet.STANDARD);
		t.flow.start(m);
		skipToTurn();
		TapAreas.sizeTo(t.targets.bids(), m.handSize() + 1);
		t.targets.bid(0).set(100, 100, 58, 58);
		t.input.tap(105, 105);
		while (!t.flow.yourTurnToPlay()) {
			t.flow.update(1 / 30f);
		}
		Card c = m.hand(Seating.YOU).get(0);
		t.handOrder.add(c);
		t.handRects.add(new Rectangle(300, 10, 80, 110));
		t.input.touchDown(340, 60);
		t.input.touchDragged(340, 200);
		assertTrue(t.input.dragging());
		assertTrue(t.input.touchUp(340, 260));
		assertEquals("play " + c, t.did.get(0));
	}

	@Test
	public void aTapBeforeTheScoresSkipsTheGameEnd() {
		t.flow.start(Match.random(4, new Random(3)));
		t.pretend = TableState.GAME_OVER;
		t.input.tap(300, 300);
		assertEquals("[skip end]", t.did.toString());
	}

	@Test
	public void aButtonNotYetDrawnCantBeHit() {
		t.flow.start(Match.random(4, new Random(2)));
		t.input.tap(0, 0);
		assertFalse(t.input.menuOpen());
		t.targets.clearGameOverButtons();
		t.pretend = TableState.GAME_OVER;
		t.results = true;
		t.input.tap(0, 0);
		assertTrue(t.did.isEmpty());
	}

	@Test
	public void aCardThatDoesntFollowSuitGivesAHint() {
		for (long seed = 1; seed < 50; seed++) {
			FakeTable f = new FakeTable();
			Match m = new Match(4, new Random(seed), 2, RuleSet.STANDARD.withOrder(HandOrder.DOWN));
			f.flow.start(m);
			f.flow.skip();
			while (!f.flow.yourTurnToPlay()) {
				if (f.flow.yourTurnToBid()) {
					f.flow.bid(m.isLegalBid(0) ? 0 : 1);
				}
				f.flow.update(1 / 30f);
			}
			for (Card c : m.hand(Seating.YOU)) {
				if (!m.legalPlays(Seating.YOU).contains(c)) {
					f.handOrder.add(c);
					f.handRects.add(new Rectangle(300, 10, 80, 110));
					f.input.tap(340, 60);
					assertEquals("[hint " + Wording.mustFollow(m.leadSuit()) + "]", f.did.toString());
					assertTrue("the card is still yours", m.hand(Seating.YOU).contains(c));
					return;
				}
			}
		}
		assertTrue("no deal with a card that can't follow", false);
	}

	private static Match reached(RuleSet rules, Predicate<Match> where) {
		Bot bot = Bot.of(Difficulty.NORMAL, new Random(1));
		for (long seed = 0; seed < 500; seed++) {
			Match m = Match.random(4, new Random(seed), rules);
			while (m.phase() != Match.Phase.GAME_OVER) {
				if (where.test(m)) {
					return m;
				}
				if (m.phase() == Match.Phase.HAND_OVER) {
					m.nextHand();
				} else {
					m.apply(bot.chooseMove(m.view(m.currentSeat())));
				}
			}
		}
		throw new AssertionError("never reached");
	}

	@Test
	public void aTapOnASuitPicksTrump() {
		Match m = reached(RuleSet.STANDARD.withPickTrump(true), x -> x.pickingTrump() && x.currentSeat() == Seating.YOU);
		t.flow.resume(m);
		assertTrue(t.flow.yourTurnToPick());
		t.targets.suit(Suit.HEARTS).set(100, 100, 80, 80);
		t.input.tap(140, 140);
		assertEquals(Suit.HEARTS, m.trump());
		assertEquals("pick HEARTS", t.did.get(0));
	}

	@Test
	public void aWidowCardThenOneOfYoursSwapsThem() {
		Match m = reached(RuleSet.STANDARD.withWidow(true), x -> x.swapping() && x.currentSeat() == Seating.YOU);
		t.flow.resume(m);
		assertTrue(t.flow.yourTurnToSwap());
		Card take = m.widow().get(1), give = m.hand(Seating.YOU).get(0);
		t.targets.widowCard(1).set(300, 300, 60, 90);
		t.handOrder.add(give);
		t.handRects.add(new Rectangle(300, 10, 80, 110));
		t.input.tap(330, 340);
		assertEquals(1, t.targets.widowChoice());
		t.input.tap(340, 60);
		assertTrue(m.hand(Seating.YOU).contains(take) && m.widow().contains(give));
		assertEquals("swap " + give, t.did.get(0));
	}

	@Test
	public void keepMyHandLeavesTheWidowAlone() {
		Match m = reached(RuleSet.STANDARD.withWidow(true), x -> x.swapping() && x.currentSeat() == Seating.YOU);
		List<Card> hand = new ArrayList<Card>(m.hand(Seating.YOU));
		t.flow.resume(m);
		t.targets.button(OverlayTargets.Button.KEEP_HAND).set(100, 100, 200, 40);
		t.input.tap(200, 120);
		assertEquals(hand, m.hand(Seating.YOU));
		assertEquals(Match.Phase.BIDDING, m.phase());
	}

	@Test
	public void aBidHangsOnTheLastTrickOnlyWhenThatTrickDecidesIt() {
		int hanging = 0, settled = 0;
		for (long seed = 0; seed < 60; seed++) {
			final long s = seed;
			Match m = reached(RuleSet.STANDARD, x -> x.phase() == Match.Phase.PLAYING && x.handSize() > 1 && x.currentSeat() == Seating.YOU
					&& x.hand(Seating.YOU).size() == 1 && x.handIndex() == 1 + (int) (s % 6));
			TableFlow flow = new TableFlow();
			flow.seat(Seating.of(4, 5, 6), new Random(1));
			flow.resume(m);
			int need = m.bid(Seating.YOU) - m.tricksWon(Seating.YOU);
			boolean decides = need == 0 || need == 1;
			assertEquals(decides, flow.bidHangsOnLastTrick());
			if (decides) {
				hanging++;
			} else {
				settled++;
			}
		}
		assertTrue(hanging > 0 && settled > 0);
		Match early = reached(RuleSet.STANDARD, x -> x.phase() == Match.Phase.PLAYING && x.hand(Seating.YOU).size() > 1);
		TableFlow flow = new TableFlow();
		flow.resume(early);
		assertFalse("not before the last trick", flow.bidHangsOnLastTrick());
	}
}
