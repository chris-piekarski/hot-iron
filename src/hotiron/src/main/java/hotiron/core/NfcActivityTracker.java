package hotiron.core;

import java.util.ArrayDeque;
import java.util.function.LongSupplier;

/**
 * Temporal NFC classifier. A one-sweep flash is not labeled; on/off
 * history over ~2–8 s distinguishes a door-reader field from polling
 * and Morse-like HiFER CW. Does not decode text.
 */
public final class NfcActivityTracker
{
	public static final float ATTACK_PER_SEC = 1.4f;
	public static final float DECAY_PER_SEC = 0.45f;
	public static final float SHOW_AT = 0.50f;
	public static final float DROP_AT = 0.05f;
	static final double MAX_STEP_SEC = 0.25;
	static final long HISTORY_MS = 8000L;

	private final LongSupplier clockMs;
	private final ArrayDeque<Sample> samples = new ArrayDeque<>();
	private float confidence;
	private long lastUpdateMs;
	private NfcActivity last = NfcActivity.quiet();

	public NfcActivityTracker()
	{
		this(System::currentTimeMillis);
	}

	public NfcActivityTracker(LongSupplier clockMs)
	{
		this.clockMs = clockMs == null ? System::currentTimeMillis : clockMs;
	}

	public synchronized void reset()
	{
		samples.clear();
		confidence = 0f;
		lastUpdateMs = 0;
		last = NfcActivity.quiet();
	}

	public synchronized NfcActivity last()
	{
		return last;
	}

	public synchronized NfcActivity update(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		return update(ds, startMHz, endMHz, clockMs.getAsLong());
	}

	public synchronized NfcActivity update(DatasetSpectrum ds, double startMHz, double endMHz, long nowMs)
	{
		boolean visible = NfcBandPlan.viewIsNfc(startMHz, endMHz);
		if (!visible)
		{
			last = NfcActivity.hidden();
			return last;
		}
		NfcObservation obs = NfcBandPlan.observe(ds, startMHz, endMHz);
		double dt = stepSec(lastUpdateMs, nowMs);
		lastUpdateMs = nowMs;
		if (obs.anyEnergy())
			confidence = clamp01(confidence + (float) (dt * ATTACK_PER_SEC));
		else
			confidence = clamp01(confidence - (float) (dt * DECAY_PER_SEC));
		samples.addLast(new Sample(nowMs, obs.carrierOn || obs.harmonic2 || obs.harmonic3));
		while (!samples.isEmpty() && nowMs - samples.peekFirst().tMs > HISTORY_MS)
			samples.removeFirst();
		Keying key = keying(nowMs);
		NfcActivity.Kind kind = classify(obs, key, confidence);
		if (confidence < SHOW_AT && kind != NfcActivity.Kind.QUIET)
			kind = NfcActivity.Kind.QUIET;
		if (confidence < DROP_AT)
		{
			last = NfcActivity.quietVisible();
			return last;
		}
		float pollHz = 0f;
		if (key.periodMs > 20f && key.periodMs < 2000f)
			pollHz = 1000f / key.periodMs;
		last = new NfcActivity(kind, obs.carrierDbm, obs.carrierMhz, key.duty, key.onMs, key.offMs, pollHz,
				confidence, obs.sidebandAb, obs.sidebandF, obs.sidebandV, obs.harmonic2, obs.harmonic3, true);
		return last;
	}

	private NfcActivity.Kind classify(NfcObservation obs, Keying key, float conf)
	{
		if (conf < DROP_AT && !obs.anyEnergy())
			return NfcActivity.Kind.QUIET;
		if (obs.sidebandAb)
			return NfcActivity.Kind.NFC_AB;
		if (obs.sidebandF)
			return NfcActivity.Kind.NFC_F;
		if (obs.sidebandV)
			return NfcActivity.Kind.NFC_V;
		if (!obs.carrierOn && (obs.harmonic2 || obs.harmonic3))
			return NfcActivity.Kind.FIELD_ON;
		if (!obs.carrierOn && !obs.anyEnergy())
			return NfcActivity.Kind.QUIET;
		if (key.duty >= 0.85f)
			return NfcActivity.Kind.FIELD_ON;
		boolean keyed = key.transitions >= 3 && key.onMs >= 40f && key.onMs <= 900f && key.offMs >= 40f
				&& key.offMs <= 1500f;
		if (keyed && key.duty >= 0.05f && key.duty <= 0.45f && key.periodMs >= 80f && key.periodMs <= 700f
				&& key.regular)
			return NfcActivity.Kind.POLLING;
		if (keyed && obs.narrowCarrier)
			return NfcActivity.Kind.HIFER;
		if (keyed)
			return NfcActivity.Kind.CW;
		if (obs.carrierOn)
			return NfcActivity.Kind.UNKNOWN;
		return NfcActivity.Kind.QUIET;
	}

	private Keying keying(long nowMs)
	{
		if (samples.size() < 3)
			return new Keying(0f, Float.NaN, Float.NaN, 0f, 0, false);
		long t0 = samples.peekFirst().tMs;
		long span = Math.max(1L, nowMs - t0);
		long on = 0;
		Sample prev = null;
		int transitions = 0;
		ArrayDeque<Long> ons = new ArrayDeque<>();
		ArrayDeque<Long> offs = new ArrayDeque<>();
		long runStart = t0;
		for (Sample s : samples)
		{
			if (prev != null)
			{
				long dt = Math.max(0L, s.tMs - prev.tMs);
				if (prev.on)
					on += dt;
				if (prev.on != s.on)
				{
					transitions++;
					long run = Math.max(1L, s.tMs - runStart);
					if (prev.on)
						ons.addLast(run);
					else
						offs.addLast(run);
					runStart = s.tMs;
				}
			}
			prev = s;
		}
		if (prev != null)
		{
			long dt = Math.max(0L, nowMs - prev.tMs);
			if (prev.on)
				on += dt;
		}
		float duty = on / (float) span;
		float onMs = median(ons);
		float offMs = median(offs);
		float period = (Float.isFinite(onMs) && Float.isFinite(offMs)) ? onMs + offMs : Float.NaN;
		boolean regular = regular(ons) && regular(offs);
		return new Keying(duty, onMs, offMs, period, transitions, regular);
	}

	private static float median(ArrayDeque<Long> vals)
	{
		if (vals.isEmpty())
			return Float.NaN;
		long[] a = new long[vals.size()];
		int i = 0;
		for (Long v : vals)
			a[i++] = v.longValue();
		java.util.Arrays.sort(a);
		return a[a.length / 2];
	}

	private static boolean regular(ArrayDeque<Long> vals)
	{
		if (vals.size() < 2)
			return false;
		float med = median(vals);
		if (!(med > 0))
			return false;
		int ok = 0;
		for (Long v : vals)
		{
			if (Math.abs(v.longValue() - med) <= 0.35f * med + 25f)
				ok++;
		}
		return ok >= vals.size() - 1;
	}

	private static double stepSec(long lastMs, long nowMs)
	{
		if (lastMs <= 0)
			return 0.02;
		double dt = (nowMs - lastMs) / 1000.0;
		if (dt <= 0)
			return 0.02;
		return Math.min(MAX_STEP_SEC, dt);
	}

	private static float clamp01(float v)
	{
		return v < 0f ? 0f : v > 1f ? 1f : v;
	}

	private static final class Sample
	{
		final long tMs;
		final boolean on;

		Sample(long tMs, boolean on)
		{
			this.tMs = tMs;
			this.on = on;
		}
	}

	private static final class Keying
	{
		final float duty;
		final float onMs;
		final float offMs;
		final float periodMs;
		final int transitions;
		final boolean regular;

		Keying(float duty, float onMs, float offMs, float periodMs, int transitions, boolean regular)
		{
			this.duty = duty;
			this.onMs = onMs;
			this.offMs = offMs;
			this.periodMs = periodMs;
			this.transitions = transitions;
			this.regular = regular;
		}
	}
}
