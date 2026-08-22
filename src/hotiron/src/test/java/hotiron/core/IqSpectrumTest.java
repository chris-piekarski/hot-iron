package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IqSpectrumTest {

	@Test
	void negativePilotTonePeaksInFftshift() {
		IqSpectrum spec = new IqSpectrum();
		double f0 = -2.69e6;
		byte[] iq = complexTone(f0, IqSpectrum.FFT_N);
		float[] db = spec.accept(iq, iq.length);
		assertNotNull(db);
		assertEquals(IqSpectrum.FFT_N, db.length);
		int peak = 0;
		for (int i = 1; i < db.length; i++)
		{
			if (db[i] > db[peak])
				peak = i;
		}
		int expect = IqSpectrum.binForHz(f0);
		assertEquals(expect, peak, 2, "peak bin " + peak + " want " + expect);
		assertTrue(db[peak] > -15f, "tone should be strong, was " + db[peak]);
	}

	@Test
	void silenceIsLow() {
		IqSpectrum spec = new IqSpectrum();
		byte[] z = new byte[IqSpectrum.FFT_N * 2];
		float[] db = spec.accept(z, z.length);
		assertNotNull(db);
		for (float v : db)
			assertTrue(v < -60f, "silence bin " + v);
	}

	@Test
	void shortChunkYieldsNothing() {
		IqSpectrum spec = new IqSpectrum();
		assertNull(spec.accept(new byte[16], 16));
		assertFalse(spec.hasFrame());
	}

	@Test
	void customFmSampleRateMapsTheLocalRfAxis() {
		IqSpectrum spec = new IqSpectrum(WfmDemodulator.IQ_RATE_HZ);
		double toneHz = 500_000;
		byte[] iq = complexTone(toneHz, IqSpectrum.FFT_N, WfmDemodulator.IQ_RATE_HZ);
		float[] db = spec.accept(iq, iq.length);
		int peak = 0;
		for (int i = 1; i < db.length; i++)
			if (db[i] > db[peak])
				peak = i;
		assertEquals(WfmDemodulator.IQ_RATE_HZ, spec.sampleRate());
		assertEquals(WfmDemodulator.IQ_RATE_HZ / (float) IqSpectrum.FFT_N, spec.binHz());
		assertEquals(spec.binForFrequencyHz(toneHz), peak, 2);
	}

	private static byte[] complexTone(double hz, int pairs) {
		return complexTone(hz, pairs, IqSpectrum.SAMPLE_RATE);
	}

	private static byte[] complexTone(double hz, int pairs, int sampleRate) {
		byte[] iq = new byte[pairs * 2];
		double w = 2 * Math.PI * hz / sampleRate;
		for (int i = 0; i < pairs; i++)
		{
			iq[2 * i] = (byte) Math.round(Math.cos(w * i) * 100);
			iq[2 * i + 1] = (byte) Math.round(Math.sin(w * i) * 100);
		}
		return iq;
	}
}
