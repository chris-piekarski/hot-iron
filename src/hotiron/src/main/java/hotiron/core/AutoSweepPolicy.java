package hotiron.core;

/**
 * Auto FFT Bin / samples from the operator sweep span. Bias is a snappy
 * waterfall: cap plotted bins, keep dwell at one hardware block, and only
 * step the discrete FFT list when the span has moved enough to leave a
 * hysteresis band (so pan / 1.5× zoom does not thrash USB).
 */
public final class AutoSweepPolicy
{
	/** Same choices as the FFT Bin spinner, finest first. */
	public static final int[] BIN_HZ = { 2445, 5_000, 10_000, 20_000, 50_000, 100_000, 200_000, 500_000, 1_000_000,
			2_000_000, 5_000_000 };

	/**
	 * Fresh pick: finest list entry whose dataset length is at most this.
	 * ~4k points keeps JFreeChart and the native FFT budget in the fast
	 * lane; zoomed-in windows still land on 2.4–10 kHz bins.
	 */
	public static final int TARGET_BINS = 4000;
	/** Keep the current bin while plotted length stays in this window. */
	public static final int KEEP_MIN_BINS = 1500;
	public static final int KEEP_MAX_BINS = 6000;
	/** Auto always uses one 8192-sample FFT block per hop. */
	public static final int SAMPLES = SweepSamples.SAMPLES_PER_BLOCK;

	private AutoSweepPolicy()
	{
	}

	public static final class Choice
	{
		public final int fftBinHz;
		public final int samples;

		public Choice(int fftBinHz, int samples)
		{
			this.fftBinHz = fftBinHz;
			this.samples = SweepSamples.requireValid(samples);
		}

		public boolean matches(int fftBinHz, int samples)
		{
			return this.fftBinHz == fftBinHz && this.samples == samples;
		}
	}

	public static String[] binLabels()
	{
		String[] labels = new String[BIN_HZ.length];
		for (int i = 0; i < BIN_HZ.length; i++)
			labels[i] = binLabel(BIN_HZ[i]);
		return labels;
	}

	public static String binLabel(int hz)
	{
		String digits = Integer.toString(hz);
		StringBuilder sb = new StringBuilder(digits.length() + 4);
		int n = digits.length();
		for (int i = 0; i < n; i++)
		{
			if (i > 0 && (n - i) % 3 == 0)
				sb.append(' ');
			sb.append(digits.charAt(i));
		}
		return sb.toString();
	}

	public static boolean isBinChoice(int hz)
	{
		for (int bin : BIN_HZ)
		{
			if (bin == hz)
				return true;
		}
		return false;
	}

	/**
	 * Dataset length using the same formula as {@link DatasetSpectrum}.
	 */
	public static int datasetBins(int spanMHz, int fftBinHz)
	{
		int span = Math.max(1, spanMHz);
		int bin = Math.max(1, fftBinHz);
		return (int) (Math.ceil(span) * 1_000_000d / bin);
	}

	/** Finest list bin whose dataset length is {@code <= TARGET_BINS}. */
	public static int chooseBinHz(int spanMHz)
	{
		for (int bin : BIN_HZ)
		{
			if (datasetBins(spanMHz, bin) <= TARGET_BINS)
				return bin;
		}
		return BIN_HZ[BIN_HZ.length - 1];
	}

	/**
	 * Keep {@code currentBinHz} while it still plots inside the hysteresis
	 * window; otherwise a fresh {@link #chooseBinHz(int)}.
	 */
	public static int chooseBinHz(int spanMHz, int currentBinHz)
	{
		if (isBinChoice(currentBinHz))
		{
			int n = datasetBins(spanMHz, currentBinHz);
			if (n >= KEEP_MIN_BINS && n <= KEEP_MAX_BINS)
				return currentBinHz;
		}
		return chooseBinHz(spanMHz);
	}

	public static Choice choose(int spanMHz, int currentBinHz)
	{
		return new Choice(chooseBinHz(spanMHz, currentBinHz), SAMPLES);
	}

	public static Choice choose(FrequencyRange range, int currentBinHz)
	{
		int span = range == null ? 1 : Math.max(1, range.spanMHz());
		return choose(span, currentBinHz);
	}

	/**
	 * Write FFT Bin / samples for {@code range} when Auto is on. Returns
	 * true if a radio setting changed (caller may restart). No-op while
	 * Auto is off or the radio is parked in Listen/Watch.
	 */
	public static boolean apply(HackRFSettings settings, FrequencyRange range)
	{
		if (settings == null || range == null)
			return false;
		if (settings.isAutoSweep() == null || !Boolean.TRUE.equals(settings.isAutoSweep().getValue()))
			return false;
		if (settings.isListening() != null && Boolean.TRUE.equals(settings.isListening().getValue()))
			return false;
		int currentBin = settings.getFFTBinHz().getValue();
		int currentSamples = settings.getSamples().getValue();
		Choice next = choose(range, currentBin);
		if (next.matches(currentBin, currentSamples))
			return false;
		if (currentBin != next.fftBinHz)
			settings.getFFTBinHz().setValue(next.fftBinHz);
		if (currentSamples != next.samples)
			settings.getSamples().setValue(next.samples);
		return true;
	}
}
