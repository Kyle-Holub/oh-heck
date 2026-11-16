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

	/**
	 * The fonts are baked large (see tools/make_fonts.py) and drawn scaled
	 * down. Multiply a size factor by these to get the font scale: a factor of
	 * 1 gives roughly 16 world units.
	 */
	public static final float UI_BASE = 16f / 42f, SERIF_BASE = 16f / 52f;

	public final Texture table;
	public final Texture cardSheet;
	public final Texture cardBack;
	public final Texture titleText;
	/** Title art from the original game. */
	public final Texture bidTricksText, setText, trickWonText, makeBidText;
	/** Player bar icons from the original game: dealer, trick, trick over the bid. */
	public final Texture dealerIcon, trickIcon, overIcon;
	/** A 1x1 white texture, tinted to draw panels and buttons. */
	public final Texture white;
	/** Sans-serif text for the table and panels. */
	public final BitmapFont font;
	/** Serif text for titles and menus, like the original game. */
	public final BitmapFont serif;

	private final TextureRegion[][] faces = new TextureRegion[4][13];
	private final TextureRegion back;

	public Assets() {
		table = linear(new Texture(Gdx.files.internal("images/table8.jpg")));
		cardSheet = mipmapped("images/cards.png");
		cardBack = mipmapped("images/cardback.png");
		titleText = mipmapped("images/titleText.png");
		bidTricksText = mipmapped("images/bidTricks.png");
		setText = mipmapped("images/setText.png");
		trickWonText = mipmapped("images/trickWonText.png");
		makeBidText = mipmapped("images/makeBidText.png");
		dealerIcon = pixelArt("images/dealerIcon.png");
		trickIcon = pixelArt("images/trickIcon.png");
		overIcon = pixelArt("images/underBidIcon.png");
		for (int suit = 0; suit < 4; suit++) {
			for (int rank = 0; rank < 13; rank++) {
				faces[suit][rank] = new TextureRegion(cardSheet, rank * CARD_W, suit * CARD_H, CARD_W, CARD_H);
			}
		}
		back = new TextureRegion(cardBack);
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		white = new Texture(p);
		p.dispose();
		font = bitmapFont("ui");
		serif = bitmapFont("serif");
	}

	public TextureRegion face(Card c) {
		return faces[c.suit().ordinal()][c.rank().ordinal()];
	}

	public TextureRegion back() {
		return back;
	}

	private static BitmapFont bitmapFont(String name) {
		Texture t = mipmapped("fonts/" + name + ".png");
		BitmapFont f = new BitmapFont(Gdx.files.internal("fonts/" + name + ".fnt"), new TextureRegion(t), false);
		f.setUseIntegerPositions(false);
		return f;
	}

	private static Texture mipmapped(String path) {
		Texture t = new Texture(Gdx.files.internal(path), true);
		t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
		return t;
	}

	/** Small icons: scaled up with hard pixel edges rather than blurred. */
	private static Texture pixelArt(String path) {
		Texture t = new Texture(Gdx.files.internal(path));
		t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		return t;
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
		titleText.dispose();
		for (Texture t : new Texture[] { bidTricksText, setText, trickWonText, makeBidText, dealerIcon, trickIcon, overIcon }) {
			t.dispose();
		}
		white.dispose();
		font.getRegion().getTexture().dispose();
		serif.getRegion().getTexture().dispose();
		font.dispose();
		serif.dispose();
	}
}
