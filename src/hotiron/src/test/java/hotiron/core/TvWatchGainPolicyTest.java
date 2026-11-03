package hotiron.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TvWatchGainPolicyTest
{
	@Test
	void leavesHeadroomOnStrongUhfChannels()
	{
		TvChannel uhf = TvChannelPlan.findByFccChannel(28);
		int total = TvWatchGainPolicy.seed(uhf);
		assertEquals(62, total);
		assertEquals(40, GainPolicy.lnaGain(total));
		assertEquals(22, GainPolicy.vgaGain(total));
	}

	@Test
	void givesVhfMoreGainThanUhf()
	{
		assertEquals(70, TvWatchGainPolicy.seed(TvChannelPlan.findByFccChannel(7)));
		assertEquals(62, TvWatchGainPolicy.seed(null));
	}

	@Test
	void retuneHoldsWhenRmsAlreadyNearTarget()
	{
		assertEquals(62, TvWatchGainPolicy.retune(62, 0.53f));
		assertEquals(62, TvWatchGainPolicy.retune(62, 0.00f));
		assertFalse(TvWatchGainPolicy.shouldTrimIf(62, 0.53f));
	}

	@Test
	void retuneRaisesWeakIfAndLowersClippedIf()
	{
		assertEquals(68, TvWatchGainPolicy.retune(62, 0.25f));
		assertEquals(70, TvWatchGainPolicy.retune(62, 0.21f));
		assertEquals(68, TvWatchGainPolicy.retune(72, 0.69f));
		assertTrue(TvWatchGainPolicy.shouldTrimIf(62, 0.25f));
		assertTrue(TvWatchGainPolicy.shouldTrimIf(72, 0.69f));
	}

	@Test
	void holdsIfWhileRsIsUsableUnlessClipped()
	{
		assertTrue(TvWatchGainPolicy.shouldHoldIf(true, 48, 0.40f));
		assertTrue(TvWatchGainPolicy.shouldHoldIf(false, 16, 0.40f));
		assertFalse(TvWatchGainPolicy.shouldHoldIf(true, 48, 0.70f));
		assertFalse(TvWatchGainPolicy.shouldHoldIf(false, 0, 0.25f));
	}

	@Test
	void autoWatchTurnsOnTheRfAmpWithoutTheOperatorCheckbox()
	{
		assertTrue(TvWatchGainPolicy.antennaLna(true, false));
		assertTrue(TvWatchGainPolicy.antennaLna(true, true));
		assertFalse(TvWatchGainPolicy.antennaLna(false, false));
		assertTrue(TvWatchGainPolicy.antennaLna(false, true));
	}

	@Test
	void spectrumSnrAndInterfererMeasureChannelAgainstGuardBands()
	{
		TvChannel ch = TvChannelPlan.findByFccChannel(33); // 584..590 MHz, center 587 MHz
		int n = 256;
		float[] mhz = new float[n];
		float[] dbfs = new float[n];
		for (int i = 0; i < n; i++)
		{
			mhz[i] = 579f + (16f * i) / (n - 1);
			double d = Math.abs(mhz[i] - ch.centerMHz());
			dbfs[i] = d <= 2.8 ? -30f : -45f;
		}
		float snr = TvWatchGainPolicy.spectrumSnrDb(mhz, dbfs, ch);
		assertTrue(Float.isFinite(snr) && snr > 12f && snr < 16f, "expected ~14 dB SNR, got " + snr);
		assertTrue(TvWatchGainPolicy.interfererExcessDb(mhz, dbfs, ch) < 0f);

		// Add a strong adjacent interferer at +6 MHz (+20 dB above the wanted channel)
		dbfs[n - 10] = -10f;
		float interf = TvWatchGainPolicy.interfererExcessDb(mhz, dbfs, ch);
		assertEquals(20f, interf, 0.1f);
		assertEquals(56, TvWatchGainPolicy.retuneForQuality(62, 0.48f, 0f, 10f, snr, interf));
	}

	@Test
	void chooseOptimalGainPicksLowestUnclippedStepWithinHalfDbOfBestSnr()
	{
		java.util.List<TvWatchGainPolicy.GainPoint> pts = java.util.List.of(
				new TvWatchGainPolicy.GainPoint(46, 17.0f, 14f, 0.25f, 0f),
				new TvWatchGainPolicy.GainPoint(54, 19.8f, 16f, 0.38f, 0f),
				new TvWatchGainPolicy.GainPoint(62, 20.1f, 16.2f, 0.50f, 0f),
				new TvWatchGainPolicy.GainPoint(70, 21.5f, 17f, 0.65f, 0.01f)); // clipped, excluded
		assertEquals(54, TvWatchGainPolicy.chooseOptimalGain(pts, 62),
				"54 dB is within 0.5 dB of 62 dB (20.1 dB) and 70 dB is clipped");
	}
}
