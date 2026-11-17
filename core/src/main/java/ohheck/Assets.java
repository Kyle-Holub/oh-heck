package ohheck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import rules.Card;

/** Textures and fonts shared by the screens. */
public final class Assets implements Disposable {

	/** A card's size in world units (the original game's pixels). */
	public static final int CARD_W = 73, CARD_H = 98;
	/** A card's size in the high-resolution sheet drawn by tools/make_art.py (3x). */
	private static final int SHEET_W = 219, SHEET_H = 294;
	/**
	 * Transparent margin around each card in the blurred sheet, in full-size
	 * sheet pixels (the blurred sheet is stored at half size; only the ratio
	 * to the card matters).
	 */
	public static final int BLUR_PAD = 24;

	/**
	 * The fonts are baked large (see tools/make_fonts.py) and drawn scaled
	 * down. Multiply a size factor by these to get the font scale: a factor of
	 * 1 gives roughly 16 world units.
	 */
	public static final float UI_BASE = 16f / 42f, SERIF_BASE = 16f / 52f;

	public final Texture table;
	public final Texture cardSheet, cardBack, cardSheetBlur, cardBackBlur;
	/** Soft shadow under a card: the card covers the middle 120x160 of 160x200. */
	public final Texture shadow;
	/** A round white glow, tinted when drawn (sparkles). */
	public final Texture glow;
	/** Rounded frames: panels, the current player's panel, and buttons. */
	public final NinePatch panel, panelTurn, button, buttonAlt, buttonOff;
	public final Texture titleText;
	/** Title art from the original game. */
	public final Texture bidTricksText, setText, trickWonText, makeBidText, gameOverText;
	/** The bid tracker's cards: a trick not bid on yet, a trick bid, a bid beyond the hand. */
	public final Texture bidOpen, bidTaken, bidOver;
	/** Difficulty badges, indexed by Difficulty.ordinal(): one, two or three spades on a chip. */
	public final Texture[] levelBadge = new Texture[3];
	/** Player bar icons: dealer button, trick, trick over the bid, a bid of zero. */
	public final Texture dealerIcon, trickIcon, overIcon, zeroIcon;
	/** A 1x1 white texture, tinted to draw panels and buttons. */
	public final Texture white;
	/** A soft-edged rounded bar, for strokes that stay smooth when rotated (the tally marks). */
	public final Texture stroke;
	/** Sans-serif text for the table and panels. */
	public final BitmapFont font;
	/** Serif text for titles and menus, like the original game. */
	public final BitmapFont serif;

	private final TextureRegion[][] faces = new TextureRegion[4][13];
	private final TextureRegion back, backBlur;
	private final TextureRegion[][] blurred = new TextureRegion[4][13];

	public Assets() {
		table = linear(new Texture(Gdx.files.internal("images/table.jpg")));
		cardSheet = mipmapped("images/cards_hd.png");
		cardBack = mipmapped("images/back_hd.png");
		cardSheetBlur = mipmapped("images/cards_blur.png");
		cardBackBlur = mipmapped("images/back_blur.png");
		shadow = linear(new Texture(Gdx.files.internal("images/shadow.png")));
		glow = mipmapped("images/glow.png");
		panel = ninePatch("panel");
		panelTurn = ninePatch("panel_turn");
		button = ninePatch("button");
		buttonAlt = ninePatch("button_alt");
		buttonOff = ninePatch("button_off");
		titleText = mipmapped("images/titleText.png");
		bidTricksText = mipmapped("images/bidTricks.png");
		setText = mipmapped("images/setText.png");
		trickWonText = mipmapped("images/trickWonText.png");
		makeBidText = mipmapped("images/makeBidText.png");
		gameOverText = mipmapped("images/gameOver.png");
		dealerIcon = mipmapped("images/icon_dealer.png");
		trickIcon = mipmapped("images/icon_trick.png");
		overIcon = mipmapped("images/icon_over.png");
		zeroIcon = mipmapped("images/icon_zero.png");
		bidOpen = mipmapped("images/bid_open.png");
		bidTaken = mipmapped("images/bid_taken.png");
		bidOver = mipmapped("images/bid_over.png");
		String[] levels = { "easy", "normal", "hard" };
		for (int i = 0; i < levels.length; i++) {
			levelBadge[i] = mipmapped("images/level_" + levels[i] + ".png");
		}
		// the blurred sheet is stored at reduced size: take its cells from its own dimensions
		int bw = cardSheetBlur.getWidth() / 13, bh = cardSheetBlur.getHeight() / 4;
		for (int suit = 0; suit < 4; suit++) {
			for (int rank = 0; rank < 13; rank++) {
				faces[suit][rank] = new TextureRegion(cardSheet, rank * SHEET_W, suit * SHEET_H, SHEET_W, SHEET_H);
				blurred[suit][rank] = new TextureRegion(cardSheetBlur, rank * bw, suit * bh, bw, bh);
			}
		}
		back = new TextureRegion(cardBack);
		backBlur = new TextureRegion(cardBackBlur);
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		white = new Texture(p);
		p.dispose();
		stroke = strokeTexture(32, 160);
		font = bitmapFont("ui");
		serif = bitmapFont("serif");
	}

	/** A capsule w wide and h tall with anti-aliased edges and a little transparent margin. */
	private static Texture strokeTexture(int w, int h) {
		Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		p.setBlending(Pixmap.Blending.None);
		float r = w / 2f - 2f; // radius, leaving a 2px clear border so filtering softens the edge
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				float px = x + 0.5f - w / 2f, py = y + 0.5f;
				float cy = Math.max(r + 2f, Math.min(h - r - 2f, py)); // nearest point on the centre line
				float d = (float) Math.sqrt(px * px + (py - cy) * (py - cy));
				float a = Math.max(0f, Math.min(1f, r - d + 0.5f));
				p.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, a));
			}
		}
		Texture t = new Texture(p, true);
		t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
		p.dispose();
		return t;
	}

	public TextureRegion face(Card c) {
		return faces[c.suit().ordinal()][c.rank().ordinal()];
	}

	public TextureRegion back() {
		return back;
	}

	/** Blurred versions, with a margin around the card (see {@link #blurMarginX()}). */
	public TextureRegion blurredFace(Card c) {
		return blurred[c.suit().ordinal()][c.rank().ordinal()];
	}

	public TextureRegion blurredBack() {
		return backBlur;
	}

	/** Fraction of a blurred region's width taken by the margin on each side. */
	public static float blurMarginX() {
		return BLUR_PAD / (float) (SHEET_W + 2 * BLUR_PAD);
	}

	public static float blurMarginY() {
		return BLUR_PAD / (float) (SHEET_H + 2 * BLUR_PAD);
	}

	private static NinePatch ninePatch(String name) {
		Texture t = new Texture(Gdx.files.internal("images/" + name + ".png"));
		t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		NinePatch p = new NinePatch(t, 16, 16, 16, 16);
		return p;
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

	private static Texture linear(Texture t) {
		t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		return t;
	}

	@Override
	public void dispose() {
		table.dispose();
		cardSheet.dispose();
		cardBack.dispose();
		cardSheetBlur.dispose();
		cardBackBlur.dispose();
		shadow.dispose();
		glow.dispose();
		for (NinePatch p : new NinePatch[] { panel, panelTurn, button, buttonAlt, buttonOff }) {
			p.getTexture().dispose();
		}
		titleText.dispose();
		for (Texture t : new Texture[] { bidTricksText, setText, trickWonText, makeBidText, gameOverText, dealerIcon, trickIcon, overIcon, zeroIcon, bidOpen, bidTaken, bidOver }) {
			t.dispose();
		}
		white.dispose();
		for (Texture t : levelBadge) {
			t.dispose();
		}
		stroke.dispose();
		font.getRegion().getTexture().dispose();
		serif.getRegion().getTexture().dispose();
		font.dispose();
		serif.dispose();
	}
}
