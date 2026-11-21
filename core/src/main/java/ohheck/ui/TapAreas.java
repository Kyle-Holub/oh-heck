package ohheck.ui;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

public final class TapAreas {

	private TapAreas() {
	}

	public static void sizeTo(List<Rectangle> list, int n) {
		while (list.size() > n) {
			list.remove(list.size() - 1);
		}
		while (list.size() < n) {
			list.add(new Rectangle());
		}
	}
}
