package ohheck.table;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import ohheck.anim.Ease;
import ohheck.campaign.Chapter;
import ohheck.campaign.PracticeCoach.Moment;
import ohheck.campaign.PracticeCoach.Tip;
import ohheck.campaign.PracticeCoach;
import ohheck.campaign.PracticeRound;
import ohheck.rules.Match;
import ohheck.rules.Move;
import ohheck.ui.Assets;
import ohheck.ui.Painter;
import ohheck.ui.Palette;

/*
 * The practice round at the table: reads which moment the table is at, asks the coach for a tip, and holds
 * the table still while a tip is up, until it is tapped away. A first practice round ends with a finale: the
 * rivals' cards gather into one, shake and turn over into the Chapter 1 card.
 */
final class PracticeGuide {

	private final PracticeCoach coach = new PracticeCoach();
	private Tip tip;
	private static final String CHAPTER = "CHAPTER";
	private static final float FINALE_GATHER = 0.5f, FINALE_SHAKE = 1.3f, FINALE_FLASH = 1.45f, FINALE_FLIP = 0.4f, FINALE_LENGTH = 2.9f,
			FINALE_W = 150, FINALE_H = FINALE_W * 98 / 73;
	private float finaleAt = -1;
	private boolean finaleDone;

	void startFinale(float now) {
		if (finaleAt < 0) {
			finaleAt = now;
		}
	}

	boolean finishing() {
		return finaleAt >= 0;
	}

	boolean finaleFlips(float before, float now) {
		float at = finaleAt + FINALE_FLASH + FINALE_FLIP / 2;
		return before < at && now >= at;
	}

	boolean finaleEnds(float now) {
		if (finaleDone || now - finaleAt < FINALE_LENGTH) {
			return false;
		}
		finaleDone = true;
		return true;
	}

	void drawFinale(Painter paint, TableLayout layout, Match m, float now, float w, float h) {
		Assets assets = paint.assets;
		SpriteBatch batch = paint.batch;
		float t = now - finaleAt;
		paint.fade(Math.min(1, t * 3));
		paint.rect(0, 0, w, h, Palette.BACKDROP);
		paint.fade(1f);
		float cx = w / 2, cy = h / 2;
		if (t < FINALE_GATHER) {
			float k = Ease.IN.apply(t / FINALE_GATHER);
			for (int seat = 1; seat < m.players(); seat++) {
				float sx = layout.panelX(seat) + PlayerPanel.W / 2, sy = layout.panelTop(seat) - PlayerPanel.H / 2;
				float x = MathUtils.lerp(sx, cx, k), y = MathUtils.lerp(sy, cy, k), cw = MathUtils.lerp(FINALE_W * 0.6f, FINALE_W, k);
				float ch = cw * 98 / 73;
				batch.draw(assets.back(), x - cw / 2, y - ch / 2, cw / 2, ch / 2, cw, ch, 1, 1, (1 - k) * (seat * 20 - 40));
			}
			return;
		}
		float shake = t < FINALE_SHAKE ? (t - FINALE_GATHER) / (FINALE_SHAKE - FINALE_GATHER) : 0;
		float flip = MathUtils.clamp((t - FINALE_FLASH) / FINALE_FLIP, 0, 1), sx = Math.abs(MathUtils.cos(flip * MathUtils.PI));
		float glowA = t < FINALE_FLASH ? 0.3f + 0.6f * shake : 0.6f + 0.2f * MathUtils.sin(t * 3);
		paint.glow(cx - FINALE_W * 1.3f, cy - FINALE_H, FINALE_W * 2.6f, FINALE_H * 2, Palette.EMBER, glowA);
		float wobble = MathUtils.sin(t * 40) * 6 * shake, cw = FINALE_W * Math.max(0.03f, sx);
		if (flip < 0.5f) {
			batch.draw(assets.back(), cx - cw / 2, cy - FINALE_H / 2, cw / 2, FINALE_H / 2, cw, FINALE_H, 1, 1, wobble);
		} else {
			batch.draw(assets.campaignFace, cx - cw / 2, cy - FINALE_H / 2, cw, FINALE_H);
			if (flip >= 1) {
				paint.text(paint.ui(0.9f, Palette.GOLD), CHAPTER, cx, cy + FINALE_H / 2 - 18, true);
				paint.numeral(1, cx, cy + FINALE_H * 0.2f, 48, Palette.GOLD_LIGHT);
				float e = 50;
				batch.draw(assets.emblem(Chapter.ONE.emblem), cx - e / 2, cy - e / 2 - 14, e, e);
				paint.text(paint.serif(1.4f, Palette.GOLD_LIGHT), Chapter.ONE.mode.label, cx, cy - FINALE_H / 2 + 46, true);
			}
		}
		float flash = t - FINALE_FLASH;
		if (flash >= 0 && flash < 0.2f) {
			paint.fade(0.35f * (1 - flash / 0.2f));
			paint.rect(0, 0, w, h, Palette.CREAM);
			paint.fade(1f);
		}
	}


	boolean update(TableScene s) {
		if (tip == null) {
			for (Moment moment : Moment.values()) {
				if (at(moment, s)) {
					tip = coach.next(moment, s.match());
					if (tip != null) {
						break;
					}
				}
			}
		}
		return tip != null;
	}

	Tip tip() {
		return tip;
	}

	void dismiss() {
		if (tip != null) {
			coach.done(tip);
			tip = null;
		}
	}

	boolean allows(Match m, Move move) {
		return PracticeRound.allowed(m, move);
	}

	private static boolean at(Moment moment, TableScene s) {
		TableFlow f = s.flow();
		Match m = s.match();
		TableState state = f.state();
		switch (moment) {
		case SAT_DOWN:
			return state == TableState.PICK_DEALER;
		case DEALER_PICKED:
			return state == TableState.PICK_DEALER && DealerPick.settled(f.stateTime())
					|| m.handIndex() == 0 && (state == TableState.SHUFFLE || state == TableState.DEAL);
		case HAND_DEALT:
			return state == TableState.TURN && f.trumpFaceUp();
		case YOUR_BID:
			return f.yourTurnToBid() && f.trumpFaceUp();
		case YOUR_PLAY:
			return f.yourTurnToPlay();
		case HAND_SCORED:
			return state == TableState.RESULTS && f.stateTime() >= ScoreCard.LENGTH;
		case FINISHED:
			return state == TableState.GAME_OVER && s.resultsShown();
		default:
			return false;
		}
	}
}
