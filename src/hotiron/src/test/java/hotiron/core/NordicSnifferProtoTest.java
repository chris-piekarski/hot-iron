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
		assertEquals(NordicSnifferProto.VERSION, h.version);
		assertEquals(7, h.counter);
		assertEquals(NordicSnifferProto.PING_REQ, h.type);
		assertEquals(0, h.payload.length);
	}

	@Test
	void toFrameReadsAdvIndAddressAndRssi()
	{
		byte[] payload = new byte[18];
		payload[0] = 10;
		payload[2] = 37;
		payload[3] = 50;
		payload[10] = 0x00;
		payload[11] = 6;
		payload[12] = (byte) 0xFF;
		payload[13] = (byte) 0xEE;
		payload[14] = (byte) 0xDD;
		payload[15] = (byte) 0xCC;
		payload[16] = (byte) 0xBB;
		payload[17] = (byte) 0xAA;
		byte[] slip = NordicSnifferProto.hostPacket(NordicSnifferProto.EVENT_PACKET_ADV, payload, 1);
		BleFrame frame = NordicSnifferProto.toFrame(slip, 1234L);
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
	void pingAndEmptyPacketsAreNotFrames()
	{
		assertNull(NordicSnifferProto.toFrame(NordicSnifferProto.pingReq(0), 1L));
		assertNull(NordicSnifferProto.toFrame(NordicSnifferProto.scanCont(1), 1L));
		assertNull(NordicSnifferProto.toFrame(new byte[] { 1, 2, 3 }, 1L));
	}
}
