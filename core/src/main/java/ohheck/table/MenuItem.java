package ohheck.table;

/** The pause menu's choices. */
enum MenuItem {
	RESUME("Resume"),
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

	static final MenuItem[] ALL = values();

	final String label;

	MenuItem(String label) {
		this.label = label;
	}

	boolean closesMenu() {
		return true;
	}

	void choose(TableActions act) {
	}
}
