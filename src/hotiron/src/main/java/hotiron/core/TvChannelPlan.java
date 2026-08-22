package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * US TV channels 2–36 (47 CFR 73.603 / ATSC 1.0 6 MHz). Post-600 MHz
 * repack: no ch 37+. Gaps at 72–76 and 88–174 (FM + aviation).
 */
public final class TvChannelPlan
{
	public static final int FIRST_FCC_CHANNEL = 2;
	public static final int LAST_FCC_CHANNEL = 36;
	public static final int WIDTH_MHZ = 6;
	public static final double PILOT_OFFSET_MHZ = 0.31;
	/** 16 MS/s with an 8 MHz analog filter leaves 1 MHz guard around the 6 MHz ATSC brick. */
	public static final int IQ_RATE_HZ = 16_000_000;
	public static final float DETECT_MARGIN_DB = 8f;
	public static final float NOISE_PERCENTILE = 0.20f;

	public static final List<TvChannel> CHANNELS;

	static
	{
		List<TvChannel> list = new ArrayList<>();
		for (int ch = FIRST_FCC_CHANNEL; ch <= LAST_FCC_CHANNEL; ch++)
		{
			int low = lowMHzOf(ch);
			if (low > 0)
				list.add(new TvChannel(ch, low));
		}
		CHANNELS = Collections.unmodifiableList(list);
	}

	private TvChannelPlan()
	{
	}

	public static int lowMHzOf(int fccChannel)
	{
		if (fccChannel >= 2 && fccChannel <= 4)
			return 54 + WIDTH_MHZ * (fccChannel - 2);
		if (fccChannel >= 5 && fccChannel <= 6)
			return 76 + WIDTH_MHZ * (fccChannel - 5);
		if (fccChannel >= 7 && fccChannel <= 13)
			return 174 + WIDTH_MHZ * (fccChannel - 7);
		if (fccChannel >= 14 && fccChannel <= 36)
			return 470 + WIDTH_MHZ * (fccChannel - 14);
		return -1;
	}

	public static TvChannel findByFccChannel(int fccChannel)
	{
		int low = lowMHzOf(fccChannel);
		if (low < 0)
			return null;
		int idx = indexOfFcc(fccChannel);
		if (idx < 0)
			return null;
		return CHANNELS.get(idx);
	}

	public static TvChannel containingMHz(double mhz)
	{
		for (TvChannel ch : CHANNELS)
		{
			if (mhz >= ch.lowMHz && mhz < ch.highMHz())
				return ch;
		}
		return null;
	}

	/** Nearest in-plan channel, clamping to 2 or 36. */
	public static TvChannel clamp(int fccChannel)
	{
		TvChannel exact = findByFccChannel(fccChannel);
		if (exact != null)
			return exact;
		if (fccChannel <= FIRST_FCC_CHANNEL)
			return CHANNELS.get(0);
		return CHANNELS.get(CHANNELS.size() - 1);
	}

	public static TvChannel tune(int fccChannel, int direction)
	{
		int dir = direction < 0 ? -1 : 1;
		int idx = indexOfFcc(fccChannel);
		if (idx < 0)
			idx = 0;
		return CHANNELS.get(wrap(idx + dir, CHANNELS.size()));
	}

	public static List<TvChannel> visibleOccupancy(double startMHz, double endMHz)
	{
		if (endMHz < startMHz)
			return List.of();
		List<TvChannel> out = new ArrayList<>();
		for (TvChannel ch : CHANNELS)
		{
			if (ch.occupancyOverlaps(startMHz, endMHz))
				out.add(ch);
		}
		return out;
	}

	/**
	 * Occupied 6 MHz bricks: mean power in the channel beats the view
	 * noise by {@link #DETECT_MARGIN_DB}. ATSC looks like a plateau, not
	 * an FM needle.
	 */
	public static List<TvStationHit> detectStations(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		if (ds == null || endMHz < startMHz)
			return List.of();
		int n = ds.spectrumLength();
		if (n == 0)
			return List.of();
		long loHz = Math.round(startMHz * 1_000_000d);
		long hiHz = Math.round(endMHz * 1_000_000d);
		float noise = FmChannelPlan.percentileInRange(ds, loHz, hiHz, NOISE_PERCENTILE);
		if (!Float.isFinite(noise))
			return List.of();
		float thresh = noise + DETECT_MARGIN_DB;
		List<TvStationHit> out = new ArrayList<>();
		for (TvChannel ch : visibleOccupancy(startMHz, endMHz))
		{
			float mean = meanPowerInChannel(ds, ch);
			if (!Float.isFinite(mean) || mean < thresh)
				continue;
			out.add(new TvStationHit(ch, mean));
		}
		return Collections.unmodifiableList(out);
	}

	static float meanPowerInChannel(DatasetSpectrum ds, TvChannel ch)
	{
		long loHz = Math.round(ch.lowMHz * 1_000_000d);
		long hiHz = Math.round(ch.highMHz() * 1_000_000d);
		double sum = 0;
		int c = 0;
		int n = ds.spectrumLength();
		for (int i = 0; i < n; i++)
		{
			double fHz = ds.getFrequency(i);
			if (fHz < loHz || fHz >= hiHz)
				continue;
			float p = ds.getPower(i);
			if (p <= SpectrumPowerScale.EMPTY_CEILING || !Float.isFinite(p))
				continue;
			sum += p;
			c++;
		}
		if (c == 0)
			return Float.NaN;
		return (float) (sum / c);
	}

	private static int indexOfFcc(int fccChannel)
	{
		for (int i = 0; i < CHANNELS.size(); i++)
		{
			if (CHANNELS.get(i).fccChannel == fccChannel)
				return i;
		}
		return -1;
	}

	private static int wrap(int idx, int n)
	{
		int m = idx % n;
		return m < 0 ? m + n : m;
	}
}
