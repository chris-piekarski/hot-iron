package hotiron.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * US FM broadcast channels from 47 CFR 73.201: 88–108 MHz, 100 channels
 * of 200 kHz, centers 88.1–107.9. Channel 200 (87.9) is not listed.
 */
public final class FmChannelPlan
{
	public static final int FIRST_FCC_CHANNEL = 201;
	public static final int LAST_FCC_CHANNEL = 300;
	public static final int FIRST_CENTER_KHZ = 88100;
	public static final int LAST_CENTER_KHZ = 107900;
	public static final int STEP_KHZ = 200;
	public static final int HALF_KHZ = 100;
	public static final int VIEW_START_MHZ = 88;
	public static final int VIEW_END_MHZ = 108;
	/** Peak must beat the 20th-percentile noise by this many dB. */
	public static final float DETECT_MARGIN_DB = 8f;
	/** Already-seen stations stay if they remain this far above noise. */
	public static final float DETECT_HOLD_DB = 3f;
	public static final float NOISE_PERCENTILE = 0.20f;

	public static final List<FmChannel> CHANNELS;

	static
	{
		List<FmChannel> list = new ArrayList<>();
		for (int ch = FIRST_FCC_CHANNEL; ch <= LAST_FCC_CHANNEL; ch++)
		{
			int kHz = FIRST_CENTER_KHZ + STEP_KHZ * (ch - FIRST_FCC_CHANNEL);
			list.add(new FmChannel(ch, kHz));
		}
		CHANNELS = Collections.unmodifiableList(list);
	}

	private FmChannelPlan()
	{
	}

	public static List<FmChannel> visibleOccupancy(double startMHz, double endMHz)
	{
		if (endMHz < startMHz)
			return Collections.emptyList();
		List<FmChannel> out = new ArrayList<>();
		for (FmChannel ch : CHANNELS)
		{
			if (ch.occupancyOverlaps(startMHz, endMHz))
				out.add(ch);
		}
		return out;
	}

	public static FmChannel findByFccChannel(int fccChannel)
	{
		if (fccChannel < FIRST_FCC_CHANNEL || fccChannel > LAST_FCC_CHANNEL)
			return null;
		return CHANNELS.get(fccChannel - FIRST_FCC_CHANNEL);
	}

	public static FmChannel findByCenterKHz(int centerKHz)
	{
		if (centerKHz < FIRST_CENTER_KHZ || centerKHz > LAST_CENTER_KHZ)
			return null;
		int rem = (centerKHz - FIRST_CENTER_KHZ) % STEP_KHZ;
		if (rem != 0)
			return null;
		return CHANNELS.get((centerKHz - FIRST_CENTER_KHZ) / STEP_KHZ);
	}

	/**
	 * Snap {@code mhz} to the nearest US FM center, or {@code null} if it
	 * is more than half a channel off the raster (or outside 88.1–107.9).
	 */
	public static FmChannel nearest(double mhz)
	{
		int kHz = (int) Math.round(mhz * 1000.0);
		int idx = (int) Math.round((kHz - FIRST_CENTER_KHZ) / (double) STEP_KHZ);
		if (idx < 0 || idx >= CHANNELS.size())
			return null;
		FmChannel ch = CHANNELS.get(idx);
		if (Math.abs(ch.centerKHz - kHz) > HALF_KHZ)
			return null;
		return ch;
	}

	/** Nearest in-band dial, clamping to 88.1 or 107.9. */
	public static FmChannel clamp(double mhz)
	{
		FmChannel exact = nearest(mhz);
		if (exact != null)
			return exact;
		int kHz = (int) Math.round(mhz * 1000.0);
		if (kHz <= FIRST_CENTER_KHZ)
			return CHANNELS.get(0);
		if (kHz >= LAST_CENTER_KHZ)
			return CHANNELS.get(CHANNELS.size() - 1);
		int idx = (int) Math.round((kHz - FIRST_CENTER_KHZ) / (double) STEP_KHZ);
		if (idx < 0)
			idx = 0;
		if (idx >= CHANNELS.size())
			idx = CHANNELS.size() - 1;
		return CHANNELS.get(idx);
	}

	public static boolean overlapsBroadcast(double startMHz, double endMHz)
	{
		return startMHz < VIEW_END_MHZ && endMHz > VIEW_START_MHZ;
	}

	/**
	 * Live stations in {@code ds}: local maxima at least
	 * {@link #DETECT_MARGIN_DB} above the noise floor, snapped to the
	 * 200 kHz dial. {@code previous} hits are kept if they still clear
	 * {@code margin − hold} so labels do not flicker.
	 */
	public static List<FmStationHit> detectStations(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		return detectStations(ds, startMHz, endMHz, DETECT_MARGIN_DB, DETECT_HOLD_DB, List.of());
	}

	/**
	 * Same peak test on a parked-IQ row (absolute MHz vs dBFS).
	 */
	public static List<FmStationHit> detectStations(float[] mhz, float[] dbfs)
	{
		if (mhz == null || dbfs == null || mhz.length == 0 || mhz.length != dbfs.length)
			return List.of();
		int n = mhz.length;
		float[] sorted = dbfs.clone();
		Arrays.sort(sorted);
		float noise = sorted[Math.min(n - 1, Math.max(0, (int) Math.floor(NOISE_PERCENTILE * (n - 1))))];
		if (!Float.isFinite(noise))
			return List.of();
		float freshThresh = noise + DETECT_MARGIN_DB;
		Map<Integer, FmStationHit> byFcc = new LinkedHashMap<>();
		for (int i = 0; i < n; i++)
		{
			float p = dbfs[i];
			if (p < freshThresh)
				continue;
			if (i > 0 && p < dbfs[i - 1])
				continue;
			if (i + 1 < n && p <= dbfs[i + 1])
				continue;
			FmChannel ch = nearest(mhz[i]);
			if (ch == null)
				continue;
			FmStationHit existing = byFcc.get(ch.fccChannel);
			if (existing == null || p > existing.powerDbm)
				byFcc.put(ch.fccChannel, new FmStationHit(ch, p));
		}
		List<FmStationHit> out = new ArrayList<>(byFcc.values());
		out.sort((a, b) -> Double.compare(a.channel.centerMHz(), b.channel.centerMHz()));
		return Collections.unmodifiableList(out);
	}

	public static List<FmStationHit> detectStations(DatasetSpectrum ds, double startMHz, double endMHz,
			float marginDb, float holdDb, List<FmStationHit> previous)
	{
		if (ds == null || endMHz < startMHz)
			return List.of();
		int n = ds.spectrumLength();
		if (n == 0)
			return List.of();
		long loHz = Math.round(startMHz * 1_000_000d);
		long hiHz = Math.round(endMHz * 1_000_000d);
		float noise = percentileInRange(ds, loHz, hiHz, NOISE_PERCENTILE);
		if (!Float.isFinite(noise))
			return List.of();
		float freshThresh = noise + marginDb;
		float holdThresh = noise + Math.max(0f, marginDb - holdDb);

		Map<Integer, FmStationHit> byFcc = new LinkedHashMap<>();
		if (previous != null)
		{
			for (FmStationHit old : previous)
			{
				if (old == null || old.channel == null || !old.channel.occupancyOverlaps(startMHz, endMHz))
					continue;
				float p = maxPowerInChannel(ds, old.channel);
				if (p >= holdThresh)
					byFcc.put(old.channel.fccChannel, new FmStationHit(old.channel, p));
			}
		}

		for (int i = 0; i < n; i++)
		{
			double fHz = ds.getFrequency(i);
			if (fHz < loHz || fHz > hiHz)
				continue;
			float p = ds.getPower(i);
			if (p < freshThresh)
				continue;
			if (i > 0 && p < ds.getPower(i - 1))
				continue;
			if (i + 1 < n && p <= ds.getPower(i + 1))
				continue;
			FmChannel ch = nearest(fHz / 1_000_000d);
			if (ch == null)
				continue;
			FmStationHit existing = byFcc.get(ch.fccChannel);
			if (existing == null || p > existing.powerDbm)
				byFcc.put(ch.fccChannel, new FmStationHit(ch, p));
		}

		List<FmStationHit> out = new ArrayList<>(byFcc.values());
		out.sort((a, b) -> Double.compare(a.channel.centerMHz(), b.channel.centerMHz()));
		return Collections.unmodifiableList(out);
	}

	static float maxPowerInChannel(DatasetSpectrum ds, FmChannel ch)
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

	static float percentileInRange(DatasetSpectrum ds, long loHz, long hiHz, float percentile)
	{
		int n = ds.spectrumLength();
		float[] buf = new float[n];
		int c = 0;
		for (int i = 0; i < n; i++)
		{
			double fHz = ds.getFrequency(i);
			if (fHz < loHz || fHz > hiHz)
				continue;
			buf[c++] = ds.getPower(i);
		}
		if (c == 0)
			return Float.NaN;
		Arrays.sort(buf, 0, c);
		int idx = (int) Math.floor(Math.max(0f, Math.min(1f, percentile)) * (c - 1));
		return buf[idx];
	}
}
