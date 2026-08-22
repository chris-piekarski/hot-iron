package hotiron.core;

import java.util.Arrays;

/**
 * Real-time audio FFT of 48 kHz mono PCM. Hann-windowed 1024-point
 * frames, 50% hop, power in dBFS for bins up to {@link #DISPLAY_HZ}.
 */
public final class AudioSpectrum
{
	public static final int FFT_N = 1024;
	public static final int HOP = FFT_N / 2;
	public static final int SAMPLE_RATE = WfmDemodulator.AUDIO_RATE_HZ;
	public static final float DISPLAY_HZ = 16_000f;
	public static final float BIN_HZ = (float) SAMPLE_RATE / FFT_N;

	private final float[] window = new float[FFT_N];
	private final float[] acc = new float[FFT_N];
	private final float[] re = new float[FFT_N];
	private final float[] im = new float[FFT_N];
	private final int bins;
	private final float[] db;
	private int filled;
	private boolean primed;

	public AudioSpectrum()
	{
		bins = Math.min(FFT_N / 2, (int) Math.floor(DISPLAY_HZ / BIN_HZ) + 1);
		db = new float[bins];
		double wsum = 0;
		for (int i = 0; i < FFT_N; i++)
		{
			window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (FFT_N - 1)));
			wsum += window[i];
		}
		float scale = (float) (2.0 / wsum);
		for (int i = 0; i < FFT_N; i++)
			window[i] *= scale;
	}

	public int binCount()
	{
		return bins;
	}

	public float binHz()
	{
		return BIN_HZ;
	}

	public float displayHz()
	{
		return DISPLAY_HZ;
	}

	/**
	 * Append PCM. Returns a copy of the latest dB row when a new FFT
	 * completes, otherwise {@code null}.
	 */
	public synchronized void reset()
	{
		filled = 0;
		primed = false;
		Arrays.fill(acc, 0);
		Arrays.fill(db, -120f);
	}

	public synchronized float[] accept(short[] pcm, int n)
	{
		if (pcm == null || n <= 0)
			return null;
		int limit = Math.min(n, pcm.length);
		float[] last = null;
		for (int i = 0; i < limit; i++)
		{
			if (filled < FFT_N)
				acc[filled++] = pcm[i] / 32768f;
			if (filled < FFT_N)
				continue;
			last = transform();
			System.arraycopy(acc, HOP, acc, 0, FFT_N - HOP);
			filled = FFT_N - HOP;
			primed = true;
		}
		return last;
	}

	public synchronized float[] latestDb()
	{
		return db.clone();
	}

	public boolean hasFrame()
	{
		return primed;
	}

	@FunctionalInterface
	public interface FrameListener
	{
		void onFrame(float[] db);
	}

	private float[] transform()
	{
		for (int i = 0; i < FFT_N; i++)
		{
			re[i] = acc[i] * window[i];
			im[i] = 0;
		}
		fft(re, im, FFT_N);
		for (int k = 0; k < bins; k++)
		{
			double mag = Math.hypot(re[k], im[k]);
			db[k] = (float) (20.0 * Math.log10(mag + 1e-12));
		}
		return db.clone();
	}

	/** In-place radix-2 DIT FFT. */
	static void fft(float[] re, float[] im, int n)
	{
		int j = 0;
		for (int i = 0; i < n; i++)
		{
			if (i < j)
			{
				float tr = re[j];
				re[j] = re[i];
				re[i] = tr;
				float ti = im[j];
				im[j] = im[i];
				im[i] = ti;
			}
			int m = n >> 1;
			while (m >= 1 && j >= m)
			{
				j -= m;
				m >>= 1;
			}
			j += m;
		}
		for (int len = 2; len <= n; len <<= 1)
		{
			double ang = -2 * Math.PI / len;
			float wr = (float) Math.cos(ang);
			float wi = (float) Math.sin(ang);
			for (int i = 0; i < n; i += len)
			{
				float ur = 1;
				float ui = 0;
				int half = len >> 1;
				for (int k = 0; k < half; k++)
				{
					int i0 = i + k;
					int i1 = i0 + half;
					float tr = ur * re[i1] - ui * im[i1];
					float ti = ur * im[i1] + ui * re[i1];
					re[i1] = re[i0] - tr;
					im[i1] = im[i0] - ti;
					re[i0] += tr;
					im[i0] += ti;
					float nr = ur * wr - ui * wi;
					ui = ur * wi + ui * wr;
					ur = nr;
				}
			}
		}
	}
}
