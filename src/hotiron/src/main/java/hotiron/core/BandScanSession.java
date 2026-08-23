package hotiron.core;

import java.util.List;
import java.util.Optional;

/**
 * Dwell each survey window long enough for the station tracker to rise
 * above {@code SHOW_AT}, then advance (TV is VHF then UHF).
 */
public final class BandScanSession
{
	public static final int DWELL_MS = 2500;

	private BandScan kind = BandScan.OFF;
	private int windowIndex;
	private long windowStartedMs;

	public void start(BandScan kind, long nowMs)
	{
		if (kind == null || kind == BandScan.OFF)
			throw new IllegalArgumentException("kind");
		this.kind = kind;
		this.windowIndex = 0;
		this.windowStartedMs = nowMs;
	}

	public void stop()
	{
		kind = BandScan.OFF;
		windowIndex = 0;
		windowStartedMs = 0;
	}

	public boolean active()
	{
		return kind != BandScan.OFF;
	}

	public BandScan kind()
	{
		return kind;
	}

	public FrequencyRange currentWindow()
	{
		List<FrequencyRange> windows = windows(kind);
		if (windows.isEmpty())
			return null;
		int i = Math.min(windowIndex, windows.size() - 1);
		return windows.get(i);
	}

	public boolean shouldFinish(long nowMs)
	{
		if (!active())
			return false;
		List<FrequencyRange> windows = windows(kind);
		return dwellElapsed(nowMs) && windowIndex + 1 >= windows.size();
	}

	public Optional<FrequencyRange> nextWindowIfDue(long nowMs)
	{
		if (!active() || !dwellElapsed(nowMs))
			return Optional.empty();
		List<FrequencyRange> windows = windows(kind);
		if (windowIndex + 1 >= windows.size())
			return Optional.empty();
		windowIndex++;
		windowStartedMs = nowMs;
		return Optional.of(windows.get(windowIndex));
	}

	public static FrequencyRange fmWindow()
	{
		return new FrequencyRange(FmChannelPlan.VIEW_START_MHZ, FmChannelPlan.VIEW_END_MHZ);
	}

	public static FrequencyRange tvVhfWindow()
	{
		return new FrequencyRange(TvChannelPlan.VHF_VIEW_START_MHZ, TvChannelPlan.VHF_VIEW_END_MHZ);
	}

	public static FrequencyRange tvUhfWindow()
	{
		return new FrequencyRange(TvChannelPlan.UHF_VIEW_START_MHZ, TvChannelPlan.UHF_VIEW_END_MHZ);
	}

	public static List<FrequencyRange> windows(BandScan kind)
	{
		if (kind == BandScan.FM)
			return List.of(fmWindow());
		if (kind == BandScan.TV)
			return List.of(tvVhfWindow(), tvUhfWindow());
		return List.of();
	}

	private boolean dwellElapsed(long nowMs)
	{
		return nowMs - windowStartedMs >= DWELL_MS;
	}
}
