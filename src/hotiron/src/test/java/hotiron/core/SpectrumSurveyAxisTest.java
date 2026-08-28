package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpectrumSurveyAxisTest
{
	@Test
	void hardwareFloorIsTheLeftEdgeAndMaxIsTheRight()
	{
		assertEquals(0, SpectrumSurveyAxis.mhzToFraction(FrequencyRange.MIN_MHZ), 1e-9);
		assertEquals(0, SpectrumSurveyAxis.mhzToX(FrequencyRange.MIN_MHZ, 1000));
		assertEquals(0, SpectrumSurveyAxis.mhzToX(0, 1000), "below the radio floor clamps left");
		assertEquals(1, SpectrumSurveyAxis.mhzToFraction(FrequencyRange.MAX_MHZ), 1e-9);
		assertEquals(999, SpectrumSurveyAxis.mhzToX(FrequencyRange.MAX_MHZ, 1000));
		assertEquals(FrequencyRange.MIN_MHZ, SpectrumSurveyAxis.fractionToMhz(0), 1e-9);
	}

	@Test
	void logScaleKeepsHfLeftOfMicrowave()
	{
		double nfc = SpectrumSurveyAxis.mhzToFraction(13.56);
		double fm = SpectrumSurveyAxis.mhzToFraction(98);
		double wifi2 = SpectrumSurveyAxis.mhzToFraction(2437);
		double wifi5 = SpectrumSurveyAxis.mhzToFraction(5500);
		assertTrue(nfc < fm);
		assertTrue(fm < wifi2);
		assertTrue(wifi2 < wifi5);
		assertTrue(nfc > 0.2, "NFC must sit on the radio, not in a fake 0 Hz gutter");
		assertTrue(fm < 0.55, "FM must stay on the left half so the chip is readable");
	}

	@Test
	void ticksMarkTheRadioNotZeroHz()
	{
		java.util.List<SpectrumSurveyAxis.Tick> ticks = SpectrumSurveyAxis.ticks();
		assertEquals(FrequencyRange.MIN_MHZ, ticks.get(0).mhz, 1e-9);
		assertEquals("1 MHz", ticks.get(0).label);
		assertEquals(FrequencyRange.MAX_MHZ, ticks.get(ticks.size() - 1).mhz, 1e-9);
		assertEquals("7.25 GHz", ticks.get(ticks.size() - 1).label);
		for (SpectrumSurveyAxis.Tick t : ticks)
			assertTrue(t.mhz >= FrequencyRange.MIN_MHZ, "no 0 Hz tick");
	}

	@Test
	void fractionRoundTripHoldsOnTheLogSpan()
	{
		double[] mhz = { 1, 10, 88, 108, 2402, 7250 };
		for (double m : mhz)
		{
			double back = SpectrumSurveyAxis.fractionToMhz(SpectrumSurveyAxis.mhzToFraction(m));
			assertEquals(m, back, m * 0.02 + 0.05);
		}
	}

	@Test
	void bandCenterIsGeometricOnTheLogSpan()
	{
		assertEquals(Math.sqrt(88 * 108), SpectrumSurveyAxis.bandCenterMHz(88, 108), 1e-9);
	}
}
