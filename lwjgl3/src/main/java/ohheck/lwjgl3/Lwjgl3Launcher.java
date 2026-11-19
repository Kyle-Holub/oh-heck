package ohheck.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import ohheck.OhHeckGame;

public class Lwjgl3Launcher {
	public static void main(String[] args) {
		new Lwjgl3Application(new OhHeckGame(), config(1300, 600));
	}

	static Lwjgl3ApplicationConfiguration config(int w, int h) {
		Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
		c.setTitle("Oh Heck!");
		c.setWindowedMode(w, h);
		c.useVsync(true);
		c.setForegroundFPS(60);
		return c;
	}
}
