package hotiron.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FmListenGainPolicyTest
{
	@Test
	void parkedFmUsesIfGainAfterMaxLna()
	{
		int total = FmListenGainPolicy.seed(FmChannelPlan.clamp(97.3));
		assertEquals(64, total);
		assertEquals(40, GainPolicy.lnaGain(total));
		assertEquals(24, GainPolicy.vgaGain(total));
	}

	@Test
	void nullChannelStillSeedsTheFmBroadcastBand()
	{
		assertEquals(64, FmListenGainPolicy.seed(null));
		assertTrue(FmListenGainPolicy.seed(FmChannelPlan.clamp(88.1)) >= 48);
	}
}
