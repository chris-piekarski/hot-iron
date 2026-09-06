package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperatorBriefTest
{
	@Test
	void clipsTitleBodyMarksAndNormalizesFace()
	{
		String longTitle = "x".repeat(OperatorBrief.TITLE_MAX + 10);
		String longBody = "y".repeat(OperatorBrief.BODY_MAX + 3);
		List<OperatorBrief.Mark> marks = new java.util.ArrayList<>();
		for (int i = 0; i < OperatorBrief.MARKS_MAX + 2; i++)
			marks.add(new OperatorBrief.Mark(2400 + i, "L" + i));
		OperatorBrief b = new OperatorBrief("BLE", longTitle, longBody, marks, 1L, "grok");
		assertEquals("ble", b.face);
		assertEquals(OperatorBrief.TITLE_MAX, b.title.length());
		assertEquals(OperatorBrief.BODY_MAX, b.body.length());
		assertEquals(OperatorBrief.MARKS_MAX, b.marks.size());
		assertFalse(b.isEmpty());
		assertTrue(b.toJson().contains("\"face\":\"ble\""));
	}

	@Test
	void clearIsEmptyAndWatchMapsToTv()
	{
		assertTrue(OperatorBrief.clear("nfc").isEmpty());
		assertEquals("tv", OperatorBrief.normalizeFace("watch"));
		assertEquals("fm", OperatorBrief.normalizeFace("listen"));
		assertEquals("sweep", OperatorBrief.normalizeFace("nope"));
		assertEquals("ble", OperatorBrief.faceOf(BandToolKind.BLE));
		assertEquals("sweep", OperatorBrief.faceOf(null));
		assertEquals("ble", OperatorBrief.inferFace("sweep", true));
		assertEquals("gnss", OperatorBrief.inferFace("gnss", false));
		assertEquals("tv", OperatorBrief.inferFace("watch", false));
	}
}
