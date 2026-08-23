package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.core.NfcFrame;

class NfcSniffHudTest
{
	@Test
	void textNamesFieldThenLastFrame()
	{
		assertTrue(NfcSniffHud.text(null, false).contains("no field"));
		assertTrue(NfcSniffHud.text(null, true).contains("field on"));
		NfcFrame reqa = new NfcFrame(1, 0x0101, 0x0102, 0, 0, 106000, 0, 0, "REQA", "26");
		assertTrue(NfcSniffHud.text(reqa, true).contains("REQA"));
	}
}
