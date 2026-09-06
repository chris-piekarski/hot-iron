package hotiron.core;

/**
 * Parked CB AM uses the HF sweep seed (11 m). Do not write Antenna LNA.
 */
public final class CbListenGainPolicy
{
	private CbListenGainPolicy()
	{
	}

	public static int seed()
	{
		return AutoGainPolicy.seedGain(CbChannelPlan.VIEW_START_MHZ, CbChannelPlan.VIEW_END_MHZ);
	}
}
