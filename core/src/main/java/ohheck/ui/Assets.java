package ohheck.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import ohheck.campaign.Cosmetic;
import ohheck.players.Difficulty;
import ohheck.rules.Card;
import ohheck.rules.Suit;

public final class Assets implements Disposable {

	public static final int CARD_W = 73, CARD_H = 98;
	private static final int SHEET_W = 219, SHEET_H = 294;
	public static final int BLUR_PAD = 24;

	public static final float UI_BASE = 16f / 42f, SERIF_BASE = 16f / 52f;

	public Texture table;
	public final Texture cardSheet, cardSheetBlur, cardSlot, badge, padlock;
	public final Texture[] momentum = new Texture[3];
	public final Texture stoneWall, stoneFloor, campaignMap, seal, sealLocked, giltRing;
	public final Texture campaignFace, plaque, plaqueLocked, stepTable, stepShots, stepDuel, pipOn, pipOff;
	public final Texture menuContinue, menuCampaign, menuCustom, menuScores;
	private final Texture[] emblems = new Texture[Suit.values().length];
	private final Texture[] tables = new Texture[Cosmetic.values().length], backs = new Texture[Cosmetic.values().length],
			backBlurs = new Texture[Cosmetic.values().length];
	public final Texture shadow;
	public final Texture glow;
	public final NinePatch panel, panelTurn, panelWait, button, buttonAlt, buttonOff;
	public final Texture titleText;
	public final Texture bidTricksText, setText, trickWonText, makeBidText, gameOverText;
	public final Texture bidOpen, bidTaken, bidOver;
	public final Texture[] levelBadge = new Texture[Difficulty.values().length];
	public final Texture dealerIcon, trickIcon, overIcon, zeroIcon, waitingIcon;
	public final Texture bidDigits;
	private final TextureRegion[] digits = new TextureRegion[11];
	public final Texture white, fadeDown;
	public final Texture stroke;
	public final BitmapFont font;
	public final BitmapFont serif;

	private final TextureRegion[][] faces = new TextureRegion[4][13];
	private final TextureRegion back, backBlur;
	private final TextureRegion[][] blurred = new TextureRegion[4][13];

	private final Array<Disposable> owned = new Array<Disposable>();

	private <T extends Disposable> T own(T d) {
		owned.add(d);
		return d;
	}

	public Assets() {
		table = feltOf(Cosmetic.CLASSIC);
		cardSheet = mipmapped("images/cards_hd.png");
		badge = mipmapped("images/badge.png");
		padlock = mipmapped("images/padlock.png");
		for (int i = 0; i < momentum.length; i++) {
			momentum[i] = mipmapped("images/momentum_" + i + ".png");
		}
		campaignFace = mipmapped("images/campaign_face.png");
		plaque = mipmapped("images/plaque.png");
		stoneWall = tiled("images/stone_wall.png");
		stoneFloor = tiled("images/stone_floor.png");
		campaignMap = mipmapped("images/map.jpg");
		seal = mipmapped("images/seal.png");
		sealLocked = mipmapped("images/seal_locked.png");
		giltRing = mipmapped("images/gilt_ring.png");
		plaqueLocked = mipmapped("images/plaque_locked.png");
		stepTable = mipmapped("images/step_table.png");
		stepShots = mipmapped("images/step_shots.png");
		stepDuel = mipmapped("images/step_duel.png");
		pipOn = mipmapped("images/pip_on.png");
		pipOff = mipmapped("images/pip_off.png");
		menuContinue = mipmapped("images/menu_continue.png");
		menuCampaign = mipmapped("images/menu_campaign.png");
		menuCustom = mipmapped("images/menu_custom.png");
		menuScores = mipmapped("images/menu_scores.png");
		for (Suit s : Suit.values()) {
			emblems[s.ordinal()] = mipmapped("images/emblem_" + s.name().toLowerCase() + ".png");
		}
		cardSlot = mipmapped("images/card_slot.png");
		cardSheetBlur = mipmapped("images/cards_blur.png");
		shadow = linear(own(new Texture(Gdx.files.internal("images/shadow.png"))));
		glow = mipmapped("images/glow.png");
		panel = ninePatch("panel");
		panelTurn = ninePatch("panel_turn");
		panelWait = ninePatch("panel_wait");
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
		waitingIcon = mipmapped("images/icon_waiting.png");
		bidDigits = mipmapped("images/bid_digits.png");
		cutDigits();
		bidOpen = mipmapped("images/bid_open.png");
		bidTaken = mipmapped("images/bid_taken.png");
		bidOver = mipmapped("images/bid_over.png");
		for (Difficulty d : Difficulty.values()) {
			levelBadge[d.ordinal()] = mipmapped("images/level_" + d.key() + ".png");
		}
		cutCards();
		back = new TextureRegion(backOf(Cosmetic.CLASSIC));
		backBlur = new TextureRegion(blurredBackOf(Cosmetic.CLASSIC));
		white = own(whitePixel());
		fadeDown = own(linear(fadeDownTexture()));
		stroke = own(strokeTexture(32, 160));
		font = bitmapFont("ui");
		serif = bitmapFont("serif");
	}
	private void cutDigits() {
		for (int i = 0; i < digits.length; i++) {
			digits[i] = new TextureRegion(bidDigits, i * DIGIT_W, 0, DIGIT_W, DIGIT_H);
		}
	}

	private void cutCards() {
		int bw = cardSheetBlur.getWidth() / 13, bh = cardSheetBlur.getHeight() / 4;
		for (int suit = 0; suit < 4; suit++) {
			for (int rank = 0; rank < 13; rank++) {
				faces[suit][rank] = new TextureRegion(cardSheet, rank * SHEET_W, suit * SHEET_H, SHEET_W, SHEET_H);
				blurred[suit][rank] = new TextureRegion(cardSheetBlur, rank * bw, suit * bh, bw, bh);
			}
		}
	}

	private static Texture fadeDownTexture() {
		Pixmap p = new Pixmap(1, 64, Pixmap.Format.RGBA8888);
		p.setBlending(Pixmap.Blending.None);
		for (int y = 0; y < 64; y++) {
			float a = 1 - y / 63f;
			p.setColor(1, 1, 1, a * a);
			p.drawPixel(0, y);
		}
		Texture t = new Texture(p);
		p.dispose();
		return t;
	}

	private static Texture whitePixel() {
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		Texture t = new Texture(p);
		p.dispose();
		return t;
	}

	private static Texture strokeTexture(int w, int h) {
		Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		p.setBlending(Pixmap.Blending.None);
		float r = w / 2f - 2f;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				float px = x + 0.5f - w / 2f, py = y + 0.5f;
				float cy = Math.max(r + 2f, Math.min(h - r - 2f, py));
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

	public void dress(Cosmetic cardBack, Cosmetic felt) {
		table = feltOf(felt);
		Color.rgb888ToColor(Palette.SPARKLE, felt.sparkle);
		back.setRegion(backOf(cardBack));
		backBlur.setRegion(blurredBackOf(cardBack));
	}

	public Texture backOf(Cosmetic c) {
		if (backs[c.ordinal()] == null) {
			backs[c.ordinal()] = mipmapped("images/" + c.back + ".png");
		}
		return backs[c.ordinal()];
	}

	private Texture blurredBackOf(Cosmetic c) {
		if (backBlurs[c.ordinal()] == null) {
			backBlurs[c.ordinal()] = mipmapped("images/" + c.backBlur + ".png");
		}
		return backBlurs[c.ordinal()];
	}

	public Texture feltOf(Cosmetic c) {
		if (tables[c.ordinal()] == null) {
			tables[c.ordinal()] = linear(own(new Texture(Gdx.files.internal("images/" + c.felt + ".jpg"))));
		}
		return tables[c.ordinal()];
	}

	public Texture emblem(Suit s) {
		return emblems[s.ordinal()];
	}

	public TextureRegion back() {
		return back;
	}

	public TextureRegion blurredFace(Card c) {
		return blurred[c.suit().ordinal()][c.rank().ordinal()];
	}

	public TextureRegion blurredBack() {
		return backBlur;
	}

	public static float blurMarginX() {
		return BLUR_PAD / (float) (SHEET_W + 2 * BLUR_PAD);
	}

	public static float blurMarginY() {
		return BLUR_PAD / (float) (SHEET_H + 2 * BLUR_PAD);
	}

	private NinePatch ninePatch(String name) {
		Texture t = own(new Texture(Gdx.files.internal("images/" + name + ".png")));
		t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		NinePatch p = new NinePatch(t, 16, 16, 16, 16);
		return p;
	}

	private BitmapFont bitmapFont(String name) {
		Texture t = mipmapped("fonts/" + name + ".png");
		BitmapFont f = own(new BitmapFont(Gdx.files.internal("fonts/" + name + ".fnt"), new TextureRegion(t), false));
		f.setUseIntegerPositions(false);
		return f;
	}

	public static final int DIGIT_W = 64, DIGIT_H = 88, PLAIN_ZERO = 10;

	public TextureRegion digit(int n) {
		return digits[n];
	}

	private Texture mipmapped(String path) {
		Texture t = own(new Texture(Gdx.files.internal(path), true));
		t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
		return t;
	}

	private Texture tiled(String path) {
		Texture t = mipmapped(path);
		t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
		return t;
	}

	private static Texture linear(Texture t) {
		t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		return t;
	}

	@Override
	public void dispose() {
		for (Disposable d : owned) {
			d.dispose();
		}
		owned.clear();
	}
}
