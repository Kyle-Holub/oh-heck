package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;

import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.HandOrder;
import ohheck.rules.Match;
import ohheck.rules.RuleSet;

/** What your taps do at the table, against a table with no screen. */
public class TableInputTest {

	/** A table without a screen: a real flow and match, tap areas set by hand, actions written down. */
	static final class FakeTable implements TableScene, TableActions {
		final TableFlow flow = new TableFlow();
		final TableLayout layout = new TableLayout();
		final TableOverlays overlays = new TableOverlays(null, this, layout, null);
		final TableInput input = new TableInput(this, this);
		final List<Card> handOrder = new ArrayList<Card>();
		final List<Rectangle> handRects = new ArrayList<Rectangle>();
		final List<String> did = new ArrayList<String>();
		/** The state to report; null to report the flow's own. */
		TableState pretend;

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
		public TableOverlays overlays() {
			return overlays;
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
			return false;
		}

		@Override
		public void humanPlay(Card c, float fromX, float fromY) {
			did.add("play " + c);
			flow.play(c);
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

	@Test
	public void theMenuOpensAndGoesWhereItSays() {
		t.flow.start(Match.random(4, new Random(2)));
		t.overlays.menuButton.set(700, 0, 80, 40);
		t.input.tap(740, 20);
		assertTrue(t.input.menuOpen());
		for (int i = 0; i < TableOverlays.MENU.length; i++) {
			t.overlays.menuItems.add(new Rectangle(300, 300 - i * 60, 200, 50));
		}
		t.input.tap(400, 260); // Options
		assertEquals("options", t.did.get(0));
		assertTrue("the menu waits for your return", t.input.menuOpen());
		t.input.tap(400, 320); // Resume
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
		t.overlays.playAgainButton.set(200, 50, 170, 42);
		t.overlays.highScoresButton.set(400, 50, 170, 42);
		t.input.tap(100, 300); // a stray tap: the scores stay up
		assertTrue(t.did.isEmpty());
		t.input.tap(250, 70);
		t.input.tap(450, 70);
		assertEquals("[new game, high scores]", t.did.toString());
	}

	@Test
	public void aTapOnANumberBids() {
		Match m = new Match(4, new Random(4), 3, HandOrder.UP, RuleSet.STANDARD); // seat 3 deals, so you bid first
		t.flow.start(m);
		t.flow.skip(); // past the shuffle and deal
		assertEquals(TableState.TURN, t.flow.state());
		for (int b = 0; b <= m.handSize(); b++) {
			t.overlays.bidButtons.add(new Rectangle(100 + b * 70, 100, 58, 58));
		}
		t.input.tap(105, 105); // the 0
		assertTrue(m.hasBid(Seating.YOU));
		assertEquals(0, m.bid(Seating.YOU));
	}

	@Test
	public void aCardDraggedOntoTheTableIsPlayed() {
		Match m = new Match(4, new Random(5), 3, HandOrder.UP, RuleSet.STANDARD);
		t.flow.start(m);
		t.flow.skip();
		t.overlays.bidButtons.add(new Rectangle(100, 100, 58, 58));
		t.input.tap(105, 105); // bid 0
		while (!t.flow.yourTurnToPlay()) {
			t.flow.update(1 / 30f); // the others bid; you lead
		}
		Card c = m.hand(Seating.YOU).get(0);
		t.handOrder.add(c);
		t.handRects.add(new Rectangle(300, 10, 80, 110));
		t.input.touchDown(340, 60);
		t.input.touchDragged(340, 200, 140);
		assertTrue(t.input.dragging());
		assertTrue(t.input.touchUp(340, 260));
		assertEquals("play " + c, t.did.get(0));
	}
}
