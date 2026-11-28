package ohheck.screens;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.campaign.Briefing;
import ohheck.campaign.Chapter;
import ohheck.players.Player;
import ohheck.ui.Assets;
import ohheck.ui.CardArt;
import ohheck.ui.MenuChrome;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/*
 * A briefing of what is about to happen, over the campaign: a chapter's in pages in the tutorial's style (a picture
 * above, a caption below, the rivals' cards tapped for their habits), a stage's as a row of cards over one box of
 * text with Play beside it. Text that would overflow its card or box is set smaller to fit.
 */
final class BriefingView {

	private static final Tween LAYING = Tween.over(0.35f, Ease.OUT);
	private static final float CAPTION_H = 112, STAGE_CAPTION_H = 136, CAPTION_W = 860, BIG_W = 170, BIG_H = BIG_W * 98 / 73, SMALL_W = 120,
			SMALL_H = SMALL_W * 98 / 73, BIG_ICON = 84, ICON = 64, BUTTON_ROOM = 64, PLAYER_GAP = 24, PANEL_GAP = 34, HABIT_W = 640, HABIT_H = 62,
			LIFT = 26, GLOW_W = 1.9f, GLOW_H = 1.6f, GILT = 1.3f, FIT_PAD = 12, STAGE_TEXT = 1.1f, STAGE_TEXT_LEAST = 0.75f, STAGE_TEXT_ROOM = 30;
	private static final int CHAMPION = 4;
	private static final Color GLOW = new Color(1f, 0.78f, 0.4f, 0.3f);

	private final Painter paint;
	private final SpriteBatch batch;
	private final Assets assets;
	private final CardArt art;
	private final Rectangle goButton = new Rectangle();
	private final Rectangle[] playerCards = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };
	private final StringBuilder pageCount = new StringBuilder(), stageText = new StringBuilder();
	private Briefing briefing;
	private Runnable afterBriefing;
	private int page, rival;
	private float briefedFor, stageSize, w, h;

	BriefingView(Painter paint, CardArt art) {
		this.paint = paint;
		this.batch = paint.batch;
		this.assets = paint.assets;
		this.art = art;
	}

	boolean showing() {
		return briefing != null;
	}

	String tag() {
		return briefing.tag();
	}

	void close() {
		briefing = null;
	}

	void update(float dt) {
		briefedFor += dt;
	}

	void show(Briefing b, Runnable then) {
		briefing = b;
		afterBriefing = then;
		briefedFor = 0;
		page = 0;
		rival = 0;
		stageText.setLength(0);
		stageSize = 0;
		for (Briefing.Page p : b.pages()) {
			stageText.append(stageText.length() > 0 ? " " : "").append(p.line());
		}
	}

	void go() {
		Runnable then = afterBriefing;
		briefing = null;
		then.run();
	}

	void back() {
		if (!briefing.isStage() && page > 0) {
			page--;
			rival = 0;
		} else {
			briefing = null;
		}
	}

	void tap(float x, float y, boolean backTapped) {
		if (briefing.isStage()) {
			if (backTapped) {
				briefing = null;
			} else if (goButton.contains(x, y)) {
				go();
			}
			return;
		}
		int tapped = briefing.pages().get(page).picture() == Briefing.Picture.RIVALS ? rivalAt(x, y) : 0;
		if (backTapped) {
			back();
		} else if (tapped > 0) {
			rival = tapped == rival ? 0 : tapped;
		} else if (page < briefing.pages().size() - 1) {
			page++;
			rival = 0;
			briefedFor = LAYING.end() / 2;
		} else if (goButton.contains(x, y)) {
			go();
		}
	}

	void draw(float width, float height) {
		w = width;
		h = height;
		float a = LAYING.progress(briefedFor);
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		if (briefing.isStage()) {
			drawStage(a);
			return;
		}
		Briefing.Page p = briefing.pages().get(page);
		float cw = Math.min(w - 60, CAPTION_W), x = (w - cw) / 2, y = 12;
		float midY = (y + CAPTION_H + BUTTON_ROOM + h - MenuChrome.HEADER_H) / 2 - (1 - a) * 30;
		paint.fade(a);
		drawPicture(p, midY, a);
		paint.frame(assets.panel, x, y, cw, CAPTION_H);
		paint.fade(1f);
		paint.serif(1.35f, Palette.GOLD, a).draw(batch, p.heading(), x + 22, y + CAPTION_H - 12);
		paint.ui(1.1f, Palette.CREAM, a).draw(batch, p.line(), x + 22, y + CAPTION_H - 44, cw - 44, Align.left, true);
		BitmapFont m = paint.ui(0.9f, Palette.MUTED, a);
		pageCount.setLength(0);
		pageCount.append(page + 1).append(" / ").append(briefing.pages().size());
		paint.text(m, pageCount.toString(), x + cw - 20, y + CAPTION_H - 14, false);
		if (page == briefing.pages().size() - 1) {
			paint.button(goButton, briefing.go(), w / 2, y + CAPTION_H + 14, 220, 44, assets.button);
		} else {
			goButton.set(0, 0, 0, 0);
			paint.text(m, "Tap to continue", x + cw - 20, y + 22, false);
		}
	}

	private void drawStage(float a) {
		List<Briefing.Page> pages = briefing.pages();
		float top = h - MenuChrome.HEADER_H, boxTop = 12 + STAGE_CAPTION_H, rise = (1 - a) * 30;
		paint.centred(paint.serif(1.6f, Palette.GOLD, a), pages.get(0).heading(), w / 2, top - 22 + rise);
		float cy = (boxTop + top - 50) / 2 - rise, span = pages.size() * SMALL_W + (pages.size() - 1) * PANEL_GAP;
		for (int i = 0; i < pages.size(); i++) {
			panel(pages.get(i), (w - span) / 2 + i * (SMALL_W + PANEL_GAP) + SMALL_W / 2, cy, a);
		}
		float bw = 180, boxW = w - 60 - bw - 16;
		paint.fade(a);
		paint.frame(assets.panel, 30, 12, boxW, STAGE_CAPTION_H);
		paint.fade(1f);
		paint.ui(stageTextSize(boxW - 44), Palette.CREAM, a).draw(batch, stageText, 52, 12 + STAGE_CAPTION_H - 18, boxW - 44, Align.left, true);
		paint.button(goButton, briefing.go(), w - 30 - bw / 2, 12 + (STAGE_CAPTION_H - 44) / 2, bw, 44, assets.button);
	}

	private void panel(Briefing.Page p, float cx, float cy, float a) {
		Texture icon = switch (p.picture()) {
		case TABLE -> assets.stepTable;
		case WIN -> assets.badge;
		case SHOTS -> assets.stepShots;
		case CHAMPION -> championBadge();
		default -> null;
		};
		if (icon != null) {
			smallCard(cx, cy, icon, p.figure(), a, p.picture() == Briefing.Picture.CHAMPION);
			return;
		}
		float x = cx - SMALL_W / 2, y = cy - SMALL_H / 2;
		art.shadow(x, y, SMALL_W, SMALL_H, 4);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, SMALL_W, SMALL_H);
		batch.setColor(Color.WHITE);
		fit(true, 2.6f, Palette.GOLD_LIGHT, a, p.figure(), x, cy - 20, SMALL_W, 40);
		String heading = p.heading().toUpperCase();
		BitmapFont f = paint.ui(0.85f, Palette.GOLD, a);
		paint.glyphs.setText(f, heading);
		if (paint.glyphs.width > SMALL_W - FIT_PAD) {
			paint.ui(0.7f, Palette.GOLD, a).draw(batch, heading, x + FIT_PAD / 2, y + 52, SMALL_W - FIT_PAD, Align.center, true);
		} else {
			paint.textIn(f, heading, x, y + 14, SMALL_W, 20);
		}
	}

	private void drawPicture(Briefing.Page p, float cy, float a) {
		Chapter c = briefing.chapter();
		switch (p.picture()) {
		case CHAPTER:
			bigCard(w / 2, cy, assets.emblem(c.emblem), p.figure(), a, false);
			paint.centred(paint.ui(0.9f, Palette.GOLD, a), "CHAPTER", w / 2, cy + BIG_H / 2 - 20);
			break;
		case HOUSE_RULES:
			paint.serif(2.8f, Palette.GOLD_LIGHT, a).draw(batch, p.figure(), 0, cy + 60, w, Align.center, false);
			break;
		case RIVALS:
			for (int i = 1; i < playerCards.length; i++) {
				float rx = w / 2 + (i - 2.5f) * (SMALL_W + PLAYER_GAP), ry = cy + (i == rival ? LIFT : 0);
				playerCards[i].set(rx - SMALL_W / 2, ry - SMALL_H / 2, SMALL_W, SMALL_H);
				if (i == rival) {
					glow(rx - SMALL_W / 2, ry - SMALL_H / 2, SMALL_W, SMALL_H);
				}
				Player pl = player(c, i);
				smallCard(rx, ry, assets.levelBadge[pl.level.ordinal()], pl.name, a, i == CHAMPION);
			}
			if (rival > 0) {
				Player shown = player(c, rival);
				float bw = Math.min(w - 60, HABIT_W), bx = (w - bw) / 2, by = cy - SMALL_H / 2 - 12 - HABIT_H;
				paint.frame(assets.panel, bx, by, bw, HABIT_H);
				paint.serif(1.2f, Palette.GOLD, a).draw(batch, shown.name, bx + 18, by + HABIT_H - 10);
				paint.ui(1.0f, Palette.CREAM, a).draw(batch, shown.trait, bx + 18, by + HABIT_H - 36, bw - 36, Align.left, true);
			}
			break;
		case CHAMPION:
			bigCard(w / 2, cy, championBadge(), p.figure(), a, true);
			break;
		}
	}

	private void fit(boolean serif, float size, Color c, float a, String s, float x, float y, float bw, float bh) {
		float room = bw - FIT_PAD;
		BitmapFont f = serif ? paint.serif(size, c, a) : paint.ui(size, c, a);
		paint.glyphs.setText(f, s);
		if (paint.glyphs.width > room) {
			float smaller = size * room / paint.glyphs.width;
			f = serif ? paint.serif(smaller, c, a) : paint.ui(smaller, c, a);
		}
		paint.textIn(f, s, x, y, bw, bh);
	}

	private float stageTextSize(float width) {
		if (stageSize == 0) {
			stageSize = STAGE_TEXT;
			while (stageSize > STAGE_TEXT_LEAST) {
				paint.glyphs.setText(paint.ui(stageSize), stageText, 0, stageText.length(), Palette.CREAM, width, Align.left, true, null);
				if (paint.glyphs.height <= STAGE_CAPTION_H - STAGE_TEXT_ROOM) {
					break;
				}
				stageSize -= 0.05f;
			}
		}
		return stageSize;
	}

	private Texture championBadge() {
		return assets.levelBadge[briefing.chapter().champion().level.ordinal()];
	}

	private void icon(Texture icon, float cx, float cy, float size, boolean gilt) {
		batch.draw(icon, cx - size / 2, cy - size / 2, size, size);
		if (gilt) {
			float ring = size * GILT;
			batch.draw(assets.giltRing, cx - ring / 2, cy - ring / 2, ring, ring);
		}
	}

	private void bigCard(float cx, float cy, Texture icon, String label, float a, boolean gilt) {
		float x = cx - BIG_W / 2, y = cy - BIG_H / 2;
		glow(x, y, BIG_W, BIG_H);
		art.shadow(x, y, BIG_W, BIG_H, 8);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, BIG_W, BIG_H);
		icon(icon, cx, cy + 6, gilt ? BIG_ICON / GILT : BIG_ICON, gilt);
		batch.setColor(Color.WHITE);
		fit(true, 1.3f, Palette.GOLD_LIGHT, a, label, x, y + 22, BIG_W, 28);
	}

	private void smallCard(float cx, float cy, Texture icon, String label, float a, boolean gilt) {
		float x = cx - SMALL_W / 2, y = cy - SMALL_H / 2;
		art.shadow(x, y, SMALL_W, SMALL_H, 4);
		batch.setColor(1, 1, 1, a);
		batch.draw(assets.campaignFace, x, y, SMALL_W, SMALL_H);
		icon(icon, cx, cy + 8, gilt ? ICON / GILT : ICON, gilt);
		batch.setColor(Color.WHITE);
		fit(true, 1.1f, Palette.GOLD_LIGHT, a, label, x, y + 16, SMALL_W, 24);
	}

	private int rivalAt(float x, float y) {
		for (int i = 1; i < playerCards.length; i++) {
			if (playerCards[i].contains(x, y)) {
				return i;
			}
		}
		return 0;
	}

	private static Player player(Chapter c, int i) {
		return i == CHAMPION ? c.champion() : c.rivals.at(i);
	}

	private void glow(float x, float y, float cw, float ch) {
		float gw = cw * GLOW_W, gh = ch * GLOW_H, pulse = 0.8f + 0.2f * MathUtils.sin(briefedFor * 2f);
		paint.glow(x + cw / 2 - gw / 2, y + ch / 2 - gh / 2, gw, gh, GLOW, GLOW.a * pulse);
	}
}
