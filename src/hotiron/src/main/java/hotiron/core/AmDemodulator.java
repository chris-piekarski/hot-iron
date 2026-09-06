package hotiron.core;

/**
 * Mono AM envelope demod at 4 MS/s: mix the selected CB channel to DC,
 * decimate, subtract carrier, AGC, resample to 48 kHz. Stateful.
 */
public final class AmDemodulator
{
	public static final int IQ_RATE_HZ = 4_000_000;
	/** 20 kHz IF; 220 kHz (ch 1/40 vs LO) is an integer number of boxcar cycles. */
	public static final int IQ_DECIM = 200;
	public static final int IF_RATE_HZ = IQ_RATE_HZ / IQ_DECIM;
	public static final int AUDIO_RATE_HZ = 48_000;
	public static final int MAX_AUDIO_PER_IQ_BYTES = IQ_RATE_HZ / AUDIO_RATE_HZ + 8;

	private static final double RESAMPLE_RATIO = (double) IF_RATE_HZ / AUDIO_RATE_HZ;
	private static final float DC_A = 0.001f;
	private static final float ENV_A = 0.02f;
	private static final float AGC_A = 0.002f;
	private static final float AGC_TARGET = 0.12f;
	private static final float AUDIO_SCALE = 22000f;

	private volatile double mixStep;
	private double mixPhase;
	private double accI, accQ;
	private int accN;
	private float dc;
	private float envAbsEma;
	private float agcLevel = AGC_TARGET;
	private float prevAudio;
	private double outPos;
	private int ifIndex;
	private boolean ifPrimed;

	public AmDemodulator()
	{
		setChannelHz(CbChannelPlan.LO_HZ);
	}

	public void setChannelHz(long channelHz)
	{
		double off = (channelHz - CbChannelPlan.LO_HZ);
		mixStep = 2.0 * Math.PI * off / IQ_RATE_HZ;
	}

	public void reset()
	{
		mixPhase = 0;
		accI = 0;
		accQ = 0;
		accN = 0;
		dc = 0;
		envAbsEma = 0;
		agcLevel = AGC_TARGET;
		prevAudio = 0;
		outPos = 0;
		ifIndex = 0;
		ifPrimed = false;
	}

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
		double step = mixStep;
		int written = 0;
		for (int i = 0; i + 1 < n && written < out.length; i += 2)
		{
			double iSamp = iq[i] / 128.0;
			double qSamp = iq[i + 1] / 128.0;
			double c = Math.cos(mixPhase);
			double s = Math.sin(mixPhase);
			mixPhase += step;
			if (mixPhase > Math.PI)
				mixPhase -= 2 * Math.PI;
			else if (mixPhase < -Math.PI)
				mixPhase += 2 * Math.PI;
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
			float mag = (float) Math.hypot(iIf, qIf);
			dc += DC_A * (mag - dc);
			float env = mag - dc;
			float envAbs = env < 0 ? -env : env;
			envAbsEma += ENV_A * (envAbs - envAbsEma);
			agcLevel += AGC_A * (envAbs - agcLevel);
			float agc = AGC_TARGET / (agcLevel + 1e-4f);
			if (agc > 40f)
				agc = 40f;
			float audio = env * agc * AUDIO_SCALE;
			written += emitResampled(audio, gain, out, written);
		}
		return written;
	}

	/** Carrier magnitude after mix (0–1). */
	public float carrier()
	{
		return dc;
	}

	/** Envelope AC (modulation) after DC block. */
	public float modulation()
	{
		return envAbsEma;
	}

	/** Keep the mix; drop carrier memory after a channel hop. */
	public void retune()
	{
		accI = 0;
		accQ = 0;
		accN = 0;
		dc = 0;
		envAbsEma = 0;
		agcLevel = AGC_TARGET;
		ifPrimed = false;
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
