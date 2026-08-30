package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class WifiChannelPlanTest {

	@Test
	void wifi24IsUsChannels1To11() {
		assertEquals(11, WifiChannelPlan.WIFI_24.size());
		assertEquals(2412, WifiChannelPlan.find(WifiChannelPlan.BAND_24, 1).centerMHz, 0.001);
		assertEquals(2437, WifiChannelPlan.find(WifiChannelPlan.BAND_24, 6).centerMHz, 0.001);
		assertEquals(2462, WifiChannelPlan.find(WifiChannelPlan.BAND_24, 11).centerMHz, 0.001);
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_24, 12));
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_24, 14));
		assertTrue(WifiChannelPlan.find(WifiChannelPlan.BAND_24, 1).primary);
		assertTrue(WifiChannelPlan.find(WifiChannelPlan.BAND_24, 6).primary);
		assertTrue(WifiChannelPlan.find(WifiChannelPlan.BAND_24, 11).primary);
		assertFalse(WifiChannelPlan.find(WifiChannelPlan.BAND_24, 3).primary);
	}

	@Test
	void wifi5UsesIeee20MhzCenters() {
		assertEquals(5180, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 36).centerMHz, 0.001);
		assertEquals(5200, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 40).centerMHz, 0.001);
		assertEquals(5320, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 64).centerMHz, 0.001);
		assertEquals(5500, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 100).centerMHz, 0.001);
		assertEquals(5720, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 144).centerMHz, 0.001);
		assertEquals(5745, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 149).centerMHz, 0.001);
		assertEquals(5825, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 165).centerMHz, 0.001);
		assertEquals(5885, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 177).centerMHz, 0.001);
		assertEquals(20, WifiChannelPlan.find(WifiChannelPlan.BAND_5, 36).widthMHz, 0.001);
	}

	@Test
	void wifi5UsTwentyMhzTableMatchesIeeeAndFcc() {
		// number, center, occupied low, occupied high. Formula: center = 5000 + 5*N.
		int[][] rows = {
				{ 36, 5180, 5170, 5190 }, { 40, 5200, 5190, 5210 }, { 44, 5220, 5210, 5230 },
				{ 48, 5240, 5230, 5250 },
				{ 52, 5260, 5250, 5270 }, { 56, 5280, 5270, 5290 }, { 60, 5300, 5290, 5310 },
				{ 64, 5320, 5310, 5330 },
				{ 100, 5500, 5490, 5510 }, { 104, 5520, 5510, 5530 }, { 108, 5540, 5530, 5550 },
				{ 112, 5560, 5550, 5570 }, { 116, 5580, 5570, 5590 }, { 120, 5600, 5590, 5610 },
				{ 124, 5620, 5610, 5630 }, { 128, 5640, 5630, 5650 }, { 132, 5660, 5650, 5670 },
				{ 136, 5680, 5670, 5690 }, { 140, 5700, 5690, 5710 }, { 144, 5720, 5710, 5730 },
				{ 149, 5745, 5735, 5755 }, { 153, 5765, 5755, 5775 }, { 157, 5785, 5775, 5795 },
				{ 161, 5805, 5795, 5815 }, { 165, 5825, 5815, 5835 },
				{ 169, 5845, 5835, 5855 }, { 173, 5865, 5855, 5875 }, { 177, 5885, 5875, 5895 }
		};
		assertEquals(rows.length, WifiChannelPlan.WIFI_5.size());
		for (int[] row : rows)
		{
			WifiChannel ch = WifiChannelPlan.find(WifiChannelPlan.BAND_5, row[0]);
			assertNotNull(ch, "missing ch " + row[0]);
			assertEquals(row[1], ch.centerMHz, 0.001, "ch " + row[0] + " center");
			assertEquals(row[1], 5000 + 5 * row[0], 0.001);
			assertEquals(row[2], ch.lowMHz(), 0.001, "ch " + row[0] + " start");
			assertEquals(row[3], ch.highMHz(), 0.001, "ch " + row[0] + " end");
			assertEquals(20, ch.widthMHz, 0.001);
		}
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_5, 32), "ch 32 is not a US 20 MHz BSS");
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_5, 68), "ch 68 sits in unused 5330–5350");
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_5, 96), "ch 96 is not a US 20 MHz BSS");
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_5, 181), "ch 181 is C-V2X, not U-NII-4 Wi-Fi");
		assertEquals(5170, WifiChannelPlan.WIFI_5_VIEW_START_MHZ);
		assertEquals(5895, WifiChannelPlan.WIFI_5_VIEW_END_MHZ);
		assertTrue(WifiChannelPlan.visibleOccupancy(5330.001, 5489.999).stream()
				.noneMatch(ch -> WifiChannelPlan.BAND_5.equals(ch.band)),
				"5330–5490 is leftover U-NII-2A + U-NII-2B + unused ch 96");
		assertTrue(WifiChannelPlan.visibleOccupancy(5730.001, 5734.999).stream()
				.noneMatch(ch -> WifiChannelPlan.BAND_5.equals(ch.band)),
				"5 MHz raster step between ch 144 (UNII-2C) and ch 149 (UNII-3)");
	}

	@Test
	void visibleCentersFollowsTheSweepWindow() {
		List<WifiChannel> wifi2 = WifiChannelPlan.visibleCenters(2400, 2484);
		assertEquals(11, wifi2.size());
		assertTrue(wifi2.stream().allMatch(ch -> WifiChannelPlan.BAND_24.equals(ch.band)));

		List<WifiChannel> wifi5 = WifiChannelPlan.visibleCenters(5150, 5895);
		assertFalse(wifi5.isEmpty());
		assertTrue(wifi5.stream().allMatch(ch -> WifiChannelPlan.BAND_5.equals(ch.band)));
		assertTrue(wifi5.stream().anyMatch(ch -> ch.number == 36));
		assertTrue(wifi5.stream().anyMatch(ch -> ch.number == 165));
		assertFalse(wifi5.stream().anyMatch(ch -> ch.number == 1));

		assertTrue(WifiChannelPlan.visibleCenters(88, 108).isEmpty());
		assertTrue(WifiChannelPlan.visibleCenters(2412, 2412).stream().anyMatch(ch -> ch.number == 1));
	}

	@Test
	void occupiedWidthIs20MhzAroundCenter() {
		WifiChannel one = WifiChannelPlan.find(WifiChannelPlan.BAND_24, 1);
		WifiChannel eleven = WifiChannelPlan.find(WifiChannelPlan.BAND_24, 11);
		assertEquals(20, one.widthMHz, 0.001);
		assertEquals(one.widthMHz, eleven.widthMHz, 0.001);
		assertEquals(2402, one.lowMHz(), 0.001);
		assertEquals(2422, one.highMHz(), 0.001);
		assertEquals(2407, WifiChannelPlan.find(WifiChannelPlan.BAND_24, 2).lowMHz(), 0.001);
		assertEquals(2452, eleven.lowMHz(), 0.001);
		assertEquals(2472, eleven.highMHz(), 0.001);
		assertEquals(WifiChannelPlan.WIFI_24_VIEW_START_MHZ, (int) one.lowMHz());
		assertEquals(WifiChannelPlan.WIFI_24_VIEW_END_MHZ, (int) eleven.highMHz());
		assertEquals(WifiChannelPlan.WIFI_24_OCCUPIED_START_MHZ, one.lowMHz(), 0.001);
		assertEquals(WifiChannelPlan.WIFI_24_OCCUPIED_END_MHZ, eleven.highMHz(), 0.001);
		WifiChannel ch36 = WifiChannelPlan.find(WifiChannelPlan.BAND_5, 36);
		WifiChannel ch177 = WifiChannelPlan.find(WifiChannelPlan.BAND_5, 177);
		assertEquals(WifiChannelPlan.WIFI_5_OCCUPIED_START_MHZ, ch36.lowMHz(), 0.001);
		assertEquals(WifiChannelPlan.WIFI_5_OCCUPIED_END_MHZ, ch177.highMHz(), 0.001);
	}

	@Test
	void occupancyIncludesChannelWhenOnlyTheEdgeIsInView() {
		List<WifiChannel> tail = WifiChannelPlan.visibleOccupancy(2465, 2472);
		assertTrue(tail.stream().anyMatch(ch -> ch.number == 11));
		assertFalse(WifiChannelPlan.visibleCenters(2465, 2472).stream().anyMatch(ch -> ch.number == 11));
	}

	@Test
	void labelPriorityPutsPrimaryChannelsFirst() {
		List<WifiChannel> ordered = WifiChannelPlan.labelPriority(WifiChannelPlan.WIFI_24);
		assertEquals(1, ordered.get(0).number);
		assertEquals(6, ordered.get(1).number);
		assertEquals(11, ordered.get(2).number);
	}

	@Test
	void labelForPeakIsWifiOnlyAndPicksClosestCenter() {
		assertEquals("ch 6", WifiChannelPlan.labelForPeak(2437, 2402, 2472));
		assertEquals("ch 1", WifiChannelPlan.labelForPeak(2412, 2402, 2472));
		assertEquals("ch 36", WifiChannelPlan.labelForPeak(5180, 5170, 5895));
		assertEquals("ch 1", WifiChannelPlan.labelForPeak(5955, 5925, 7125));
		assertEquals("ch 233", WifiChannelPlan.labelForPeak(7115, 5925, 7125));
		assertNull(WifiChannelPlan.labelForPeak(97.3, 88, 108));
		assertNull(WifiChannelPlan.labelForPeak(2437, 88, 108));
	}

	@Test
	void wifi6UsesIeee20MhzCentersAndUsUnii58View() {
		assertEquals(59, WifiChannelPlan.WIFI_6.size());
		assertEquals(5955, WifiChannelPlan.find(WifiChannelPlan.BAND_6, 1).centerMHz, 0.001);
		assertEquals(5945, WifiChannelPlan.find(WifiChannelPlan.BAND_6, 1).lowMHz(), 0.001);
		assertEquals(7115, WifiChannelPlan.find(WifiChannelPlan.BAND_6, 233).centerMHz, 0.001);
		assertEquals(7125, WifiChannelPlan.find(WifiChannelPlan.BAND_6, 233).highMHz(), 0.001);
		assertEquals(5950 + 5, WifiChannelPlan.find(WifiChannelPlan.BAND_6, 1).centerMHz, 0.001);
		assertNull(WifiChannelPlan.find(WifiChannelPlan.BAND_6, 3));
		assertEquals(5925, WifiChannelPlan.WIFI_6_VIEW_START_MHZ);
		assertEquals(7125, WifiChannelPlan.WIFI_6_VIEW_END_MHZ);
		assertEquals(WifiChannelPlan.WIFI_6_OCCUPIED_START_MHZ,
				WifiChannelPlan.find(WifiChannelPlan.BAND_6, 1).lowMHz(), 0.001);
		assertEquals(WifiChannelPlan.WIFI_6_OCCUPIED_END_MHZ,
				WifiChannelPlan.find(WifiChannelPlan.BAND_6, 233).highMHz(), 0.001);
		assertTrue(WifiChannelPlan.viewIsWifi(5925, 7125));
		List<WifiChannel> six = WifiChannelPlan.visibleCenters(5925, 7125);
		assertEquals(59, six.size());
		assertTrue(six.stream().allMatch(ch -> WifiChannelPlan.BAND_6.equals(ch.band)));
	}
}
