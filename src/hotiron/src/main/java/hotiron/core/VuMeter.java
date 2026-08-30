package hotiron.core;

/**
 * Peak-envelope meter with VU-ish ballistics. Incoming PCM (pre-volume)
 * maps −48…−6 dBFS onto 0…1 so a parked FM demod actually moves the
 * needle instead of sitting on a static Scan dBm.
 */
public final class VuMeter
{
	static final float FLOOR_DB = -48f;
	static final float SPAN_DB = 42f;
	static final float ATTACK_S = 0.02f;
	static final float RELEASE_S = 0.18f;
	static final float FULL_SCALE = 32768f;

	private float level;
	private long lastMs;

	public void reset()
	{
		level = 0f;
		lastMs = 0L;
	}

	public float getLevel()
	{
		return level;
	}

	public float accept(short[] pcm, int n, long nowMs)
	{
		float peak = 0f;
		if (pcm != null)
		{
			int cap = Math.min(n, pcm.length);
			for (int i = 0; i < cap; i++)
			{
				float a = pcm[i];
				if (a < 0)
					a = -a;
				if (a > peak)
					peak = a;
			}
		}
		return tick(fromPeak(peak), nowMs);
	}

	static float fromPeak(float peak)
	{
		if (peak <= 1f)
			return 0f;
		float db = (float) (20.0 * Math.log10(peak / FULL_SCALE));
		float t = (db - FLOOR_DB) / SPAN_DB;
		if (t < 0f)
			return 0f;
		if (t > 1f)
			return 1f;
		return t;
	}

	float tick(float instant, long nowMs)
	{
		if (lastMs == 0L)
		{
			lastMs = nowMs;
			level = instant;
			return level;
		}
		float dt = (nowMs - lastMs) / 1000f;
		lastMs = nowMs;
		if (dt < 0f)
			dt = 0f;
		if (dt > 0.25f)
			dt = 0.25f;
		float tau = instant >= level ? ATTACK_S : RELEASE_S;
		float a = 1f - (float) Math.exp(-dt / Math.max(1e-4f, tau));
		level += (instant - level) * a;
		return level;
	}
}
