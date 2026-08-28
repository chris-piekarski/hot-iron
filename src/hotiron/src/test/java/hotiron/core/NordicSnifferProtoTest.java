package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NordicSnifferProtoTest
{
	@Test
	void hostPacketParsesVersionCounterAndType()
	{
		byte[] raw = NordicSnifferProto.pingReq(7);
		NordicSnifferProto.HostHeader h = NordicSnifferProto.parseHost(raw);
		assertNotNull(h);
		assertEquals(NordicSnifferProto.HOST_PROTO, h.version);
		assertEquals(7, h.counter);
		assertEquals(NordicSnifferProto.PING_REQ, h.type);
		assertEquals(0, h.payload.length);
	}

	@Test
	void toFrameReadsAdvIndAddressAndRssiFromV2Radio()
	{
		byte[] radio = new byte[4 + 3 + 6];
		System.arraycopy(NordicSnifferProto.ADV_ACCESS_ADDRESS, 0, radio, 0, 4);
		radio[4] = 0x00;
		radio[5] = 6;
		radio[6] = 0x00;
		radio[7] = (byte) 0xFF;
		radio[8] = (byte) 0xEE;
		radio[9] = (byte) 0xDD;
		radio[10] = (byte) 0xCC;
		radio[11] = (byte) 0xBB;
		radio[12] = (byte) 0xAA;
		byte[] payload = new byte[10 + radio.length];
		payload[0] = 10;
		payload[2] = 37;
		payload[3] = 50;
		System.arraycopy(radio, 0, payload, 10, radio.length);
		byte[] v2 = v2Event(NordicSnifferProto.EVENT_PACKET, payload, 1);
		BleFrame frame = NordicSnifferProto.toFrame(v2, 1234L);
		assertNotNull(frame);
		assertEquals("ADV_IND", frame.name);
		assertEquals(37, frame.channel);
		assertEquals(-50, frame.rssiDbm);
		assertEquals("AA:BB:CC:DD:EE:FF", frame.address);
		assertTrue(frame.advertising);
		assertTrue(frame.toJson().contains("ADV_IND"));
		assertTrue(frame.line().contains("ch37"));
	}

	@Test
	void parseHostReadsCapturedV2LengthPrefix()
	{
		byte[] payload = new byte[10];
		payload[0] = 10;
		byte[] raw = v2Event(NordicSnifferProto.EVENT_PACKET, payload, 0);
		NordicSnifferProto.HostHeader h = NordicSnifferProto.parseHost(raw);
		assertNotNull(h);
		assertEquals(2, h.version);
		assertEquals(NordicSnifferProto.EVENT_PACKET, h.type);
		assertEquals(10, h.payload.length);
	}

	@Test
	void pingAndEmptyPacketsAreNotFrames()
	{
		assertNull(NordicSnifferProto.toFrame(NordicSnifferProto.pingReq(0), 1L));
		assertNull(NordicSnifferProto.toFrame(NordicSnifferProto.scanCont(1), 1L));
		assertNull(NordicSnifferProto.toFrame(new byte[] { 1, 2, 3 }, 1L));
	}

	private static byte[] v2Event(int type, byte[] payload, int counter)
	{
		int pay = payload.length;
		byte[] raw = new byte[NordicSnifferProto.HEADER_LEN + pay];
		raw[0] = (byte) (pay & 0xFF);
		raw[1] = (byte) ((pay >> 8) & 0xFF);
		raw[2] = 2;
		raw[3] = (byte) (counter & 0xFF);
		raw[4] = (byte) ((counter >> 8) & 0xFF);
		raw[5] = (byte) type;
		System.arraycopy(payload, 0, raw, NordicSnifferProto.HEADER_LEN, pay);
		return raw;
	}
}
