package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class WifiBandLayerTest {

	@Test
	void wifi2ViewEmitsElevenChannelsAndPrimaryFirst() {
		FrequencyAxis axis = FrequencyAxis.of(2402, 2472, 700);
		List<BandMark> marks = WifiBandLayer.marks(axis);
		assertEquals(11, marks.size());
		assertEquals("1", marks.get(0).label);
		assertEquals(BandMark.Style.PRIMARY, marks.get(0).style);
		assertEquals(BandMark.LabelFit.DROP_IF_OVERLAP, marks.get(0).labelFit);
		assertEquals(2402, marks.get(0).lowMHz, 0.001);
		assertEquals(2422, marks.get(0).highMHz, 0.001);
		boolean saw11 = false;
		for (BandMark m : marks)
		{
			if ("11".equals(m.label))
			{
				saw11 = true;
				assertEquals(2452, m.lowMHz, 0.001);
				assertEquals(2472, m.highMHz, 0.001);
				assertEquals(20, m.highMHz - m.lowMHz, 0.001);
			}
		}
		assertTrue(saw11);
	}

	@Test
	void hidesWhenATwentyMegahertzChannelIsOnlyAFewPixels() {
		FrequencyAxis survey = FrequencyAxis.of(1, 7250, 800);
		assertFalse(WifiBandLayer.showBand24(survey));
		assertTrue(WifiBandLayer.marks(survey).isEmpty());
	}

	@Test
	void fmViewHasNoWifiMarks() {
		assertTrue(WifiBandLayer.marks(FrequencyAxis.of(88, 108, 800)).isEmpty());
	}

	@Test
	void fiveGigUsesFitOccupancy() {
		FrequencyAxis axis = FrequencyAxis.of(WifiChannelPlan.WIFI_5_VIEW_START_MHZ,
				WifiChannelPlan.WIFI_5_VIEW_END_MHZ, 725);
		List<BandMark> marks = WifiBandLayer.marks(axis);
		assertFalse(marks.isEmpty());
		assertEquals(BandMark.LabelFit.FIT_OCCUPANCY, marks.get(0).labelFit);
	}

	@Test
	void sixGigViewEmitsUniiChannels() {
		FrequencyAxis axis = FrequencyAxis.of(WifiChannelPlan.WIFI_6_VIEW_START_MHZ,
				WifiChannelPlan.WIFI_6_VIEW_END_MHZ, 1200);
		assertTrue(WifiBandLayer.showBand6(axis));
		List<BandMark> marks = WifiBandLayer.marks(axis);
		assertEquals(59, marks.size());
		assertEquals(BandMark.LabelFit.FIT_OCCUPANCY, marks.get(0).labelFit);
	}
}
