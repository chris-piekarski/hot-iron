package hotiron.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * Last-known GPS L1 C/A hits while sniff is parked. The first peak is
 * shown; a later miss does not blank the list.
 */
public final class GpsSvTracker
{
	private final TreeMap<Integer, GpsSvHit> byPrn = new TreeMap<>();
	private final TreeMap<Integer, Integer> dwell = new TreeMap<>();
	private List<GpsSvHit> last = List.of();

	public synchronized void reset()
	{
		byPrn.clear();
		dwell.clear();
		last = List.of();
	}

	public synchronized List<GpsSvHit> last()
	{
		return last;
	}

	/**
	 * Merge a search result. {@code null} or empty leaves the snapshot
	 * unchanged. Returns true when the held list changed.
	 */
	public synchronized boolean update(List<GpsSvHit> hits)
	{
		if (hits == null || hits.isEmpty())
			return false;
		boolean changed = false;
		for (GpsSvHit hit : hits)
		{
			if (hit == null || hit.prn < GpsCaCode.PRN_MIN || hit.prn > GpsCaCode.PRN_MAX)
				continue;
			int n = dwell.getOrDefault(hit.prn, 0) + 1;
			dwell.put(hit.prn, n);
			GpsSvHit stored = new GpsSvHit(hit.prn, hit.cn0DbHz, hit.dopplerHz, hit.codePhase, hit.peakRatio, n);
			GpsSvHit prev = byPrn.put(hit.prn, stored);
			if (prev == null || !displaySame(prev, stored))
				changed = true;
		}
		if (!changed)
			return false;
		last = snapshot();
		return true;
	}

	private List<GpsSvHit> snapshot()
	{
		List<GpsSvHit> out = new ArrayList<>(byPrn.values());
		out.sort(Comparator.comparingDouble((GpsSvHit h) -> h.cn0DbHz).reversed()
				.thenComparingInt(h -> h.prn));
		return List.copyOf(out);
	}

	static boolean displaySame(GpsSvHit a, GpsSvHit b)
	{
		return a.prn == b.prn && a.dopplerHz == b.dopplerHz && a.line().equals(b.line());
	}
}
