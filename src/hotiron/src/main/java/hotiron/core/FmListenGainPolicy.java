package hotiron.core;

/**
 * Parked-IQ gain for broadcast WFM. Sweep AutoGain is for a wide FFT
 * window (Wi-Fi, All, UHF) and leaves VGA at 0; Listen needs IF gain so
 * the 200 kHz station uses the 8-bit ADC.
 */
public final class FmListenGainPolicy
{
	/*
	 * VHF sweep seed is 48 dB (LNA 40 + VGA 8). +16 dB more IF targets
	 * a mid-market FM carrier around −30 dBFS without slamming full-scale
	 * on a local 50 kW stick.
	 */
	public static final int BOOST_DB = 16;

	private FmListenGainPolicy()
	{
	}

	public static int seed(FmChannel channel)
	{
		int start = FmChannelPlan.VIEW_START_MHZ;
		int end = FmChannelPlan.VIEW_END_MHZ;
		if (channel != null)
		{
			start = (int) Math.floor(channel.lowMHz());
			end = (int) Math.ceil(channel.highMHz());
		}
		return GainPolicy.clampTotal(AutoGainPolicy.seedGain(start, end) + BOOST_DB);
	}
}
