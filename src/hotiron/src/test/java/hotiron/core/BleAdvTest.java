package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BleAdvTest
{
	@Test
	void truncatedLivePduIsNordicLbs()
	{
		BleAdv a = BleAdv.parseHex("401F6AD20152E5F903190000020105110723D1BCEA5F7823…");
		assertEquals("Nordic LBS", a.service);
		assertEquals(0x05, a.flags);
		assertEquals(0, a.appearance);
		assertTrue(a.localName.isEmpty());
		assertTrue(a.uuid.isEmpty());
	}

	@Test
	void fullLbsUuidFormatsAndNames()
	{
		byte[] pdu = hex("401F6AD20152E5F903190000020105110723D1BCEA5F782315DEEF121223150000");
		BleAdv a = BleAdv.parsePdu(pdu);
		assertEquals("00001523-1212-efde-1523-785feabcd123", a.uuid);
		assertEquals("Nordic LBS", a.service);
		assertEquals(0x05, a.flags);
		assertEquals(0, a.appearance);
	}

	@Test
	void scanRspCarriesCompleteLocalName()
	{
		BleAdv a = BleAdv.parseHex("04156AD20152E5F90E094E6F726469635F426C696E6B79");
		assertEquals("Nordic_Blinky", a.localName);
	}

	@Test
	void nordicUartPrefixIsRecognized()
	{
		assertEquals("Nordic UART", BleAdv.knownFromLe("9ecadc240ee5a9e093f3a3b50100406e"));
	}

	@Test
	void pduLengthStopsBeforeCrc()
	{
		BleAdv a = BleAdv.parseHex("401F6AD20152E5F903190000020105110723D1BCEA5F782315DEEF12143315000060212D");
		assertEquals("00001533-1412-efde-1523-785feabcd123", a.uuid);
		assertEquals("Nordic LBS", a.service);
		assertTrue(a.localName.isEmpty());
	}

	private static byte[] hex(String s)
	{
		byte[] out = new byte[s.length() / 2];
		for (int i = 0; i < out.length; i++)
			out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
		return out;
	}
}
