package ohheck.campaign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ohheck.campaign.PracticeCoach.Moment;
import ohheck.campaign.PracticeCoach.Tip;
import ohheck.players.Bot;
import ohheck.players.Seating;
import ohheck.rules.Card;
import ohheck.rules.Match;
import ohheck.rules.Move;

public class PracticeRoundTest {

	private interface You {
		Move choose(Match m);
	}

	private final List<String> tips = new ArrayList<String>();
	private final PracticeCoach coach = new PracticeCoach();

	private void moment(Moment moment, Match m) {
		for (Tip t = coach.next(moment, m); t != null; t = coach.next(moment, m)) {
			tips.add(t.id());
			coach.done(t);
		}
	}

	private Match play(You you, int untilHand) {
		Match m = PracticeRound.match();
		Bot[] bots = PracticeRound.bots();
		moment(Moment.SAT_DOWN, m);
		moment(Moment.DEALER_PICKED, m);
		moment(Moment.HAND_DEALT, m);
		while (m.phase() != Match.Phase.GAME_OVER) {
			if (m.phase() == Match.Phase.HAND_OVER) {
				moment(Moment.HAND_SCORED, m);
				if (m.handIndex() + 1 >= untilHand) {
					return m;
				}
				m.nextHand();
				if (m.phase() == Match.Phase.GAME_OVER) {
					break;
				}
				moment(Moment.HAND_DEALT, m);
				continue;
			}
			int seat = m.currentSeat();
			if (seat == Seating.YOU) {
				moment(m.phase() == Match.Phase.BIDDING ? Moment.YOUR_BID : Moment.YOUR_PLAY, m);
				Move move = you.choose(m);
				assertTrue(move + " is allowed", PracticeRound.allowed(m, move));
				m.apply(move);
			} else {
				m.apply(bots[seat].chooseMove(m.view(seat)));
			}
		}
		moment(Moment.FINISHED, m);
		return m;
	}

	private static Move scripted(Match m) {
		if (m.phase() == Match.Phase.BIDDING) {
			for (int b = 0; b <= m.handSize(); b++) {
				if (m.isLegalBid(b) && PracticeRound.allowed(m, new Move.Bid(Seating.YOU, b))) {
					return new Move.Bid(Seating.YOU, b);
				}
			}
		}
		for (Card c : m.legalPlays(Seating.YOU)) {
			if (PracticeRound.allowed(m, new Move.Play(Seating.YOU, c))) {
				return new Move.Play(Seating.YOU, c);
			}
		}
		throw new AssertionError("No allowed move in hand " + (m.handIndex() + 1));
	}

	private static Move lowest(Match m) {
		return m.phase() == Match.Phase.BIDDING ? new Move.Bid(Seating.YOU, m.isLegalBid(0) ? 0 : 1)
				: new Move.Play(Seating.YOU, m.legalPlays(Seating.YOU).get(0));
	}

	@Test
	public void theScriptedHandsGoAsPlanned() {
		Match m = play(PracticeRoundTest::scripted, 1);
		assertEquals(1, m.bid(Seating.YOU));
		assertEquals(1, m.tricksWon(Seating.YOU));
		m.nextHand();
		while (m.phase() != Match.Phase.HAND_OVER) {
			m.apply(m.currentSeat() == Seating.YOU ? scripted(m) : PracticeRound.bots()[m.currentSeat()].chooseMove(m.view(m.currentSeat())));
		}
		assertEquals(1, m.tricksWon(Seating.YOU));
		assertTrue(m.lastTrick().contains(Card.parse("AC")) && m.lastTrickWinner() == Seating.YOU);
		assertTrue("the dealer is set", !m.madeBid(m.dealer()));
	}

	@Test
	public void everyLessonIsTaughtInOrderOnce() {
		play(m -> m.handIndex() < PracticeRound.SCRIPTED_HANDS ? scripted(m) : lowest(m), 99);
		assertEquals(List.of("welcome", "dealer", "deal", "trump", "ranks", "bid", "tracker", "trick", "scoring", "bid2", "follow",
				"cantFollow", "zero", "prompt2", "hook", "prompt3", "finished"), tips);
	}

	@Test
	public void theWeakHandCanNeverWinATrickAndTheDealerAlwaysHasABarredBid() {
		for (int bid = 0; bid <= 3; bid++) {
			for (int order = 0; order < 6; order++) {
				int b = bid, o = order;
				Match m = play(mm -> {
					if (mm.handIndex() < PracticeRound.SCRIPTED_HANDS) {
						return scripted(mm);
					}
					if (mm.handIndex() == 2) {
						if (mm.phase() == Match.Phase.BIDDING) {
							return new Move.Bid(Seating.YOU, mm.isLegalBid(b) ? b : 0);
						}
						List<Card> legal = mm.legalPlays(Seating.YOU);
						return new Move.Play(Seating.YOU, legal.get((o + legal.size()) % legal.size()));
					}
					if (mm.phase() == Match.Phase.BIDDING) {
						assertTrue("a barred bid for the dealer", mm.forbiddenBid() >= 0);
					}
					return lowest(mm);
				}, 99);
				assertTrue(m.phase() == Match.Phase.GAME_OVER);
			}
		}
	}

	@Test
	public void theWeakHandWinsNothingHoweverItIsPlayed() {
		for (int order = 0; order < 6; order++) {
			int o = order;
			Match m = play(mm -> {
				if (mm.handIndex() < PracticeRound.SCRIPTED_HANDS) {
					return scripted(mm);
				}
				if (mm.phase() == Match.Phase.BIDDING) {
					return new Move.Bid(Seating.YOU, 0);
				}
				List<Card> legal = mm.legalPlays(Seating.YOU);
				return new Move.Play(Seating.YOU, legal.get(o % legal.size()));
			}, 3);
			assertEquals(2, m.handIndex());
			assertEquals(0, m.tricksWon(Seating.YOU));
			assertTrue(PracticeCoach.prompt(m).startsWith("Zero made"));
		}
	}
}
