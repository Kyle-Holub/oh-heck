package ohheck.campaign;

import ohheck.players.Player;
import ohheck.players.Seating;
import ohheck.rules.GameMode;
import ohheck.rules.RuleSet;
import ohheck.rules.Suit;

/*
 * A chapter of the campaign: its game mode, the suit on its card, the three rivals at its table,
 * the champion who must be beaten in a duel to move on, and the champion's twist. At the final table the champion
 * takes the top seat and the house rules gain the twist; the duel plays the short deck with momentum, the house
 * scoring and the twist. Only ready chapters can be played yet.
 */
public enum Chapter {
	ONE(GameMode.CLASSIC, Suit.SPADES, Seating.of(0, 1, 3), 2, Twist.MOMENTUM, true),
	TWO(GameMode.HIGH_STAKES, Suit.DIAMONDS, Seating.of(4, 6, 7), 5, Twist.PICK_TRUMP, true),
	THREE(GameMode.POKER_FACE, Suit.HEARTS, Seating.of(9, 11, 5), 10, Twist.MOMENTUM, false),
	FOUR(GameMode.MARATHON, Suit.CLUBS, Seating.of(9, 10, 11), 8, Twist.MOMENTUM, false);

	public final GameMode mode;
	public final Suit emblem;
	public final Seating rivals;
	public final int championId;
	public final Twist twist;
	public final boolean ready;

	Chapter(GameMode mode, Suit emblem, Seating rivals, int championId, Twist twist, boolean ready) {
		this.mode = mode;
		this.emblem = emblem;
		this.rivals = rivals;
		this.championId = championId;
		this.twist = twist;
		this.ready = ready;
	}

	public int number() {
		return ordinal() + 1;
	}

	public RuleSet rules() {
		return mode.rules(RuleSet.STANDARD);
	}

	public RuleSet rules(Stage s) {
		if (s == Stage.DUEL) {
			RuleSet house = rules();
			return twist.apply(RuleSet.DUEL.withBigBids(house.bigBids).withBlindBids(house.blindBids));
		}
		return s == Stage.FINAL ? twist.apply(rules()) : rules();
	}

	public Seating seating(Stage s) {
		return s == Stage.DUEL ? Seating.of(championId)
				: s == Stage.FINAL ? Seating.of(rivals.idAt(1), championId, rivals.idAt(3)) : rivals;
	}

	public Player leaving() {
		return rivals.at(2);
	}

	public Player champion() {
		return Player.byId(championId);
	}

	public Chapter next() {
		return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
	}
}
