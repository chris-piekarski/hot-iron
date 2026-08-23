package hotiron.nativebridge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NfcDecNativeTest
{
	@Test
	void cstrStopsAtNulAndHexIsSpaced()
	{
		assertEquals("REQA", NfcDecNative.cstr(new byte[] { 'R', 'E', 'Q', 'A', 0, 1 }));
		assertEquals("", NfcDecNative.cstr(null));
		assertEquals("26 00", NfcDecNative.hex(new byte[] { 0x26, 0x00, 0x11 }, 2));
		assertEquals("", NfcDecNative.hex(new byte[] { 0x26 }, 0));
		assertEquals(328, NfcDecNative.FRAME_BYTES);
	}
}
