package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import rules.Card;

/** Textures and fonts shared by the screens. */
public final class Assets implements Disposable {

	public static final int CARD_W = 73, CARD_H = 98;

	public final Texture table;
	public final Texture cardSheet;
	public final Texture cardBack;
	/** A 1x1 white texture, tinted to draw panels and buttons. */
	public final Texture white;
	public final BitmapFont font;

	private final TextureRegion[][] faces = new TextureRegion[4][13];

	public Assets() {
		table = linear(new Texture(Gdx.files.internal("images/table8.jpg")));
		cardSheet = linear(new Texture(Gdx.files.internal("images/cards.png")));
		cardBack = linear(new Texture(Gdx.files.internal("images/cardback.png")));
		for (int suit = 0; suit < 4; suit++) {
			for (int rank = 0; rank < 13; rank++) {
				faces[suit][rank] = new TextureRegion(cardSheet, rank * CARD_W, suit * CARD_H, CARD_W, CARD_H);
			}
		}
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		white = new Texture(p);
		p.dispose();
		font = new BitmapFont();
		font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		font.setUseIntegerPositions(false);
	}

	public TextureRegion face(Card c) {
		return faces[c.suit().ordinal()][c.rank().ordinal()];
	}

	private static Texture linear(Texture t) {
		t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		return t;
	}

	@Override
	public void dispose() {
		table.dispose();
		cardSheet.dispose();
		cardBack.dispose();
		white.dispose();
		font.dispose();
	}
}
