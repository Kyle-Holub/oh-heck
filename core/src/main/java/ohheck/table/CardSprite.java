package ohheck.table;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.rules.Card;

/**
 * One card's picture on the table, for the whole game: where it is, its size and tilt, how
 * far it has turned face up, and how high it's lifted (its shadow). When the card's place
 * changes it doesn't jump: it glides there from wherever it is, by a {@link Tween}.
 * No libGDX: {@link CardSprites} decides the places and draws.
 */
final class CardSprite {

	/** A place and look: bottom-left corner, size, tilt (degrees), face up (0 to 1), lift. */
	static final class Pose {
		float x, y, w, h, rot, flip, lift;

		Pose set(float x, float y, float w, float h, float rot, float flip, float lift) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.rot = rot;
			this.flip = flip;
			this.lift = lift;
			return this;
		}

		Pose set(Pose p) {
			return set(p.x, p.y, p.w, p.h, p.rot, p.flip, p.lift);
		}

		boolean same(Pose p) {
			return near(x, p.x) && near(y, p.y) && near(w, p.w) && near(h, p.h) && near(rot, p.rot) && near(flip, p.flip)
					&& near(lift, p.lift);
		}

		private static boolean near(float a, float b) {
			return Math.abs(a - b) < 0.01f;
		}
	}

	final Card card;
	/** Where it's going, where it set off from, and where it is now. */
	private final Pose target = new Pose(), from = new Pose(), now = new Pose();
	private Tween motion = Tween.over(0, Ease.LINEAR);
	/** Extra lift at the start of a motion, falling away as it lands (a card dropped onto the table). */
	private float arc;
	private float start;
	private boolean placed, visible;

	CardSprite(Card card) {
		this.card = card;
	}

	/** Puts it straight at p, with no motion. */
	void snap(Pose p) {
		target.set(p);
		from.set(p);
		now.set(p);
		motion = Tween.over(0, Ease.LINEAR);
		arc = 0;
		placed = true;
	}

	/**
	 * Sends it to p if that's a new place: it glides there by how (arc: extra lift at the
	 * start), from where it is at time t. startedAgo backdates the start, for a motion that
	 * should have begun between frames.
	 */
	void moveTo(Pose p, Tween how, float arc, float t, float startedAgo) {
		if (!placed) {
			snap(p);
			return;
		}
		if (target.same(p)) {
			return;
		}
		from.set(pose(t));
		target.set(p);
		motion = how;
		this.arc = arc;
		start = t - startedAgo;
	}

	/** Where it is at time t. */
	Pose pose(float t) {
		float u = t - start, p = motion.progress(u);
		now.x = Ease.lerp(from.x, target.x, p);
		now.y = Ease.lerp(from.y, target.y, p);
		now.w = Ease.lerp(from.w, target.w, p);
		now.h = Ease.lerp(from.h, target.h, p);
		now.rot = Ease.lerp(from.rot, target.rot, p);
		now.flip = Ease.lerp(from.flip, target.flip, p);
		now.lift = Ease.lerp(from.lift, target.lift, p) + arc * (1 - motion.raw(u));
		return now;
	}

	/** Whether it's still on its way at time t. */
	boolean moving(float t) {
		return placed && !motion.done(t - start);
	}

	boolean visible() {
		return visible;
	}

	void show(boolean v) {
		visible = v;
		if (!v) {
			placed = false; // next time it's seen, it starts where it's put
		}
	}
}
