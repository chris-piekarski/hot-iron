package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.core.BandScan;
import hotiron.core.NfcActivity;

class NfcHudTest
{
	@Test
	void scanTextBeatsTheLiveSummary()
	{
		NfcActivity live = NfcActivity.quietVisible();
		assertTrue(NfcHud.text(live, BandScan.NFC).contains("27.12"));
		assertTrue(NfcHud.text(live, BandScan.OFF).contains("NFC quiet"));
		assertEquals("", NfcHud.text(NfcActivity.hidden(), BandScan.OFF));
		assertEquals("", NfcHud.text(null, BandScan.FM));
	}
}
