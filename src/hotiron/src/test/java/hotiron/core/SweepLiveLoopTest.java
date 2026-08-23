package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class SweepLiveLoopTest
{
	private static final class Rec implements SweepLiveLoop.Hooks, SweepLiveLoop.Publish
	{
		int axis;
		int paints;
		int publishes;
		int lastFrame;
		final List<Long> paintAt = new ArrayList<>();
		final List<Long> publishAt = new ArrayList<>();
		long minPublishMs = SpectrumSnapshotStoreInterval.MS;
		long lastPublish = Long.MIN_VALUE / 4;

		@Override
		public void onAxisChanged(DatasetSpectrum ds)
		{
			axis++;
		}

		@Override
		public void onPaint(DatasetSpectrumPeak ds, FrequencyRange view, long nowMs, int frame)
		{
			paints++;
			lastFrame = frame;
			paintAt.add(nowMs);
		}

		@Override
		public double sweepsPerSec()
		{
			return 12;
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

		@Override
		public boolean shouldPublish(long nowMs)
		{
			return nowMs - lastPublish >= minPublishMs;
		}

		@Override
		public void publish(DatasetSpectrum ds, List<FmStationHit> fmHits, double sweepsPerSec, long nowMs)
		{
			publishes++;
			lastPublish = nowMs;
			publishAt.add(nowMs);
			assertEquals(12, sweepsPerSec, 0.01);
		}
	}

	/** Avoid importing MCP from core tests — same 100 ms cap as the store. */
	private static final class SpectrumSnapshotStoreInterval
	{
		static final long MS = 100L;
	}

	@Test
	void firstDatasetIsAnAxisChange()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		SweepLiveLoop loop = new SweepLiveLoop(s, new StationDetectSink(), new BandScanSession(), rec, rec);
		DatasetSpectrumPeak ds = peak(20_000f, 2402, 2472);
		loop.accept(ds, new FrequencyRange(2402, 2472), 1_000);
		assertEquals(1, rec.axis);
		assertEquals(1, rec.paints);
	}

	@Test
	void gainOnlyRestartDoesNotClearAxis()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		SweepLiveLoop loop = new SweepLiveLoop(s, new StationDetectSink(), new BandScanSession(), rec, rec);
		FrequencyRange wifi = new FrequencyRange(2402, 2472);
		loop.accept(peak(20_000f, 2402, 2472), wifi, 1_000);
		loop.accept(peak(20_000f, 2402, 2472), wifi, 1_050);
		assertEquals(1, rec.axis, "same MHz/FFT must keep waterfall history");
	}

	@Test
	void publishAndPaintAreIndependentRates()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		SweepLiveLoop loop = new SweepLiveLoop(s, new StationDetectSink(), new BandScanSession(), rec, rec);
		FrequencyRange wifi = new FrequencyRange(2402, 2472);
		DatasetSpectrumPeak ds = peak(20_000f, 2402, 2472);
		loop.accept(ds, wifi, 1_000);
		assertEquals(1, rec.paints);
		assertEquals(1, rec.publishes);
		loop.accept(ds, wifi, 1_020);
		assertEquals(1, rec.paints, "20 ms is inside the chart frame");
		assertEquals(1, rec.publishes);
		loop.accept(ds, wifi, 1_040);
		assertEquals(2, rec.paints, "40 ms is a new chart frame");
		assertEquals(1, rec.publishes, "paint must not imply MCP publish");
		loop.accept(ds, wifi, 1_100);
		assertEquals(3, rec.paints);
		assertEquals(2, rec.publishes, "10 Hz publish is independent of 30 fps paint");
	}

	@Test
	void paintRunsAtChartFpsAndScanIsPaintGated()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		BandScanSession scan = new BandScanSession();
		scan.start(BandScan.FM, 0);
		SweepLiveLoop loop = new SweepLiveLoop(s, new StationDetectSink(), scan, rec, rec);
		DatasetSpectrumPeak ds = peak(100_000f, 88, 108);
		FrequencyRange fm = new FrequencyRange(88, 108);
		loop.accept(ds, fm, 1_000);
		assertTrue(scan.windowLive(), "first paint marks the scan window live");
		scan.stop();
		scan.start(BandScan.FM, 2_000);
		assertFalse(scan.windowLive());
		for (int i = 0; i < 3; i++)
			loop.accept(ds, fm, 1_010 + i);
		assertFalse(scan.windowLive(), "sub-frame sweeps must not mark live / AGC / scan");
	}

	@Test
	void paintFrameIncrementsOnlyWhenPainting()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		SweepLiveLoop loop = new SweepLiveLoop(s, new StationDetectSink(), new BandScanSession(), rec, rec);
		FrequencyRange wifi = new FrequencyRange(2402, 2472);
		DatasetSpectrumPeak ds = peak(20_000f, 2402, 2472);
		loop.accept(ds, wifi, 1_000);
		loop.accept(ds, wifi, 1_010);
		assertEquals(1, rec.lastFrame);
		loop.accept(ds, wifi, 1_000 + 1000 / SweepFramePolicy.CHART_FPS + 1);
		assertEquals(2, rec.lastFrame);
	}

	@Test
	void nullDatasetIsIgnored()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		SweepLiveLoop loop = new SweepLiveLoop(s, new StationDetectSink(), new BandScanSession(), rec, rec);
		loop.accept(null, new FrequencyRange(88, 108), 1_000);
		assertEquals(0, rec.axis);
		assertEquals(0, rec.paints);
		assertEquals(0, rec.publishes);
	}

	private static DatasetSpectrumPeak peak(float binHz, int start, int end)
	{
		return new DatasetSpectrumPeak(binHz, start, end, -150f, 15, 15_000);
	}
}
