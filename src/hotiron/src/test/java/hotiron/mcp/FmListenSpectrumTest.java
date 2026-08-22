package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import hotiron.core.IqSpectrum;
import hotiron.core.WfmDemodulator;

class FmListenSpectrumTest
{
	@Test
	void mapsFftshiftedIqBinsToAbsoluteRfMHz()
	{
		float[] row = new float[IqSpectrum.FFT_N];
		Arrays.fill(row, -80f);
		row[768] = -12f; // +1 MHz at 4 MS/s
		float binHz = WfmDemodulator.IQ_RATE_HZ / (float) IqSpectrum.FFT_N;
		FmListenSpectrum snap = FmListenSpectrum.fromRow(123, 97.3,
				97_200_000, WfmDemodulator.IQ_RATE_HZ, binHz, row);
		assertFalse(snap.isEmpty());
		assertEquals(95.2f, snap.mhz[0], 0.01f);
		assertEquals(98.2f, snap.peakMHz, 0.01f);
		assertEquals(-12f, snap.peakDbfs);
		assertTrue(snap.toJson().contains("\"powerUnit\":\"dBFS\""));
		assertTrue(snap.toJson().contains("\"dialMHz\":97.3000"));
	}
}
