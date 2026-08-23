package hotiron.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Tune is one FCC TV channel (skipping plan gaps). Seek is the next
 * occupied 6 MHz brick.
 */
public final class TvStationDial
{
	private TvStationDial()
	{
	}

	public static TvChannel tune(int fccChannel, int direction)
	{
		return TvChannelPlan.tune(fccChannel, direction);
	}

	public static TvChannel seek(List<TvStationHit> hits, int fccChannel, int direction)
	{
		int dir = direction < 0 ? -1 : 1;
		List<TvChannel> stations = uniqueSorted(hits);
		if (stations.isEmpty())
			return tune(fccChannel, dir);
		int idx = indexOf(stations, fccChannel);
		if (idx >= 0)
			return stations.get(wrap(idx + dir, stations.size()));
		if (dir > 0)
		{
			for (int i = 0; i < stations.size(); i++)
			{
				if (stations.get(i).fccChannel > fccChannel)
					return stations.get(i);
			}
			return stations.get(0);
		}
		for (int i = stations.size() - 1; i >= 0; i--)
		{
			if (stations.get(i).fccChannel < fccChannel)
				return stations.get(i);
		}
		return stations.get(stations.size() - 1);
	}

	/**
	 * Keep remembered stations outside the live IQ window; replace the
	 * in-window list with what the parked FFT just saw.
	 */
	public static List<TvStationHit> mergeLive(List<TvStationHit> remembered, List<TvStationHit> live,
			double liveStartMHz, double liveEndMHz)
	{
		LinkedHashMap<Integer, TvStationHit> byFcc = new LinkedHashMap<Integer, TvStationHit>();
		if (remembered != null)
		{
			for (TvStationHit hit : remembered)
			{
				if (hit == null || hit.channel == null)
					continue;
				if (!hit.channel.occupancyOverlaps(liveStartMHz, liveEndMHz))
					byFcc.put(hit.channel.fccChannel, hit);
			}
		}
		if (live != null)
		{
			for (TvStationHit hit : live)
			{
				if (hit == null || hit.channel == null)
					continue;
				byFcc.put(hit.channel.fccChannel, hit);
			}
		}
		List<TvStationHit> out = new ArrayList<TvStationHit>(byFcc.values());
		out.sort((a, b) -> Integer.compare(a.channel.fccChannel, b.channel.fccChannel));
		return List.copyOf(out);
	}

	public static boolean sameChannels(List<TvStationHit> a, List<TvStationHit> b)
	{
		List<TvChannel> aa = uniqueSorted(a);
		List<TvChannel> bb = uniqueSorted(b);
		if (aa.size() != bb.size())
			return false;
		for (int i = 0; i < aa.size(); i++)
		{
			if (aa.get(i).fccChannel != bb.get(i).fccChannel)
				return false;
		}
		return true;
	}

	static List<TvChannel> uniqueSorted(List<TvStationHit> hits)
	{
		List<TvChannel> out = new ArrayList<TvChannel>();
		if (hits == null)
			return out;
		for (TvStationHit hit : hits)
		{
			if (hit == null || hit.channel == null)
				continue;
			boolean seen = false;
			for (TvChannel c : out)
			{
				if (c.fccChannel == hit.channel.fccChannel)
				{
					seen = true;
					break;
				}
			}
			if (!seen)
				out.add(hit.channel);
		}
		out.sort((a, b) -> Integer.compare(a.fccChannel, b.fccChannel));
		return out;
	}

	private static int indexOf(List<TvChannel> stations, int fcc)
	{
		for (int i = 0; i < stations.size(); i++)
		{
			if (stations.get(i).fccChannel == fcc)
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
