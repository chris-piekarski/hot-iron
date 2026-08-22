package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FmStationDialTest {

	@Test
	void knobRightIsHigherDetectedStationAndWraps() {
		List<FmStationHit> hits = List.of(hit(88.1), hit(97.3), hit(101.1));
		assertEquals(101100, FmStationDial.step(hits, 97300, +1).centerKHz);
		assertEquals(88100, FmStationDial.step(hits, 101100, +1).centerKHz);
		assertEquals(101100, FmStationDial.step(hits, 88100, -1).centerKHz);
		assertEquals(97300, FmStationDial.step(hits, 101100, -1).centerKHz);
	}

	@Test
	void fromOffStationPicksNextInThatDirection() {
		List<FmStationHit> hits = List.of(hit(88.1), hit(101.1));
		assertEquals(101100, FmStationDial.step(hits, 97300, +1).centerKHz);
		assertEquals(88100, FmStationDial.step(hits, 97300, -1).centerKHz);
	}

	@Test
	void tuneIsOneRasterClickNotASeek() {
		List<FmStationHit> hits = List.of(hit(88.1), hit(97.3), hit(101.1));
		assertEquals(97500, FmStationDial.tune(97300, +1).centerKHz);
		assertEquals(97100, FmStationDial.tune(97300, -1).centerKHz);
		assertEquals(101100, FmStationDial.seek(hits, 97300, +1).centerKHz);
	}

	@Test
	void emptyDetectionsStepTheUsRaster() {
		assertEquals(97500, FmStationDial.step(List.of(), 97300, +1).centerKHz);
		assertEquals(97100, FmStationDial.step(List.of(), 97300, -1).centerKHz);
		assertEquals(88100, FmStationDial.step(List.of(), 107900, +1).centerKHz);
		assertEquals(107900, FmStationDial.step(List.of(), 88100, -1).centerKHz);
	}

	private static FmStationHit hit(double mhz) {
		return new FmStationHit(FmChannelPlan.nearest(mhz), -40f, 1f);
	}
}
