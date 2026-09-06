package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Tune / Seek on the 40-channel CB raster. Seek jumps live hits.
 */
public final class CbStationDial
{
	private CbStationDial()
	{
	}

	public static CbChannel tune(int current, int direction)
	{
		int dir = direction < 0 ? -1 : 1;
		CbChannel ch = CbChannelPlan.clamp(current);
		int next = ch.number + dir;
		if (next < CbChannelPlan.FIRST_CHANNEL)
			next = CbChannelPlan.LAST_CHANNEL;
		if (next > CbChannelPlan.LAST_CHANNEL)
			next = CbChannelPlan.FIRST_CHANNEL;
		return CbChannelPlan.find(next);
	}

	public static CbChannel seek(List<CbChannelHit> hits, int current, int direction)
	{
		int dir = direction < 0 ? -1 : 1;
		List<CbChannel> live = uniqueSorted(hits);
		if (live.isEmpty())
			return tune(current, dir);
		int idx = -1;
		for (int i = 0; i < live.size(); i++)
		{
			if (live.get(i).number == current)
			{
				idx = i;
				break;
			}
		}
		if (idx >= 0)
			return live.get(wrap(idx + dir, live.size()));
		if (dir > 0)
		{
			for (CbChannel ch : live)
			{
				if (ch.number > current)
					return ch;
			}
			return live.get(0);
		}
		for (int i = live.size() - 1; i >= 0; i--)
		{
			if (live.get(i).number < current)
				return live.get(i);
		}
		return live.get(live.size() - 1);
	}

	public static boolean sameChannels(List<CbChannelHit> a, List<CbChannelHit> b)
	{
		List<CbChannel> aa = uniqueSorted(a);
		List<CbChannel> bb = uniqueSorted(b);
		if (aa.size() != bb.size())
			return false;
		for (int i = 0; i < aa.size(); i++)
		{
			if (aa.get(i).number != bb.get(i).number)
				return false;
		}
		return true;
	}

	static List<CbChannel> uniqueSorted(List<CbChannelHit> hits)
	{
		List<CbChannel> out = new ArrayList<>();
		if (hits == null)
			return out;
		boolean[] seen = new boolean[CbChannelPlan.LAST_CHANNEL + 1];
		for (CbChannelHit hit : hits)
		{
			if (hit == null || hit.channel == null)
				continue;
			int n = hit.channel.number;
			if (n < 1 || n > CbChannelPlan.LAST_CHANNEL || seen[n])
				continue;
			seen[n] = true;
			out.add(hit.channel);
		}
		out.sort((x, y) -> Integer.compare(x.number, y.number));
		return out;
	}

	private static int wrap(int i, int n)
	{
		int m = i % n;
		return m < 0 ? m + n : m;
	}
}
