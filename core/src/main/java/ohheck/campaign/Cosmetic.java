package ohheck.campaign;

/*
 * A look for the cards and the table: the classic one, and one won by clearing each chapter. The felt also sets
 * the colour of the sparkles (sparks, fireworks, motes, the title's flares), as 0xRRGGBB.
 */
public enum Cosmetic {
	CLASSIC("Classic", null, "back_hd", "back_blur", "table", 0xFFB850),
	EMERALD("Emerald", Chapter.ONE, "back_ch1", "back_ch1_blur", "table_ch1", 0x70FF9A),
	AMETHYST("Amethyst", Chapter.TWO, "back_ch2", "back_ch2_blur", "table_ch2", 0xC888FF),
	MIDNIGHT("Midnight", Chapter.THREE, "back_ch3", "back_ch3_blur", "table_ch3", 0x80B4FF),
	GILDED("Gilded", Chapter.FOUR, "back_ch4", "back_ch4_blur", "table_ch4", 0xFFD870);

	public final String label;
	public final Chapter wonBy;
	public final String back, backBlur, felt;
	public final int sparkle;

	Cosmetic(String label, Chapter wonBy, String back, String backBlur, String felt, int sparkle) {
		this.label = label;
		this.wonBy = wonBy;
		this.back = back;
		this.backBlur = backBlur;
		this.felt = felt;
		this.sparkle = sparkle;
	}

	public boolean owned(Campaign progress) {
		return wonBy == null || progress.cleared(wonBy);
	}

	public Cosmetic nextOwned(Campaign progress) {
		Cosmetic[] all = values();
		for (int i = 1; i <= all.length; i++) {
			Cosmetic c = all[(ordinal() + i) % all.length];
			if (c.owned(progress)) {
				return c;
			}
		}
		return this;
	}

	public static Cosmetic named(String name) {
		for (Cosmetic c : values()) {
			if (c.name().equals(name)) {
				return c;
			}
		}
		return CLASSIC;
	}
}
