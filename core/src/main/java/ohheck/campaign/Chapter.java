package ohheck.campaign;

import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.rules.GameMode;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;

/*
 * A chapter of the campaign: its game mode, the suit on its card, the three rivals at its table,
 * and the champion who must be beaten in a duel to move on. Only ready chapters can be played yet.
 */
public enum Chapter {
	ONE(GameMode.CLASSIC, Suit.SPADES, Seating.of(0, 1, 3), 2, true),
	TWO(GameMode.HIGH_STAKES, Suit.DIAMONDS, Seating.of(4, 6, 7), 5, false),
	THREE(GameMode.POKER_FACE, Suit.HEARTS, Seating.of(9, 11, 5), 10, false),
	FOUR(GameMode.MARATHON, Suit.CLUBS, Seating.of(9, 10, 11), 8, false);

	public final GameMode mode;
	public final Suit emblem;
	public final Seating rivals;
	public final int championId;
	public final boolean ready;

	Chapter(GameMode mode, Suit emblem, Seating rivals, int championId, boolean ready) {
		this.mode = mode;
		this.emblem = emblem;
		this.rivals = rivals;
		this.championId = championId;
		this.ready = ready;
	}

	public int number() {
		return ordinal() + 1;
	}

	public RuleSet rules() {
		return mode.rules(RuleSet.STANDARD);
	}

	public RuleSet duelRules() {
		return RuleSet.DUEL;
	}

	public Seating duelSeating() {
		return Seating.of(championId);
	}

	public Player champion() {
		return Player.byId(championId);
	}

	public Chapter next() {
		return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
	}
}
