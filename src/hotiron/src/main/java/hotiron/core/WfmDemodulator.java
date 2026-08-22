package hotiron.core;

/**
 * Mono broadcast WFM: offset mix, decimate, polar discriminator,
 * 75 µs de-emphasis, resample to 48 kHz. Stateful; {@link #reset()}
 * between stations. No radio, no audio device.
 */
public final class WfmDemodulator
{
	public static final int IQ_RATE_HZ = 4_000_000;
	public static final int IQ_DECIM = 10;
	public static final int IF_RATE_HZ = IQ_RATE_HZ / IQ_DECIM;
	public static final int AUDIO_RATE_HZ = 48_000;
	public static final int OFFSET_HZ = 100_000;
	public static final double DEVIATION_HZ = 75_000;
	public static final double DEEMPHASIS_US = 75.0;
	public static final int MAX_AUDIO_PER_IQ_BYTES = IQ_RATE_HZ / AUDIO_RATE_HZ + 8;

	private static final double MIX_STEP = 2.0 * Math.PI * OFFSET_HZ / IQ_RATE_HZ;
	private static final double RESAMPLE_RATIO = (double) IF_RATE_HZ / AUDIO_RATE_HZ;
	private static final float DEEMPH_A = (float) Math.exp(-1.0 / (DEEMPHASIS_US * 1e-6 * IF_RATE_HZ));
	private static final float AUDIO_SCALE = (float) (IF_RATE_HZ / (2.0 * Math.PI * DEVIATION_HZ) * 20000.0);

	private double mixPhase;
	private double accI, accQ;
	private int accN;
	private float prevI = 1f;
	private float prevQ = 0f;
	private float deemph;
	private float prevAudio;
	private double outPos;
	private int ifIndex;
	private boolean ifPrimed;

	public void reset()
	{
		mixPhase = 0;
		accI = 0;
		accQ = 0;
		accN = 0;
		prevI = 1f;
		prevQ = 0f;
		deemph = 0;
		prevAudio = 0;
		outPos = 0;
		ifIndex = 0;
		ifPrimed = false;
	}

	/**
	 * Interleaved signed int8 I/Q. Writes 48 kHz mono PCM into {@code out}
	 * and returns the sample count.
	 */
	public int processIq(byte[] iq, int nbytes, int volume0to100, short[] out)
	{
		if (iq == null || out == null || nbytes < 2)
			return 0;
		int n = nbytes & ~1;
		if (n > iq.length)
			n = iq.length & ~1;
		int vol = volume0to100;
		if (vol < 0)
			vol = 0;
		if (vol > 100)
			vol = 100;
		float gain = vol / 100f;
		int written = 0;
		for (int i = 0; i + 1 < n && written < out.length; i += 2)
		{
			double iSamp = iq[i] / 128.0;
			double qSamp = iq[i + 1] / 128.0;
			double c = Math.cos(mixPhase);
			double s = Math.sin(mixPhase);
			mixPhase += MIX_STEP;
			if (mixPhase > 2 * Math.PI)
				mixPhase -= 2 * Math.PI;
			/* Mix down by OFFSET: (I+jQ) * e^{-jθ} */
			accI += iSamp * c + qSamp * s;
			accQ += qSamp * c - iSamp * s;
			accN++;
			if (accN < IQ_DECIM)
				continue;
			float iIf = (float) (accI / IQ_DECIM);
			float qIf = (float) (accQ / IQ_DECIM);
			accI = 0;
			accQ = 0;
			accN = 0;
			float det = iIf * prevI + qIf * prevQ;
			float cross = qIf * prevI - iIf * prevQ;
			float dphi = (float) Math.atan2(cross, det);
			prevI = iIf;
			prevQ = qIf;
			deemph = dphi + DEEMPH_A * (deemph - dphi);
			float audio = deemph * AUDIO_SCALE;
			written += emitResampled(audio, gain, out, written);
		}
		return written;
	}

	private int emitResampled(float sample, float gain, short[] out, int written)
	{
		if (!ifPrimed)
		{
			prevAudio = sample;
			ifPrimed = true;
			ifIndex = 0;
			outPos = 0;
			return 0;
		}
		int produced = 0;
		int idx = ifIndex + 1;
		while (outPos <= idx && written + produced < out.length)
		{
			double t = outPos - (idx - 1);
			if (t < 0)
				t = 0;
			if (t > 1)
				t = 1;
			float y = (float) (prevAudio + t * (sample - prevAudio));
			int v = Math.round(y * gain);
			if (v > 32767)
				v = 32767;
			if (v < -32768)
				v = -32768;
			out[written + produced] = (short) v;
			produced++;
			outPos += RESAMPLE_RATIO;
		}
		prevAudio = sample;
		ifIndex = idx;
		return produced;
	}
}
