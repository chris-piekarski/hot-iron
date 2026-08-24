package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class BleBandLayerTest
{
	@Test
	void tagsReadableOnIsmNotAll()
	{
		assertTrue(BleBandLayer.tagsReadable(2400, 2484));
		assertTrue(BleBandLayer.tagsReadable(FrequencyAxis.of(2402, 2472, 700)));
		assertFalse(BleBandLayer.tagsReadable(1, 7250));
		assertFalse(BleBandLayer.tagsReadable(FrequencyAxis.of(88, 108, 700)));
	}

	@Test
	void marksIncludeAdv39OnBleWindow()
	{
		List<BandMark> marks = BleBandLayer.marks(FrequencyAxis.of(2400, 2484, 800));
		assertTrue(marks.stream().anyMatch(m -> "37".equals(m.label)));
		assertTrue(marks.stream().anyMatch(m -> "38".equals(m.label)));
		assertTrue(marks.stream().anyMatch(m -> "39".equals(m.label)));
		assertTrue(marks.stream().anyMatch(m -> "ANT+".equals(m.label)));
		assertTrue(BleBandLayer.marks(FrequencyAxis.of(2402, 2472, 700)).stream()
				.noneMatch(m -> "39".equals(m.label)));
	}
}
