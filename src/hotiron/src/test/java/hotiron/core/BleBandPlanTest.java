package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BleBandPlanTest
{
	@Test
	void viewCoversAdvertisingThirtyNine()
	{
		FrequencyRange view = BleBandPlan.viewWindow();
		assertEquals(2400, view.getStartMHz());
		assertEquals(2484, view.getEndMHz());
		assertTrue(BleBandPlan.viewIsBle(2400, 2484));
		assertFalse(BleBandPlan.viewIsBle(2402, 2472), "Wi-Fi 2 cuts off ch 39");
		assertFalse(BleBandPlan.viewIsBle(1, 7250));
	}

	@Test
	void channelsMatchBleRaster()
	{
		assertEquals(2402, BleBandPlan.mhzForChannel(37), 0.001);
		assertEquals(2426, BleBandPlan.mhzForChannel(38), 0.001);
		assertEquals(2480, BleBandPlan.mhzForChannel(39), 0.001);
		assertEquals(2404, BleBandPlan.mhzForChannel(0), 0.001);
		assertEquals(2424, BleBandPlan.mhzForChannel(10), 0.001);
		assertEquals(2428, BleBandPlan.mhzForChannel(11), 0.001);
		assertEquals(2478, BleBandPlan.mhzForChannel(36), 0.001);
		assertEquals(37, BleBandPlan.channelForMhz(2402));
		assertEquals(38, BleBandPlan.channelForMhz(2426));
		assertEquals(39, BleBandPlan.channelForMhz(2480));
		assertEquals(0, BleBandPlan.channelForMhz(2404));
		assertEquals(10, BleBandPlan.channelForMhz(2424));
		assertEquals(11, BleBandPlan.channelForMhz(2428));
		assertEquals(36, BleBandPlan.channelForMhz(2478));
		assertEquals(-1, BleBandPlan.channelForMhz(88));
	}

	@Test
	void labelsOnlyOnTheBleWindow()
	{
		assertEquals("BLE 39", BleBandPlan.labelForPeak(2480, 2400, 2484));
		assertEquals("BLE 37", BleBandPlan.labelForPeak(2402, 2400, 2484));
		assertEquals("ANT+", BleBandPlan.labelForPeak(2457, 2400, 2484));
		assertNull(BleBandPlan.labelForPeak(2480, 2402, 2472));
		assertNull(BleBandPlan.labelForPeak(2437, 2402, 2472));
	}

	@Test
	void overlayShowsAdvAndAntOnIsmScale()
	{
		assertEquals(4, BleBandPlan.visibleOverlay(2400, 2484).size());
		assertEquals(3, BleBandPlan.visibleOverlay(2402, 2472).size(), "37, 38, and ANT+; 39 is off the right edge");
		assertTrue(BleBandPlan.visibleOverlay(1, 7250).isEmpty());
	}
}
