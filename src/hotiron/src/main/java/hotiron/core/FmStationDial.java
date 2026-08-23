package hotiron.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Digital detents for the FM tuner knob: jump to the next higher or lower
 * <em>detected</em> station. Empty detections fall back to the 200 kHz raster.
 */
public final class FmStationDial
{
	private FmStationDial()
	{
	}

	/**
	 * Seek: next <em>detected</em> station. {@code direction} &gt; 0 is higher
	 * MHz. Empty detections fall back to one raster step.
	 */
	public static FmChannel seek(List<FmStationHit> hits, int currentKHz, int direction)
	{
		int dir = direction < 0 ? -1 : 1;
		List<FmChannel> stations = uniqueSorted(hits);
		if (stations.isEmpty())
			return tune(currentKHz, dir);
		int idx = indexOf(stations, currentKHz);
		if (idx >= 0)
			return stations.get(wrap(idx + dir, stations.size()));
		if (dir > 0)
		{
			for (int i = 0; i < stations.size(); i++)
			{
				if (stations.get(i).centerKHz > currentKHz)
					return stations.get(i);
			}
			return stations.get(0);
		}
		for (int i = stations.size() - 1; i >= 0; i--)
		{
			if (stations.get(i).centerKHz < currentKHz)
				return stations.get(i);
		}
		return stations.get(stations.size() - 1);
	}

	/** Tune: one US 200 kHz channel, wrapping 88.1 ↔ 107.9. */
	public static FmChannel tune(int currentKHz, int direction)
	{
		return stepRaster(currentKHz, direction < 0 ? -1 : 1);
	}

	/**
	 * Keep remembered stations outside the live IQ window; replace the
	 * in-window list with what the parked FFT just saw.
	 */
	public static List<FmStationHit> mergeLive(List<FmStationHit> remembered, List<FmStationHit> live,
			double liveStartMHz, double liveEndMHz)
	{
		LinkedHashMap<Integer, FmStationHit> byFcc = new LinkedHashMap<Integer, FmStationHit>();
		if (remembered != null)
		{
			for (FmStationHit hit : remembered)
			{
				if (hit == null || hit.channel == null)
					continue;
				if (!hit.channel.occupancyOverlaps(liveStartMHz, liveEndMHz))
					byFcc.put(hit.channel.fccChannel, hit);
			}
		}
		if (live != null)
		{
			for (FmStationHit hit : live)
			{
				if (hit == null || hit.channel == null)
					continue;
				byFcc.put(hit.channel.fccChannel, hit);
			}
		}
		List<FmStationHit> out = new ArrayList<FmStationHit>(byFcc.values());
		out.sort((a, b) -> Integer.compare(a.channel.centerKHz, b.channel.centerKHz));
		return List.copyOf(out);
	}

	public static boolean sameChannels(List<FmStationHit> a, List<FmStationHit> b)
	{
		List<FmChannel> aa = uniqueSorted(a);
		List<FmChannel> bb = uniqueSorted(b);
		if (aa.size() != bb.size())
			return false;
		for (int i = 0; i < aa.size(); i++)
		{
			if (aa.get(i).centerKHz != bb.get(i).centerKHz)
				return false;
		}
		return true;
	}

	static List<FmChannel> uniqueSorted(List<FmStationHit> hits)
	{
		LinkedHashSet<Integer> seen = new LinkedHashSet<Integer>();
		List<FmChannel> out = new ArrayList<FmChannel>();
		if (hits == null)
			return out;
		for (FmStationHit hit : hits)
		{
			if (hit == null || hit.channel == null)
				continue;
			if (!seen.add(hit.channel.centerKHz))
				continue;
			out.add(hit.channel);
		}
		out.sort((a, b) -> Integer.compare(a.centerKHz, b.centerKHz));
		return out;
	}

	private static FmChannel stepRaster(int currentKHz, int dir)
	{
		FmChannel cur = FmChannelPlan.clamp(currentKHz / 1000.0);
		int idx = cur.fccChannel - FmChannelPlan.FIRST_FCC_CHANNEL;
		return FmChannelPlan.CHANNELS.get(wrap(idx + dir, FmChannelPlan.CHANNELS.size()));
	}

	private static int indexOf(List<FmChannel> stations, int kHz)
	{
		for (int i = 0; i < stations.size(); i++)
		{
			if (stations.get(i).centerKHz == kHz)
				return i;
		}
		return -1;
	}

	private static int wrap(int idx, int n)
	{
		if (n <= 0)
			return 0;
		int m = idx % n;
		return m < 0 ? m + n : m;
	}
}
