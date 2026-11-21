package ohheck.table;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.rules.Match;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;

/* A read-only view of TableFlow. */
interface FlowView {

	Match match();

	TableState state();

	float stateTime();

	float trumpTime();

	boolean isDealing();

	boolean trumpFaceUp();

	boolean yourTurnToBid();

	boolean yourTurnToPlay();

	Deal deal();

	interface Deal {
		int dealer();

		boolean isShuffling();

		float dealTime();

		float startOf(int seat, int j);

		void drawDeck(SpriteBatch batch, Assets assets, CardArt art, TableLayout layout);
	}
}
