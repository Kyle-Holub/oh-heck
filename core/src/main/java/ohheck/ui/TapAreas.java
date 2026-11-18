package ohheck.ui;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

/** Reusable tap areas: buttons drawn each frame set their rectangles here. */
public final class TapAreas {

	private TapAreas() {
	}

	/** Grows or shrinks a list of reusable tap areas to n, keeping the existing objects. */
	public static void sizeTo(List<Rectangle> list, int n) {
		while (list.size() > n) {
			list.remove(list.size() - 1);
		}
		while (list.size() < n) {
			list.add(new Rectangle());
		}
	}
}
