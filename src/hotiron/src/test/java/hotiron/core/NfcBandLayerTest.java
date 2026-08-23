package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class NfcBandLayerTest
{
	@Test
	void tagsReadableOnlyOnNfcScale()
	{
		assertTrue(NfcBandLayer.tagsReadable(12, 15));
		assertTrue(NfcBandLayer.tagsReadable(FrequencyAxis.of(26, 28, 400)));
		assertFalse(NfcBandLayer.tagsReadable(1, 7250));
		assertFalse(NfcBandLayer.tagsReadable(3, 30));
		assertFalse(NfcBandLayer.tagsReadable(FrequencyAxis.of(88, 108, 700)));
	}

	@Test
	void marksIncludeCarrierAndAbSidebands()
	{
		FrequencyAxis axis = FrequencyAxis.of(12, 15, 800);
		List<BandMark> catalog = NfcBandLayer.marks(axis, NfcActivity.quietVisible());
		assertTrue(catalog.stream().anyMatch(m -> "13.56".equals(m.label)));
		assertTrue(catalog.stream().anyMatch(m -> "A/B".equals(m.label) && m.labelMHz < 13));
		assertTrue(catalog.stream().anyMatch(m -> "A/B".equals(m.label) && m.labelMHz > 14));
		assertTrue(NfcBandLayer.marks(FrequencyAxis.of(3, 30, 900), NfcActivity.quietVisible()).isEmpty());
	}

	@Test
	void liveFieldOnRetitlesTheCarrierTick()
	{
		FrequencyAxis axis = FrequencyAxis.of(12, 15, 800);
		NfcActivity live = new NfcActivity(NfcActivity.Kind.FIELD_ON, -40f, 13.56f, 1f, Float.NaN, Float.NaN, 0f, 0.9f,
				false, false, false, false, false, true);
		List<BandMark> marks = NfcBandLayer.marks(axis, live);
		BandMark carrier = marks.stream().filter(m -> Math.abs(m.labelMHz - 13.56) < 0.02).findFirst().orElseThrow();
		assertEquals("13.56", carrier.label);
		assertEquals(BandMark.Style.TUNED, carrier.style);
		assertTrue(carrier.centerTick);
	}

	@Test
	void harmonicWindowOnlyShowsTheHarmonicTick()
	{
		List<BandMark> marks = NfcBandLayer.marks(FrequencyAxis.of(26, 28, 400), NfcActivity.quietVisible());
		assertEquals(1, marks.size());
		assertEquals("×2", marks.get(0).label);
		assertEquals(NfcBandPlan.H2_MHZ, marks.get(0).labelMHz, 0.001);
	}
}
