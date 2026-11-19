package ohheck.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.rules.Card;
import ohheck.rules.Rank;
import ohheck.rules.Suit;

public class CardSpriteTest {

	private static final Tween ONE_SECOND = Tween.over(1f, Ease.LINEAR);

	private static CardSprite.Pose at(float x) {
		return new CardSprite.Pose().set(x, 0, 10, 14, 0, 1, 0);
	}

	@Test
	public void itGlidesFromWhereItIsToItsNewPlace() {
		CardSprite s = new CardSprite(Card.of(Rank.ACE, Suit.SPADES));
		s.moveTo(at(0), ONE_SECOND, 0, 0, 0); // first placing: straight there
		assertEquals(0, s.pose(0).x, 0);
		s.moveTo(at(100), ONE_SECOND, 0, 2f, 0);
		assertEquals(50, s.pose(2.5f).x, 1e-4);
		assertTrue(s.moving(2.5f));
		s.moveTo(at(0), ONE_SECOND, 0, 2.5f, 0); // turned back halfway: it starts from where it is
		assertEquals(25, s.pose(3f).x, 1e-4);
		assertFalse(s.moving(4f));
	}

	@Test
	public void theSamePlaceDoesNotRestartItsMotion() {
		CardSprite s = new CardSprite(Card.of(Rank.TWO, Suit.HEARTS));
		s.moveTo(at(0), ONE_SECOND, 0, 0, 0);
		s.moveTo(at(100), ONE_SECOND, 0, 0, 0);
		s.moveTo(at(100), ONE_SECOND, 0, 0.5f, 0); // asked again each frame
		assertEquals(75, s.pose(0.75f).x, 1e-4);
	}

	@Test
	public void aBackdatedMotionIsAlreadyUnderWay() {
		CardSprite s = new CardSprite(Card.of(Rank.KING, Suit.CLUBS));
		s.snap(at(0));
		s.moveTo(at(100), ONE_SECOND, 0, 10f, 0.25f); // should have started a quarter second ago
		assertEquals(25, s.pose(10f).x, 1e-4);
	}

	@Test
	public void aDroppedCardFallsToTheTable() {
		CardSprite s = new CardSprite(Card.of(Rank.TEN, Suit.DIAMONDS));
		s.snap(at(0));
		s.moveTo(at(0).set(0, 0, 10, 14, 0, 1, 5), ONE_SECOND, 30, 0, 0);
		assertEquals(30, s.pose(0).lift, 1e-4);
		assertEquals(5, s.pose(1).lift, 1e-4);
	}

	@Test
	public void outOfSightItStartsAfreshWhereItsPut() {
		CardSprite s = new CardSprite(Card.of(Rank.FIVE, Suit.SPADES));
		s.show(true);
		s.moveTo(at(0), ONE_SECOND, 0, 0, 0);
		s.show(false);
		s.moveTo(at(300), ONE_SECOND, 0, 5, 0);
		assertEquals(300, s.pose(5).x, 0);
	}
}
