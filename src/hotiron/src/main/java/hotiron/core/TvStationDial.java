package hotiron.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Tune is one FCC TV channel (skipping plan gaps). Seek prefers
 * {@link TvChannelGrade#PICTURE}, then ATSC-like occupancy.
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
		List<TvChannel> stations = seekOrder(hits);
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
	 * in-window occupancy with the parked FFT, without demoting
	 * picture / no-lock memory.
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
				if (!hit.channel.occupancyOverlaps(liveStartMHz, liveEndMHz) || hit.grade.watchMemory())
					byFcc.put(hit.channel.fccChannel, hit);
			}
		}
		if (live != null)
		{
			for (TvStationHit hit : live)
			{
				if (hit == null || hit.channel == null)
					continue;
				int id = hit.channel.fccChannel;
				TvStationHit prev = byFcc.get(id);
				byFcc.put(id, prev == null ? hit : TvStationHit.merge(prev, hit));
			}
		}
		List<TvStationHit> out = new ArrayList<TvStationHit>(byFcc.values());
		out.sort((a, b) -> Integer.compare(a.channel.fccChannel, b.channel.fccChannel));
		return List.copyOf(out);
	}

	public static List<TvStationHit> keepWatchMemory(List<TvStationHit> hits)
	{
		List<TvStationHit> out = new ArrayList<>();
		if (hits == null)
			return List.of();
		for (TvStationHit hit : hits)
		{
			if (hit != null && hit.channel != null && hit.grade.watchMemory())
				out.add(hit);
		}
		return List.copyOf(out);
	}

	public static List<TvStationHit> stamp(List<TvStationHit> hits, int fccChannel, TvChannelGrade grade,
			String stage, int frames, float snrDb)
	{
		TvChannel ch = TvChannelPlan.findByFccChannel(fccChannel);
		if (ch == null)
			return hits == null ? List.of() : hits;
		LinkedHashMap<Integer, TvStationHit> byFcc = new LinkedHashMap<Integer, TvStationHit>();
		if (hits != null)
		{
			for (TvStationHit hit : hits)
			{
				if (hit == null || hit.channel == null)
					continue;
				byFcc.put(hit.channel.fccChannel, hit);
			}
		}
		TvStationHit prev = byFcc.get(fccChannel);
		if (prev == null)
			prev = new TvStationHit(ch, Float.NaN, 1f, TvChannelGrade.OCCUPIED, "", 0, Float.NaN,
					Float.NaN);
		byFcc.put(fccChannel, prev.stamp(grade, stage, frames, snrDb));
		List<TvStationHit> out = new ArrayList<TvStationHit>(byFcc.values());
		out.sort((a, b) -> Integer.compare(a.channel.fccChannel, b.channel.fccChannel));
		return List.copyOf(out);
	}

	public static boolean sameChannels(List<TvStationHit> a, List<TvStationHit> b)
	{
		List<TvStationHit> aa = uniqueHits(a);
		List<TvStationHit> bb = uniqueHits(b);
		if (aa.size() != bb.size())
			return false;
		for (int i = 0; i < aa.size(); i++)
		{
			TvStationHit x = aa.get(i);
			TvStationHit y = bb.get(i);
			if (x.channel.fccChannel != y.channel.fccChannel || x.grade != y.grade || x.frames != y.frames)
				return false;
			if (!x.stage.equals(y.stage))
				return false;
		}
		return true;
	}

	public static int pictureCount(List<TvStationHit> hits)
	{
		int n = 0;
		if (hits == null)
			return 0;
		for (TvStationHit hit : hits)
		{
			if (hit != null && hit.grade == TvChannelGrade.PICTURE)
				n++;
		}
		return n;
	}

	static List<TvChannel> uniqueSorted(List<TvStationHit> hits)
	{
		List<TvChannel> out = new ArrayList<TvChannel>();
		for (TvStationHit hit : uniqueHits(hits))
			out.add(hit.channel);
		return out;
	}

	static List<TvChannel> seekOrder(List<TvStationHit> hits)
	{
		List<TvStationHit> usable = new ArrayList<>();
		if (hits != null)
		{
			for (TvStationHit hit : uniqueHits(hits))
			{
				if (hit.grade != TvChannelGrade.NO_LOCK)
					usable.add(hit);
			}
		}
		if (usable.isEmpty())
			usable.addAll(uniqueHits(hits));
		usable.sort((a, b) -> {
			int g = Integer.compare(a.grade.seekRank(), b.grade.seekRank());
			if (g != 0)
				return g;
			return Integer.compare(a.channel.fccChannel, b.channel.fccChannel);
		});
		List<TvChannel> out = new ArrayList<TvChannel>(usable.size());
		for (TvStationHit hit : usable)
			out.add(hit.channel);
		return out;
	}

	static List<TvStationHit> uniqueHits(List<TvStationHit> hits)
	{
		LinkedHashMap<Integer, TvStationHit> byFcc = new LinkedHashMap<>();
		if (hits != null)
		{
			for (TvStationHit hit : hits)
			{
				if (hit == null || hit.channel == null)
					continue;
				byFcc.putIfAbsent(hit.channel.fccChannel, hit);
			}
		}
		List<TvStationHit> out = new ArrayList<TvStationHit>(byFcc.values());
		out.sort((a, b) -> Integer.compare(a.channel.fccChannel, b.channel.fccChannel));
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
