package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class GnssActivityTrackerTest
{
	@Test
	void aSingleSweepFlashIsNotLabeled()
	{
		AtomicLong now = new AtomicLong(0);
		GnssActivityTracker tracker = new GnssActivityTracker(now::get);
		DatasetSpectrum ds = GnssBandPlanTest.gnssSpectrum();
		GnssBandPlanTest.brick(ds, GnssBandPlan.GPS_L1, -70f);
		GnssActivity first = tracker.update(ds, 1559, 1610, 0);
		assertTrue(first.visible);
		assertFalse(first.gpsL1);
		assertTrue(first.confidence < GnssActivityTracker.SHOW_AT);
	}

	@Test
	void continuousBrickBecomesGpsL1()
	{
		AtomicLong now = new AtomicLong(0);
		GnssActivityTracker tracker = new GnssActivityTracker(now::get);
		DatasetSpectrum ds = GnssBandPlanTest.gnssSpectrum();
		GnssBandPlanTest.brick(ds, GnssBandPlan.GPS_L1, -70f);
		GnssActivity last = GnssActivity.quiet();
		for (int t = 0; t <= 800; t += 50)
		{
			now.set(t);
			last = tracker.update(ds, 1559, 1610, t);
		}
		assertTrue(last.gpsL1, last.summary());
		assertTrue(last.summary().contains("GPS/Galileo"));
		assertTrue(last.confidence >= GnssActivityTracker.SHOW_AT);
	}

	@Test
	void outsideL1IsHidden()
	{
		GnssActivityTracker tracker = new GnssActivityTracker();
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		assertFalse(tracker.update(ds, 88, 108).visible);
	}
}
