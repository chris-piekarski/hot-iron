package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AmDemodulatorTest
{
	@Test
	void oneKhzToneSurvivesEnvelopeDemodOnChannel19()
	{
		byte[] iq = modulate(CbChannelPlan.LO_HZ, 1000, 0.5, 0.08);
		AmDemodulator demod = new AmDemodulator();
		demod.setChannelHz(CbChannelPlan.LO_HZ);
		short[] pcm = new short[AmDemodulator.AUDIO_RATE_HZ];
		int n = demod.processIq(iq, iq.length, 100, pcm);
		assertTrue(n > AmDemodulator.AUDIO_RATE_HZ / 30, "got " + n);
		double e1k = goertzel(pcm, n, AmDemodulator.AUDIO_RATE_HZ, 1000);
		double e400 = goertzel(pcm, n, AmDemodulator.AUDIO_RATE_HZ, 400);
		assertTrue(e1k > e400 * 3, "1 kHz should dominate: " + e1k + " vs " + e400);
		assertTrue(rms(pcm, n) > 200);
	}

	@Test
	void mixSelectsChannel1OffTheLo()
	{
		long ch1 = CbChannelPlan.find(1).centerHz();
		byte[] iq = modulate(ch1, 1000, 0.5, 0.08);
		AmDemodulator on = new AmDemodulator();
		on.setChannelHz(ch1);
		AmDemodulator off = new AmDemodulator();
		off.setChannelHz(CbChannelPlan.LO_HZ);
		short[] a = new short[AmDemodulator.AUDIO_RATE_HZ];
		short[] b = new short[AmDemodulator.AUDIO_RATE_HZ];
		int na = on.processIq(iq, iq.length, 100, a);
		int nb = off.processIq(iq, iq.length, 100, b);
		double eOn = goertzel(a, na, AmDemodulator.AUDIO_RATE_HZ, 1000);
		double eOff = goertzel(b, nb, AmDemodulator.AUDIO_RATE_HZ, 1000);
		assertTrue(eOn > eOff * 2.5, "ch1 mix should beat LO mix: " + eOn + " vs " + eOff);
	}

	@Test
	void engineOffersIqToRecordingSink() throws Exception
	{
		RecordingAudioSink sink = new RecordingAudioSink();
		CbListenEngine engine = new CbListenEngine();
		engine.setSettleMs(0);
		engine.setChannel(19);
		engine.setVolume(100);
		engine.start(sink);
		byte[] iq = modulate(CbChannelPlan.LO_HZ, 1000, 0.5, 0.05);
		int chunk = 262144;
		for (int off = 0; off < iq.length;)
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
		while (sink.size() < 400 && System.currentTimeMillis() < deadline)
			Thread.sleep(20);
		engine.stop();
		assertTrue(sink.size() > 400, "got " + sink.size());
	}

	static byte[] modulate(long channelHz, double audioHz, double depth, double seconds)
	{
		int n = (int) Math.round(AmDemodulator.IQ_RATE_HZ * seconds);
		byte[] iq = new byte[n * 2];
		double fd = channelHz - CbChannelPlan.LO_HZ;
		double twoPi = 2 * Math.PI;
		for (int i = 0; i < n; i++)
		{
			double t = i / (double) AmDemodulator.IQ_RATE_HZ;
			double env = 1.0 + depth * Math.sin(twoPi * audioHz * t);
			double c = Math.cos(twoPi * fd * t);
			double s = Math.sin(twoPi * fd * t);
			int ii = (int) Math.round(env * c * 80);
			int qq = (int) Math.round(env * s * 80);
			if (ii > 127)
				ii = 127;
			if (ii < -127)
				ii = -127;
			if (qq > 127)
				qq = 127;
			if (qq < -127)
				qq = -127;
			iq[i * 2] = (byte) ii;
			iq[i * 2 + 1] = (byte) qq;
		}
		return iq;
	}

	static double goertzel(short[] pcm, int n, int rate, double hz)
	{
		double w = 2 * Math.PI * hz / rate;
		double coeff = 2 * Math.cos(w);
		double s0 = 0, s1 = 0, s2 = 0;
		int cap = Math.min(n, pcm.length);
		for (int i = 0; i < cap; i++)
		{
			s0 = pcm[i] + coeff * s1 - s2;
			s2 = s1;
			s1 = s0;
		}
		return s1 * s1 + s2 * s2 - coeff * s1 * s2;
	}

	static double rms(short[] pcm, int n)
	{
		double s = 0;
		int cap = Math.min(n, pcm.length);
		for (int i = 0; i < cap; i++)
			s += pcm[i] * (double) pcm[i];
		return Math.sqrt(s / Math.max(1, cap));
	}
}
