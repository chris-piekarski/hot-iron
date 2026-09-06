package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BleMacTrackerTest
{
	@Test
	void firstMacAppendsAndLaterPdusUpdateSameRow()
	{
		BleMacTracker t = new BleMacTracker();
		t.ingest(new BleFrame(1000, 37, -80, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true), 1000);
		assertEquals(1, t.rows().size());
		assertEquals("AA:BB:CC:DD:EE:FF", t.rows().get(0).address);
		t.ingest(new BleFrame(1100, 39, -70, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true), 1100);
		t.ingest(new BleFrame(1200, 38, -55, "ADV_SCAN", "11:22:33:44:55:66", "00", true), 1200);
		assertEquals(2, t.rows().size());
		assertEquals("AA:BB:CC:DD:EE:FF", t.rows().get(0).address);
		assertEquals(39, t.rows().get(0).channel);
		assertEquals(2, t.rows().get(0).hits);
		assertEquals(-70, t.rows().get(0).rssiDbm);
		assertEquals("11:22:33:44:55:66", t.rows().get(1).address);
		assertEquals(2402, BleBandPlan.mhzForChannel(37), 0.01);
		assertEquals(2480, t.rows().get(0).mhz(), 0.01);
	}

	@Test
	void quietMacDropsAfterHold()
	{
		BleMacTracker t = new BleMacTracker();
		t.ingest(new BleFrame(1, 37, -40, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true), 1);
		assertEquals(1, t.rows().size());
		t.expire(1 + BleMacTracker.HOLD_MS);
		assertEquals(1, t.rows().size());
		t.expire(2 + BleMacTracker.HOLD_MS);
		assertTrue(t.rows().isEmpty());
	}

	@Test
	void blankAddressIsIgnored()
	{
		BleMacTracker t = new BleMacTracker();
		t.ingest(new BleFrame(1, 37, -40, "ADV_IND", "", "00", true), 1);
		assertTrue(t.rows().isEmpty());
	}

	@Test
	void truncatedLbsUuidLabelsTheRow()
	{
		BleMacTracker t = new BleMacTracker();
		t.ingest(new BleFrame(1, 37, -80, "ADV_IND", "F9:E5:52:01:D2:6A",
				"401F6AD20152E5F903190000020105110723D1BCEA5F7823…", true), 1);
		assertEquals("Nordic LBS", t.rows().get(0).service);
		assertTrue(t.rows().get(0).values().startsWith("Nordic LBS"));
	}
}
