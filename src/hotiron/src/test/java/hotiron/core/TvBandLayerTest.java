package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TvBandLayerTest {

	@Test
	void tagsHideOnAWideSurvey() {
		assertFalse(TvBandLayer.tagsReadable(1, 7250));
		assertTrue(TvBandLayer.tagsReadable(470, 608));
	}

	@Test
	void selectedChannelIsTuned() {
		FrequencyAxis axis = FrequencyAxis.of(470, 608, 800);
		TvStationHit a = new TvStationHit(TvChannelPlan.findByFccChannel(14), -40f, 1f);
		TvStationHit b = new TvStationHit(TvChannelPlan.findByFccChannel(36), -30f, 1f);
		List<BandMark> marks = TvBandLayer.marks(axis, List.of(a, b), 14);
		BandMark tuned = marks.stream().filter(m -> m.style == BandMark.Style.TUNED).findFirst().orElseThrow();
		assertEquals("14", tuned.label);
	}
}
