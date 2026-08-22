package hotiron.core;

/**
 * Valid sample counts for one native sweep tuning dwell.
 */
public final class SweepSamples
{
	public static final int SAMPLES_PER_BLOCK = 8192;
	public static final int MAX_SAMPLES = 262144;

	private SweepSamples()
	{
	}

	public static int requireValid(int samples)
	{
		if (samples < SAMPLES_PER_BLOCK || samples > MAX_SAMPLES || samples % SAMPLES_PER_BLOCK != 0)
			throw new IllegalArgumentException("samples must be a multiple of " + SAMPLES_PER_BLOCK + " between "
					+ SAMPLES_PER_BLOCK + " and " + MAX_SAMPLES + ": " + samples);
		return samples;
	}

	public static int blocks(int samples)
	{
		return requireValid(samples) / SAMPLES_PER_BLOCK;
	}
}
