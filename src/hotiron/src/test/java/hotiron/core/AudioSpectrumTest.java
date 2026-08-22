package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioSpectrumTest {

	@Test
	void oneKhzTonePeaksNearOneKhz() {
		AudioSpectrum spec = new AudioSpectrum();
		short[] pcm = tone(1000, AudioSpectrum.FFT_N * 3);
		float[] db = null;
		for (int off = 0; off < pcm.length; )
		{
			int n = Math.min(512, pcm.length - off);
			short[] chunk = new short[n];
			System.arraycopy(pcm, off, chunk, 0, n);
			float[] row = spec.accept(chunk, n);
			if (row != null)
				db = row;
			off += n;
		}
		assertNotNull(db);
		int peak = 0;
		for (int i = 1; i < db.length; i++)
		{
			if (db[i] > db[peak])
				peak = i;
		}
		float hz = peak * spec.binHz();
		assertEquals(1000f, hz, spec.binHz() * 1.5f, "peak at " + hz + " Hz bin " + peak);
		assertTrue(db[peak] > -20f, "tone should be strong, was " + db[peak]);
	}

	@Test
	void silenceIsLow() {
		AudioSpectrum spec = new AudioSpectrum();
		short[] z = new short[AudioSpectrum.FFT_N * 2];
		float[] db = null;
		float[] row = spec.accept(z, z.length);
		if (row != null)
			db = row;
		assertNotNull(db);
		for (float v : db)
			assertTrue(v < -60f, "silence bin " + v);
	}

	private static short[] tone(double hz, int n) {
		short[] pcm = new short[n];
		for (int i = 0; i < n; i++)
			pcm[i] = (short) (Math.sin(2 * Math.PI * hz * i / AudioSpectrum.SAMPLE_RATE) * 28000);
		return pcm;
	}
}
