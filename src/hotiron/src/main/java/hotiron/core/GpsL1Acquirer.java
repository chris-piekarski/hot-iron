package hotiron.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GPS L1 C/A parallel code-phase search at 4 MS/s. One millisecond
 * coherent; Doppler bins are stepped. Not a tracker and not a PVT fix.
 */
public final class GpsL1Acquirer
{
	public static final int SAMPLE_RATE = 4_000_000;
	public static final int SAMPLES_MS = 4000;
	public static final int FFT_N = 4096;
	public static final int DOPPLER_MAX_HZ = 8_000;
	public static final int DOPPLER_STEP_HZ = 500;
	public static final float DETECT_RATIO = 8f;
	public static final int MAX_HITS = 16;

	private final int dopplerMaxHz;
	private final int dopplerStepHz;
	private final int[] dopplers;
	private final float[][] codeRe;
	private final float[][] codeIm;
	private final float[] mixRe = new float[FFT_N];
	private final float[] mixIm = new float[FFT_N];
	private final float[] prodRe = new float[FFT_N];
	private final float[] prodIm = new float[FFT_N];

	public GpsL1Acquirer()
	{
		this(DOPPLER_MAX_HZ, DOPPLER_STEP_HZ);
	}

	public GpsL1Acquirer(int dopplerMaxHz, int dopplerStepHz)
	{
		this.dopplerMaxHz = Math.max(0, dopplerMaxHz);
		this.dopplerStepHz = Math.max(1, dopplerStepHz);
		this.dopplers = dopplerBins(this.dopplerMaxHz, this.dopplerStepHz);
		this.codeRe = new float[GpsCaCode.PRN_MAX + 1][FFT_N];
		this.codeIm = new float[GpsCaCode.PRN_MAX + 1][FFT_N];
		for (int prn = GpsCaCode.PRN_MIN; prn <= GpsCaCode.PRN_MAX; prn++)
		{
			float[] chips = GpsCaCode.resample(GpsCaCode.chips(prn), SAMPLES_MS);
			float[] re = codeRe[prn];
			float[] im = codeIm[prn];
			System.arraycopy(chips, 0, re, 0, SAMPLES_MS);
			AudioSpectrum.fft(re, im, FFT_N);
			for (int k = 0; k < FFT_N; k++)
				im[k] = -im[k];
		}
	}

	public int sampleRate()
	{
		return SAMPLE_RATE;
	}

	public int samplesMs()
	{
		return SAMPLES_MS;
	}

	public synchronized List<GpsSvHit> search(byte[] iq)
	{
		if (iq == null || iq.length < SAMPLES_MS * 2)
			return List.of();
		float[] i = new float[SAMPLES_MS];
		float[] q = new float[SAMPLES_MS];
		int n = Math.min(iq.length / 2, SAMPLES_MS);
		for (int k = 0; k < n; k++)
		{
			i[k] = iq[k * 2] / 128f;
			q[k] = iq[k * 2 + 1] / 128f;
		}
		return search(i, q);
	}

	public synchronized List<GpsSvHit> search(float[] i, float[] q)
	{
		if (i == null || q == null || i.length < SAMPLES_MS || q.length < SAMPLES_MS)
			return List.of();
		float[] peakP = new float[GpsCaCode.PRN_MAX + 1];
		float[] peakRatio = new float[GpsCaCode.PRN_MAX + 1];
		int[] peakFd = new int[GpsCaCode.PRN_MAX + 1];
		int[] peakPh = new int[GpsCaCode.PRN_MAX + 1];
		float strongest = 0;
		for (int prn = GpsCaCode.PRN_MIN; prn <= GpsCaCode.PRN_MAX; prn++)
		{
			for (int fd : dopplers)
			{
				mix(i, q, fd);
				AudioSpectrum.fft(mixRe, mixIm, FFT_N);
				float[] cr = codeRe[prn];
				float[] ci = codeIm[prn];
				for (int k = 0; k < FFT_N; k++)
				{
					prodRe[k] = mixRe[k] * cr[k] - mixIm[k] * ci[k];
					prodIm[k] = mixRe[k] * ci[k] + mixIm[k] * cr[k];
				}
				ifft(prodRe, prodIm, FFT_N);
				int peakAt = 0;
				float peak = 0;
				double sum = 0;
				for (int n = 0; n < SAMPLES_MS; n++)
				{
					float p = prodRe[n] * prodRe[n] + prodIm[n] * prodIm[n];
					sum += p;
					if (p > peak)
					{
						peak = p;
						peakAt = n;
					}
				}
				float mean = (float) ((sum - peak) / Math.max(1, SAMPLES_MS - 1));
				if (!(mean > 0))
					continue;
				float ratio = peak / mean;
				if (peak > peakP[prn])
				{
					peakP[prn] = peak;
					peakRatio[prn] = ratio;
					peakFd[prn] = fd;
					peakPh[prn] = peakAt;
				}
			}
			if (peakP[prn] > strongest)
				strongest = peakP[prn];
		}
		List<GpsSvHit> hits = new ArrayList<>();
		float bestRatio = 0;
		for (int prn = GpsCaCode.PRN_MIN; prn <= GpsCaCode.PRN_MAX; prn++)
		{
			if (peakRatio[prn] > bestRatio)
				bestRatio = peakRatio[prn];
		}
		float ratioFloor = bestRatio / 10f;
		float powerFloor = strongest / 400f;
		for (int prn = GpsCaCode.PRN_MIN; prn <= GpsCaCode.PRN_MAX; prn++)
		{
			if (peakRatio[prn] < DETECT_RATIO || peakRatio[prn] < ratioFloor || peakP[prn] < powerFloor)
				continue;
			hits.add(new GpsSvHit(prn, cn0(peakRatio[prn]), peakFd[prn], peakPh[prn], peakRatio[prn]));
		}
		hits.sort(Comparator.comparingDouble((GpsSvHit h) -> h.cn0DbHz).reversed());
		if (hits.size() > MAX_HITS)
			return List.copyOf(hits.subList(0, MAX_HITS));
		return List.copyOf(hits);
	}

	private void mix(float[] i, float[] q, int fdHz)
	{
		java.util.Arrays.fill(mixRe, 0);
		java.util.Arrays.fill(mixIm, 0);
		double w = 2.0 * Math.PI * fdHz / SAMPLE_RATE;
		for (int n = 0; n < SAMPLES_MS; n++)
		{
			double c = Math.cos(w * n);
			double s = Math.sin(w * n);
			mixRe[n] = (float) (i[n] * c + q[n] * s);
			mixIm[n] = (float) (q[n] * c - i[n] * s);
		}
	}

	static float cn0(float peakRatio)
	{
		double snr = Math.max(peakRatio - 1f, 1e-6);
		return (float) (10.0 * Math.log10(snr / 0.001));
	}

	static int[] dopplerBins(int maxHz, int stepHz)
	{
		int n = (maxHz / stepHz) * 2 + 1;
		int[] out = new int[n];
		int k = 0;
		for (int fd = -maxHz; fd <= maxHz; fd += stepHz)
			out[k++] = fd;
		return out;
	}

	static void ifft(float[] re, float[] im, int n)
	{
		for (int i = 0; i < n; i++)
			im[i] = -im[i];
		AudioSpectrum.fft(re, im, n);
		float s = 1f / n;
		for (int i = 0; i < n; i++)
		{
			re[i] *= s;
			im[i] = -im[i] * s;
		}
	}

	/** int8 IQ of one C/A millisecond (tests). */
	public static byte[] syntheticMs(int prn, int dopplerHz, int codeShift, float amp)
	{
		return syntheticMs(prn, dopplerHz, codeShift, amp, 0.22f);
	}

	public static byte[] syntheticMs(int prn, int dopplerHz, int codeShift, float amp, float noise)
	{
		float[] chips = GpsCaCode.resample(GpsCaCode.chips(prn), SAMPLES_MS);
		byte[] iq = new byte[SAMPLES_MS * 2];
		double w = 2.0 * Math.PI * dopplerHz / SAMPLE_RATE;
		float a = amp <= 0 ? 0.35f : Math.min(0.95f, amp);
		int shift = codeShift % SAMPLES_MS;
		if (shift < 0)
			shift += SAMPLES_MS;
		java.util.Random rng = new java.util.Random(1L);
		for (int n = 0; n < SAMPLES_MS; n++)
		{
			float chip = chips[(n + shift) % SAMPLES_MS];
			double c = Math.cos(w * n);
			double s = Math.sin(w * n);
			float ni = noise > 0 ? (float) (rng.nextGaussian() * noise) : 0f;
			float nq = noise > 0 ? (float) (rng.nextGaussian() * noise) : 0f;
			int ii = Math.round((chip * a * (float) c + ni) * 127f);
			int qq = Math.round((chip * a * (float) s + nq) * 127f);
			iq[n * 2] = (byte) Math.max(-127, Math.min(127, ii));
			iq[n * 2 + 1] = (byte) Math.max(-127, Math.min(127, qq));
		}
		return iq;
	}
}
