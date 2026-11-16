package ohheck;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import rules.Bot;
import rules.Card;
import rules.Deck;

/**
 * Phase 0 placeholder: deals a hand with the shared rules code and draws it
 * with the original card art. Tap to deal again.
 */
public class OhHeckGame extends ApplicationAdapter {

	private static final float WORLD_W = 800, WORLD_H = 480;
	private static final int CARD_W = 73, CARD_H = 98;
	private static final float CARD_SCALE = 1.4f;

	private ExtendViewport viewport;
	private SpriteBatch batch;
	private BitmapFont font;
	private Texture table, cardSheet;
	private TextureRegion[][] faces;

	private final Random rng = new Random();
	private final Bot bot = new Bot();
	private List<Card> hand;
	private Card trumpCard;
	private int botBid;

	@Override
	public void create() {
		viewport = new ExtendViewport(WORLD_W, WORLD_H);
		batch = new SpriteBatch();
		font = new BitmapFont();
		font.getData().setScale(1.5f);
		table = new Texture(Gdx.files.internal("images/table8.jpg"));
		cardSheet = new Texture(Gdx.files.internal("images/cards.png"));
		cardSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		faces = new TextureRegion[4][13];
		for (int suit = 0; suit < 4; suit++) {
			for (int rank = 0; rank < 13; rank++) {
				faces[suit][rank] = new TextureRegion(cardSheet, rank * CARD_W, suit * CARD_H, CARD_W, CARD_H);
			}
		}
		deal();
	}

	private void deal() {
		Deck deck = Deck.shuffled(rng);
		hand = deck.deal(8);
		trumpCard = deck.deal();
		botBid = bot.chooseBid(hand, trumpCard.suit(), 0);
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void render() {
		if (Gdx.input.justTouched()) {
			deal();
		}

		ScreenUtils.clear(Color.BLACK);
		viewport.apply();
		batch.setProjectionMatrix(viewport.getCamera().combined);
		float w = viewport.getWorldWidth(), h = viewport.getWorldHeight();
		float cw = CARD_W * CARD_SCALE, ch = CARD_H * CARD_SCALE;

		batch.begin();
		batch.draw(table, 0, 0, w, h);

		font.setColor(Color.WHITE);
		font.draw(batch, "Oh Heck!  libGDX dev build", 24, h - 20);
		font.draw(batch, "Trump card:", w - cw - 190, h - 60);
		batch.draw(face(trumpCard), w - cw - 24, h - 40 - ch, cw, ch);
		font.draw(batch, "The computer would bid " + botBid + " with this hand.", 24, h - 60);
		font.draw(batch, "Tap anywhere to deal again.", 24, h - 95);

		float gap = Math.min(cw + 8, (w - 48 - cw) / (hand.size() - 1));
		float start = (w - (gap * (hand.size() - 1) + cw)) / 2;
		for (int i = 0; i < hand.size(); i++) {
			batch.draw(face(hand.get(i)), start + i * gap, 24, cw, ch);
		}
		batch.end();
	}

	private TextureRegion face(Card c) {
		return faces[c.suit().ordinal()][c.rank().ordinal()];
	}

	@Override
	public void dispose() {
		batch.dispose();
		font.dispose();
		table.dispose();
		cardSheet.dispose();
	}
}
