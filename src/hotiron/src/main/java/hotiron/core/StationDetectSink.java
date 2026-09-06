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
	private final GnssActivityTracker gnssTracker;
	private final CbChannelTracker cbTracker;
	private List<FmStationHit> fmHits = List.of();
	private List<TvStationHit> tvHits = List.of();
	private NfcActivity nfc = NfcActivity.quiet();
	private GnssActivity gnss = GnssActivity.quiet();
	private List<CbChannelHit> cbHits = List.of();

	public StationDetectSink()
	{
		this(System::currentTimeMillis);
	}

	public StationDetectSink(LongSupplier clockMs)
	{
		this.fmTracker = new FmStationTracker(clockMs);
		this.tvTracker = new TvStationTracker(clockMs);
		this.nfcTracker = new NfcActivityTracker(clockMs);
		this.gnssTracker = new GnssActivityTracker(clockMs);
		this.cbTracker = new CbChannelTracker(clockMs);
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

	public GnssActivity lastGnss()
	{
		return gnss;
	}

	public List<CbChannelHit> lastCb()
	{
		return cbHits;
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

	public void resetGnss()
	{
		gnssTracker.reset();
		gnss = GnssActivity.quiet();
	}

	public void resetCb()
	{
		cbTracker.reset();
		cbHits = List.of();
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
		if (GnssBandPlan.overlapsL1(startMHz, endMHz))
			gnssTracker.reset();
		if (CbChannelPlan.overlapsBand(startMHz, endMHz))
			cbTracker.reset();
	}

	public void update(DatasetSpectrum ds, FrequencyRange view, HackRFSettings settings)
	{
		if (ds == null || view == null || settings == null)
			return;
		double start = view.getStartMHz();
		double end = view.getEndMHz();
		if (FmChannelPlan.overlapsBroadcast(start, end))
		{
			boolean scan = settings.getBandScan() != null && settings.getBandScan().getValue() == BandScan.FM;
			float margin = scan ? FmChannelPlan.SCAN_MARGIN_DB : FmChannelPlan.DETECT_MARGIN_DB;
			fmHits = fmTracker.update(ds, start, end, margin);
			if (scan)
				fmHits = FmStationDial.strongest(fmHits);
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
		gnss = gnssTracker.update(ds, start, end);
		if (CbChannelPlan.overlapsBand(start, end))
		{
			boolean scan = settings.getBandScan() != null && settings.getBandScan().getValue() == BandScan.CB;
			float margin = scan ? CbChannelPlan.SCAN_MARGIN_DB : CbChannelPlan.DETECT_MARGIN_DB;
			cbHits = cbTracker.update(ds, start, end, margin);
			publishCb(settings, cbHits);
		}
	}

	public static void publishCb(HackRFSettings settings, List<CbChannelHit> hits)
	{
		if (settings == null || settings.getDetectedCbStations() == null)
			return;
		List<CbChannelHit> cur = settings.getDetectedCbStations().getValue();
		if (CbStationDial.sameChannels(cur, hits))
			return;
		settings.getDetectedCbStations().setValue(hits == null ? List.of() : List.copyOf(hits));
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
