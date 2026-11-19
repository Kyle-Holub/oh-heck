package ohheck.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ohheck.platform.Audio;
import ohheck.platform.GameOptions;
import ohheck.platform.Settings;
import ohheck.records.Records;

public interface Services {

	String version();

	String build();

	GameOptions options();

	SpriteBatch batch();

	Assets assets();

	Settings settings();

	Audio audio();

	PostProcessor post();

	Records records();

	void fitViewportToTarget();

	Tour tour();
}
