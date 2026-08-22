package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class FmStationTrackerTest {

	@Test
	void aSingleSweepFlashIsNotShown() {
		AtomicLong now = new AtomicLong(0);
		FmStationTracker tracker = new FmStationTracker(now::get);
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		assertTrue(tracker.update(ds, 88, 108).isEmpty());
		assertTrue(tracker.confidenceOf(247) > 0f);
		assertTrue(tracker.confidenceOf(247) < FmStationTracker.SHOW_AT);
	}

	@Test
	void confidenceRisesUntilTheStationIsLabeled() {
		AtomicLong now = new AtomicLong(0);
		FmStationTracker tracker = new FmStationTracker(now::get);
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		List<FmStationHit> hits = List.of();
		for (int t = 0; t <= 600; t += 10) {
			now.set(t);
			hits = tracker.update(ds, 88, 108);
		}
		assertEquals(1, hits.size());
		assertEquals("97.3", hits.get(0).label());
		assertTrue(hits.get(0).confidence >= FmStationTracker.SHOW_AT);
	}

	@Test
	void labelHoldsAfterThePeakDropsThenDecaysAway() {
		AtomicLong now = new AtomicLong(0);
		FmStationTracker tracker = new FmStationTracker(now::get);
		DatasetSpectrum live = fmSpectrum();
		spike(live, 97.3, -40f);
		for (int t = 0; t <= 800; t += 10) {
			now.set(t);
			tracker.update(live, 88, 108);
		}
		assertEquals("97.3", tracker.update(live, 88, 108).get(0).label());

		DatasetSpectrum quiet = fmSpectrum();
		now.set(900);
		List<FmStationHit> held = tracker.update(quiet, 88, 108);
		assertEquals(1, held.size(), "must remain readable after a one-sweep dropout");
		assertEquals("97.3", held.get(0).label());

		for (int t = 900; t <= 4000; t += 50) {
			now.set(t);
			held = tracker.update(quiet, 88, 108);
		}
		assertTrue(held.isEmpty(), "confidence must decay away after ~2–3 s of silence");
		assertEquals(0f, tracker.confidenceOf(247), 0.001f);
	}

	@Test
	void resetClearsPartialConfidence() {
		AtomicLong now = new AtomicLong(0);
		FmStationTracker tracker = new FmStationTracker(now::get);
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		now.set(200);
		tracker.update(ds, 88, 108);
		assertTrue(tracker.confidenceOf(247) > 0f);
		tracker.reset();
		assertEquals(0f, tracker.confidenceOf(247), 0.001f);
		assertTrue(tracker.update(ds, 88, 108).isEmpty());
	}

	@Test
	void twoStationsCanLockIndependently() {
		AtomicLong now = new AtomicLong(0);
		FmStationTracker tracker = new FmStationTracker(now::get);
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		spike(ds, 101.1, -35f);
		List<FmStationHit> hits = List.of();
		for (int t = 0; t <= 700; t += 10) {
			now.set(t);
			hits = tracker.update(ds, 88, 108);
		}
		assertEquals(2, hits.size());
		assertEquals("97.3", hits.get(0).label());
		assertEquals("101.1", hits.get(1).label());
	}

	private static DatasetSpectrum fmSpectrum() {
		return new DatasetSpectrum(100_000f, 88, 108, -90f);
	}

	private static void spike(DatasetSpectrum ds, double mhz, float dbm) {
		double targetHz = mhz * 1_000_000d;
		int best = 0;
		double bestErr = Double.POSITIVE_INFINITY;
		for (int i = 0; i < ds.spectrumLength(); i++) {
			double err = Math.abs(ds.getFrequency(i) - targetHz);
			if (err < bestErr) {
				bestErr = err;
				best = i;
			}
		}
		ds.getSpectrumArray()[best] = dbm;
	}
}
