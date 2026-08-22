package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FmBandLayerTest {

	@Test
	void tagsReadableOnlyAtFmScale() {
		assertTrue(FmBandLayer.tagsReadable(88, 108));
		assertTrue(FmBandLayer.tagsReadable(FrequencyAxis.of(96, 102, 400)));
		assertFalse(FmBandLayer.tagsReadable(1, 7250));
		assertFalse(FmBandLayer.tagsReadable(30, 300));
		assertFalse(FmBandLayer.tagsReadable(FrequencyAxis.of(2402, 2472, 700)));
	}

	@Test
	void marksStrongestStationFirstAndSkipsEmpty() {
		FrequencyAxis axis = FrequencyAxis.of(88, 108, 800);
		assertTrue(FmBandLayer.marks(axis, List.of()).isEmpty());
		FmStationHit weak = new FmStationHit(FmChannelPlan.nearest(97.3), -50f, 0.4f);
		FmStationHit strong = new FmStationHit(FmChannelPlan.nearest(101.1), -30f, 0.9f);
		List<BandMark> marks = FmBandLayer.marks(axis, List.of(weak, strong));
		assertEquals(2, marks.size());
		assertEquals("101.1", marks.get(0).label);
		assertEquals(BandMark.Style.PRIMARY, marks.get(0).style);
		assertTrue(marks.get(0).centerTick);
		assertFalse(marks.get(0).edgeTicks);
		assertEquals(0.9f, marks.get(0).intensity, 0.001f);
	}

	@Test
	void selectedStationIsTunedAndFullHeight() {
		FrequencyAxis axis = FrequencyAxis.of(88, 108, 800);
		FmStationHit a = new FmStationHit(FmChannelPlan.nearest(97.3), -40f, 1f);
		FmStationHit b = new FmStationHit(FmChannelPlan.nearest(101.1), -30f, 1f);
		List<BandMark> marks = FmBandLayer.marks(axis, List.of(a, b), 97300);
		BandMark tuned = marks.stream().filter(m -> m.style == BandMark.Style.TUNED).findFirst().orElseThrow();
		assertEquals("97.3", tuned.label);
		assertTrue(tuned.centerTick);
		assertFalse(tuned.fullHeightFill);
		assertEquals(1f, tuned.intensity, 0.001f);
	}

	@Test
	void marksEmptyOnASurveyEvenWithHits() {
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f, 1f);
		assertTrue(FmBandLayer.marks(FrequencyAxis.of(1, 7250, 900), List.of(hit)).isEmpty());
	}
}
