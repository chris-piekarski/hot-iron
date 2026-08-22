package hotiron.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
