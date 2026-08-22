package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import hotiron.core.IqSpectrum;
import hotiron.core.TvChannelPlan;

class TvWatchSpectrumTest
{
	@Test
	void mapsFftshiftedIqBinsAroundTheTvChannelCenter()
	{
		float[] row = new float[IqSpectrum.FFT_N];
		Arrays.fill(row, -90f);
		row[IqSpectrum.FFT_N / 2 + 64] = -10f; // +1 MHz at 16 MS/s
		float binHz = TvChannelPlan.IQ_RATE_HZ / (float) IqSpectrum.FFT_N;
		TvWatchSpectrum snap = TvWatchSpectrum.fromRow(123, 33,
				587_000_000, TvChannelPlan.IQ_RATE_HZ, binHz, row);
		assertFalse(snap.isEmpty());
		assertEquals(579f, snap.mhz[0], 0.02f);
		assertEquals(588f, snap.peakMHz, 0.02f);
		assertEquals(-10f, snap.peakDbfs);
		assertTrue(snap.toJson().contains("\"tvChannel\":33"));
		assertTrue(snap.toJson().contains("\"powerUnit\":\"dBFS\""));
	}
}
