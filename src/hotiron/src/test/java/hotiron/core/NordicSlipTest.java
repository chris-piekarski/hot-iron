package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class NordicSlipTest
{
	@Test
	void encodeThenDecodeRoundTrips()
	{
		byte[] payload = { 0x01, (byte) NordicSlip.END, 0x02, (byte) NordicSlip.ESC, 0x03 };
		byte[] wire = NordicSlip.encode(payload);
		assertEquals(NordicSlip.END, wire[0] & 0xFF);
		assertEquals(NordicSlip.END, wire[wire.length - 1] & 0xFF);
		NordicSlip.Decoder dec = new NordicSlip.Decoder();
		List<byte[]> frames = dec.push(wire, 0, wire.length);
		assertEquals(1, frames.size());
		assertArrayEquals(payload, frames.get(0));
	}

	@Test
	void decoderIgnoresNoiseUntilEnd()
	{
		NordicSlip.Decoder dec = new NordicSlip.Decoder();
		assertTrue(dec.push(new byte[] { 0x11, 0x22 }, 0, 2).isEmpty());
		byte[] framed = NordicSlip.encode(new byte[] { 0x42 });
		List<byte[]> frames = dec.push(framed, 0, framed.length);
		assertEquals(1, frames.size());
		assertEquals(0x42, frames.get(0)[0] & 0xFF);
	}
}
