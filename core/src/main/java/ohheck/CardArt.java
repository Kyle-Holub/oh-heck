package ohheck;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import rules.Card;

/** Drawing helpers for cards on the table: shadows, flips, stacks and the original title art. */
final class CardArt {

	private final SpriteBatch batch;
	private final Assets assets;

	CardArt(SpriteBatch batch, Assets assets) {
		this.batch = batch;
		this.assets = assets;
	}

	/** A soft shadow under a card; the higher the card is lifted, the further and fainter it falls. */
	void shadow(float x, float y, float cw, float ch, float lift) {
		float sw = cw * 160f / 120f, sh = ch * 200f / 160f;
		batch.setColor(1f, 1f, 1f, MathUtils.clamp(0.6f - lift * 0.008f, 0.28f, 0.6f));
		batch.draw(assets.shadow, x + cw / 2 - sw / 2 + lift * 0.2f, y + ch / 2 - sh / 2 - 2 - lift * 0.4f, sw, sh);
		batch.setColor(Color.WHITE);
	}

	/**
	 * A card turning over about its upright centre line: p = 0 face down, 0.5
	 * edge on, 1 face up. Uses the batch's current tint.
	 */
	void flip(Card c, float x, float y, float cw, float ch, float p) {
		flip(c, x, y, cw, ch, p, 0f);
	}

	/** As above, turned by rot degrees about the card's centre. */
	void flip(Card c, float x, float y, float cw, float ch, float p, float rot) {
		float sx = p >= 1f ? 1f : Math.abs(MathUtils.cos(MathUtils.PI * p));
		TextureRegion r = p < 0.5f ? assets.back() : assets.face(c);
		if (rot == 0f) {
			batch.draw(r, x + cw * (1 - sx) / 2, y, cw * sx, ch);
		} else {
			batch.draw(r, x + cw * (1 - sx) / 2, y, cw * sx / 2, ch / 2, cw * sx, ch, 1f, 1f, rot);
		}
	}

	/** A deck seen from above at an angle: count cards stacked with a slight offset, centred on (cx, cy). */
	void stack(float cx, float cy, int count, float cw, float ch, float rotation) {
		int shown = Math.min(count, 14);
		for (int i = 0; i < shown; i++) {
			float x = cx - cw / 2 - i * 0.45f, y = cy - ch / 2 + i * 0.7f;
			if (i == 0) {
				shadow(x, y, cw, ch, 4 + shown * 0.4f);
			}
			batch.draw(assets.back(), x, y, cw / 2, ch / 2, cw, ch, 1f, 1f, rotation);
		}
	}

	/** One of the original title-art images, centred on (cx, cy). */
	void art(Texture t, float cx, float cy, float scale, float alpha) {
		float aw = t.getWidth() * scale, ah = t.getHeight() * scale;
		batch.setColor(1f, 1f, 1f, alpha);
		batch.draw(t, cx - aw / 2, cy - ah / 2, aw, ah);
		batch.setColor(Color.WHITE);
	}
}
