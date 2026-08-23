package hotiron.core;

/**
 * Timing and eligibility for one full-sweep callback. Paint, MCP publish,
 * Auto-gain, and axis-history are independent decisions so a new live
 * feature does not inherit the chart frame rate.
 */
public final class SweepFramePolicy
{
	public static final int CHART_FPS = 30;

	private SweepFramePolicy()
	{
	}

	public static boolean shouldPaint(long lastPaintMs, long nowMs)
	{
		return nowMs - lastPaintMs > 1000 / CHART_FPS;
	}

	public static boolean axisChanged(DatasetSpectrum previous, DatasetSpectrum next)
	{
		return previous == null || !previous.sameAxisAs(next);
	}

	public static boolean maySeedAutoGain(HackRFSettings settings, boolean scanActive)
	{
		if (settings == null || scanActive)
			return false;
		if (settings.isAutoGain() == null || !Boolean.TRUE.equals(settings.isAutoGain().getValue()))
			return false;
		if (settings.isListening() != null && Boolean.TRUE.equals(settings.isListening().getValue()))
			return false;
		return true;
	}

	public static boolean mayConsiderAutoGain(HackRFSettings settings, boolean scanActive)
	{
		if (!maySeedAutoGain(settings, scanActive))
			return false;
		if (settings.isCapturingPaused() != null && Boolean.TRUE.equals(settings.isCapturingPaused().getValue()))
			return false;
		if (settings.isRadioReleased() != null && Boolean.TRUE.equals(settings.isRadioReleased().getValue()))
			return false;
		return true;
	}
}
