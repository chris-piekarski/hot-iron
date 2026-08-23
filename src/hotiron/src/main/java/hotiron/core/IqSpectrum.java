package hotiron.core;

import java.util.Arrays;

/**
 * Real-time FFT of parked int8 IQ (FM Listen, TV Watch, or NFC sniff). Hann-windowed 1024-point
 * frames, fftshifted so bin 0 is −fs/2. Power in dBFS.
 */
public final class IqSpectrum
{
	public static final int FFT_N = 1024;
	public static final int SAMPLE_RATE = TvChannelPlan.IQ_RATE_HZ;
	public static final float BIN_HZ = (float) SAMPLE_RATE / FFT_N;
	/** Full captured span (Hz), −fs/2 … +fs/2. */
	public static final float DISPLAY_HZ = SAMPLE_RATE;

	private final int sampleRate;
	private final float binHz;
	private final float displayHz;
	private final float[] window = new float[FFT_N];
	private final float[] re = new float[FFT_N];
	private final float[] im = new float[FFT_N];
	private final float[] db = new float[FFT_N];
	private boolean primed;

	public IqSpectrum()
	{
		this(SAMPLE_RATE);
	}

	public IqSpectrum(int sampleRate)
	{
		this.sampleRate = sampleRate > 0 ? sampleRate : SAMPLE_RATE;
		this.binHz = (float) this.sampleRate / FFT_N;
		this.displayHz = this.sampleRate;
		double wsum = 0;
		for (int i = 0; i < FFT_N; i++)
		{
			window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (FFT_N - 1)));
			wsum += window[i];
		}
		float scale = (float) (2.0 / wsum);
		for (int i = 0; i < FFT_N; i++)
			window[i] *= scale;
		Arrays.fill(db, -120f);
	}

	public int binCount()
	{
		return FFT_N;
	}

	public float binHz()
	{
		return binHz;
	}

	public int sampleRate()
	{
		return sampleRate;
	}

	public float displayHz()
	{
		return displayHz;
	}

	public synchronized void reset()
	{
		primed = false;
		Arrays.fill(db, -120f);
	}

	public boolean hasFrame()
	{
		return primed;
	}

	/**
	 * One FFT from the tail of this IQ chunk. Returns a copy of the
	 * dB row (fftshifted), or {@code null} if there are too few samples.
	 */
	public synchronized float[] accept(byte[] iq, int nbytes)
	{
		if (iq == null)
			return null;
		int n = Math.min(nbytes, iq.length) & ~1;
		int pairs = n / 2;
		if (pairs < FFT_N)
			return null;
		int start = pairs - FFT_N;
		for (int i = 0; i < FFT_N; i++)
		{
			int o = (start + i) * 2;
			re[i] = (iq[o] / 128f) * window[i];
			im[i] = (iq[o + 1] / 128f) * window[i];
		}
		AudioSpectrum.fft(re, im, FFT_N);
		int half = FFT_N / 2;
		for (int k = 0; k < FFT_N; k++)
		{
			int src = (k + half) % FFT_N;
			double mag = Math.hypot(re[src], im[src]);
			db[k] = (float) (20.0 * Math.log10(mag + 1e-12));
		}
		primed = true;
		return db.clone();
	}

	public synchronized float[] latestDb()
	{
		return db.clone();
	}

	/** fftshifted bin index for a baseband frequency (Hz). */
	public static int binForHz(double hz)
	{
		int bin = (int) Math.round(hz / BIN_HZ) + FFT_N / 2;
		if (bin < 0)
			return 0;
		if (bin >= FFT_N)
			return FFT_N - 1;
		return bin;
	}

	public int binForFrequencyHz(double hz)
	{
		int bin = (int) Math.round(hz / binHz) + FFT_N / 2;
		if (bin < 0)
			return 0;
		if (bin >= FFT_N)
			return FFT_N - 1;
		return bin;
	}
}
