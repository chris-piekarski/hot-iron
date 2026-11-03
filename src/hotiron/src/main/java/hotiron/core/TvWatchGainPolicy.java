package hotiron.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Conservative parked-IQ gain for ATSC's 8-bit receive path, upgraded with
 * decode-quality (equalizer SNR / in-channel spectrum SNR) and ADC
 * clip/interferer avoidance.
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
	public static final float CLIP_FRACTION_LIMIT = 0.001f;
	public static final float SNR_TOL_DB = 0.5f;
	public static final float MIN_USABLE_SNR_DB = 3.0f;
	public static final float HEALTHY_EQ_SNR_DB = 15.5f;
	public static final float INTERFERER_DB = 15.0f;
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

	public static boolean isClipped(float rmsIq, float clipFraction)
	{
		if (Float.isFinite(rmsIq) && rmsIq >= CLIP_RMS)
			return true;
		return Float.isFinite(clipFraction) && clipFraction > CLIP_FRACTION_LIMIT;
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

	public static boolean shouldTrimIf(int currentTotal, float rmsIq, float clipFraction,
			float eqSnrDb, float spectrumSnrDb, float interfererDb)
	{
		if (isClipped(rmsIq, clipFraction))
			return retuneForQuality(currentTotal, rmsIq, clipFraction, eqSnrDb, spectrumSnrDb, interfererDb)
					!= GainPolicy.clampTotal(currentTotal);
		if (!Float.isFinite(rmsIq) || rmsIq < MIN_RMS)
			return false;
		return retuneForQuality(currentTotal, rmsIq, clipFraction, eqSnrDb, spectrumSnrDb, interfererDb)
				!= GainPolicy.clampTotal(currentTotal);
	}

	/**
	 * Hold IF while Reed-Solomon is usable so a VGA hop does not dump the
	 * equalizer. Clip still trims immediately.
	 */
	public static boolean shouldHoldIf(boolean locked, long rsGoodWindow, float rmsIq)
	{
		return shouldHoldIf(locked, rsGoodWindow, rmsIq, 0f);
	}

	public static boolean shouldHoldIf(boolean locked, long rsGoodWindow, float rmsIq, float clipFraction)
	{
		if (!Float.isFinite(rmsIq) || isClipped(rmsIq, clipFraction))
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

	/**
	 * Quality-aware IF trim: backs off immediately on ADC clip or when a
	 * strong out-of-channel interferer in the 16 MS/s capture is swamping
	 * an unlocked decoder; otherwise trims toward {@link #TARGET_RMS}.
	 */
	public static int retuneForQuality(int currentTotal, float rmsIq, float clipFraction,
			float eqSnrDb, float spectrumSnrDb, float interfererDb)
	{
		int cur = GainPolicy.clampTotal(currentTotal);
		if (Float.isFinite(clipFraction) && clipFraction > CLIP_FRACTION_LIMIT)
		{
			int drop = clipFraction > 0.05f ? 16 : (clipFraction > 0.01f ? 10 : 6);
			int rmsTarget = Float.isFinite(rmsIq) && rmsIq >= MIN_RMS ? retune(cur, rmsIq) : cur;
			return GainPolicy.clampTotal(Math.min(cur - drop, rmsTarget));
		}
		if (Float.isFinite(rmsIq) && rmsIq >= CLIP_RMS)
			return retune(cur, rmsIq);
		if (Float.isFinite(interfererDb) && interfererDb >= INTERFERER_DB
				&& (!Float.isFinite(eqSnrDb) || eqSnrDb < HEALTHY_EQ_SNR_DB)
				&& (!Float.isFinite(rmsIq) || rmsIq > 0.30f))
		{
			return GainPolicy.clampTotal(cur - 6);
		}
		return retune(cur, rmsIq);
	}

	/**
	 * In-channel SNR (dB) of the 6 MHz ATSC channel against the quietest
	 * quarter of out-of-channel bins in the 16 MS/s capture (OnAir
	 * {@code spectrumSnrDb}). Returns {@code Float.NaN} if the spectrum
	 * does not span both the channel and its guard skirts.
	 */
	public static float spectrumSnrDb(float[] mhz, float[] dbfs, TvChannel channel)
	{
		if (mhz == null || dbfs == null || channel == null || mhz.length < 64 || mhz.length != dbfs.length)
			return Float.NaN;
		int n = mhz.length;
		double spanMHz = mhz[n - 1] - mhz[0];
		if (!(spanMHz > 0))
			return Float.NaN;
		double binMHz = spanMHz / (n - 1);
		double centerMHz = channel.centerMHz();
		double halfFlatMHz = 0.40 * TvChannelPlan.WIDTH_MHZ;
		double skirtMHz = 0.75 * TvChannelPlan.WIDTH_MHZ;
		double inSum = 0;
		int inN = 0;
		float[] outside = new float[n];
		int outN = 0;
		for (int i = 0; i < n; i++)
		{
			float db = dbfs[i];
			if (!Float.isFinite(db) || db <= SpectrumPowerScale.EMPTY_CEILING)
				continue;
			double d = Math.abs(mhz[i] - centerMHz);
			if (d < 2.5 * binMHz)
				continue;
			float p = (float) Math.pow(10.0, db / 10.0);
			if (!Float.isFinite(p))
				continue;
			if (d <= halfFlatMHz)
			{
				inSum += p;
				inN++;
			}
			else if (d >= skirtMHz)
			{
				outside[outN++] = p;
			}
		}
		if (inN < 4 || outN < 16)
			return Float.NaN;
		Arrays.sort(outside, 0, outN);
		double noise = outside[outN / 4] * Math.pow(10.0, 0.055);
		if (!(noise > 0))
			return Float.NaN;
		double sig = inSum / inN;
		return (float) (10.0 * Math.log10(Math.max(sig / noise - 1.0, 1e-3)));
	}

	/**
	 * Strongest smoothed bin outside the channel skirts (±4.5 MHz) minus
	 * the strongest bin inside the 6 MHz channel (±3.0 MHz). Positive
	 * values >= 15 dB indicate a strong adjacent interferer in the capture.
	 */
	public static float interfererExcessDb(float[] mhz, float[] dbfs, TvChannel channel)
	{
		if (mhz == null || dbfs == null || channel == null || mhz.length < 64 || mhz.length != dbfs.length)
			return Float.NaN;
		int n = mhz.length;
		double spanMHz = mhz[n - 1] - mhz[0];
		if (!(spanMHz > 0))
			return Float.NaN;
		double binMHz = spanMHz / (n - 1);
		double centerMHz = channel.centerMHz();
		double halfChanMHz = 0.50 * TvChannelPlan.WIDTH_MHZ;
		double skirtMHz = 0.75 * TvChannelPlan.WIDTH_MHZ;
		float inPk = Float.NEGATIVE_INFINITY;
		float outPk = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < n; i++)
		{
			float db = dbfs[i];
			if (!Float.isFinite(db) || db <= SpectrumPowerScale.EMPTY_CEILING)
				continue;
			double d = Math.abs(mhz[i] - centerMHz);
			if (d < 2.5 * binMHz)
				continue;
			if (d <= halfChanMHz)
				inPk = Math.max(inPk, db);
			else if (d > skirtMHz)
				outPk = Math.max(outPk, db);
		}
		if (!Float.isFinite(inPk) || !Float.isFinite(outPk))
			return Float.NaN;
		return outPk - inPk;
	}

	public static final class GainPoint
	{
		public final int gainDb;
		public final float eqSnrDb;
		public final float spectrumSnrDb;
		public final float rmsIq;
		public final float clipFraction;

		public GainPoint(int gainDb, float eqSnrDb, float spectrumSnrDb, float rmsIq, float clipFraction)
		{
			this.gainDb = GainPolicy.clampTotal(gainDb);
			this.eqSnrDb = eqSnrDb;
			this.spectrumSnrDb = spectrumSnrDb;
			this.rmsIq = rmsIq;
			this.clipFraction = clipFraction;
		}

		public boolean clipped()
		{
			return isClipped(rmsIq, clipFraction);
		}

		public boolean hasEqSnr()
		{
			return Float.isFinite(eqSnrDb) && eqSnrDb >= MIN_USABLE_SNR_DB;
		}

		public boolean hasSpectrumSnr()
		{
			return Float.isFinite(spectrumSnrDb) && spectrumSnrDb >= MIN_USABLE_SNR_DB;
		}
	}

	/**
	 * Choose the best gain step from a sweep (OnAir {@code GainOptimiser::choose}):
	 * exclude clipped steps; prefer decoder {@code eqSnrDb}, else
	 * {@code spectrumSnrDb}, else RMS closest to {@link #TARGET_RMS}. Steps
	 * within {@link #SNR_TOL_DB} of the peak tie, and the lowest gain wins.
	 */
	public static int chooseOptimalGain(List<GainPoint> points, int fallbackGain)
	{
		if (points == null || points.isEmpty())
			return GainPolicy.clampTotal(fallbackGain);
		GainPoint lowest = null;
		List<GainPoint> safe = new ArrayList<>();
		for (GainPoint p : points)
		{
			if (p == null)
				continue;
			if (lowest == null || p.gainDb < lowest.gainDb)
				lowest = p;
			if (!p.clipped())
				safe.add(p);
		}
		if (lowest == null)
			return GainPolicy.clampTotal(fallbackGain);
		if (safe.isEmpty())
			return lowest.gainDb;

		float bestEq = Float.NEGATIVE_INFINITY;
		for (GainPoint p : safe)
		{
			if (p.hasEqSnr() && p.eqSnrDb > bestEq)
				bestEq = p.eqSnrDb;
		}
		if (Float.isFinite(bestEq))
		{
			float lim = bestEq - SNR_TOL_DB;
			int win = Integer.MAX_VALUE;
			for (GainPoint p : safe)
			{
				if (p.hasEqSnr() && p.eqSnrDb >= lim && p.gainDb < win)
					win = p.gainDb;
			}
			if (win != Integer.MAX_VALUE)
				return win;
		}

		float bestSpec = Float.NEGATIVE_INFINITY;
		for (GainPoint p : safe)
		{
			if (p.hasSpectrumSnr() && p.spectrumSnrDb > bestSpec)
				bestSpec = p.spectrumSnrDb;
		}
		if (Float.isFinite(bestSpec))
		{
			float lim = bestSpec - SNR_TOL_DB;
			int win = Integer.MAX_VALUE;
			for (GainPoint p : safe)
			{
				if (p.hasSpectrumSnr() && p.spectrumSnrDb >= lim && p.gainDb < win)
					win = p.gainDb;
			}
			if (win != Integer.MAX_VALUE)
				return win;
		}

		GainPoint bestRms = safe.get(0);
		for (GainPoint p : safe)
		{
			float da = Math.abs(p.rmsIq - TARGET_RMS);
			float db = Math.abs(bestRms.rmsIq - TARGET_RMS);
			if (da < db - 1e-6f || (Math.abs(da - db) <= 1e-6f && p.gainDb < bestRms.gainDb))
				bestRms = p;
		}
		return bestRms.gainDb;
	}
}
