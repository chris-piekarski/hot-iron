package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class GnssBandLayerTest
{
	@Test
	void tagsReadableOnlyOnL1Scale()
	{
		assertTrue(GnssBandLayer.tagsReadable(1559, 1610));
		assertTrue(GnssBandLayer.tagsReadable(FrequencyAxis.of(1573, 1578, 400)));
		assertFalse(GnssBandLayer.tagsReadable(1, 7250));
		assertFalse(GnssBandLayer.tagsReadable(FrequencyAxis.of(88, 108, 700)));
	}

	@Test
	void marksIncludeGpsBdsAndGlo()
	{
		FrequencyAxis axis = FrequencyAxis.of(1559, 1610, 800);
		List<BandMark> catalog = GnssBandLayer.marks(axis, GnssActivity.quiet());
		assertTrue(catalog.stream().anyMatch(m -> "GPS".equals(m.label)));
		assertTrue(catalog.stream().anyMatch(m -> "BDS".equals(m.label)));
		assertTrue(catalog.stream().anyMatch(m -> "GLO".equals(m.label)));
		assertTrue(GnssBandLayer.marks(FrequencyAxis.of(88, 108, 400), GnssActivity.quiet()).isEmpty());
	}

	@Test
	void liveGpsRetitlesTheGpsTick()
	{
		FrequencyAxis axis = FrequencyAxis.of(1559, 1610, 800);
		GnssActivity live = new GnssActivity(true, false, false, -70f, Float.NaN, Float.NaN, -90f, 0.8f, true);
		List<BandMark> marks = GnssBandLayer.marks(axis, live);
		BandMark gps = marks.stream().filter(m -> "GPS".equals(m.label)).findFirst().orElseThrow();
		assertEquals(BandMark.Style.TUNED, gps.style);
		assertTrue(gps.centerTick);
	}
}
