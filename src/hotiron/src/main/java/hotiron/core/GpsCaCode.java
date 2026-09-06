package hotiron.core;

/**
 * GPS C/A Gold codes (IS-GPS-200), PRN 1–32. Chips are ±1.
 */
public final class GpsCaCode
{
	public static final int CHIPS = 1023;
	public static final int PRN_MIN = 1;
	public static final int PRN_MAX = 32;
	/** 1-indexed G2 taps per PRN (IS-GPS-200 Table 3-Ia). */
	static final int[][] G2_TAPS = {
			{ 2, 6 }, { 3, 7 }, { 4, 8 }, { 5, 9 }, { 1, 9 }, { 2, 10 }, { 1, 8 }, { 2, 9 }, { 3, 10 },
			{ 2, 3 }, { 3, 4 }, { 5, 6 }, { 6, 7 }, { 7, 8 }, { 8, 9 }, { 9, 10 }, { 1, 4 }, { 2, 5 },
			{ 3, 6 }, { 4, 7 }, { 5, 8 }, { 6, 9 }, { 1, 3 }, { 4, 6 }, { 5, 7 }, { 6, 8 }, { 7, 9 },
			{ 8, 10 }, { 1, 6 }, { 2, 7 }, { 3, 8 }, { 4, 9 }
	};

	private GpsCaCode()
	{
	}

	public static float[] chips(int prn)
	{
		if (prn < PRN_MIN || prn > PRN_MAX)
			throw new IllegalArgumentException("prn");
		int[] taps = G2_TAPS[prn - 1];
		int[] g1 = new int[10];
		int[] g2 = new int[10];
		java.util.Arrays.fill(g1, 1);
		java.util.Arrays.fill(g2, 1);
		float[] out = new float[CHIPS];
		for (int i = 0; i < CHIPS; i++)
		{
			int g1o = g1[9];
			int g2o = g2[taps[0] - 1] ^ g2[taps[1] - 1];
			out[i] = (g1o ^ g2o) == 0 ? 1f : -1f;
			int f1 = g1[2] ^ g1[9];
			int f2 = g2[1] ^ g2[2] ^ g2[5] ^ g2[7] ^ g2[8] ^ g2[9];
			for (int s = 9; s > 0; s--)
			{
				g1[s] = g1[s - 1];
				g2[s] = g2[s - 1];
			}
			g1[0] = f1;
			g2[0] = f2;
		}
		return out;
	}

	/** Resample 1023 chips onto {@code n} samples covering 1 ms. */
	public static float[] resample(float[] chips, int n)
	{
		if (chips == null || chips.length != CHIPS || n <= 0)
			return new float[0];
		float[] out = new float[n];
		for (int i = 0; i < n; i++)
		{
			int k = (int) ((long) i * CHIPS / n);
			if (k >= CHIPS)
				k = CHIPS - 1;
			out[i] = chips[k];
		}
		return out;
	}
}
