package ohheck.table;

/**
 * The pause menu's choices, top to bottom: each one's label and what it does. The first is
 * drawn as the main button. A new choice is one more constant.
 */
enum MenuItem {
	RESUME("Resume"),
	/** Options, over the table: the menu is still open when you come back. */
	OPTIONS("Options") {
		@Override
		boolean closesMenu() {
			return false;
		}

		@Override
		void choose(TableActions act) {
			act.showOptions();
		}
	},
	/** A fresh table, at the difficulty now chosen in Options. */
	NEW_GAME("New game") {
		@Override
		void choose(TableActions act) {
			act.newGame();
		}
	},
	MAIN_MENU("Main menu") {
		@Override
		void choose(TableActions act) {
			act.showTitle();
		}
	};

	/** Every choice, top to bottom (made once: values() copies the array each call). */
	static final MenuItem[] ALL = values();

	final String label;

	MenuItem(String label) {
		this.label = label;
	}

	/** Whether choosing it closes the menu (true unless overridden). */
	boolean closesMenu() {
		return true;
	}

	/** What it does beyond closing the menu (nothing, unless overridden). */
	void choose(TableActions act) {
	}
}
