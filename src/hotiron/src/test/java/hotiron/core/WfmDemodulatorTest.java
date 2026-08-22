package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WfmDemodulatorTest {

	@Test
	void oneKhzToneSurvivesOffsetLoAndDemod() {
		byte[] iq = modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.08);
		WfmDemodulator demod = new WfmDemodulator();
		short[] pcm = new short[WfmDemodulator.AUDIO_RATE_HZ];
		int n = demod.processIq(iq, iq.length, 100, pcm);
		assertTrue(n > WfmDemodulator.AUDIO_RATE_HZ / 30, "expected ~80 ms of audio, got " + n);
		double e1k = goertzel(pcm, n, WfmDemodulator.AUDIO_RATE_HZ, 1000);
		double e400 = goertzel(pcm, n, WfmDemodulator.AUDIO_RATE_HZ, 400);
		double e2k = goertzel(pcm, n, WfmDemodulator.AUDIO_RATE_HZ, 2000);
		assertTrue(e1k > e400 * 4, "1 kHz should dominate 400 Hz: " + e1k + " vs " + e400);
		assertTrue(e1k > e2k * 4, "1 kHz should dominate 2 kHz: " + e1k + " vs " + e2k);
		assertTrue(rms(pcm, n) > 200, "audio should not be silence");
	}

	@Test
	void deemphasisAttenuatesTenKhzVsOneKhz() {
		byte[] one = modulate(1000, 40_000, 0.06);
		byte[] ten = modulate(10000, 40_000, 0.06);
		WfmDemodulator a = new WfmDemodulator();
		WfmDemodulator b = new WfmDemodulator();
		short[] pcm1 = new short[WfmDemodulator.AUDIO_RATE_HZ];
		short[] pcm10 = new short[WfmDemodulator.AUDIO_RATE_HZ];
		int n1 = a.processIq(one, one.length, 100, pcm1);
		int n10 = b.processIq(ten, ten.length, 100, pcm10);
		double e1 = goertzel(pcm1, n1, WfmDemodulator.AUDIO_RATE_HZ, 1000);
		double e10 = goertzel(pcm10, n10, WfmDemodulator.AUDIO_RATE_HZ, 10000);
		assertTrue(e1 > e10, "75 µs de-emphasis must cut 10 kHz vs 1 kHz: " + e1 + " vs " + e10);
	}

	@Test
	void volumeZeroIsSilence() {
		byte[] iq = modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.04);
		WfmDemodulator demod = new WfmDemodulator();
		short[] pcm = new short[WfmDemodulator.AUDIO_RATE_HZ];
		int n = demod.processIq(iq, iq.length, 0, pcm);
		assertTrue(n > 0);
		assertEquals(0, rms(pcm, n), 0.01);
	}

	@Test
	void engineOffersIqToRecordingSink() throws Exception {
		RecordingAudioSink sink = new RecordingAudioSink();
		FmListenEngine engine = new FmListenEngine();
		engine.setVolume(100);
		engine.start(sink);
		byte[] iq = modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.05);
		int chunk = 262144;
		for (int off = 0; off < iq.length; )
		{
			int n = Math.min(chunk, iq.length - off);
			if ((n & 1) == 1)
				n--;
			byte[] part = new byte[n];
			System.arraycopy(iq, off, part, 0, n);
			engine.offerIq(part);
			off += n;
		}
		long deadline = System.currentTimeMillis() + 2000;
		while (sink.size() < 500 && System.currentTimeMillis() < deadline)
			Thread.sleep(20);
		engine.stop();
		assertTrue(sink.size() > 500, "demod thread should emit PCM, got " + sink.size());
		assertTrue(engine.offeredChunks() > 0);
	}

	static byte[] modulate(double audioHz, double deviationHz, double seconds) {
		int n = (int) Math.round(WfmDemodulator.IQ_RATE_HZ * seconds);
		byte[] iq = new byte[n * 2];
		double phase = 0;
		double twoPi = 2 * Math.PI;
		for (int i = 0; i < n; i++)
		{
			double t = i / (double) WfmDemodulator.IQ_RATE_HZ;
			double inst = WfmDemodulator.OFFSET_HZ + deviationHz * Math.sin(twoPi * audioHz * t);
			phase += twoPi * inst / WfmDemodulator.IQ_RATE_HZ;
			iq[i * 2] = (byte) Math.round(Math.cos(phase) * 100);
			iq[i * 2 + 1] = (byte) Math.round(Math.sin(phase) * 100);
		}
		return iq;
	}

	static double goertzel(short[] x, int n, double fs, double f) {
		double w = 2 * Math.PI * f / fs;
		double cr = 2 * Math.cos(w);
		double s0 = 0, s1 = 0, s2 = 0;
		int start = Math.min(n / 10, n);
		for (int i = start; i < n; i++)
		{
			s0 = x[i] + cr * s1 - s2;
			s2 = s1;
			s1 = s0;
		}
		return s1 * s1 + s2 * s2 - cr * s1 * s2;
	}

	static double rms(short[] x, int n) {
		if (n <= 0)
			return 0;
		double s = 0;
		int start = Math.min(n / 10, n);
		for (int i = start; i < n; i++)
			s += (double) x[i] * x[i];
		return Math.sqrt(s / Math.max(1, n - start));
	}
}
