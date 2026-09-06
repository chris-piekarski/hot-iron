package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GpsCaCodeTest
{
	@Test
	void prn1Has1023ChipsAndGoldAutocorrelation()
	{
		float[] c = GpsCaCode.chips(1);
		assertEquals(1023, c.length);
		assertEquals(1f, c[0] * c[0], 0);
		double ac0 = 0;
		for (float v : c)
			ac0 += v;
		assertEquals(1.0, Math.abs(ac0), 0.01, "balanced Gold mean is ±1");
		double peak = dot(c, 0);
		assertEquals(1023.0, peak, 0.01);
		for (int lag = 1; lag < 1023; lag += 17)
		{
			double r = dot(c, lag);
			assertTrue(Math.abs(r + 1) < 0.01 || Math.abs(r + 65) < 0.01 || Math.abs(r - 63) < 0.01,
					"lag " + lag + " R=" + r);
		}
	}

	@Test
	void distinctPrnsAreNotTheSame()
	{
		float[] a = GpsCaCode.chips(1);
		float[] b = GpsCaCode.chips(2);
		int same = 0;
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] == b[i])
				same++;
		}
		assertTrue(same < 700, "PRN 1 and 2 should differ");
	}

	@Test
	void resampleCoversOneMillisecond()
	{
		float[] chips = GpsCaCode.chips(5);
		float[] r = GpsCaCode.resample(chips, 4000);
		assertEquals(4000, r.length);
		assertEquals(chips[0], r[0]);
		assertEquals(chips[1022], r[3999]);
	}

	@Test
	void rejectsOutOfRangePrn()
	{
		assertThrows(IllegalArgumentException.class, () -> GpsCaCode.chips(0));
		assertThrows(IllegalArgumentException.class, () -> GpsCaCode.chips(33));
	}

	private static double dot(float[] c, int lag)
	{
		double s = 0;
		int n = c.length;
		for (int i = 0; i < n; i++)
			s += c[i] * c[(i + lag) % n];
		return s;
	}
}
