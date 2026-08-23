package hotiron.core;

/**
 * One full-sweep tick: axis, MCP-rate detect/publish, then (at chart FPS)
 * detect again, scan, and paint. Scan and AGC stay paint-gated — that is
 * current HotIron behavior; do not run them on every native sweep.
 */
public final class SweepLiveLoop
{
	public interface Hooks
	{
		void onAxisChanged(DatasetSpectrum ds);

		void onPaint(DatasetSpectrumPeak ds, FrequencyRange view, long nowMs, int frame);

		double sweepsPerSec();

		void clearWaterfall();

		void retune(FrequencyRange next);

		void finishScan();

		Hooks NOOP = new Hooks()
		{
			@Override
			public void onAxisChanged(DatasetSpectrum ds)
			{
			}

			@Override
			public void onPaint(DatasetSpectrumPeak ds, FrequencyRange view, long nowMs, int frame)
			{
			}

			@Override
			public double sweepsPerSec()
			{
				return 0;
			}

			@Override
			public void clearWaterfall()
			{
			}

			@Override
			public void retune(FrequencyRange next)
			{
			}

			@Override
			public void finishScan()
			{
			}
		};
	}

	public interface Publish
	{
		boolean shouldPublish(long nowMs);

		void publish(DatasetSpectrum ds, java.util.List<FmStationHit> fmHits, double sweepsPerSec, long nowMs);

		Publish NOOP = new Publish()
		{
			@Override
			public boolean shouldPublish(long nowMs)
			{
				return false;
			}

			@Override
			public void publish(DatasetSpectrum ds, java.util.List<FmStationHit> fmHits, double sweepsPerSec,
					long nowMs)
			{
			}
		};
	}

	private final HackRFSettings settings;
	private final StationDetectSink detect;
	private final BandScanSession scan;
	private final Publish publish;
	private final Hooks hooks;
	private DatasetSpectrum previous;
	private long lastPaintMs;
	private int paintFrame;

	public SweepLiveLoop(HackRFSettings settings, StationDetectSink detect, BandScanSession scan, Publish publish,
			Hooks hooks)
	{
		if (settings == null)
			throw new IllegalArgumentException("settings");
		this.settings = settings;
		this.detect = detect == null ? new StationDetectSink() : detect;
		this.scan = scan == null ? new BandScanSession() : scan;
		this.publish = publish == null ? Publish.NOOP : publish;
		this.hooks = hooks == null ? Hooks.NOOP : hooks;
	}

	public StationDetectSink detect()
	{
		return detect;
	}

	public int paintFrame()
	{
		return paintFrame;
	}

	public void accept(DatasetSpectrumPeak ds, FrequencyRange view, long nowMs)
	{
		if (ds == null || view == null)
			return;
		boolean axis = SweepFramePolicy.axisChanged(previous, ds);
		previous = ds;
		if (axis)
		{
			detect.onAxisChanged(ds.getFreqStartMHz(), ds.getFreqStopMHz());
			hooks.onAxisChanged(ds);
		}
		if (publish.shouldPublish(nowMs))
		{
			detect.update(ds, view, settings);
			publish.publish(ds, detect.lastFm(), hooks.sweepsPerSec(), nowMs);
		}
		if (!SweepFramePolicy.shouldPaint(lastPaintMs, nowMs))
			return;
		lastPaintMs = nowMs;
		paintFrame++;
		detect.update(ds, view, settings);
		BandScanSink.advance(scan, ds, nowMs, new BandScanSink.Effects()
		{
			@Override
			public void clearWaterfall()
			{
				hooks.clearWaterfall();
			}

			@Override
			public void retune(FrequencyRange next)
			{
				hooks.retune(next);
			}

			@Override
			public void finishScan()
			{
				hooks.finishScan();
			}
		});
		hooks.onPaint(ds, view, nowMs, paintFrame);
	}
}
