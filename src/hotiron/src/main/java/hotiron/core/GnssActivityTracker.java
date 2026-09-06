package hotiron.core;

import java.util.function.LongSupplier;

/**
 * Temporal L1 occupancy. A one-sweep flash is not labeled.
 */
public final class GnssActivityTracker
{
	public static final float ATTACK_PER_SEC = 1.4f;
	public static final float DECAY_PER_SEC = 0.45f;
	public static final float SHOW_AT = 0.50f;
	public static final float DROP_AT = 0.05f;
	static final double MAX_STEP_SEC = 0.25;

	private final LongSupplier clockMs;
	private float confidence;
	private long lastUpdateMs;
	private GnssActivity last = GnssActivity.hidden();

	public GnssActivityTracker()
	{
		this(System::currentTimeMillis);
	}

	public GnssActivityTracker(LongSupplier clockMs)
	{
		this.clockMs = clockMs == null ? System::currentTimeMillis : clockMs;
	}

	public synchronized void reset()
	{
		confidence = 0f;
		lastUpdateMs = 0;
		last = GnssActivity.hidden();
	}

	public synchronized GnssActivity last()
	{
		return last;
	}

	public synchronized GnssActivity update(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		return update(ds, startMHz, endMHz, clockMs.getAsLong());
	}

	public synchronized GnssActivity update(DatasetSpectrum ds, double startMHz, double endMHz, long nowMs)
	{
		if (!GnssBandPlan.viewIsGnss(startMHz, endMHz))
		{
			last = GnssActivity.hidden();
			return last;
		}
		GnssBandPlan.GnssObservation obs = GnssBandPlan.observe(ds, startMHz, endMHz);
		double dt = stepSec(lastUpdateMs, nowMs);
		lastUpdateMs = nowMs;
		if (obs.any())
			confidence = clamp01(confidence + (float) (dt * ATTACK_PER_SEC));
		else
			confidence = clamp01(confidence - (float) (dt * DECAY_PER_SEC));
		boolean show = confidence >= SHOW_AT;
		if (confidence < DROP_AT)
		{
			last = GnssActivity.quiet();
			return last;
		}
		last = new GnssActivity(show && obs.gpsL1, show && obs.bdsB1, show && obs.gloL1, obs.gpsL1Dbm,
				obs.bdsB1Dbm, obs.gloL1Dbm, obs.noiseDbm, confidence, true);
		return last;
	}

	private static double stepSec(long lastMs, long nowMs)
	{
		if (lastMs <= 0)
			return 0.05;
		double dt = (nowMs - lastMs) / 1000.0;
		if (dt < 0)
			return 0;
		return Math.min(dt, MAX_STEP_SEC);
	}

	private static float clamp01(float v)
	{
		if (v < 0f)
			return 0f;
		if (v > 1f)
			return 1f;
		return v;
	}
}
