package ohheck.screens;

import java.util.Arrays;
import java.util.List;

/* One category of the Options screen: its tab's name and its settings, top to bottom. */
final class OptionsPage {

	final String name;
	final List<OptionItem> items;

	OptionsPage(String name, OptionItem... items) {
		this.name = name;
		this.items = Arrays.asList(items);
	}
}
