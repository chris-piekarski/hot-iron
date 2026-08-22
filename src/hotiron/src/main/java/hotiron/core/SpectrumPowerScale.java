package hotiron.core;

import java.util.Arrays;

/**
 * Live dB window for the spectrum Y-axis. A single −110…+20 scale is too
 * tall for typical Quick Select views, so FM/Wi-Fi peaks look flat.
 * Unfilled hop holes stay at the dataset init value and are ignored.
 */
public final class SpectrumPowerScale
{
	public static final float DEFAULT_LOW = -100f;
	public static final float DEFAULT_HIGH = 20f;
	public static final float EMPTY_CEILING = -140f;
	public static final float MIN_SPAN_DB = 40f;
	/** Headroom above the peak / below the noise so the trace is not against the ticks. */
	public static final float PAD_DB = 10f;
	/** Display edges lock to multiples of 10 dB. */
	public static final float TICK_DB = 10f;
	/**
	 * Extra dB the padded target must overshoot the display edge before
	 * the axis expands. Combined with {@link #PAD_DB} this keeps a 10 dB
	 * tick from hopping on ordinary wobble.
	 */
	public static final float EXPAND_HYSTERESIS_DB = 10f;
	/** Spare room required before the axis is allowed to shrink one tick. */
	public static final float SHRINK_SLACK_DB = 25f;
	/** Minimum time between shrinks (expand is immediate). */
	public static final long SHRINK_HOLD_MS = 3000L;

	public final float lowDb;
	public final float highDb;
	final long lastAdjustMs;
	/** Loudest padded high seen since the last display change. */
	final float watchHighDb;
	/** Quietest padded low seen since the last display change. */
	final float watchLowDb;

	public SpectrumPowerScale(float lowDb, float highDb)
	{
		this(lowDb, highDb, 0L);
	}

	public SpectrumPowerScale(float lowDb, float highDb, long lastAdjustMs)
	{
		this(lowDb, highDb, lastAdjustMs, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
	}

	SpectrumPowerScale(float lowDb, float highDb, long lastAdjustMs, float watchHighDb, float watchLowDb)
	{
		this.lowDb = lowDb;
		this.highDb = highDb;
		this.lastAdjustMs = lastAdjustMs;
		this.watchHighDb = watchHighDb;
		this.watchLowDb = watchLowDb;
	}

	public double span()
	{
		return highDb - lowDb;
	}

	/** Fixed operator window (−100…+20), also used when the dataset is empty. */
	public boolean isUnset()
	{
		return lowDb == DEFAULT_LOW && highDb == DEFAULT_HIGH;
	}

	public static SpectrumPowerScale defaults()
	{
		return new SpectrumPowerScale(DEFAULT_LOW, DEFAULT_HIGH);
	}

	/**
	 * Noise floor from the 10th percentile of filled bins; top from the
	 * strongest filled bin so real peaks set the ceiling. Hop holes at
	 * −150 dB are skipped.
	 */
	public static SpectrumPowerScale fromDataset(DatasetSpectrum dataset)
	{
		if (dataset == null)
			return defaults();
		int n = dataset.spectrumLength();
		if (n == 0)
			return defaults();
		float[] filled = new float[n];
		int count = 0;
		for (int i = 0; i < n; i++)
		{
			float p = dataset.getPower(i);
			if (p <= EMPTY_CEILING || !Float.isFinite(p))
				continue;
			filled[count++] = p;
		}
		if (count < 4)
			return defaults();
		Arrays.sort(filled, 0, count);
		float noise = filled[(int) Math.floor(0.10 * (count - 1))];
		float peak = filled[count - 1];
		if (peak < noise)
			return defaults();
		float lo = noise - PAD_DB;
		float hi = peak + PAD_DB;
		if (hi - lo < MIN_SPAN_DB)
		{
			float mid = (hi + lo) / 2f;
			lo = mid - MIN_SPAN_DB / 2f;
			hi = mid + MIN_SPAN_DB / 2f;
		}
		return new SpectrumPowerScale(lo, hi);
	}

	public SpectrumPowerScale blendToward(SpectrumPowerScale target, float alpha)
	{
		if (target == null)
			return this;
		float a = Math.max(0f, Math.min(1f, alpha));
		return new SpectrumPowerScale(lowDb + (target.lowDb - lowDb) * a,
				highDb + (target.highDb - highDb) * a);
	}

	/**
	 * Operator-friendly follow: hold the current 10 dB ticks through small
	 * wobble, jump immediately if a peak/noise would clip, and shrink at
	 * most one tick per {@link #SHRINK_HOLD_MS} when there is lots of spare
	 * room.
	 */
	public SpectrumPowerScale follow(SpectrumPowerScale target)
	{
		return follow(target, System.currentTimeMillis());
	}

	public SpectrumPowerScale follow(SpectrumPowerScale target, long nowMs)
	{
		if (target == null || target.isUnset())
			return this;
		if (isUnset())
			return target.displayTicks().watching(target).stamped(nowMs);
		float watchH = Math.max(watchHighDb, target.highDb);
		float watchL = Math.min(watchLowDb, target.lowDb);
		boolean clipHigh = target.highDb > highDb + EXPAND_HYSTERESIS_DB;
		boolean clipLow = target.lowDb < lowDb - EXPAND_HYSTERESIS_DB;
		if (clipHigh || clipLow)
		{
			float lo = Math.min(lowDb, target.lowDb);
			float hi = Math.max(highDb, target.highDb);
			return new SpectrumPowerScale(lo, hi, nowMs, target.highDb, target.lowDb).ensureMinSpan()
					.displayTicks();
		}
		if (nowMs - lastAdjustMs < SHRINK_HOLD_MS)
			return new SpectrumPowerScale(lowDb, highDb, lastAdjustMs, watchH, watchL);
		// Shrink only if the whole hold window stayed well inside the ticks.
		// A single Wi-Fi packet during the window keeps the ceiling put.
		boolean shrinkHigh = watchH < highDb - SHRINK_SLACK_DB;
		boolean shrinkLow = watchL > lowDb + SHRINK_SLACK_DB;
		if (!shrinkHigh && !shrinkLow)
			return new SpectrumPowerScale(lowDb, highDb, nowMs, target.highDb, target.lowDb);
		float lo = lowDb;
		float hi = highDb;
		if (shrinkHigh)
			hi = highDb - TICK_DB;
		if (shrinkLow)
			lo = lowDb + TICK_DB;
		SpectrumPowerScale next = new SpectrumPowerScale(lo, hi, nowMs, target.highDb, target.lowDb)
				.ensureMinSpan().displayTicks();
		if (next.lowDb == lowDb && next.highDb == highDb)
			return new SpectrumPowerScale(lowDb, highDb, nowMs, target.highDb, target.lowDb);
		return next;
	}

	public SpectrumPowerScale stamped(long nowMs)
	{
		return new SpectrumPowerScale(lowDb, highDb, nowMs, watchHighDb, watchLowDb);
	}

	SpectrumPowerScale watching(SpectrumPowerScale target)
	{
		if (target == null)
			return this;
		return new SpectrumPowerScale(lowDb, highDb, lastAdjustMs, target.highDb, target.lowDb);
	}

	public SpectrumPowerScale displayTicks()
	{
		float lo = (float) (Math.floor(lowDb / TICK_DB) * TICK_DB);
		float hi = (float) (Math.ceil(highDb / TICK_DB) * TICK_DB);
		if (hi - lo < MIN_SPAN_DB)
			hi = lo + MIN_SPAN_DB;
		return new SpectrumPowerScale(lo, hi, lastAdjustMs, watchHighDb, watchLowDb);
	}

	public boolean sameDisplayAs(SpectrumPowerScale other)
	{
		if (other == null)
			return false;
		SpectrumPowerScale a = displayTicks();
		SpectrumPowerScale b = other.displayTicks();
		return a.lowDb == b.lowDb && a.highDb == b.highDb;
	}

	SpectrumPowerScale ensureMinSpan()
	{
		if (highDb - lowDb >= MIN_SPAN_DB)
			return this;
		float mid = (highDb + lowDb) / 2f;
		return new SpectrumPowerScale(mid - MIN_SPAN_DB / 2f, mid + MIN_SPAN_DB / 2f, lastAdjustMs, watchHighDb,
				watchLowDb);
	}

	@Override
	public String toString()
	{
		return lowDb + "…" + highDb + " dB";
	}
}
