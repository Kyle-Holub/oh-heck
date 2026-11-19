package ohheck.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import ohheck.rules.Card;

/** Card drawing helpers: shadows, flips, stacks and title art. */
public final class CardArt {

	private final SpriteBatch batch;
	private final Assets assets;

	public CardArt(SpriteBatch batch, Assets assets) {
		this.batch = batch;
		this.assets = assets;
	}

	public void shadow(float x, float y, float cw, float ch, float lift) {
		float sw = cw * 160f / 120f, sh = ch * 200f / 160f;
		batch.setColor(1f, 1f, 1f, MathUtils.clamp(0.6f - lift * 0.008f, 0.28f, 0.6f));
		batch.draw(assets.shadow, x + cw / 2 - sw / 2 + lift * 0.2f, y + ch / 2 - sh / 2 - 2 - lift * 0.4f, sw, sh);
		batch.setColor(Color.WHITE);
	}

	public void flip(Card c, float x, float y, float cw, float ch, float p) {
		flip(c, x, y, cw, ch, p, 0f);
	}

	public void flip(Card c, float x, float y, float cw, float ch, float p, float rot) {
		float sx = p >= 1f ? 1f : Math.abs(MathUtils.cos(MathUtils.PI * p));
		TextureRegion r = p < 0.5f ? assets.back() : assets.face(c);
		if (rot == 0f) {
			batch.draw(r, x + cw * (1 - sx) / 2, y, cw * sx, ch);
		} else {
			batch.draw(r, x + cw * (1 - sx) / 2, y, cw * sx / 2, ch / 2, cw * sx, ch, 1f, 1f, rot);
		}
	}

	public void stack(float cx, float cy, int count, float cw, float ch, float rotation) {
		int shown = Math.min(count, 14);
		for (int i = 0; i < shown; i++) {
			float x = cx - cw / 2 - i * 0.45f, y = cy - ch / 2 + i * 0.7f;
			if (i == 0) {
				shadow(x, y, cw, ch, 4 + shown * 0.4f);
			}
			batch.draw(assets.back(), x, y, cw / 2, ch / 2, cw, ch, 1f, 1f, rotation);
		}
	}

	public void art(Texture t, float cx, float cy, float scale, float alpha) {
		float aw = t.getWidth() * scale, ah = t.getHeight() * scale;
		batch.setColor(1f, 1f, 1f, alpha);
		batch.draw(t, cx - aw / 2, cy - ah / 2, aw, ah);
		batch.setColor(Color.WHITE);
	}
}
