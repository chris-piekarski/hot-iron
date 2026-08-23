package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FmStationDialTest {

	@Test
	void knobRightIsHigherDetectedStationAndWraps() {
		List<FmStationHit> hits = List.of(hit(88.1), hit(97.3), hit(101.1));
		assertEquals(101100, FmStationDial.seek(hits, 97300, +1).centerKHz);
		assertEquals(88100, FmStationDial.seek(hits, 101100, +1).centerKHz);
		assertEquals(101100, FmStationDial.seek(hits, 88100, -1).centerKHz);
		assertEquals(97300, FmStationDial.seek(hits, 101100, -1).centerKHz);
	}

	@Test
	void fromOffStationPicksNextInThatDirection() {
		List<FmStationHit> hits = List.of(hit(88.1), hit(101.1));
		assertEquals(101100, FmStationDial.seek(hits, 97300, +1).centerKHz);
		assertEquals(88100, FmStationDial.seek(hits, 97300, -1).centerKHz);
	}

	@Test
	void tuneIsOneRasterClickNotASeek() {
		List<FmStationHit> hits = List.of(hit(88.1), hit(97.3), hit(101.1));
		assertEquals(97500, FmStationDial.tune(97300, +1).centerKHz);
		assertEquals(97100, FmStationDial.tune(97300, -1).centerKHz);
		assertEquals(101100, FmStationDial.seek(hits, 97300, +1).centerKHz);
	}

	@Test
	void mergeLiveKeepsStationsOutsideTheParkedWindow() {
		List<FmStationHit> remembered = List.of(hit(88.1), hit(97.3), hit(101.1));
		List<FmStationHit> live = List.of(hit(97.3), hit(97.7));
		List<FmStationHit> merged = FmStationDial.mergeLive(remembered, live, 95.2, 99.2);
		assertEquals(4, merged.size());
		assertEquals(88100, merged.get(0).channel.centerKHz);
		assertEquals(97300, merged.get(1).channel.centerKHz);
		assertEquals(97700, merged.get(2).channel.centerKHz);
		assertEquals(101100, merged.get(3).channel.centerKHz);
		assertEquals(97700, FmStationDial.seek(merged, 97300, +1).centerKHz);
		assertEquals(101100, FmStationDial.seek(merged, 97700, +1).centerKHz);
	}

	@Test
	void emptyDetectionsStepTheUsRaster() {
		assertEquals(97500, FmStationDial.seek(List.of(), 97300, +1).centerKHz);
		assertEquals(97100, FmStationDial.seek(List.of(), 97300, -1).centerKHz);
		assertEquals(88100, FmStationDial.seek(List.of(), 107900, +1).centerKHz);
		assertEquals(107900, FmStationDial.seek(List.of(), 88100, -1).centerKHz);
	}

	private static FmStationHit hit(double mhz) {
		return new FmStationHit(FmChannelPlan.nearest(mhz), -40f, 1f);
	}
}
