package ohheck.table;

import ohheck.anim.Ease;
import ohheck.anim.Tween;
import ohheck.rules.Card;

/* One card's pose on the table, gliding to each new target. */
final class CardSprite {

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
	private final Pose target = new Pose(), from = new Pose(), now = new Pose();
	private Tween motion = Tween.over(0, Ease.LINEAR);
	private float arc;
	private float start;
	private boolean placed, visible;

	CardSprite(Card card) {
		this.card = card;
	}

	void snap(Pose p) {
		target.set(p);
		from.set(p);
		now.set(p);
		motion = Tween.over(0, Ease.LINEAR);
		arc = 0;
		placed = true;
	}

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

	float landedFor(float t) {
		return placed && arc > 0 ? t - start - motion.end() : -1;
	}

	boolean moving(float t) {
		return placed && !motion.done(t - start);
	}

	boolean visible() {
		return visible;
	}

	void show(boolean v) {
		visible = v;
		if (!v) {
			placed = false;
		}
	}
}
