package hotiron.core;

/**
 * Operator AGC for the HackRF: keep the live peak in a comfortable
 * window so the spectrum and waterfall are neither all-blue (too little
 * gain / ADC mush) nor all-red (clipping). LNA is filled first
 * ({@link GainPolicy}); the RF amp is never touched.
 */
public final class AutoGainPolicy
{
	/** Displayed peak we aim for after LNA+VGA. */
	public static final float TARGET_PEAK_DBM = -28f;
	/** Do not raise gain while the peak-hold is at or above this. */
	public static final float HOLD_LOW_DBM = -42f;
	/** Ease gain down if the peak-hold sits above this (not yet clipping). */
	public static final float HOLD_HIGH_DBM = -16f;
	/** Instant peak at or above this is treated as ADC clip. */
	public static final float CLIP_DBM = -8f;
	public static final float HARD_CLIP_DBM = -2f;
	public static final long SETTLE_MS = 2500L;
	/** Consecutive ~30 fps frames of sustained hot before easing down (not a Wi-Fi flash). */
	public static final int SUSTAINED_HOT_FRAMES = 12;
	/** Consecutive frames at {@link #HARD_CLIP_DBM} before dropping during settle. */
	public static final int HARD_CLIP_FRAMES = 3;
	/** Peak-hold half-life so a Wi-Fi packet is remembered through a quiet gap. */
	public static final double PEAK_HOLD_HALF_LIFE_SEC = 4.0;
	public static final int MIN_FILLED_BINS = 8;
	/** Mid-frequency jump that counts as a new Quick Select / band. */
	public static final int BAND_SHIFT_MHZ = 40;

	private AutoGainPolicy()
	{
	}

	public static final class Observation
	{
		public final float peakDbm;
		public final float noiseDbm;
		public final int filledBins;
		public final int currentGain;
		public final int startMHz;
		public final int endMHz;

		public Observation(float peakDbm, float noiseDbm, int filledBins, int currentGain, int startMHz, int endMHz)
		{
			this.peakDbm = peakDbm;
			this.noiseDbm = noiseDbm;
			this.filledBins = filledBins;
			this.currentGain = currentGain;
			this.startMHz = startMHz;
			this.endMHz = endMHz;
		}

		public boolean usable()
		{
			return filledBins >= MIN_FILLED_BINS && Float.isFinite(peakDbm);
		}
	}

	/**
	 * Conservative seed per band. Live data takes over after one settle.
	 * 2.4 GHz starts lower (local APs run hot); VHF/FM starts higher.
	 */
	public static int seedGain(int startMHz, int endMHz)
	{
		int mid = startMHz / 2 + endMHz / 2;
		if (mid < 30)
			return GainPolicy.clampTotal(56);
		if (mid < 300)
			return GainPolicy.clampTotal(48);
		if (mid < 1000)
			return GainPolicy.clampTotal(40);
		if (mid < 3000)
			return GainPolicy.clampTotal(32);
		return GainPolicy.clampTotal(40);
	}

	public static boolean bandShifted(int start0, int end0, int start1, int end1)
	{
		int mid0 = start0 / 2 + end0 / 2;
		int mid1 = start1 / 2 + end1 / 2;
		return Math.abs(mid0 - mid1) >= BAND_SHIFT_MHZ;
	}

	public static Observation observe(DatasetSpectrum ds, int currentGain, int startMHz, int endMHz)
	{
		if (ds == null)
			return new Observation(Float.NaN, Float.NaN, 0, currentGain, startMHz, endMHz);
		int n = ds.spectrumLength();
		float peak = Float.NEGATIVE_INFINITY;
		int filled = 0;
		float[] buf = new float[n];
		for (int i = 0; i < n; i++)
		{
			float p = ds.getPower(i);
			if (DatasetSpectrum.isChartHole(p))
				continue;
			buf[filled++] = p;
			if (p > peak)
				peak = p;
		}
		if (filled < MIN_FILLED_BINS)
			return new Observation(Float.NaN, Float.NaN, filled, currentGain, startMHz, endMHz);
		java.util.Arrays.sort(buf, 0, filled);
		float noise = buf[(int) Math.floor(0.10 * (filled - 1))];
		return new Observation(peak, noise, filled, currentGain, startMHz, endMHz);
	}

	/**
	 * One-shot gain step. {@code peakHold} (burst memory) only blocks a
	 * raise. Lowering uses {@code peakNow}: clip immediately, otherwise
	 * stay. A remembered Wi-Fi packet must not yank gain back down.
	 */
	public static int decide(int currentGain, float peakNow, float peakHold, float noiseDbm)
	{
		int gain = GainPolicy.clampTotal(currentGain);
		if (!Float.isFinite(peakNow))
			return gain;
		if (peakNow >= HARD_CLIP_DBM)
			return GainPolicy.clampTotal(gain - 8);
		float quiet = Float.isFinite(peakHold) ? peakHold : peakNow;
		if (quiet < HOLD_LOW_DBM)
			return GainPolicy.clampTotal(gain + Math.min(8, stepForError(TARGET_PEAK_DBM - quiet)));
		return gain;
	}

	/**
	 * After a raise, if the peak rose far less than the gain we added we
	 * are compressing — back off. A peak that dropped (burst gone) is not
	 * compression and must not reverse the raise.
	 */
	public static int afterRaise(int currentGain, int dGain, float dPeak)
	{
		int gain = GainPolicy.clampTotal(currentGain);
		if (dGain < 8 || !Float.isFinite(dPeak) || dPeak < 0f)
			return gain;
		if (dPeak < dGain * 0.4f)
			return GainPolicy.clampTotal(gain - Math.max(8, dGain / 2));
		return gain;
	}

	public static int stepForError(float errDb)
	{
		if (!(errDb > 0) || !Float.isFinite(errDb))
			return 0;
		if (errDb >= 24)
			return 24;
		if (errDb >= 16)
			return 16;
		if (errDb >= 8)
			return 8;
		if (errDb >= 4)
			return 4;
		return 0;
	}

	public static float decayPeakHold(float hold, float peakNow, double dtSec)
	{
		if (!Float.isFinite(hold) || peakNow > hold)
			return peakNow;
		if (!(dtSec > 0))
			return hold;
		double keep = Math.pow(0.5, dtSec / PEAK_HOLD_HALF_LIFE_SEC);
		return (float) (peakNow + (hold - peakNow) * keep);
	}

	/**
	 * Stateful loop: seed on band change, wait for the radio after each
	 * apply, remember bursts so Wi-Fi quiet gaps do not pump gain up.
	 */
	public static final class Loop
	{
		int lastStartMHz = Integer.MIN_VALUE;
		int lastEndMHz = Integer.MIN_VALUE;
		long settleUntilMs;
		int lastAppliedGain = -1;
		float peakHold = Float.NaN;
		long lastHoldMs;
		int hotFrames;
		int hardClipFrames;

		public void reset()
		{
			lastStartMHz = Integer.MIN_VALUE;
			lastEndMHz = Integer.MIN_VALUE;
			settleUntilMs = 0;
			lastAppliedGain = -1;
			peakHold = Float.NaN;
			lastHoldMs = 0;
			hotFrames = 0;
			hardClipFrames = 0;
		}

		public void markSettling(long nowMs)
		{
			settleUntilMs = nowMs + SETTLE_MS;
		}

		/**
		 * If the new window is a different band, return the seed gain
		 * (or null if already there). Small zooms inside a band return null.
		 */
		public Integer seedIfBandShifted(int startMHz, int endMHz, int currentGain)
		{
			if (lastStartMHz != Integer.MIN_VALUE && !bandShifted(lastStartMHz, lastEndMHz, startMHz, endMHz))
				return null;
			lastStartMHz = startMHz;
			lastEndMHz = endMHz;
			int seed = seedGain(startMHz, endMHz);
			lastAppliedGain = seed;
			peakHold = Float.NaN;
			hotFrames = 0;
			hardClipFrames = 0;
			if (seed != GainPolicy.clampTotal(currentGain))
				return Integer.valueOf(seed);
			return null;
		}

		public Integer consider(Observation o, long nowMs)
		{
			if (o == null || !o.usable())
				return null;
			if (lastStartMHz == Integer.MIN_VALUE
					|| bandShifted(lastStartMHz, lastEndMHz, o.startMHz, o.endMHz))
			{
				Integer seed = seedIfBandShifted(o.startMHz, o.endMHz, o.currentGain);
				if (seed != null)
					return seed;
				if (settleUntilMs == 0)
					settleUntilMs = nowMs + SETTLE_MS;
			}
			double dt = lastHoldMs <= 0 ? 0 : Math.max(0, (nowMs - lastHoldMs) / 1000.0);
			peakHold = decayPeakHold(peakHold, o.peakDbm, dt);
			lastHoldMs = nowMs;

			if (o.peakDbm >= HARD_CLIP_DBM)
				hardClipFrames++;
			else
				hardClipFrames = 0;
			boolean hardClip = hardClipFrames >= HARD_CLIP_FRAMES;
			if (!hardClip && nowMs < settleUntilMs)
				return null;

			int gain = GainPolicy.clampTotal(o.currentGain);
			if (o.peakDbm > HOLD_HIGH_DBM && o.peakDbm < HARD_CLIP_DBM)
				hotFrames++;
			else if (o.peakDbm <= HOLD_HIGH_DBM)
				hotFrames = 0;

			int next = decide(gain, o.peakDbm, peakHold, o.noiseDbm);
			if (next >= gain && hotFrames >= SUSTAINED_HOT_FRAMES)
				next = GainPolicy.clampTotal(gain - 8);
			if (next == gain)
				return null;
			hotFrames = 0;
			hardClipFrames = 0;
			lastAppliedGain = next;
			return Integer.valueOf(next);
		}
	}
}
