package ohheck.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import ohheck.OhHeckGame;

/** Starts the game on Android. */
public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useImmersiveMode = true;
		config.useWakelock = true; // keep the screen on while the game is open
		config.useAccelerometer = false;
		config.useCompass = false;
		initialize(new OhHeckGame(), config);
	}
}
