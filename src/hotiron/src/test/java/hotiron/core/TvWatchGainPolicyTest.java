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
}
