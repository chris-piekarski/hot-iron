package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * After occupancy Scan, park Watch on the strongest UHF ATSC-like bricks
 * until MPEG-2 frames appear or the dwell expires. Exclusive USB — same
 * {@code startWatch} path as the Watch button.
 */
public final class TvQualifySession
{
	/** Long enough to pass polarity retry and a first RS window. */
	public static final int DWELL_MS = 20_000;
	public static final int MAX_UHF = 6;
	public static final int MIN_WATCH_MS_FOR_NO_LOCK = 8_000;

	private final List<Integer> fccs;
	private int index;
	private long channelStartedMs;
	private boolean run;

	public TvQualifySession(List<Integer> fccs)
	{
		this.fccs = fccs == null ? List.of() : List.copyOf(fccs);
	}

	/**
	 * UHF {@link TvChannelGrade#ATSC_LIKE} hits, strongest first, skipping
	 * picture / no-lock memory. Empty when Scan found nothing to prove.
	 */
	public static List<Integer> queue(List<TvStationHit> hits)
	{
		List<TvStationHit> cand = new ArrayList<>();
		if (hits != null)
		{
			for (TvStationHit hit : hits)
			{
				if (hit == null || hit.channel == null || hit.channel.vhf())
					continue;
				if (hit.grade != TvChannelGrade.ATSC_LIKE)
					continue;
				cand.add(hit);
			}
		}
		cand.sort((a, b) -> Float.compare(b.powerDbm, a.powerDbm));
		List<Integer> out = new ArrayList<>();
		for (TvStationHit hit : cand)
		{
			if (out.size() >= MAX_UHF)
				break;
			out.add(Integer.valueOf(hit.channel.fccChannel));
		}
		return List.copyOf(out);
	}

	public void start(long nowMs)
	{
		index = 0;
		channelStartedMs = nowMs;
		run = !fccs.isEmpty();
	}

	public void cancel()
	{
		run = false;
	}

	public boolean active()
	{
		return run && index < fccs.size();
	}

	public int currentFcc()
	{
		if (!active())
			return 0;
		return fccs.get(index).intValue();
	}

	public int remaining()
	{
		if (!active())
			return 0;
		return fccs.size() - index;
	}

	public boolean shouldAdvance(long nowMs, int frames)
	{
		if (!active())
			return false;
		if (frames > 0)
			return true;
		return nowMs - channelStartedMs >= DWELL_MS;
	}

	/**
	 * @return true if another channel is queued
	 */
	public boolean advance(long nowMs)
	{
		if (!run)
			return false;
		index++;
		if (index >= fccs.size())
		{
			run = false;
			return false;
		}
		channelStartedMs = nowMs;
		return true;
	}

	public List<Integer> fccs()
	{
		return fccs;
	}
}
