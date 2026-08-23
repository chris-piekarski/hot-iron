package hotiron.core;

import java.util.Arrays;

/**
 * Rolling envelope of the <em>13.56 MHz carrier</em>, not wideband |IQ|.
 * Mixes the parked +2 MHz IF to baseband, boxcar-decimates, then |z|.
 */
public final class NfcEnvelopeTrace
{
	public static final float IF_HZ = (float) (NfcBandPlan.CARRIER_MHZ * 1_000_000.0 - NfcSniffEngine.LO_HZ);
	public static final float SAMPLE_HZ = 20_000f;
	public static final float WINDOW_S = 0.500f;
	public static final int WINDOW_SAMPLES = 10_000;
	public static final float DECODER_FLOOR_LINEAR = 0.01f;
	public static final float DECODER_FLOOR_DB = -40f;
	public static final float EMPTY_DB = -150f;

	private final float[] ring = new float[WINDOW_SAMPLES];
	private int write;
	private int size;

	public NfcEnvelopeTrace()
	{
		reset();
	}

	public synchronized void reset()
	{
		Arrays.fill(ring, EMPTY_DB);
		write = 0;
		size = 0;
	}

	public synchronized void acceptIq(byte[] iq)
	{
		float[] chunk = mixCarrierDb(iq, NfcSniffEngine.IQ_RATE_HZ, IF_HZ, SAMPLE_HZ);
		for (int i = 0; i < chunk.length; i++)
		{
			ring[write] = chunk[i];
			write = (write + 1) % ring.length;
			if (size < ring.length)
				size++;
		}
	}

	/**
	 * Oldest-first, always {@link #WINDOW_SAMPLES}. Unfilled prefix is
	 * {@link #EMPTY_DB} so the time axis stays {@link #WINDOW_S}.
	 */
	public synchronized float[] snapshot()
	{
		float[] out = new float[ring.length];
		int pad = ring.length - size;
		Arrays.fill(out, 0, pad, EMPTY_DB);
		if (size == 0)
			return out;
		int start = (write - size + ring.length) % ring.length;
		for (int i = 0; i < size; i++)
			out[pad + i] = ring[(start + i) % ring.length];
		return out;
	}

	public synchronized int filled()
	{
		return size;
	}

	/**
	 * Newest live sample, or {@link #EMPTY_DB} if the snapshot is still pad.
	 */
	public static float latestLive(float[] db)
	{
		if (db == null)
			return EMPTY_DB;
		for (int i = db.length - 1; i >= 0; i--)
		{
			if (db[i] > EMPTY_DB + 1f)
				return db[i];
		}
		return EMPTY_DB;
	}

	/**
	 * Mix {@code ifHz} to DC, boxcar-decimate to {@code outHz}, return dBFS.
	 * Uses a rotating NCO (not per-sample {@code cos}/{@code sin}).
	 */
	public static float[] mixCarrierDb(byte[] iq, float inHz, float ifHz, float outHz)
	{
		if (iq == null || iq.length < 2 || !(inHz > 0) || !(outHz > 0))
			return new float[0];
		int pairs = iq.length / 2;
		int stride = Math.max(1, Math.round(inHz / outHz));
		int n = pairs / stride;
		if (n <= 0)
		{
			n = 1;
			stride = pairs;
		}
		double w = 2.0 * Math.PI * ifHz / inHz;
		double cr = Math.cos(w);
		double sr = Math.sin(w);
		double re = 1.0;
		double im = 0.0;
		float[] db = new float[n];
		int p = 0;
		for (int i = 0; i < n; i++)
		{
			double accI = 0;
			double accQ = 0;
			for (int k = 0; k < stride && p < pairs; k++, p++)
			{
				float iS = iq[2 * p] / 128.0f;
				float qS = iq[2 * p + 1] / 128.0f;
				accI += iS * re + qS * im;
				accQ += qS * re - iS * im;
				double nr = re * cr - im * sr;
				im = re * sr + im * cr;
				re = nr;
			}
			double osc = Math.hypot(re, im);
			if (osc > 0)
			{
				re /= osc;
				im /= osc;
			}
			float mag = (float) Math.hypot(accI / stride, accQ / stride);
			db[i] = (float) (20.0 * Math.log10(Math.max(mag, 1e-6f)));
		}
		return db;
	}

	/**
	 * Wideband |IQ| mean-decimate (tests / comparison). Not the 13.56 path.
	 */
	public static float[] decimateIq(byte[] iq, float inHz, float outHz)
	{
		if (iq == null || iq.length < 2 || !(inHz > 0) || !(outHz > 0))
			return new float[0];
		int pairs = iq.length / 2;
		int stride = Math.max(1, Math.round(inHz / outHz));
		int n = pairs / stride;
		if (n <= 0)
		{
			n = 1;
			stride = pairs;
		}
		float[] db = new float[n];
		for (int i = 0; i < n; i++)
		{
			int at = i * stride;
			double acc = 0;
			for (int k = 0; k < stride; k++)
			{
				int p = at + k;
				float iS = iq[2 * p] / 128.0f;
				float qS = iq[2 * p + 1] / 128.0f;
				acc += Math.sqrt(iS * iS + qS * qS);
			}
			float mag = (float) (acc / stride);
			db[i] = (float) (20.0 * Math.log10(Math.max(mag, 1e-6f)));
		}
		return db;
	}
}
