package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * US CB AM (47 CFR 95.563 / 95.2265): 40 channels, 26.965–27.405 MHz.
 * 11 m personal radio, not amateur 10 m.
 */
public final class CbChannelPlan
{
	public static final int FIRST_CHANNEL = 1;
	public static final int LAST_CHANNEL = 40;
	public static final int HIGHWAY_CHANNEL = 19;
	public static final int EMERGENCY_CHANNEL = 9;
	public static final int HALF_KHZ = 5;
	public static final int VIEW_START_MHZ = 26;
	public static final int VIEW_END_MHZ = 28;
	public static final long LO_HZ = 27_185_000L;
	public static final float DETECT_MARGIN_DB = 8f;
	public static final float SCAN_MARGIN_DB = 12f;
	public static final float DETECT_HOLD_DB = 3f;
	public static final float NOISE_PERCENTILE = 0.20f;
	public static final double MAX_CLASSIFY_SPAN_MHZ = 8;

	/** FCC channel centers in kHz, 1-indexed via list order. */
	static final int[] CENTER_KHZ = { 26965, 26975, 26985, 27005, 27015, 27025, 27035, 27055, 27065, 27075, 27085,
			27105, 27115, 27125, 27135, 27155, 27165, 27175, 27185, 27205, 27215, 27225, 27255, 27235, 27245, 27265,
			27275, 27285, 27295, 27305, 27315, 27325, 27335, 27345, 27355, 27365, 27375, 27385, 27395, 27405 };

	public static final List<CbChannel> CHANNELS;

	static
	{
		List<CbChannel> list = new ArrayList<>(CENTER_KHZ.length);
		for (int i = 0; i < CENTER_KHZ.length; i++)
			list.add(new CbChannel(i + 1, CENTER_KHZ[i]));
		CHANNELS = Collections.unmodifiableList(list);
	}

	private CbChannelPlan()
	{
	}

	public static FrequencyRange viewWindow()
	{
		return new FrequencyRange(VIEW_START_MHZ, VIEW_END_MHZ);
	}

	public static boolean overlapsBand(double startMHz, double endMHz)
	{
		return endMHz > VIEW_START_MHZ && startMHz < VIEW_END_MHZ;
	}

	public static boolean viewIsCb(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz || endMHz - startMHz > MAX_CLASSIFY_SPAN_MHZ)
			return false;
		return overlapsBand(startMHz, endMHz);
	}

	public static CbChannel find(int number)
	{
		if (number < FIRST_CHANNEL || number > LAST_CHANNEL)
			return null;
		return CHANNELS.get(number - 1);
	}

	public static CbChannel clamp(int number)
	{
		if (number < FIRST_CHANNEL)
			return CHANNELS.get(0);
		if (number > LAST_CHANNEL)
			return CHANNELS.get(CHANNELS.size() - 1);
		return CHANNELS.get(number - 1);
	}

	public static CbChannel nearest(double mhz)
	{
		int kHz = (int) Math.round(mhz * 1000.0);
		CbChannel best = null;
		int bestErr = Integer.MAX_VALUE;
		for (CbChannel ch : CHANNELS)
		{
			int err = Math.abs(ch.centerKHz - kHz);
			if (err < bestErr)
			{
				bestErr = err;
				best = ch;
			}
		}
		if (best == null || bestErr >= HALF_KHZ * 2)
			return null;
		return best;
	}

	public static String labelForPeak(double peakMhz, double viewStartMHz, double viewEndMHz)
	{
		if (!viewIsCb(viewStartMHz, viewEndMHz))
			return null;
		CbChannel ch = nearest(peakMhz);
		return ch == null ? null : "CB " + ch.number;
	}

	public static List<CbChannelHit> detect(DatasetSpectrum ds, double startMHz, double endMHz, float marginDb)
	{
		if (ds == null || !overlapsBand(startMHz, endMHz))
			return List.of();
		long loHz = Math.round(startMHz * 1_000_000d);
		long hiHz = Math.round(endMHz * 1_000_000d);
		float noise = FmChannelPlan.percentileInRange(ds, loHz, hiHz, NOISE_PERCENTILE);
		if (!Float.isFinite(noise))
			return List.of();
		float thresh = noise + (marginDb > 0f ? marginDb : DETECT_MARGIN_DB);
		List<CbChannelHit> out = new ArrayList<>();
		for (CbChannel ch : CHANNELS)
		{
			if (!ch.occupancyOverlaps(startMHz, endMHz))
				continue;
			float p = maxPower(ds, ch);
			if (Float.isFinite(p) && p >= thresh)
				out.add(new CbChannelHit(ch, p));
		}
		return Collections.unmodifiableList(out);
	}

	static float maxPower(DatasetSpectrum ds, CbChannel ch)
	{
		long loHz = Math.round(ch.lowMHz() * 1_000_000d);
		long hiHz = Math.round(ch.highMHz() * 1_000_000d);
		float max = Float.NEGATIVE_INFINITY;
		int n = ds.spectrumLength();
		for (int i = 0; i < n; i++)
		{
			double fHz = ds.getFrequency(i);
			if (fHz < loHz || fHz > hiHz)
				continue;
			float p = ds.getPower(i);
			if (p > max)
				max = p;
		}
		return max;
	}
}
