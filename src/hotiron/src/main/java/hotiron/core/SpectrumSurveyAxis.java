package hotiron.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-span operator survey matching the HackRF selectable window:
 * {@link FrequencyRange#MIN_MHZ} (1 MHz) at the left,
 * {@link FrequencyRange#MAX_MHZ} (7250 MHz) at the right. Logarithmic so
 * HF/FM stay usable next to 2.4 / 5 GHz. No 0 Hz gutter — the radio
 * cannot tune there.
 */
public final class SpectrumSurveyAxis
{
	public static final double MIN_MHZ = FrequencyRange.MIN_MHZ;
	public static final double MAX_MHZ = FrequencyRange.MAX_MHZ;

	public static final class Tick
	{
		public final double mhz;
		public final String label;

		Tick(double mhz, String label)
		{
			this.mhz = mhz;
			this.label = label;
		}
	}

	private SpectrumSurveyAxis()
	{
	}

	public static double mhzToFraction(double mhz)
	{
		if (!(mhz > 0) || Double.isNaN(mhz) || mhz <= MIN_MHZ)
			return 0;
		if (mhz >= MAX_MHZ)
			return 1;
		return Math.log(mhz / MIN_MHZ) / Math.log(MAX_MHZ / MIN_MHZ);
	}

	public static double fractionToMhz(double fraction)
	{
		if (!(fraction > 0) || Double.isNaN(fraction))
			return MIN_MHZ;
		if (fraction >= 1)
			return MAX_MHZ;
		return MIN_MHZ * Math.pow(MAX_MHZ / MIN_MHZ, fraction);
	}

	public static int mhzToX(double mhz, int width)
	{
		int w = Math.max(1, width);
		int x = (int) Math.round(mhzToFraction(mhz) * (w - 1));
		if (x < 0)
			return 0;
		if (x > w - 1)
			return w - 1;
		return x;
	}

	public static double bandCenterMHz(double startMHz, double endMHz)
	{
		if (endMHz < startMHz)
			return startMHz;
		if (startMHz <= MIN_MHZ && endMHz <= MIN_MHZ)
			return (startMHz + endMHz) / 2.0;
		double a = Math.max(MIN_MHZ, startMHz);
		double b = Math.max(a, endMHz);
		return Math.sqrt(a * b);
	}

	/** Decade marks plus the hardware min/max so the strip labels the radio. */
	public static List<Tick> ticks()
	{
		List<Tick> ticks = new ArrayList<>();
		ticks.add(new Tick(MIN_MHZ, label(MIN_MHZ)));
		for (double decade = 10; decade < MAX_MHZ - 0.5; decade *= 10)
			ticks.add(new Tick(decade, label(decade)));
		Tick last = ticks.get(ticks.size() - 1);
		if (Math.abs(last.mhz - MAX_MHZ) > 0.5)
			ticks.add(new Tick(MAX_MHZ, label(MAX_MHZ)));
		return List.copyOf(ticks);
	}

	static String label(double mhz)
	{
		if (mhz >= 1000)
		{
			double ghz = mhz / 1000.0;
			if (Math.abs(ghz - Math.rint(ghz)) < 1e-6)
				return ((int) Math.rint(ghz)) + " GHz";
			return String.format(Locale.US, "%.2f GHz", Double.valueOf(ghz));
		}
		if (Math.abs(mhz - 1) < 1e-6)
			return "1 MHz";
		return Integer.toString((int) Math.round(mhz));
	}
}
