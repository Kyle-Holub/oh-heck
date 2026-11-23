package ohheck.campaign;

import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.rules.GameMode;
import ohheck.rules.RuleSet;

/*
 * A chapter of the campaign: its game mode, the three rivals at its table, and the champion
 * who must be beaten in a duel to move on. Only ready chapters can be played yet.
 */
public enum Chapter {
	ONE(GameMode.CLASSIC, Seating.of(0, 1, 3), 2, true),
	TWO(GameMode.HIGH_STAKES, Seating.of(4, 6, 7), 5, false),
	THREE(GameMode.POKER_FACE, Seating.of(9, 11, 5), 10, false),
	FOUR(GameMode.MARATHON, Seating.of(9, 10, 11), 8, false);

	public final GameMode mode;
	public final Seating rivals;
	public final int championId;
	public final boolean ready;

	Chapter(GameMode mode, Seating rivals, int championId, boolean ready) {
		this.mode = mode;
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
