package hotiron.core;

/**
 * Splits a requested total gain into HackRF LNA (step 8, 0–40) and VGA
 * (step 2, only after LNA is already 40).
 */
public final class GainPolicy {
	public static final int LNA_MAX = 40;
	public static final int LNA_STEP = 8;
	public static final int VGA_MAX = 60;
	public static final int VGA_STEP = 2;
	public static final int TOTAL_MAX = LNA_MAX + VGA_MAX;

	private GainPolicy() {
	}

	/** Snap to a legal LNA-then-VGA total. */
	public static int clampTotal(int totalGain) {
		if (totalGain < 0)
			totalGain = 0;
		if (totalGain > TOTAL_MAX)
			totalGain = TOTAL_MAX;
		return lnaGain(totalGain) + vgaGain(totalGain);
	}

	public static int lnaGain(int totalGain) {
		int lnaGain = totalGain / LNA_STEP * LNA_STEP;
		if (lnaGain > LNA_MAX)
			lnaGain = LNA_MAX;
		if (lnaGain < 0)
			lnaGain = 0;
		return lnaGain;
	}

	public static int vgaGain(int totalGain) {
		int lnaGain = lnaGain(totalGain);
		if (lnaGain != LNA_MAX)
			return 0;
		int vga = (totalGain - lnaGain) & ~1;
		if (vga > VGA_MAX)
			vga = VGA_MAX;
		if (vga < 0)
			vga = 0;
		return vga;
	}
}
