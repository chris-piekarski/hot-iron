package hotiron.core;

/**
 * Conservative parked-IQ gain for ATSC's 8-bit receive path.
 */
public final class TvWatchGainPolicy
{
	/*
	 * Watch needs more IF than a UHF sweep seed (40 dB). +22 dB is the
	 * first guess (LNA 40 + VGA 22). Live RMS then trims IF toward
	 * {@link #TARGET_RMS} without reopening USB: 0.25 RMS finds field
	 * sync but Reed-Solomon stays at the 16-packet flush; 0.69 RMS
	 * clipped and field sync vanished.
	 */
	public static final int BOOST_DB = 22;
	public static final float TARGET_RMS = 0.50f;
	public static final float MIN_RMS = 0.05f;
	public static final float MIN_STEP_DB = 2.0f;
	public static final float CLIP_RMS = 0.62f;
	public static final long FIRST_TRIM_MS = 1500;
	public static final long TRIM_SETTLE_MS = 2500;

	/**
	 * Auto Watch turns on the HackRF RF amp (+14 dB). The operator
	 * checkbox is left alone so sweep resume does not keep the amp or
	 * fire a restart. Manual Watch honors the checkbox.
	 */
	public static boolean antennaLna(boolean autoGain, boolean operatorLna)
	{
		return autoGain || operatorLna;
	}

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

	/**
	 * True when a live IF trim would move the radio. Field sync is not a
	 * reason to skip: 0.25 RMS still trains PN511, but RS never recovers.
	 */
	public static boolean shouldTrimIf(int currentTotal, float rmsIq)
	{
		if (!Float.isFinite(rmsIq) || rmsIq < MIN_RMS)
			return false;
		return retune(currentTotal, rmsIq) != GainPolicy.clampTotal(currentTotal);
	}

	/**
	 * Hold IF while Reed-Solomon is usable so a VGA hop does not dump the
	 * equalizer. Clip still trims immediately.
	 */
	public static boolean shouldHoldIf(boolean locked, long rsGoodWindow, float rmsIq)
	{
		if (!Float.isFinite(rmsIq) || rmsIq >= CLIP_RMS)
			return false;
		return locked || rsGoodWindow >= 16;
	}

	/** IF trim so 8-bit 8VSB sits near {@link #TARGET_RMS}. */
	public static int retune(int currentTotal, float rmsIq)
	{
		int cur = GainPolicy.clampTotal(currentTotal);
		if (!Float.isFinite(rmsIq) || rmsIq < MIN_RMS)
			return cur;
		float db = (float) (20.0 * Math.log10(TARGET_RMS / rmsIq));
		if (!Float.isFinite(db) || Math.abs(db) < MIN_STEP_DB)
			return cur;
		return GainPolicy.clampTotal(cur + Math.round(db));
	}
}
