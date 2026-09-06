package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperatorBriefLayerTest
{
	@Test
	void marksStayOnTheSharedAxis()
	{
		OperatorBrief brief = new OperatorBrief("ble", "Room", "note",
				List.of(new OperatorBrief.Mark(2437.6, "Wi-Fi 6"), new OperatorBrief.Mark(88.5, "FM")), 1L, "mcp");
		List<BandMark> marks = OperatorBriefLayer.marks(FrequencyAxis.of(2400, 2484, 800), brief);
		assertEquals(1, marks.size());
		assertEquals("Wi-Fi 6", marks.get(0).label);
		assertTrue(marks.get(0).centerTick);
		assertTrue(OperatorBriefLayer.marks(FrequencyAxis.of(2400, 2484, 800), null).isEmpty());
	}
}
