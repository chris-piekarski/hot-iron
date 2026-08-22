package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RadioIdentityTest {

	@Test
	void absentCopyIsOperatorFriendly() {
		assertFalse(RadioIdentity.ABSENT.present);
		assertEquals("No radio detected", RadioIdentity.ABSENT.displayBoard());
		assertTrue(RadioIdentity.ABSENT.statusHtml().contains("No radio detected"));
		assertTrue(RadioIdentity.ABSENT.statusHtml().contains("USB"));
		assertTrue(RadioIdentity.ABSENT.tooltip(false).toLowerCase().contains("usb"));
	}

	@Test
	void stripsFirmwarePrefixAndShowsShortSerial() {
		RadioIdentity id = RadioIdentity.of("HackRF One",
				"0000000000000000a1b2c3d4e5f60708", "v2026.01.3 (git-abc)", "1.16");
		assertEquals("HackRF One", id.displayBoard());
		assertEquals("2026.01.3", id.displayFirmware());
		assertEquals("e5f60708", id.shortSerial());
		assertTrue(id.statusHtml().contains("HackRF One"));
		assertTrue(id.statusHtml().contains("SN e5f60708"));
		assertTrue(id.statusHtml().contains("FW 2026.01.3"));
		assertTrue(id.tooltip(true).contains("Sweep running"));
		assertTrue(id.tooltip(true).contains("0000000000000000a1b2c3d4e5f60708"));
		assertTrue(id.tooltip(true).contains("1.16"));
	}

	@Test
	void formatMcuSerialIs32HexDigits() {
		assertEquals("00000001000000020000000300000004",
				RadioIdentity.formatMcuSerial(new int[] { 1, 2, 3, 4 }));
		assertNull(RadioIdentity.formatMcuSerial(null));
		assertNull(RadioIdentity.formatMcuSerial(new int[] { 1, 2 }));
	}

	@Test
	void missingBoardFallsBackToHackrf() {
		RadioIdentity id = RadioIdentity.of("  ", null, null, null);
		assertEquals("HackRF", id.displayBoard());
		assertTrue(id.statusHtml().contains("Radio open"));
	}

	@Test
	void htmlEscapesBoardName() {
		RadioIdentity id = RadioIdentity.of("A <B>", "aa", "1", null);
		assertFalse(id.statusHtml().contains("<B>"));
		assertTrue(id.statusHtml().contains("&lt;B&gt;"));
	}
}
