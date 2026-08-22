package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FmChannelPlanTest {

	@Test
	void usBandIsOneHundredChannelsFrom881To1079() {
		assertEquals(100, FmChannelPlan.CHANNELS.size());
		FmChannel first = FmChannelPlan.findByFccChannel(201);
		FmChannel last = FmChannelPlan.findByFccChannel(300);
		assertEquals(88100, first.centerKHz);
		assertEquals(107900, last.centerKHz);
		assertEquals(88.0, first.lowMHz(), 0.0001);
		assertEquals(88.2, first.highMHz(), 0.0001);
		assertEquals(107.8, last.lowMHz(), 0.0001);
		assertEquals(108.0, last.highMHz(), 0.0001);
		assertEquals(FmChannelPlan.VIEW_START_MHZ, (int) first.lowMHz());
		assertEquals(FmChannelPlan.VIEW_END_MHZ, (int) last.highMHz());
		assertNull(FmChannelPlan.findByFccChannel(200));
		assertNull(FmChannelPlan.findByFccChannel(301));
	}

	@Test
	void ninetySevenThreeIsAStationCenter() {
		FmChannel ch = FmChannelPlan.nearest(97.3);
		assertNotNull(ch);
		assertEquals(247, ch.fccChannel);
		assertEquals(97300, ch.centerKHz);
		assertEquals("97.3", ch.label());
		assertEquals(97.2, ch.lowMHz(), 0.0001);
		assertEquals(97.4, ch.highMHz(), 0.0001);
		assertSame(ch, FmChannelPlan.findByCenterKHz(97300));
		assertNull(FmChannelPlan.findByCenterKHz(97200));
	}

	@Test
	void clampPinsListenDialToTheUsRaster() {
		assertEquals(88100, FmChannelPlan.clamp(80).centerKHz);
		assertEquals(107900, FmChannelPlan.clamp(120).centerKHz);
		assertEquals(97300, FmChannelPlan.clamp(97.3).centerKHz);
		assertEquals(97300, FmChannelPlan.clamp(97.35).centerKHz);
	}

	@Test
	void nearestSnapsInsideAChannelAndRejectsOutsideTheBand() {
		assertEquals(97300, FmChannelPlan.nearest(97.29).centerKHz);
		assertEquals(97300, FmChannelPlan.nearest(97.31).centerKHz);
		assertEquals(88100, FmChannelPlan.nearest(88.1).centerKHz);
		assertNull(FmChannelPlan.nearest(87.9));
		assertNull(FmChannelPlan.nearest(108.1));
		assertNull(FmChannelPlan.nearest(50));
	}

	@Test
	void visibleOccupancyFollowsTheSweepWindow() {
		List<FmChannel> all = FmChannelPlan.visibleOccupancy(88, 108);
		assertEquals(100, all.size());
		assertEquals(5, FmChannelPlan.visibleOccupancy(97.2, 98.2).size());
		assertTrue(FmChannelPlan.visibleOccupancy(2400, 2500).isEmpty());
		assertTrue(FmChannelPlan.visibleOccupancy(108, 88).isEmpty());
	}

	@Test
	void detectStationsIgnoresFlatNoise() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		assertTrue(FmChannelPlan.detectStations(ds, 88, 108).isEmpty());
		assertTrue(FmChannelPlan.detectStations(null, 88, 108).isEmpty());
	}

	@Test
	void detectStationsSnapsAPeakToNinetySevenThree() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		ds.getSpectrumArray()[binAt(ds, 97.3)] = -40f;
		List<FmStationHit> hits = FmChannelPlan.detectStations(ds, 88, 108);
		assertEquals(1, hits.size());
		assertEquals("97.3", hits.get(0).label());
		assertEquals(-40f, hits.get(0).powerDbm, 0.01f);
	}

	@Test
	void detectStationsKeepsTwoSeparatedPeaks() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		ds.getSpectrumArray()[binAt(ds, 97.3)] = -40f;
		ds.getSpectrumArray()[binAt(ds, 101.1)] = -35f;
		List<FmStationHit> hits = FmChannelPlan.detectStations(ds, 88, 108);
		assertEquals(2, hits.size());
		assertEquals("97.3", hits.get(0).label());
		assertEquals("101.1", hits.get(1).label());
	}

	@Test
	void detectStationsDoesNotLabelWeakerAdjacentBins() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		int center = binAt(ds, 97.3);
		ds.getSpectrumArray()[center] = -40f;
		ds.getSpectrumArray()[center - 1] = -55f;
		ds.getSpectrumArray()[center + 1] = -58f;
		List<FmStationHit> hits = FmChannelPlan.detectStations(ds, 88, 108);
		assertEquals(1, hits.size());
		assertEquals("97.3", hits.get(0).label());
	}

	@Test
	void detectStationsIgnoresAPeakBelowTheMargin() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		ds.getSpectrumArray()[binAt(ds, 97.3)] = -85f;
		assertTrue(FmChannelPlan.detectStations(ds, 88, 108).isEmpty());
	}

	@Test
	void detectStationsHoldsAStationAcrossASmallFade() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -90f);
		ds.getSpectrumArray()[binAt(ds, 97.3)] = -40f;
		List<FmStationHit> first = FmChannelPlan.detectStations(ds, 88, 108);
		assertEquals(1, first.size());
		ds.getSpectrumArray()[binAt(ds, 97.3)] = -45f;
		List<FmStationHit> held = FmChannelPlan.detectStations(ds, 88, 108, 8f, 3f, first);
		assertEquals(1, held.size());
		assertEquals("97.3", held.get(0).label());
		ds.getSpectrumArray()[binAt(ds, 97.3)] = -88f;
		assertTrue(FmChannelPlan.detectStations(ds, 88, 108, 8f, 3f, held).isEmpty());
	}

	private static int binAt(DatasetSpectrum ds, double mhz) {
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
		return best;
	}
}
