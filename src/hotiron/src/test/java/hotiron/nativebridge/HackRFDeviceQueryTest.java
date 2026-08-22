package hotiron.nativebridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import hotiron.core.RadioIdentity;

class HackRFDeviceQueryTest {

	@Test
	void parseFirmwareAcceptsVPrefixAndSuffix() {
		int[] parts = HackRFDeviceQuery.parseFirmwareParts("v2024.02.1");
		assertNotNull(parts);
		assertEquals(2024, parts[0]);
		assertEquals(2, parts[1]);
		assertEquals(1, parts[2]);

		parts = HackRFDeviceQuery.parseFirmwareParts("2024.02.1 (git-abc)");
		assertNotNull(parts);
		assertEquals(2024, parts[0]);
	}

	@Test
	void parseFirmwareRejectsEmpty() {
		assertNull(HackRFDeviceQuery.parseFirmwareParts(null));
		assertNull(HackRFDeviceQuery.parseFirmwareParts(""));
		assertNull(HackRFDeviceQuery.parseFirmwareParts("unknown"));
	}

	@Test
	void compareFirmwareOrdersByYearMonthPatch() {
		assertEquals(0, HackRFDeviceQuery.compareFirmware("2024.02.1", "v2024.02.1"));
		assertTrue(HackRFDeviceQuery.compareFirmware("2024.02.1", "2018.01.1") > 0);
		assertTrue(HackRFDeviceQuery.compareFirmware("2023.01.1", "2024.02.1") < 0);
		assertTrue(HackRFDeviceQuery.compareFirmware("2024.04.1", "2024.02.1") > 0);
	}

	@Test
	void compareFirmwareRejectsGarbage() {
		assertThrows(IllegalArgumentException.class, () -> HackRFDeviceQuery.compareFirmware("nope", "2024.02.1"));
	}

	@Test
	void meetsMinimumFirmware() {
		assertTrue(HackRFDeviceQuery.meetsMinimumFirmware("2024.02.1", HackRFDeviceQuery.MIN_FIRMWARE));
		assertTrue(HackRFDeviceQuery.meetsMinimumFirmware("2025.01.1", HackRFDeviceQuery.MIN_FIRMWARE));
		assertFalse(HackRFDeviceQuery.meetsMinimumFirmware("2018.01.1", HackRFDeviceQuery.MIN_FIRMWARE));
		assertFalse(HackRFDeviceQuery.meetsMinimumFirmware("unknown", HackRFDeviceQuery.MIN_FIRMWARE));
	}

	@Test
	void absentQueryMapsToAbsentIdentity() {
		assertFalse(new HackRFDeviceQuery.Info(-1, -1, null, 0, 0xFF, null, null, null, null).toIdentity().present);
	}

	@Test
	void openedInfoMapsBoardFirmwareAndSerial() {
		HackRFDeviceQuery.Info info = new HackRFDeviceQuery.Info(0, 0, "v2026.01.3", 0x0110, 2, "HackRF One",
				"2026.01.3", "git", "0000000000000000a1b2c3d4e5f60708");
		RadioIdentity id = info.toIdentity();
		assertTrue(id.present);
		assertEquals("HackRF One", id.displayBoard());
		assertEquals("2026.01.3", id.displayFirmware());
		assertEquals("e5f60708", id.shortSerial());
		assertEquals("1.16", id.usbApi);
	}

	@Test
	void knownBoardIdsAreHackrfFamily() {
		assertFalse(HackRFDeviceQuery.isKnownHackrfBoard(0));
		assertTrue(HackRFDeviceQuery.isKnownHackrfBoard(1));
		assertTrue(HackRFDeviceQuery.isKnownHackrfBoard(2));
		assertTrue(HackRFDeviceQuery.isKnownHackrfBoard(4));
		assertTrue(HackRFDeviceQuery.isKnownHackrfBoard(5));
		assertFalse(HackRFDeviceQuery.isKnownHackrfBoard(0xFF));
	}
}
