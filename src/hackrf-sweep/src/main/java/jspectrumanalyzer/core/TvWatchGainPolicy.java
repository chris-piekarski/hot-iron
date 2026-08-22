package jspectrumanalyzer.core;

/**
 * Conservative parked-IQ gain for ATSC's 8-bit receive path.
 */
public final class TvWatchGainPolicy
{
	/*
	 * Watch needs more gain than a sweep seed, but +32 dB drove strong UHF
	 * channels to nearly full-scale RMS. +22 dB targets roughly 0.25 RMS
	 * on the strongest observed UHF brick while retaining ADC headroom.
	 */
	public static final int BOOST_DB = 22;

	private TvWatchGainPolicy()
	{
	}

	public static int seed(TvChannel channel)
	{
		if (channel == null)
			return GainPolicy.clampTotal(40 + BOOST_DB);
		return GainPolicy.clampTotal(
				AutoGainPolicy.seedGain(channel.lowMHz, channel.highMHz()) + BOOST_DB);
	}
}
