package hotiron.core;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Sweep-time FM/TV station detect. Writes {@link HackRFSettings} Seek lists
 * only when the channel set changes. Zoomed-out views leave the last hits
 * in memory (MCP {@code publishContext} hides them when the span is wide).
 */
public final class StationDetectSink
{
	private final FmStationTracker fmTracker;
	private final TvStationTracker tvTracker;
	private final NfcActivityTracker nfcTracker;
	private List<FmStationHit> fmHits = List.of();
	private List<TvStationHit> tvHits = List.of();
	private NfcActivity nfc = NfcActivity.quiet();

	public StationDetectSink()
	{
		this(System::currentTimeMillis);
	}

	public StationDetectSink(LongSupplier clockMs)
	{
		this.fmTracker = new FmStationTracker(clockMs);
		this.tvTracker = new TvStationTracker(clockMs);
		this.nfcTracker = new NfcActivityTracker(clockMs);
	}

	public List<FmStationHit> lastFm()
	{
		return fmHits;
	}

	public List<TvStationHit> lastTv()
	{
		return tvHits;
	}

	public NfcActivity lastNfc()
	{
		return nfc;
	}

	public void resetFm()
	{
		fmTracker.reset();
	}

	public void resetTv()
	{
		tvTracker.reset();
	}

	public void resetNfc()
	{
		nfcTracker.reset();
		nfc = NfcActivity.quiet();
	}

	/**
	 * Axis change on a live broadcast window drops tracker memory so a
	 * retune does not keep stale 97.3 labels.
	 */
	public void onAxisChanged(int startMHz, int endMHz)
	{
		if (FmChannelPlan.overlapsBroadcast(startMHz, endMHz))
			fmTracker.reset();
		if (TvChannelPlan.overlapsBroadcast(startMHz, endMHz))
			tvTracker.reset();
		if (NfcBandPlan.overlapsInterest(startMHz, endMHz))
			nfcTracker.reset();
	}

	public void update(DatasetSpectrum ds, FrequencyRange view, HackRFSettings settings)
	{
		if (ds == null || view == null || settings == null)
			return;
		double start = view.getStartMHz();
		double end = view.getEndMHz();
		if (FmChannelPlan.overlapsBroadcast(start, end))
		{
			fmHits = fmTracker.update(ds, start, end);
			publishFm(settings, fmHits);
		}
		if (TvChannelPlan.overlapsBroadcast(start, end))
		{
			List<TvStationHit> remembered = settings.getDetectedTvStations() != null
					? settings.getDetectedTvStations().getValue()
					: List.of();
			tvHits = TvStationDial.mergeLive(remembered, tvTracker.update(ds, start, end), start, end);
			publishTv(settings, tvHits);
		}
		nfc = nfcTracker.update(ds, start, end);
	}

	public static void publishFm(HackRFSettings settings, List<FmStationHit> hits)
	{
		if (settings == null || settings.getDetectedFmStations() == null)
			return;
		List<FmStationHit> cur = settings.getDetectedFmStations().getValue();
		if (FmStationDial.sameChannels(cur, hits))
			return;
		settings.getDetectedFmStations().setValue(hits == null ? List.of() : List.copyOf(hits));
	}

	public static void publishTv(HackRFSettings settings, List<TvStationHit> hits)
	{
		if (settings == null || settings.getDetectedTvStations() == null)
			return;
		List<TvStationHit> cur = settings.getDetectedTvStations().getValue();
		if (TvStationDial.sameChannels(cur, hits))
			return;
		settings.getDetectedTvStations().setValue(hits == null ? List.of() : List.copyOf(hits));
	}
}
