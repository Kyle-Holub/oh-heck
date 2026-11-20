package ohheck.screens;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;

import ohheck.ui.Painter;
import ohheck.ui.Palette;

/* One setting on an options page: its name and a line on what it does, with a control on the right. */
abstract class OptionItem {

	static final float ROW_H = 66, PAD = 22, PILL_W = 150, PILL_H = 38;

	final String label, detail;
	final Rectangle bounds = new Rectangle();

	OptionItem(String label, String detail) {
		this.label = label;
		this.detail = detail;
	}

	static OptionItem toggle(String label, String detail, BooleanSupplier on, Runnable flip) {
		return new Choice(label, detail, () -> on.getAsBoolean() ? "On" : "Off", on, flip);
	}

	static OptionItem choice(String label, String detail, Supplier<String> value, Runnable next) {
		return new Choice(label, detail, value, () -> true, next);
	}

	static OptionItem link(String label, String detail, Runnable open) {
		return new Link(label, detail, open);
	}

	float height() {
		return ROW_H;
	}

	void draw(Painter paint, float x, float y, float w) {
		float h = height();
		bounds.set(x, y, w, h);
		float top = y + h;
		BitmapFont name = paint.serif(1.3f, Palette.CREAM);
		name.draw(paint.batch, label, x + PAD, top - 10);
		BitmapFont about = paint.ui(0.85f, Palette.MUTED);
		about.draw(paint.batch, detail, x + PAD, top - 38);
		drawControl(paint, x + w - PAD, y, h);
	}

	abstract void drawControl(Painter paint, float right, float y, float h);

	abstract void tap(float x, float y);

	boolean grabs(float x, float y) {
		return false;
	}

	void dragTo(float x) {
	}

	private static final class Choice extends OptionItem {
		private final Supplier<String> value;
		private final BooleanSupplier lit;
		private final Runnable next;

		Choice(String label, String detail, Supplier<String> value, BooleanSupplier lit, Runnable next) {
			super(label, detail);
			this.value = value;
			this.lit = lit;
			this.next = next;
		}

		@Override
		void drawControl(Painter paint, float right, float y, float h) {
			boolean on = lit.getAsBoolean();
			float px = right - PILL_W, py = y + (h - PILL_H) / 2;
			paint.frame(on ? paint.assets.button : paint.assets.buttonOff, px, py, PILL_W, PILL_H);
			paint.textIn(paint.ui(1.25f, on ? Palette.CREAM : Palette.MUTED), value.get(), px, py, PILL_W, PILL_H);
		}

		@Override
		void tap(float x, float y) {
			next.run();
		}
	}

	private static final class Link extends OptionItem {
		private final Runnable open;

		Link(String label, String detail, Runnable open) {
			super(label, detail);
			this.open = open;
		}

		@Override
		void drawControl(Painter paint, float right, float y, float h) {
			BitmapFont arrow = paint.serif(1.8f, Palette.GOLD);
			paint.text(arrow, ">", right, y + h / 2 + 14, false);
		}

		@Override
		void tap(float x, float y) {
			open.run();
		}
	}
}
