package hotiron.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps waterfall rows (newest at the top) to a left-side time axis.
 * Ages are relative to the newest filled row so Pause does not drift labels.
 */
public final class WaterfallTimeScale
{
	public static final int DEFAULT_MAX_TICKS = 7;
	public static final int MIN_TICK_GAP_PX = 14;
	static final double[] STEPS_SEC = { 0.1, 0.2, 0.5, 1, 2, 5, 10, 15, 20, 30, 60, 120, 300, 600, 900, 1800,
			3600, 7200, 14400, 21600, 43200, 86400 };

	private WaterfallTimeScale()
	{
	}

	public static final class Tick
	{
		public final int y;
		public final double ageSec;
		public final String label;

		public Tick(int y, double ageSec, String label)
		{
			this.y = y;
			this.ageSec = ageSec;
			this.label = label == null ? "" : label;
		}

		@Override
		public String toString()
		{
			return y + ":" + label;
		}
	}

	/** Newest row first; a 0 entry ends the filled prefix. */
	public static int filledCount(long[] rowTimesMs)
	{
		if (rowTimesMs == null)
			return 0;
		int n = 0;
		for (int i = 0; i < rowTimesMs.length; i++)
		{
			if (rowTimesMs[i] <= 0)
				break;
			n++;
		}
		return n;
	}

	public static double spanSeconds(long[] rowTimesMs)
	{
		int n = filledCount(rowTimesMs);
		if (n < 2)
			return 0;
		return Math.max(0, (rowTimesMs[0] - rowTimesMs[n - 1]) / 1000.0);
	}

	public static double ageAtRow(long[] rowTimesMs, int row)
	{
		int n = filledCount(rowTimesMs);
		if (n <= 0 || row < 0)
			return 0;
		if (row >= n)
			return spanSeconds(rowTimesMs);
		return Math.max(0, (rowTimesMs[0] - rowTimesMs[row]) / 1000.0);
	}

	/**
	 * Screen Y for an age, mapping buffer row 0 to y=0 and the last buffer
	 * row to {@code displayHeight-1} (same stretch as the raster).
	 */
	public static int yForAge(long[] rowTimesMs, int displayHeight, double ageSec)
	{
		if (rowTimesMs == null || rowTimesMs.length == 0 || displayHeight <= 1)
			return 0;
		int filled = filledCount(rowTimesMs);
		if (filled <= 1 || rowTimesMs[0] <= 0)
			return 0;
		long target = rowTimesMs[0] - Math.round(Math.max(0, ageSec) * 1000.0);
		int row = filled - 1;
		for (int i = 0; i < filled; i++)
		{
			if (rowTimesMs[i] <= target)
			{
				row = i;
				break;
			}
		}
		return rowToY(row, rowTimesMs.length, displayHeight);
	}

	public static double ageAtY(long[] rowTimesMs, int displayHeight, int y)
	{
		if (rowTimesMs == null || rowTimesMs.length == 0 || displayHeight <= 1)
			return 0;
		int row = (int) Math.round(y * (rowTimesMs.length - 1) / (double) (displayHeight - 1));
		if (row < 0)
			row = 0;
		if (row >= rowTimesMs.length)
			row = rowTimesMs.length - 1;
		return ageAtRow(rowTimesMs, row);
	}

	public static int rowToY(int row, int bufferRows, int displayHeight)
	{
		if (bufferRows <= 1 || displayHeight <= 1)
			return 0;
		if (row <= 0)
			return 0;
		if (row >= bufferRows - 1)
			return displayHeight - 1;
		return (int) Math.round(row * (displayHeight - 1) / (double) (bufferRows - 1));
	}

	public static double niceStep(double spanSec, int maxTicks)
	{
		if (!(spanSec > 0) || maxTicks < 2)
			return spanSec > 0 ? spanSec : 1;
		double raw = spanSec / (maxTicks - 1);
		for (int i = 0; i < STEPS_SEC.length; i++)
		{
			if (STEPS_SEC[i] >= raw)
				return STEPS_SEC[i];
		}
		return Math.ceil(raw / 86400.0) * 86400.0;
	}

	public static String formatAge(double seconds)
	{
		if (!Double.isFinite(seconds) || seconds <= 0.05)
			return "now";
		if (seconds < 0.95)
			return String.format("%.1fs", Double.valueOf(seconds));
		if (seconds < 60)
			return String.format("%.0fs", Double.valueOf(seconds));
		int total = (int) Math.round(seconds);
		if (total < 3600)
		{
			int m = total / 60;
			int s = total % 60;
			if (s == 0)
				return m + "m";
			return String.format("%d:%02d", Integer.valueOf(m), Integer.valueOf(s));
		}
		int h = total / 3600;
		int rem = total % 3600;
		if (rem < 30)
			return h + "h";
		return String.format("%dh%02d", Integer.valueOf(h), Integer.valueOf(rem / 60));
	}

	public static List<Tick> ticks(long[] rowTimesMs, int displayHeight, int maxTicks)
	{
		if (rowTimesMs == null || displayHeight < 8 || maxTicks < 1)
			return List.of();
		int filled = filledCount(rowTimesMs);
		if (filled <= 0 || rowTimesMs[0] <= 0)
			return List.of();
		List<Tick> out = new ArrayList<>();
		out.add(new Tick(0, 0, "now"));
		double span = spanSeconds(rowTimesMs);
		if (span < 0.15 || filled < 4)
			return List.copyOf(out);
		int cap = Math.max(2, maxTicks);
		double step = niceStep(span, cap);
		int lastY = 0;
		for (double t = step; t <= span + step * 0.01; t += step)
		{
			int y = yForAge(rowTimesMs, displayHeight, t);
			if (y > displayHeight - 1)
				y = displayHeight - 1;
			if (y - lastY < MIN_TICK_GAP_PX)
				continue;
			out.add(new Tick(y, t, formatAge(t)));
			lastY = y;
		}
		return List.copyOf(out);
	}

	/** Evenly spaced fake history at {@code framesPerSec} for tests / fallback. */
	public static long[] rowsAtRate(int rows, double framesPerSec, long newestMs)
	{
		if (rows < 1 || !(framesPerSec > 0))
			return new long[Math.max(0, rows)];
		long[] times = new long[rows];
		double msPer = 1000.0 / framesPerSec;
		for (int i = 0; i < rows; i++)
			times[i] = newestMs - Math.round(i * msPer);
		return times;
	}

	public static List<Tick> ticksFromRate(int displayHeight, double framesPerSec, int maxTicks)
	{
		return ticks(rowsAtRate(displayHeight, framesPerSec, 1_000_000L), displayHeight, maxTicks);
	}

	public static List<Tick> empty()
	{
		return Collections.emptyList();
	}
}
