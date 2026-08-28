package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BandContextTest
{
	@Test
	void wifi2ShowsBleNotBroadcast()
	{
		BandContext c = BandContext.of(new FrequencyRange(2402, 2472), false, ListenService.FM, false,
				BandScan.OFF);
		assertFalse(c.shows(BandToolKind.FM));
		assertFalse(c.shows(BandToolKind.TV));
		assertFalse(c.shows(BandToolKind.NFC));
		assertTrue(c.shows(BandToolKind.BLE));
		assertTrue(c.any());
	}

	@Test
	void fmPresetShowsFmOnly()
	{
		BandContext c = BandContext.of(new FrequencyRange(88, 108), false, null, false, BandScan.OFF);
		assertTrue(c.shows(BandToolKind.FM));
		assertFalse(c.shows(BandToolKind.TV));
		assertFalse(c.shows(BandToolKind.NFC));
		assertFalse(c.shows(BandToolKind.BLE));
	}

	@Test
	void vtvShowsFmAndTv()
	{
		BandContext c = BandContext.of(new FrequencyRange(54, 216), false, null, false, BandScan.OFF);
		assertTrue(c.shows(BandToolKind.FM), "FM occupies ~12% of V-TV");
		assertTrue(c.shows(BandToolKind.TV));
		assertFalse(c.shows(BandToolKind.NFC));
		assertFalse(c.shows(BandToolKind.BLE));
	}

	@Test
	void utvShowsTvOnly()
	{
		BandContext c = BandContext.of(new FrequencyRange(470, 608), false, null, false, BandScan.OFF);
		assertFalse(c.shows(BandToolKind.FM));
		assertTrue(c.shows(BandToolKind.TV));
		assertFalse(c.shows(BandToolKind.BLE));
	}

	@Test
	void nfcPresetShowsNfcNotHfSurvey()
	{
		BandContext nfc = BandContext.of(new FrequencyRange(12, 15), false, null, false, BandScan.OFF);
		assertTrue(nfc.shows(BandToolKind.NFC));
		BandContext hf = BandContext.of(new FrequencyRange(3, 30), false, null, false, BandScan.OFF);
		assertFalse(hf.shows(BandToolKind.NFC), "HF 3–30 is a survey; sniff is the 12–15 PHY window");
		assertFalse(hf.any());
	}

	@Test
	void allAndUhfHideBandTools()
	{
		assertFalse(BandContext.of(new FrequencyRange(1, 7250), false, null, false, BandScan.OFF).any());
		assertFalse(BandContext.of(new FrequencyRange(300, 3000), false, null, false, BandScan.OFF).any());
	}

	@Test
	void parkedListenPinsFmOnWifi2()
	{
		BandContext c = BandContext.of(new FrequencyRange(2402, 2472), true, ListenService.FM, false,
				BandScan.OFF);
		assertTrue(c.shows(BandToolKind.FM));
		assertTrue(c.shows(BandToolKind.BLE));
	}

	@Test
	void parkedWatchPinsTv()
	{
		BandContext c = BandContext.of(new FrequencyRange(2402, 2472), true, ListenService.TV, false,
				BandScan.OFF);
		assertTrue(c.shows(BandToolKind.TV));
		assertFalse(c.shows(BandToolKind.FM));
	}

	@Test
	void bleSniffPinsTheToolOnAll()
	{
		BandContext c = BandContext.of(new FrequencyRange(1, 7250), false, null, true, BandScan.OFF);
		assertTrue(c.shows(BandToolKind.BLE));
		assertFalse(c.shows(BandToolKind.FM));
	}

	@Test
	void fmScanPinsFm()
	{
		BandContext c = BandContext.of(new FrequencyRange(1, 7250), false, null, false, BandScan.FM);
		assertTrue(c.shows(BandToolKind.FM));
	}

	@Test
	void hysteresisKeepsTvAcrossASmallZoomOut()
	{
		FrequencyRange vtv = new FrequencyRange(54, 216);
		BandContext shown = BandContext.of(vtv, false, null, false, BandScan.OFF);
		assertTrue(shown.shows(BandToolKind.TV));
		FrequencyRange wider = new FrequencyRange(50, 250);
		BandContext next = BandContext.of(wider, false, null, false, BandScan.OFF);
		assertFalse(next.shows(BandToolKind.TV), "200 MHz is past TvBandLayer.MAX_VIEW_SPAN");
		BandContext held = next.stabilize(shown, wider);
		assertTrue(held.shows(BandToolKind.TV));
		FrequencyRange vhf = new FrequencyRange(30, 300);
		BandContext dropped = BandContext.of(vhf, false, null, false, BandScan.OFF).stabilize(held, vhf);
		assertFalse(dropped.shows(BandToolKind.TV));
	}

	@Test
	void ofKindsAndEquals()
	{
		BandContext a = BandContext.of(BandToolKind.FM, BandToolKind.BLE);
		BandContext b = BandContext.of(BandToolKind.BLE, BandToolKind.FM);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, BandContext.of(BandToolKind.FM));
		assertTrue(a.shows(BandToolKind.FM));
		assertFalse(a.shows(null));
	}
}
